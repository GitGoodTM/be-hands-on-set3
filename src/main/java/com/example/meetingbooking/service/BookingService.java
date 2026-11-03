package com.example.meetingbooking.service;

import com.example.meetingbooking.api.*;
import com.example.meetingbooking.model.*;
import com.example.meetingbooking.repo.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.*;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class BookingService {
    private final RoomRepository rooms;
    private final BookingRepository bookings;
    public BookingService(RoomRepository rooms, BookingRepository bookings) {
        this.rooms = rooms; this.bookings = bookings;
    }

    @Transactional
    public Booking create(BookingRequest request) {
        // Lock the database ROOM ROW before conflict checking: works across app instances on one shared DB.
        Room room = rooms.findByIdForUpdate(request.roomId())
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Room not found"));
        String key = request.idempotencyKey();
        if (key != null && !key.isBlank()) {
            Optional<Booking> previous = bookings.findByIdempotencyKey(key);
            if (previous.isPresent()) {
                Booking old = previous.get();
                if (!matches(old, request)) throw new ApiException(HttpStatus.CONFLICT, "Idempotency key used with different booking details");
                return old;
            }
        }
        Map<String, String> errors = new LinkedHashMap<>();
        LocalDateTime start = request.start(), end = request.end();
        if (start == null || end == null) errors.put("start/end", "Both times are required");
        else {
            if (!start.isBefore(end)) errors.put("end", "End must be after start");
            if (!start.toLocalDate().equals(end.toLocalDate())) errors.put("end", "Booking must be on one day");
            if (start.toLocalTime().isBefore(LocalTime.of(9,0)) || end.toLocalTime().isAfter(LocalTime.of(18,0)))
                errors.put("hours", "Use working hours 09:00–18:00");
            if (start.getMinute() % 15 != 0 || end.getMinute() % 15 != 0 || start.getSecond() != 0 || end.getSecond() != 0 || start.getNano() != 0 || end.getNano() != 0)
                errors.put("steps", "Start/end must align to 15-minute boundaries");
            if (Duration.between(start, end).compareTo(Duration.ofHours(4)) > 0) errors.put("duration", "Maximum duration is 4 hours");
            if (start.isBefore(LocalDateTime.now())) errors.put("start", "Cannot book in the past");
        }
        Set<String> attendees = normalize(request.attendees());
        String organiser = request.organiserEmail() == null ? "" : request.organiserEmail().trim().toLowerCase(Locale.ROOT);
        attendees.add(organiser);
        if (!room.active) errors.put("roomId", "Room is inactive");
        if (attendees.size() > room.capacity) errors.put("attendees", "Room capacity exceeded");
        if (!errors.isEmpty()) throw new ApiException(HttpStatus.BAD_REQUEST, errors);
        List<Booking> collisions = bookings.activeOverlaps(room.id, start, end);
        if (!collisions.isEmpty()) throw new ApiException(HttpStatus.CONFLICT, "Clashes with booking #" + collisions.get(0).id + ": " + collisions.get(0).title);
        Booking b = new Booking();
        b.room = room; b.title = request.title(); b.organiserEmail = organiser;
        b.attendees = attendees; b.start = start; b.end = end;
        b.createdAt = Instant.now(); b.idempotencyKey = key == null || key.isBlank() ? null : key;
        return bookings.saveAndFlush(b);
    }

    private boolean matches(Booking b, BookingRequest r) {
        Set<String> attendees = normalize(r.attendees());
        String organiser = r.organiserEmail() == null ? "" : r.organiserEmail().trim().toLowerCase(Locale.ROOT);
        attendees.add(organiser);
        return Objects.equals(b.room.id, r.roomId()) && Objects.equals(b.title, r.title())
            && Objects.equals(b.organiserEmail, organiser) && b.attendees.equals(attendees)
            && Objects.equals(b.start, r.start()) && Objects.equals(b.end, r.end());
    }

    public static Set<String> normalize(Collection<String> emails) {
        return emails == null ? new LinkedHashSet<>() : emails.stream()
            .filter(Objects::nonNull).map(x -> x.trim().toLowerCase(Locale.ROOT))
            .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    @Transactional(readOnly = true)
    public Booking get(Long id) {
        return bookings.findById(id).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Booking not found"));
    }

    @Transactional(readOnly = true)
    public List<Booking> roomDay(Long id, LocalDate date) {
        if (!rooms.existsById(id)) throw new ApiException(HttpStatus.NOT_FOUND, "Room not found");
        return bookings.forRoomOnDate(id, date.atStartOfDay(), date.plusDays(1).atStartOfDay());
    }

    @Transactional
    public void cancel(Long id) {
        Booking b = get(id);
        // Serialized on room so cancellation does not race with new booking checks.
        rooms.findByIdForUpdate(b.room.id).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Room not found"));
        b.cancelled = true;
        bookings.save(b);
    }
}
