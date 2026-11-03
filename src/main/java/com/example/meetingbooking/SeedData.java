package com.example.meetingbooking;

import com.example.meetingbooking.model.Room;
import com.example.meetingbooking.repo.RoomRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.*;
import java.util.*;

@Configuration
public class SeedData {
    @Bean CommandLineRunner seedRooms(RoomRepository rooms) {
        return args -> {
            if (rooms.count() == 0) rooms.saveAll(List.of(
                new Room("Atlas", 4, true), new Room("Borealis", 6, true),
                new Room("Cedar", 8, true), new Room("Delta", 12, true),
                new Room("Everest", 20, true)));
        };
    }
}
