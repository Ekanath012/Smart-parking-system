package com.example.parking.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Entity representing the final calculated bill for a parking session.
 */
@Entity
@Table(name = "bills")
public class Bill {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "booking_id", nullable = false, unique = true)
    private Booking booking;

    @Column(name = "total_hours", nullable = false)
    private Integer totalHours;

    @Column(name = "hourly_rate", nullable = false)
    private Double hourlyRate;

    @Column(name = "total_amount", nullable = false)
    private Double totalAmount;

    @Column(name = "actual_duration_minutes")
    private Long actualDurationMinutes;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status", nullable = false, length = 20)
    private PaymentStatus paymentStatus = PaymentStatus.PAID;

    @Column(name = "payment_time")
    private LocalDateTime paymentTime = LocalDateTime.now();

    public Bill() {
    }

    public Bill(Booking booking, Integer totalHours, Double hourlyRate, Double totalAmount) {
        this.booking = booking;
        this.totalHours = totalHours;
        this.hourlyRate = hourlyRate;
        this.totalAmount = totalAmount;
        this.paymentStatus = PaymentStatus.PAID;
        this.paymentTime = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Booking getBooking() {
        return booking;
    }

    public void setBooking(Booking booking) {
        this.booking = booking;
    }

    public Integer getTotalHours() {
        return totalHours;
    }

    public void setTotalHours(Integer totalHours) {
        this.totalHours = totalHours;
    }

    public Double getHourlyRate() {
        return hourlyRate;
    }

    public void setHourlyRate(Double hourlyRate) {
        this.hourlyRate = hourlyRate;
    }

    public Double getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(Double totalAmount) {
        this.totalAmount = totalAmount;
    }

    public Long getActualDurationMinutes() {
        return actualDurationMinutes;
    }

    public void setActualDurationMinutes(Long actualDurationMinutes) {
        this.actualDurationMinutes = actualDurationMinutes;
    }

    public PaymentStatus getPaymentStatus() {
        return paymentStatus;
    }

    public void setPaymentStatus(PaymentStatus paymentStatus) {
        this.paymentStatus = paymentStatus;
    }

    public LocalDateTime getPaymentTime() {
        return paymentTime;
    }

    public void setPaymentTime(LocalDateTime paymentTime) {
        this.paymentTime = paymentTime;
    }
}
