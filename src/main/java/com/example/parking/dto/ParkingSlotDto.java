package com.example.parking.dto;

import com.example.parking.model.SlotStatus;
import com.example.parking.model.SlotType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Data Transfer Object for Parking Slot creation and updates.
 */
public class ParkingSlotDto {

    private Long id;

    @NotBlank(message = "Slot number is required (e.g. A-01)")
    @Size(max = 20, message = "Slot number must not exceed 20 characters")
    private String slotNumber;

    @NotNull(message = "Slot type is required")
    private SlotType slotType = SlotType.CAR;

    @NotNull(message = "Hourly rate is required")
    @Min(value = 1, message = "Hourly rate must be at least ₹1")
    private Double hourlyRate = 20.0;

    @NotBlank(message = "Floor level is required")
    private String floorLevel = "Ground";

    private SlotStatus status = SlotStatus.AVAILABLE;

    public ParkingSlotDto() {
    }

    public ParkingSlotDto(String slotNumber, SlotType slotType, Double hourlyRate, String floorLevel) {
        this.slotNumber = slotNumber;
        this.slotType = slotType;
        this.hourlyRate = hourlyRate;
        this.floorLevel = floorLevel;
        this.status = SlotStatus.AVAILABLE;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getSlotNumber() {
        return slotNumber;
    }

    public void setSlotNumber(String slotNumber) {
        this.slotNumber = slotNumber;
    }

    public SlotType getSlotType() {
        return slotType;
    }

    public void setSlotType(SlotType slotType) {
        this.slotType = slotType;
    }

    public Double getHourlyRate() {
        return hourlyRate;
    }

    public void setHourlyRate(Double hourlyRate) {
        this.hourlyRate = hourlyRate;
    }

    public String getFloorLevel() {
        return floorLevel;
    }

    public void setFloorLevel(String floorLevel) {
        this.floorLevel = floorLevel;
    }

    public SlotStatus getStatus() {
        return status;
    }

    public void setStatus(SlotStatus status) {
        this.status = status;
    }
}
