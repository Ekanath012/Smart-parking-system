package com.example.parking.dto;

import java.util.List;

/** Recent notifications plus the total unread count for the signed-in admin. */
public record NotificationListResponse(List<NotificationDto> notifications, long unreadCount) {
}
