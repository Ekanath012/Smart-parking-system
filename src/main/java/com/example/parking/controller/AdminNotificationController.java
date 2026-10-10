package com.example.parking.controller;

import com.example.parking.dto.NotificationDto;
import com.example.parking.dto.NotificationListResponse;
import com.example.parking.exception.ResourceNotFoundException;
import com.example.parking.service.AdminNotificationService;
import com.example.parking.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * JSON API behind the Admin Dashboard notification bell.
 * Protected twice: URL rule in SecurityConfig ({@code /admin/api/** -> ROLE_ADMIN}) and
 * {@code @PreAuthorize} here. Every operation is scoped to the signed-in admin's own notifications.
 */
@RestController
@RequestMapping("/admin/api/notifications")
@PreAuthorize("hasRole('ADMIN')")
public class AdminNotificationController {

    private final AdminNotificationService notificationService;
    private final UserService userService;

    public AdminNotificationController(AdminNotificationService notificationService, UserService userService) {
        this.notificationService = notificationService;
        this.userService = userService;
    }

    /** Recent notifications (newest first) and the unread count. */
    @GetMapping
    public NotificationListResponse list(@RequestParam(value = "limit", defaultValue = "20") int limit) {
        return notificationService.list(currentAdminId(), limit);
    }

    /** One notification with its full booking details. */
    @GetMapping("/{id}")
    public NotificationDto get(@PathVariable("id") Long id) {
        return notificationService.get(currentAdminId(), id);
    }

    @PostMapping("/{id}/read")
    public Map<String, Long> markRead(@PathVariable("id") Long id) {
        return Map.of("unreadCount", notificationService.markRead(currentAdminId(), id));
    }

    @PostMapping("/read-all")
    public Map<String, Long> markAllRead() {
        return Map.of("unreadCount", notificationService.markAllRead(currentAdminId()));
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleNotFound(ResourceNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Notification not found"));
    }

    private Long currentAdminId() {
        return userService.getAuthenticatedUser().getId();
    }
}
