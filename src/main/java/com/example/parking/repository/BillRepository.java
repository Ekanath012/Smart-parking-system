package com.example.parking.repository;

import com.example.parking.model.Bill;
import com.example.parking.model.Booking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Spring Data JPA Repository for Bill entity.
 * Revenue ONLY accumulates from completed check-out bills.
 */
@Repository
public interface BillRepository extends JpaRepository<Bill, Long> {

    Optional<Bill> findByBooking(Booking booking);

    @Query("SELECT COALESCE(SUM(b.totalAmount), 0.0) FROM Bill b")
    Double calculateTotalRevenue();
}
