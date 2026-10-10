package com.example.parking.dto;

import java.time.LocalDateTime;

/**
 * Data Transfer Object for carrying complete Booking details to views and responses.
 * Guarantees zero null fields for UI rendering.
 */
public class BookingResponseDto {

    private Long id;
    private String bookingReference;
    private String userName;
    private String userEmail;
    private String vehicleNumber;
    private String slotNumber;
    private String slotType;
    private Double hourlyRate;
    private String floorLevel;
    private LocalDateTime entryTime;
    private LocalDateTime exitTime;
    private String status;
    private Double totalFee;
    private Long totalHours;
    private String qrCodeBase64;
    private String qrCodeUrl;

    public BookingResponseDto() {}

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getBookingReference() { return bookingReference; }
    public void setBookingReference(String bookingReference) { this.bookingReference = bookingReference; }

    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }

    public String getUserEmail() { return userEmail; }
    public void setUserEmail(String userEmail) { this.userEmail = userEmail; }

    public String getVehicleNumber() { return vehicleNumber; }
    public void setVehicleNumber(String vehicleNumber) { this.vehicleNumber = vehicleNumber; }

    public String getSlotNumber() { return slotNumber; }
    public void setSlotNumber(String slotNumber) { this.slotNumber = slotNumber; }

    public String getSlotType() { return slotType; }
    public void setSlotType(String slotType) { this.slotType = slotType; }

    public Double getHourlyRate() { return hourlyRate; }
    public void setHourlyRate(Double hourlyRate) { this.hourlyRate = hourlyRate; }

    public String getFloorLevel() { return floorLevel; }
    public void setFloorLevel(String floorLevel) { this.floorLevel = floorLevel; }

    public LocalDateTime getEntryTime() { return entryTime; }
    public void setEntryTime(LocalDateTime entryTime) { this.entryTime = entryTime; }

    public LocalDateTime getExitTime() { return exitTime; }
    public void setExitTime(LocalDateTime exitTime) { this.exitTime = exitTime; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Double getTotalFee() { return totalFee; }
    public void setTotalFee(Double totalFee) { this.totalFee = totalFee; }

    public Long getTotalHours() { return totalHours; }
    public void setTotalHours(Long totalHours) { this.totalHours = totalHours; }

    public String getQrCodeBase64() { return qrCodeBase64; }
    public void setQrCodeBase64(String qrCodeBase64) { this.qrCodeBase64 = qrCodeBase64; }

    public String getQrCodeUrl() { return qrCodeUrl; }
    public void setQrCodeUrl(String qrCodeUrl) { this.qrCodeUrl = qrCodeUrl; }
}
