package com.example.meetingbooking.repo;

import com.example.meetingbooking.model.Booking;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.time.*;
import java.util.*;

public interface BookingRepository extends JpaRepository<Booking, Long> {
    Optional<Booking> findByIdempotencyKey(String key);
    @Query("select b from Booking b where b.room.id = :roomId and b.cancelled = false and b.start < :end and b.end > :start order by b.start")
    List<Booking> activeOverlaps(@Param("roomId") Long roomId, @Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
    @Query("select b from Booking b where b.room.id = :roomId and b.start >= :start and b.start < :end order by b.start")
    List<Booking> forRoomOnDate(@Param("roomId") Long roomId, @Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
    @Query("select distinct b from Booking b join b.attendees a where b.cancelled = false and a in :emails and b.start < :end and b.end > :start")
    List<Booking> busyForAttendees(@Param("emails") Collection<String> emails, @Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
}
