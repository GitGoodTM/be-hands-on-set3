package com.example.meetingbooking;

import com.example.meetingbooking.api.*;
import com.example.meetingbooking.model.*;
import com.example.meetingbooking.repo.*;
import com.example.meetingbooking.service.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class BookingIntegrationTest {
    @Autowired BookingService service;
    @Autowired FreeSlotService freeSlots;
    @Autowired BookingRepository bookings;
    @Autowired RoomRepository rooms;

    private LocalDate date() { return LocalDate.now().plusDays(3); }
    private BookingRequest req(Long room, String title, int sh, int sm, int eh, int em, String key) {
        return new BookingRequest(room, title, "organiser@example.com", List.of("a@example.com"),
            date().atTime(sh, sm), date().atTime(eh, em), key);
    }
    private Long room() { return rooms.findAll().get(0).id; }

    @Test void overlapBackToBackAndRetry() {
        Long id = room();
        Booking a = service.create(req(id, "First", 10, 0, 11, 0, UUID.randomUUID().toString()));
        ApiException conflict = assertThrows(ApiException.class, () -> service.create(req(id, "Overlap", 10, 30, 11, 30, UUID.randomUUID().toString())));
        assertEquals(HttpStatus.CONFLICT, conflict.status);
        Booking next = service.create(req(id, "Next", 11, 0, 12, 0, UUID.randomUUID().toString()));
        assertNotEquals(a.id, next.id);
        assertEquals(a.id, service.create(req(id, "First", 10, 0, 11, 0, a.idempotencyKey)).id);
        service.cancel(a.id);
        assertTrue(service.get(a.id).cancelled);
    }

    @Test void concurrencyTwentyRequestsOnlyOneSucceeds() throws Exception {
        Long id = room();
        LocalDate d = LocalDate.now().plusDays(7);
        ExecutorService executor = Executors.newFixedThreadPool(20);
        CountDownLatch ready = new CountDownLatch(20), go = new CountDownLatch(1);
        List<Future<HttpStatus>> results = new ArrayList<>();
        try {
            for (int i = 0; i < 20; i++) {
                final int n = i;
                results.add(executor.submit(() -> {
                    ready.countDown(); go.await();
                    try {
                        service.create(new BookingRequest(id, "Simultaneous-" + n, "organiser@example.com", List.of("a@example.com"),
                            d.atTime(15, 0), d.atTime(16, 0), "race-" + UUID.randomUUID()));
                        return HttpStatus.CREATED;
                    } catch (ApiException e) { return e.status; }
                }));
            }
            assertTrue(ready.await(10, TimeUnit.SECONDS)); go.countDown();
            long created = 0, conflicts = 0;
            for (Future<HttpStatus> result : results) {
                HttpStatus status = result.get(30, TimeUnit.SECONDS);
                if (status == HttpStatus.CREATED) created++;
                if (status == HttpStatus.CONFLICT) conflicts++;
            }
            assertEquals(1, created); assertEquals(19, conflicts);
            assertEquals(1, bookings.activeOverlaps(id, d.atTime(15,0), d.atTime(16,0)).size());
        } finally { go.countDown(); executor.shutdownNow(); }
    }

    @Test void freeSlotsMergeBusyIntervalsAndIgnoreCancelled() {
        LocalDate d = LocalDate.now().plusDays(11);
        List<Booking> created = new ArrayList<>();
        Long r1 = rooms.findAll().get(0).id;
        Long r2 = rooms.findAll().get(1).id;
        created.add(service.create(new BookingRequest(r1,"A1","a@example.com", List.of(),d.atTime(10,0),d.atTime(11,0),UUID.randomUUID().toString())));
        created.add(service.create(new BookingRequest(r2,"A2","a@example.com", List.of(),d.atTime(9,30),d.atTime(10,30),UUID.randomUUID().toString())));
        created.add(service.create(new BookingRequest(r1,"B1","b@example.com", List.of(),d.atTime(13,0),d.atTime(14,0),UUID.randomUUID().toString())));
        created.add(service.create(new BookingRequest(r2,"B2","b@example.com", List.of(),d.atTime(13,15),d.atTime(13,45),UUID.randomUUID().toString())));
        List<FreeSlotService.Slot> slots = freeSlots.find(d, 30, List.of("a@example.com", "b@example.com"));
        assertEquals(3, slots.size());
        assertEquals(d.atTime(9,0), slots.get(0).start()); assertEquals(d.atTime(9,30), slots.get(0).end());
        assertEquals(d.atTime(11,0), slots.get(1).start()); assertEquals(d.atTime(13,0), slots.get(1).end());
        assertEquals(d.atTime(14,0), slots.get(2).start()); assertEquals(d.atTime(18,0), slots.get(2).end());
        for (Booking b : created) service.cancel(b.id);
    }
}
