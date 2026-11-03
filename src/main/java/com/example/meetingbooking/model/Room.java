package com.example.meetingbooking.model;

import jakarta.persistence.*;

@Entity
@Table(name = "rooms")
public class Room {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;
    @Column(nullable = false, unique = true)
    public String name;
    public int capacity;
    public boolean active;

    public Room() {}
    public Room(String name, int capacity, boolean active) {
        this.name = name; this.capacity = capacity; this.active = active;
    }
}
