package com.example.parking.service;

import com.example.parking.dto.NotificationDto;
import com.example.parking.dto.NotificationListResponse;
import com.example.parking.exception.ResourceNotFoundException;
import com.example.parking.model.*;
import com.example.parking.repository.BillRepository;
import com.example.parking.repository.BookingRepository;
import com.example.parking.repository.NotificationRepository;
import com.example.parking.repository.UserRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Creates and serves database-backed admin notifications.
 * <p>
 * Write methods use REQUIRES_NEW: they run right after the booking / checkout transaction has committed,
 * in their own transaction, so a notification problem can never roll back (or be rolled back with) the booking.
 */
@Service
public class AdminNotificationService {

    private static final Logger logger = LoggerFactory.getLogger(AdminNotificationService.class);
    private static final int MAX_LIST_SIZE = 50;
    private static final int MAX_MESSAGE_LENGTH = 500;
    private static final DateTimeFormatter DISPLAY = DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a", Locale.ENGLISH);
    private static final TypeReference<Map<String, Map<String, String>>> DETAILS_TYPE = new TypeReference<>() { };

    private final NotificationRepository notificationRepository;
    private final BookingRepository bookingRepository;
    private final BillRepository billRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    public AdminNotificationService(NotificationRepository notificationRepository,
                                    BookingRepository bookingRepository,
                                    BillRepository billRepository,
                                    UserRepository userRepository,
                                    ObjectMapper objectMapper) {
        this.notificationRepository = notificationRepository;
        this.bookingRepository = bookingRepository;
        this.billRepository = billRepository;
        this.userRepository = userRepository;
        this.objectMapper = objectMapper;
    }

    // ================================================================== content snapshots

    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    public NotificationContent buildBookingCreatedContent(Long bookingId) {
        Booking booking = loadBooking(bookingId);
        return NotificationContentFactory.bookingCreated(booking);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    public NotificationContent buildBookingCancelledContent(Long bookingId) {
        Booking booking = loadBooking(bookingId);
        return NotificationContentFactory.bookingCancelled(booking);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    public NotificationContent buildParkingCompletedContent(Long bookingId) {
        Booking booking = loadBooking(bookingId);
        Bill bill = billRepository.findByBooking(booking)
                .orElseThrow(() -> new ResourceNotFoundException("Bill", "bookingId", bookingId));
        return NotificationContentFactory.parkingCompleted(booking, bill);
    }

    // ================================================================== create

    /**
     * Stores one notification per enabled administrator.
     *
     * @param bookingId related booking, or null for system notices
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void saveForAdmins(NotificationType type, NotificationContent content, Long bookingId) {
        List<User> admins = userRepository.findEnabledByRoleName(RoleName.ROLE_ADMIN);
        if (admins.isEmpty()) {
            logger.warn("No enabled administrator found; notification '{}' was not stored.", content.title());
            return;
        }

        String detailsJson = toJson(content.sections());
        Booking bookingRef = bookingId == null ? null : bookingRepository.getReferenceById(bookingId);

        for (User admin : admins) {
            Notification n = new Notification();
            n.setType(type);
            n.setTitle(truncate(content.title(), 150));
            n.setMessage(truncate(content.message(), MAX_MESSAGE_LENGTH));
            n.setBooking(bookingRef);
            n.setBookingReference(content.bookingReference());
            n.setDetails(detailsJson);
            n.setRecipient(admin);
            notificationRepository.save(n);
        }
        logger.info("Stored '{}' notification for {} admin(s), booking {}.",
                content.title(), admins.size(), content.bookingReference());
    }

    /** Records a SYSTEM_ERROR notice (e.g. the admin email could not be sent). */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordSystemNotice(String title, String message, String bookingReference) {
        saveForAdmins(NotificationType.SYSTEM_ERROR,
                NotificationContentFactory.systemNotice(title, message, bookingReference), null);
    }

    // ================================================================== read side (per admin)

    @Transactional(readOnly = true)
    public NotificationListResponse list(Long adminId, int limit) {
        int size = Math.min(Math.max(limit, 1), MAX_LIST_SIZE);
        List<NotificationDto> items = notificationRepository.findRecent(adminId, PageRequest.of(0, size))
                .stream().map(this::toDto).toList();
        return new NotificationListResponse(items, notificationRepository.countByRecipientIdAndReadStatusFalse(adminId));
    }

    @Transactional(readOnly = true)
    public NotificationDto get(Long adminId, Long notificationId) {
        return toDto(findOwned(adminId, notificationId));
    }

    @Transactional
    public long markRead(Long adminId, Long notificationId) {
        Notification n = findOwned(adminId, notificationId);
        if (!n.isReadStatus()) {
            n.setReadStatus(true);
            n.setReadAt(LocalDateTime.now());
            notificationRepository.save(n);
        }
        return notificationRepository.countByRecipientIdAndReadStatusFalse(adminId);
    }

    @Transactional
    public long markAllRead(Long adminId) {
        notificationRepository.markAllRead(adminId, LocalDateTime.now());
        return notificationRepository.countByRecipientIdAndReadStatusFalse(adminId);
    }

    // ================================================================== internals

    private Booking loadBooking(Long bookingId) {
        return bookingRepository.findByIdWithDetails(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking", "id", bookingId));
    }

    private Notification findOwned(Long adminId, Long notificationId) {
        return notificationRepository.findByIdAndRecipientId(notificationId, adminId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification", "id", notificationId));
    }

    private NotificationDto toDto(Notification n) {
        return new NotificationDto(
                n.getId(),
                n.getType().name(),
                n.getType().getLabel(),
                n.getTitle(),
                n.getMessage(),
                n.getBooking() == null ? null : n.getBooking().getId(),
                n.getBookingReference(),
                n.getCreatedAt().format(DISPLAY),
                n.isReadStatus(),
                n.getReadAt() == null ? null : n.getReadAt().format(DISPLAY),
                fromJson(n.getDetails()));
    }

    private String toJson(Map<String, Map<String, String>> sections) {
        try {
            return objectMapper.writeValueAsString(sections);
        } catch (JsonProcessingException e) {
            logger.warn("Could not serialise notification details: {}", e.getOriginalMessage());
            return "{}";
        }
    }

    private Map<String, Map<String, String>> fromJson(String json) {
        if (json == null || json.isBlank()) {
            return new LinkedHashMap<>();
        }
        try {
            return objectMapper.readValue(json, DETAILS_TYPE);
        } catch (JsonProcessingException e) {
            logger.warn("Could not parse stored notification details: {}", e.getOriginalMessage());
            return new LinkedHashMap<>();
        }
    }

    private static String truncate(String value, int max) {
        if (value == null) {
            return "";
        }
        return value.length() <= max ? value : value.substring(0, max - 3) + "...";
    }
}
