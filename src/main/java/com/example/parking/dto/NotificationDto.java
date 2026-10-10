package com.example.parking.dto;

import java.util.Map;

/**
 * JSON view of a notification for the Admin Dashboard.
 *
 * @param details ordered map: section heading -> (label -> value), pre-formatted for display
 */
public record NotificationDto(Long id,
                              String type,
                              String typeLabel,
                              String title,
                              String message,
                              Long bookingId,
                              String bookingReference,
                              String createdAt,
                              boolean read,
                              String readAt,
                              Map<String, Map<String, String>> details) {
}
