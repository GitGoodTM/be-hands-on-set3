package com.example.meetingbooking.api;

import java.util.Map;

public record ApiError(int status, String error, Map<String, String> details) {}
