package com.example.parking.service;

import com.example.parking.model.Bill;
import com.example.parking.model.Booking;
import com.example.parking.model.BookingStatus;
import com.example.parking.model.ParkingSlot;
import com.example.parking.model.SlotType;
import com.example.parking.model.User;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Builds {@link NotificationContent} from persisted entities.
 * Every value is taken from the database entities (timestamps, rate, bill) - nothing is hardcoded.
 * Must be called while the Booking's user / parkingSlot associations are initialised.
 */
public final class NotificationContentFactory {

    private static final String NA = "N/A";
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.ENGLISH);
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("hh:mm a", Locale.ENGLISH);
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a", Locale.ENGLISH);

    private NotificationContentFactory() {
    }

    /** Content for "New Parking Booking". */
    public static NotificationContent bookingCreated(Booking b) {
        ParkingSlot slot = b.getParkingSlot();
        User user = b.getUser();

        Map<String, Map<String, String>> sections = new LinkedHashMap<>();
        sections.put("Customer Details", fields(
                "Name", nz(user == null ? null : user.getName()),
                "Email", nz(user == null ? null : user.getEmail()),
                "Phone", nz(user == null ? null : user.getPhone())));
        sections.put("Vehicle Details", fields(
                "Vehicle Number", nz(b.getVehicleNumber()),
                "Vehicle Type", vehicleType(slot)));
        sections.put("Parking Details", fields(
                "Slot Number", slot == null ? NA : nz(slot.getSlotNumber()),
                "Floor", slot == null ? NA : nz(slot.getFloorLevel()),
                "Entry Date", date(b.getEntryTime()),
                "Entry Time", time(b.getEntryTime()),
                "Selected Duration", "Open-ended (billed hourly at exit)",
                "Parking Rate", rate(slot == null ? null : slot.getHourlyRate())));
        sections.put("Booking Details", fields(
                "Booking Reference", nz(b.getBookingReference()),
                "Booking Status", statusLabel(b.getStatus()),
                "Booking Created", dateTime(b.getCreatedAt())));

        String message = String.format("%s booked slot %s (%s) for vehicle %s",
                nz(user == null ? null : user.getName()),
                slot == null ? NA : nz(slot.getSlotNumber()),
                slot == null ? NA : nz(slot.getFloorLevel()),
                nz(b.getVehicleNumber()));

        return new NotificationContent("New Parking Booking", message, b.getBookingReference(),
                user == null ? null : user.getName(), user == null ? null : user.getEmail(), sections);
    }

    /** Content for "Parking Completed". Duration is computed from the real entry/exit timestamps. */
    public static NotificationContent parkingCompleted(Booking b, Bill bill) {
        ParkingSlot slot = b.getParkingSlot();
        User user = b.getUser();

        String duration = formatDuration(b.getEntryTime(), b.getExitTime());
        Double appliedRate = bill.getHourlyRate() != null ? bill.getHourlyRate()
                : (slot == null ? null : slot.getHourlyRate());

        Map<String, Map<String, String>> sections = new LinkedHashMap<>();
        sections.put("Customer Details", fields(
                "Name", nz(user == null ? null : user.getName()),
                "Email", nz(user == null ? null : user.getEmail()),
                "Phone", nz(user == null ? null : user.getPhone())));
        sections.put("Vehicle Details", fields(
                "Vehicle Number", nz(b.getVehicleNumber()),
                "Vehicle Type", vehicleType(slot)));
        sections.put("Parking Details", fields(
                "Slot Number", slot == null ? NA : nz(slot.getSlotNumber()),
                "Floor", slot == null ? NA : nz(slot.getFloorLevel()),
                "Entry Date", date(b.getEntryTime()),
                "Entry Time", time(b.getEntryTime()),
                "Exit Date", date(b.getExitTime()),
                "Exit Time", time(b.getExitTime()),
                "Total Duration", duration));
        sections.put("Billing Details", fields(
                "Parking Rate", rate(appliedRate),
                "Billed Hours", bill.getTotalHours() == null ? NA : bill.getTotalHours() + " (rounded up to the next hour)",
                "Final Amount", money(bill.getTotalAmount()),
                "Payment Status", bill.getPaymentStatus() == null ? NA : titleCase(bill.getPaymentStatus().name())));
        sections.put("Booking Details", fields(
                "Booking Reference", nz(b.getBookingReference()),
                "Booking Status", statusLabel(b.getStatus()),
                "Booking Created", dateTime(b.getCreatedAt())));

        String message = String.format("%s | %s | Slot %s | Duration %s | Amount %s | %s",
                nz(user == null ? null : user.getName()),
                nz(b.getVehicleNumber()),
                slot == null ? NA : nz(slot.getSlotNumber()),
                duration,
                money(bill.getTotalAmount()),
                bill.getPaymentStatus() == null ? NA : titleCase(bill.getPaymentStatus().name()));

        return new NotificationContent("Parking Completed", message, b.getBookingReference(),
                user == null ? null : user.getName(), user == null ? null : user.getEmail(), sections);
    }

    /** Content for a cancelled booking. */
    public static NotificationContent bookingCancelled(Booking b) {
        ParkingSlot slot = b.getParkingSlot();
        User user = b.getUser();
        Map<String, Map<String, String>> sections = new LinkedHashMap<>();
        sections.put("Customer Details", fields(
                "Name", nz(user == null ? null : user.getName()),
                "Email", nz(user == null ? null : user.getEmail()),
                "Phone", nz(user == null ? null : user.getPhone())));
        sections.put("Booking Details", fields(
                "Booking Reference", nz(b.getBookingReference()),
                "Vehicle Number", nz(b.getVehicleNumber()),
                "Slot Number", slot == null ? NA : nz(slot.getSlotNumber()),
                "Floor", slot == null ? NA : nz(slot.getFloorLevel()),
                "Cancelled At", dateTime(b.getExitTime()),
                "Booking Status", statusLabel(b.getStatus())));
        String message = String.format("%s cancelled booking %s for slot %s",
                nz(user == null ? null : user.getName()), nz(b.getBookingReference()),
                slot == null ? NA : nz(slot.getSlotNumber()));
        return new NotificationContent("Booking Cancelled", message, b.getBookingReference(),
                user == null ? null : user.getName(), user == null ? null : user.getEmail(), sections);
    }

    /** Content for a system / error notice (e.g. the admin email could not be delivered). */
    public static NotificationContent systemNotice(String title, String message, String bookingReference) {
        Map<String, Map<String, String>> sections = new LinkedHashMap<>();
        sections.put("Details", fields(
                "Booking Reference", bookingReference == null ? NA : bookingReference,
                "Issue", message));
        return new NotificationContent(title, message, bookingReference, null, null, sections);
    }

    // ------------------------------------------------------------------ helpers

    private static Map<String, String> fields(String... keyValues) {
        Map<String, String> map = new LinkedHashMap<>();
        for (int i = 0; i + 1 < keyValues.length; i += 2) {
            map.put(keyValues[i], keyValues[i + 1]);
        }
        return map;
    }

    private static String nz(String value) {
        return (value == null || value.isBlank()) ? NA : value.trim();
    }

    private static String date(LocalDateTime t) {
        return t == null ? NA : t.format(DATE);
    }

    private static String time(LocalDateTime t) {
        return t == null ? NA : t.format(TIME);
    }

    private static String dateTime(LocalDateTime t) {
        return t == null ? NA : t.format(DATE_TIME);
    }

    /** "3h 15m", "45m", or "Less than 1 minute". Based purely on the stored timestamps. */
    static String formatDuration(LocalDateTime entry, LocalDateTime exit) {
        if (entry == null || exit == null) {
            return NA;
        }
        long minutes = Math.max(0, Duration.between(entry, exit).toMinutes());
        if (minutes == 0) {
            return "Less than 1 minute";
        }
        long h = minutes / 60;
        long m = minutes % 60;
        return h > 0 ? h + "h " + m + "m" : m + "m";
    }

    private static String vehicleType(ParkingSlot slot) {
        SlotType type = slot == null ? null : slot.getSlotType();
        if (type == null) {
            return NA;
        }
        return switch (type) {
            case CAR, FOUR_WHEELER -> "Car (Four Wheeler)";
            case BIKE, TWO_WHEELER -> "Bike (Two Wheeler)";
            case EV -> "Electric Vehicle (EV)";
        };
    }

    private static String statusLabel(BookingStatus status) {
        if (status == null) {
            return NA;
        }
        return switch (status) {
            case CHECKED_IN -> "Active (Checked In)";
            case ACTIVE -> "Active";
            default -> titleCase(status.name());
        };
    }

    private static String titleCase(String enumName) {
        String lower = enumName.toLowerCase(Locale.ENGLISH).replace('_', ' ');
        return Character.toUpperCase(lower.charAt(0)) + lower.substring(1);
    }

    /** Rupee amount; whole numbers render without decimals (₹65), otherwise two decimals (₹32.50). */
    static String money(Double amount) {
        if (amount == null) {
            return NA;
        }
        BigDecimal bd = BigDecimal.valueOf(amount).setScale(2, RoundingMode.HALF_UP);
        boolean whole = bd.stripTrailingZeros().scale() <= 0;
        return "\u20B9" + (whole ? bd.setScale(0, RoundingMode.HALF_UP).toPlainString() : bd.toPlainString());
    }

    private static String rate(Double hourlyRate) {
        return hourlyRate == null ? NA : money(hourlyRate) + "/hour";
    }
}
