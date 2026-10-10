package com.example.parking.dto;

import com.example.parking.model.Bill;
import com.example.parking.model.Booking;

/**
 * Mapper utility converting Booking domain entities to clean BookingResponseDto objects.
 */
public class BookingMapper {

    public static BookingResponseDto toDto(Booking booking, Bill bill) {
        if (booking == null) return null;

        BookingResponseDto dto = new BookingResponseDto();
        dto.setId(booking.getId());
        dto.setBookingReference(booking.getBookingReference() != null ? booking.getBookingReference() : "SPK-" + booking.getId());
        
        if (booking.getUser() != null) {
            dto.setUserName(booking.getUser().getName());
            dto.setUserEmail(booking.getUser().getEmail());
        } else {
            dto.setUserName("Customer");
            dto.setUserEmail("N/A");
        }

        dto.setVehicleNumber(booking.getVehicleNumber() != null ? booking.getVehicleNumber() : "UNKNOWN");
        
        if (booking.getParkingSlot() != null) {
            dto.setSlotNumber(booking.getParkingSlot().getSlotNumber());
            dto.setSlotType(booking.getParkingSlot().getSlotType() != null ? booking.getParkingSlot().getSlotType().name() : "CAR");
            dto.setHourlyRate(booking.getParkingSlot().getHourlyRate() != null ? booking.getParkingSlot().getHourlyRate() : 20.0);
            dto.setFloorLevel(booking.getParkingSlot().getFloorLevel() != null ? booking.getParkingSlot().getFloorLevel() : "Ground");
        } else {
            dto.setSlotNumber("N/A");
            dto.setSlotType("CAR");
            dto.setHourlyRate(20.0);
            dto.setFloorLevel("Ground");
        }

        dto.setEntryTime(booking.getEntryTime());
        if (booking.getId() != null && booking.getQrCodePath() != null && !booking.getQrCodePath().isBlank()) {
            dto.setQrCodeUrl("/booking/qr/view/" + booking.getId());
        }
        dto.setExitTime(booking.getExitTime());
        dto.setStatus(booking.getStatus() != null ? booking.getStatus().name() : "CHECKED_IN");

        if (bill != null) {
            dto.setTotalFee(bill.getTotalAmount());
            dto.setTotalHours(bill.getTotalHours() != null ? bill.getTotalHours().longValue() : 1L);
        } else {
            dto.setTotalFee(0.0);
            dto.setTotalHours(0L);
        }

        return dto;
    }
}
