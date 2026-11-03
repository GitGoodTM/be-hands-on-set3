package com.example.meetingbooking.model;

import jakarta.persistence.*;
import java.time.*;
import java.util.*;

@Entity
@Table(name = "bookings", indexes = {
    @Index(name = "idx_bookings_room_start", columnList = "room_id,start_time"),
    @Index(name = "idx_bookings_start", columnList = "start_time")
})
public class Booking {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;
    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    @JoinColumn(name = "room_id", nullable = false)
    public Room room;
    @Column(nullable = false)
    public String title;
    @Column(name = "organiser_email", nullable = false)
    public String organiserEmail;
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "booking_attendees", joinColumns = @JoinColumn(name = "booking_id"))
    @Column(name = "email", nullable = false)
    public Set<String> attendees = new LinkedHashSet<>();
    @Column(name = "start_time", nullable = false)
    public LocalDateTime start;
    @Column(name = "end_time", nullable = false)
    public LocalDateTime end;
    @Column(nullable = false)
    public Instant createdAt;
    @Column(nullable = false)
    public boolean cancelled = false;
    @Column(unique = true)
    public String idempotencyKey;

    public Booking() {}
}
