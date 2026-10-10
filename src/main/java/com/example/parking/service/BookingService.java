package com.example.parking.service;

import com.example.parking.dto.BookingResponseDto;
import com.example.parking.model.Bill;
import com.example.parking.model.Booking;
import com.example.parking.model.User;

import java.util.List;
import java.util.Optional;

/**
 * Service interface for Slot Booking, Check-In, Check-Out, and Billing business operations.
 */
public interface BookingService {

    Booking bookSlot(Long slotId, String vehicleNumber, User user);

    BookingResponseDto reserveSlot(Long slotId, String vehicleNumber, User user);

    Bill processCheckout(Long bookingId, User user);

    void cancelBooking(Long bookingId, User user);

    Optional<Booking> getActiveBooking(User user);

    List<Booking> getUserBookingHistory(User user);

    Booking getBookingById(Long bookingId);

    Booking getBookingByReference(String reference);
}
