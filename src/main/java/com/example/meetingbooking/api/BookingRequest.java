package com.example.meetingbooking.api;

import jakarta.validation.constraints.*;
import java.time.LocalDateTime;
import java.util.List;

public record BookingRequest(
    @NotNull Long roomId,
    @NotBlank String title,
    @NotBlank @Email String organiserEmail,
    @NotNull List<@NotBlank @Email String> attendees,
    @NotNull LocalDateTime start,
    @NotNull LocalDateTime end,
    String idempotencyKey
) {}
