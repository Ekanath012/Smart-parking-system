package com.example.parking.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

/**
 * Data Transfer Object for Slot Booking requests.
 */
public class BookingRequestDto {

    @NotNull(message = "Parking slot ID is required")
    private Long slotId;

    @NotBlank(message = "Vehicle number is required")
    @Pattern(regexp = "^[A-Z0-9-]{4,15}$", message = "Vehicle number must be valid alphanumeric (e.g. KA-01-AB-1234)")
    private String vehicleNumber;

    public BookingRequestDto() {
    }

    public BookingRequestDto(Long slotId, String vehicleNumber) {
        this.slotId = slotId;
        this.vehicleNumber = vehicleNumber;
    }

    public Long getSlotId() {
        return slotId;
    }

    public void setSlotId(Long slotId) {
        this.slotId = slotId;
    }

    public String getVehicleNumber() {
        return vehicleNumber;
    }

    public void setVehicleNumber(String vehicleNumber) {
        this.vehicleNumber = vehicleNumber;
    }
}
