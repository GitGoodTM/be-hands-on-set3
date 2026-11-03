package com.example.meetingbooking.service;

import com.example.meetingbooking.api.ApiException;
import com.example.meetingbooking.model.*;
import com.example.meetingbooking.repo.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.*;
import java.util.*;

@Service
public class FreeSlotService {
    private final BookingRepository bookings;
    private final RoomRepository rooms;
    public FreeSlotService(BookingRepository bookings, RoomRepository rooms) {
        this.bookings = bookings; this.rooms = rooms;
    }
    public record RoomOption(Long id, String name, int capacity) {}
    public record Slot(LocalDateTime start, LocalDateTime end, List<RoomOption> rooms) {}
    private record Interval(LocalDateTime start, LocalDateTime end) {}

    @Transactional(readOnly = true)
    public List<Slot> find(LocalDate date, int durationMinutes, List<String> attendeeEmails) {
        if (date == null || durationMinutes <= 0 || durationMinutes > 540 || durationMinutes % 15 != 0 || attendeeEmails == null || attendeeEmails.isEmpty())
            throw new ApiException(HttpStatus.BAD_REQUEST, "Date, attendee emails and duration in 15-minute increments (15–540) required");
        Set<String> attendees = BookingService.normalize(attendeeEmails);
        if (attendees.isEmpty() || attendees.contains("")) throw new ApiException(HttpStatus.BAD_REQUEST, "Attendee emails required");
        LocalDateTime startOfDay = date.atTime(9, 0), endOfDay = date.atTime(18, 0);
        List<Interval> busy = new ArrayList<>();
        for (Booking b : bookings.busyForAttendees(attendees, startOfDay, endOfDay)) {
            busy.add(new Interval(max(startOfDay, b.start), min(endOfDay, b.end)));
        }
        busy.sort(Comparator.comparing(Interval::start).thenComparing(Interval::end));
        List<Interval> free = new ArrayList<>();
        LocalDateTime cursor = startOfDay;
        for (Interval interval : busy) {
            if (cursor.isBefore(interval.start)) free.add(new Interval(cursor, interval.start));
            if (cursor.isBefore(interval.end)) cursor = interval.end;
        }
        if (cursor.isBefore(endOfDay)) free.add(new Interval(cursor, endOfDay));
        List<Room> availableRooms = rooms.findByActiveTrueOrderByNameAsc();
        List<Slot> result = new ArrayList<>();
        for (Interval interval : free) {
            if (Duration.between(interval.start, interval.end).toMinutes() < durationMinutes) continue;
            List<RoomOption> options = new ArrayList<>();
            for (Room room : availableRooms) {
                if (room.capacity >= attendees.size() && bookings.activeOverlaps(room.id, interval.start, interval.end).isEmpty()) {
                    options.add(new RoomOption(room.id, room.name, room.capacity));
                }
            }
            result.add(new Slot(interval.start, interval.end, options));
        }
        return result;
    }
    private static LocalDateTime max(LocalDateTime a, LocalDateTime b) { return a.isAfter(b) ? a : b; }
    private static LocalDateTime min(LocalDateTime a, LocalDateTime b) { return a.isBefore(b) ? a : b; }
}
