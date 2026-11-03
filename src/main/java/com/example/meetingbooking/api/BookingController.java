package com.example.meetingbooking.api;

import com.example.meetingbooking.model.*;
import com.example.meetingbooking.repo.RoomRepository;
import com.example.meetingbooking.service.*;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import java.time.*;
import java.util.*;

@RestController
@RequestMapping("/api")
public class BookingController {
    private final BookingService bookingService;
    private final FreeSlotService freeSlotService;
    private final RoomRepository rooms;
    public BookingController(BookingService bookingService, FreeSlotService freeSlotService, RoomRepository rooms) {
        this.bookingService = bookingService; this.freeSlotService = freeSlotService; this.rooms = rooms;
    }
    @GetMapping("/rooms")
    public List<Room> rooms() { return rooms.findAll(); }
    @PostMapping("/bookings")
    public ResponseEntity<Booking> create(@Valid @RequestBody BookingRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(bookingService.create(request));
    }
    @GetMapping("/bookings/{id}")
    public Booking get(@PathVariable Long id) { return bookingService.get(id); }
    @DeleteMapping("/bookings/{id}")
    public ResponseEntity<Void> cancel(@PathVariable Long id) {
        bookingService.cancel(id); return ResponseEntity.noContent().build();
    }
    @GetMapping("/rooms/{id}/bookings")
    public List<Booking> roomDay(@PathVariable Long id, @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return bookingService.roomDay(id, date);
    }
    @GetMapping("/free-slots")
    public List<FreeSlotService.Slot> freeSlots(
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
        @RequestParam int durationMinutes,
        @RequestParam List<String> attendees) {
        return freeSlotService.find(date, durationMinutes, attendees);
    }
}
