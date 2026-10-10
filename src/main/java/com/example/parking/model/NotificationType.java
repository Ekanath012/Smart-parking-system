package com.example.parking.model;

/**
 * Category of an administrator notification. Drives the icon/colour used on the Admin Dashboard.
 */
public enum NotificationType {
    NEW_BOOKING("New Parking Booking"),
    PARKING_ACTIVE("Parking Active"),
    PARKING_COMPLETED("Parking Completed"),
    PAYMENT_COMPLETED("Payment Completed"),
    BOOKING_CANCELLED("Booking Cancelled"),
    SYSTEM_ERROR("System Notice");

    private final String label;

    NotificationType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
