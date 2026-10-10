package com.example.parking.service;

import com.example.parking.dto.BookingMapper;
import com.example.parking.dto.BookingResponseDto;
import com.example.parking.event.BookingCreatedEvent;
import com.example.parking.event.ParkingCompletedEvent;
import com.example.parking.exception.BookingException;
import com.example.parking.exception.BookingStateException;
import com.example.parking.exception.ResourceNotFoundException;
import com.example.parking.exception.SlotNotAvailableException;
import com.example.parking.model.*;
import com.example.parking.repository.BillRepository;
import com.example.parking.repository.BookingRepository;
import com.example.parking.repository.ParkingSlotRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.io.IOException;
import java.util.List;
import java.util.Optional;

/**
 * Service implementation for managing vehicle slot bookings, QR Code generation, checkout calculations, and billing receipts.
 * Uses constructor injection.
 */
@Service
public class BookingServiceImpl implements BookingService {

    private static final Logger logger = LoggerFactory.getLogger(BookingServiceImpl.class);

    private final BookingRepository bookingRepository;
    private final ParkingSlotRepository slotRepository;
    private final BillRepository billRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Value("${app.qr.code.directory:./uploads/qrcodes/}")
    private String qrCodeDirectory;

    public BookingServiceImpl(BookingRepository bookingRepository,
                               ParkingSlotRepository slotRepository,
                               BillRepository billRepository,
                               ApplicationEventPublisher eventPublisher) {
        this.bookingRepository = bookingRepository;
        this.slotRepository = slotRepository;
        this.billRepository = billRepository;
        this.eventPublisher = eventPublisher;
    }

    @Override
    @Transactional
    public Booking bookSlot(Long slotId, String vehicleNumber, User user) {
        // Enforce rule: If user already has an active booking, return it instead of throwing an error!
        Optional<Booking> existingActive = bookingRepository.findActiveBookingByUserId(user.getId());
        if (existingActive.isPresent()) {
            logger.info("User {} already has an active booking {}.", user.getEmail(), existingActive.get().getBookingReference());
            throw new BookingStateException("You already have an active parking booking. Complete or cancel it before booking another slot.");
        }

        // Fetch parking slot entity
        ParkingSlot slot = slotRepository.findByIdForUpdate(slotId)
                .orElseThrow(() -> new ResourceNotFoundException("ParkingSlot", "id", slotId));

        // Check if slot is available
        if (slot.getStatus() != SlotStatus.AVAILABLE) {
            throw new SlotNotAvailableException("Parking slot '" + slot.getSlotNumber() + "' is currently " + slot.getStatus() + ".");
        }

        // Clean & format vehicle number plate
        String formattedVehicleNumber = vehicleNumber.trim().replaceAll("\\s+", "").toUpperCase();

        // Generate unique booking reference number
        String bookingRef = "SPK-" + System.currentTimeMillis() % 1000000 + "-" + (1000 + (int)(Math.random() * 9000));

        // Create booking instance
        Booking booking = new Booking();
        booking.setBookingReference(bookingRef);
        booking.setUser(user);
        booking.setParkingSlot(slot);
        booking.setVehicleNumber(formattedVehicleNumber);
        booking.setEntryTime(LocalDateTime.now());
        booking.setStatus(BookingStatus.CHECKED_IN);

        // Persist a short, filesystem-relative QR path. The actual PNG is generated below after the
        // booking has an id and is committed as part of the same transaction. Never store Base64 image
        // data in qr_code_path: the database column is intentionally a short VARCHAR.
        String qrFileName = bookingRef + ".png";
        Path qrDirectory = Paths.get(qrCodeDirectory).toAbsolutePath().normalize();
        Path qrFile = qrDirectory.resolve(qrFileName).normalize();
        if (!qrFile.startsWith(qrDirectory)) {
            throw new BookingException("Invalid QR code file path.");
        }

        // Update slot status to OCCUPIED
        slot.setStatus(SlotStatus.OCCUPIED);
        slotRepository.save(slot);

        Booking saved = bookingRepository.save(booking);

        try {
            Files.createDirectories(qrDirectory);
            String qrText = "SMART_PARKING_PASS|" + bookingRef + "|" + formattedVehicleNumber + "|" + slot.getSlotNumber();
            Files.write(qrFile, com.example.parking.util.QrCodeGenerator.generateQrCodeBytes(qrText, 350, 350));
            saved.setQrCodePath("uploads/qrcodes/" + qrFileName);
            saved = bookingRepository.save(saved);
        } catch (Exception ex) {
            try { Files.deleteIfExists(qrFile); } catch (IOException ignored) { }
            logger.error("QR generation failed for booking {}", bookingRef, ex);
            throw new BookingException("Booking could not be completed because the QR code could not be generated.");
        }

        // Admin dashboard notification + admin email. Handled only AFTER this transaction commits,
        // and isolated from it, so a notification/email problem can never fail or roll back the booking.
        eventPublisher.publishEvent(new BookingCreatedEvent(saved.getId()));

        return saved;
    }

    @Override
    @Transactional
    public BookingResponseDto reserveSlot(Long slotId, String vehicleNumber, User user) {
        Booking booking = bookSlot(slotId, vehicleNumber, user);
        return BookingMapper.toDto(booking, null);
    }

    @Override
    @Transactional
    public Bill processCheckout(Long bookingId, User user) {
        Booking booking = bookingRepository.findByIdForUpdate(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking", "id", bookingId));

        // Security check: Ensure booking belongs to user (or admin)
        if (!booking.getUser().getId().equals(user.getId()) && user.getRoles().stream().noneMatch(r -> r.getName() == RoleName.ROLE_ADMIN)) {
            throw new BookingException("You are not authorized to check out this booking.");
        }

        if (booking.getStatus() == BookingStatus.COMPLETED) {
            return billRepository.findByBooking(booking)
                    .orElseThrow(() -> new ResourceNotFoundException("Bill", "bookingId", bookingId));
        }
        if (!(booking.getStatus() == BookingStatus.BOOKED || booking.getStatus() == BookingStatus.CHECKED_IN
                || booking.getStatus() == BookingStatus.ACTIVE || booking.getStatus() == BookingStatus.RESERVED)) {
            throw new BookingStateException("This booking cannot be checked out because its status is " + booking.getStatus() + ".");
        }

        LocalDateTime exitTime = LocalDateTime.now();
        booking.setExitTime(exitTime);
        booking.setStatus(BookingStatus.COMPLETED);

        // Calculate parking duration (minimum 1 hour, rounded up to nearest hour)
        Duration duration = Duration.between(booking.getEntryTime(), exitTime);
        long minutes = duration.toMinutes();
        int hours = (int) Math.ceil((double) Math.max(minutes, 1) / 60.0);

        Double hourlyRate = booking.getParkingSlot().getHourlyRate() != null ? booking.getParkingSlot().getHourlyRate() : 20.0;
        Double totalAmount = hours * hourlyRate;

        // Save Bill record (this increases revenue in BillRepository)
        Bill bill = new Bill(booking, hours, hourlyRate, totalAmount);
        bill.setActualDurationMinutes(Math.max(minutes, 0L));
        billRepository.save(bill);

        // Release Parking Slot status back to AVAILABLE
        ParkingSlot slot = booking.getParkingSlot();
        slot.setStatus(SlotStatus.AVAILABLE);
        slotRepository.save(slot);

        bookingRepository.save(booking);

        // Admin "Parking Completed" notification + email, dispatched after this transaction commits.
        eventPublisher.publishEvent(new ParkingCompletedEvent(booking.getId()));

        return bill;
    }

    @Override
    @Transactional
    public void cancelBooking(Long bookingId, User user) {
        Booking booking = bookingRepository.findByIdForUpdate(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking", "id", bookingId));

        boolean admin = user.getRoles().stream().anyMatch(r -> r.getName() == RoleName.ROLE_ADMIN);
        if (!admin && (booking.getUser() == null || !booking.getUser().getId().equals(user.getId()))) {
            throw new BookingException("You are not authorized to cancel this booking.");
        }
        if (!(booking.getStatus() == BookingStatus.BOOKED || booking.getStatus() == BookingStatus.CHECKED_IN
                || booking.getStatus() == BookingStatus.ACTIVE || booking.getStatus() == BookingStatus.RESERVED)) {
            throw new BookingException("Only an active booking can be cancelled.");
        }

        ParkingSlot slot = booking.getParkingSlot();
        booking.setStatus(BookingStatus.CANCELLED);
        booking.setExitTime(LocalDateTime.now());
        if (slot != null && slot.getStatus() != SlotStatus.MAINTENANCE) {
            slot.setStatus(SlotStatus.AVAILABLE);
            slotRepository.save(slot);
        }
        bookingRepository.save(booking);
        eventPublisher.publishEvent(new com.example.parking.event.BookingCancelledEvent(booking.getId()));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Booking> getActiveBooking(User user) {
        return bookingRepository.findActiveBookingByUserId(user.getId());
    }

    @Override
    @Transactional(readOnly = true)
    public List<Booking> getUserBookingHistory(User user) {
        return bookingRepository.findByUserWithDetailsOrderByCreatedAtDesc(user);
    }

    @Override
    @Transactional(readOnly = true)
    public Booking getBookingById(Long bookingId) {
        return bookingRepository.findByIdWithDetails(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking", "id", bookingId));
    }

    @Override
    @Transactional(readOnly = true)
    public Booking getBookingByReference(String reference) {
        return bookingRepository.findByBookingReferenceWithDetails(reference)
                .orElseThrow(() -> new ResourceNotFoundException("Booking", "reference", reference));
    }
}
