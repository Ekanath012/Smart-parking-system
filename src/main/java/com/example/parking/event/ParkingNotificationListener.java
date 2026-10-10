package com.example.parking.event;

import com.example.parking.model.NotificationType;
import com.example.parking.service.AdminEmailService;
import com.example.parking.service.AdminNotificationService;
import com.example.parking.service.NotificationContent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Reacts to parking events ONLY AFTER the booking / checkout transaction has committed.
 * That guarantees the booking is already safely stored before any notification work starts, and that
 * nothing here can roll the booking back. Each step (dashboard notification, email) is independent:
 * a failure in one is logged and does not stop the other.
 */
@Component
public class ParkingNotificationListener {

    private static final Logger logger = LoggerFactory.getLogger(ParkingNotificationListener.class);

    private static final String BOOKING_COLOR = "#2563eb";
    private static final String COMPLETED_COLOR = "#16a34a";
    private static final String CANCELLED_COLOR = "#f59e0b";

    private final AdminNotificationService notificationService;
    private final AdminEmailService emailService;
    private final com.example.parking.service.CustomerEmailService customerEmailService;

    public ParkingNotificationListener(AdminNotificationService notificationService,
                                       AdminEmailService emailService,
                                       com.example.parking.service.CustomerEmailService customerEmailService) {
        this.notificationService = notificationService;
        this.emailService = emailService;
        this.customerEmailService = customerEmailService;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onBookingCreated(BookingCreatedEvent event) {
        NotificationContent content;
        try {
            content = notificationService.buildBookingCreatedContent(event.bookingId());
        } catch (Exception ex) {
            logger.error("Could not build admin notification for new booking id {}: {}", event.bookingId(), ex.toString());
            return;
        }
        dispatch(NotificationType.NEW_BOOKING, content, event.bookingId(), BOOKING_COLOR);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onParkingCompleted(ParkingCompletedEvent event) {
        NotificationContent content;
        try {
            content = notificationService.buildParkingCompletedContent(event.bookingId());
        } catch (Exception ex) {
            logger.error("Could not build admin notification for completed booking id {}: {}", event.bookingId(), ex.toString());
            return;
        }
        dispatch(NotificationType.PARKING_COMPLETED, content, event.bookingId(), COMPLETED_COLOR);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onBookingCancelled(BookingCancelledEvent event) {
        NotificationContent content;
        try {
            content = notificationService.buildBookingCancelledContent(event.bookingId());
        } catch (Exception ex) {
            logger.error("Could not build admin notification for cancelled booking id {}: {}", event.bookingId(), ex.toString());
            return;
        }
        dispatch(NotificationType.BOOKING_CANCELLED, content, event.bookingId(), CANCELLED_COLOR);
    }

    private void dispatch(NotificationType type, NotificationContent content, Long bookingId, String color) {
        // 1) Dashboard notification (database-backed)
        try {
            notificationService.saveForAdmins(type, content, bookingId);
        } catch (Exception ex) {
            logger.error("Could not store dashboard notification for booking id {}: {}", bookingId, ex.toString());
        }
        // 2) Email (async; handles and records its own failures)
        try {
            emailService.sendAsync(content, color);
            customerEmailService.sendParkingEventAsync(content, color);
        } catch (Exception ex) {
            logger.error("Could not queue admin email for booking id {}: {}", bookingId, ex.toString());
        }
    }
}
