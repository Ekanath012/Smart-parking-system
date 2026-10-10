package com.example.parking.controller;

import com.example.parking.dto.BookingMapper;
import com.example.parking.dto.BookingResponseDto;
import com.example.parking.exception.BookingException;
import com.example.parking.exception.BookingStateException;
import com.example.parking.exception.ResourceNotFoundException;
import com.example.parking.exception.SlotNotAvailableException;
import com.example.parking.model.Bill;
import com.example.parking.model.Booking;
import com.example.parking.model.User;
import com.example.parking.repository.BillRepository;
import com.example.parking.repository.BookingRepository;
import com.example.parking.service.BookingService;
import com.example.parking.service.UserService;
import com.example.parking.util.PdfReceiptGenerator;
import com.example.parking.util.QrCodeGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Controller managing User slot booking requests, Booking Confirmation view, 
 * QR Code Pass downloads, vehicle checkout, and downloadable PDF receipts.
 */
@Controller
public class BookingController {

    private static final Logger logger = LoggerFactory.getLogger(BookingController.class);

    private final BookingService bookingService;
    private final UserService userService;
    private final BillRepository billRepository;
    private final BookingRepository bookingRepository;

    @Value("${app.pdf.receipt.directory:./uploads/receipts/}")
    private String receiptDirectory;

    public BookingController(BookingService bookingService,
                             UserService userService,
                             BillRepository billRepository,
                             BookingRepository bookingRepository) {
        this.bookingService = bookingService;
        this.userService = userService;
        this.billRepository = billRepository;
        this.bookingRepository = bookingRepository;
    }

    /**
     * Handle Slot Booking Form Submission & Redirect to Booking Confirmation Page.
     */
    @PostMapping("/user/book")
    public String handleBookSlot(@RequestParam(value = "slotId", required = false) Long slotId,
                                 @RequestParam(value = "vehicleNumber", required = false) String vehicleNumber,
                                 RedirectAttributes redirectAttributes) {
        if (slotId == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "Invalid slot selection. Please select an available slot.");
            return "redirect:/user/dashboard";
        }

        if (vehicleNumber == null || vehicleNumber.trim().isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Vehicle registration number is required.");
            return "redirect:/user/dashboard";
        }

        try {
            User currentUser = userService.getAuthenticatedUser();
            Booking booking = bookingService.bookSlot(slotId, vehicleNumber, currentUser);
            
            redirectAttributes.addFlashAttribute("successMessage", "Booking Confirmed Successfully!");
            redirectAttributes.addFlashAttribute("success", "Booking Confirmed Successfully!");
            return "redirect:/booking/confirmation/" + booking.getId();
        } catch (BookingException | BookingStateException | SlotNotAvailableException | ResourceNotFoundException ex) {
            logger.warn("Booking failed: {}", ex.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/user/dashboard";
        } catch (Exception ex) {
            logger.error("Unexpected error during booking: ", ex);
            // Never show raw exception / SQL text to the driver; the details are in the server log above.
            redirectAttributes.addFlashAttribute("errorMessage", "We could not complete your booking right now. Please try again, or contact support if it keeps happening.");
            return "redirect:/user/dashboard";
        }
    }

    /**
     * Display Booking Confirmation Page with complete DTO mapping & QR Code.
     */
    @GetMapping("/booking/confirmation/{bookingId}")
    public String showBookingConfirmation(@PathVariable("bookingId") Long bookingId, Model model) {
        User currentUser = userService.getAuthenticatedUser();
        Booking booking = bookingService.getBookingById(bookingId);

        // Security check
        if (!booking.getUser().getId().equals(currentUser.getId())) {
            throw new BookingException("Unauthorized access to booking confirmation.");
        }

        BookingResponseDto dto = BookingMapper.toDto(booking, null);
        // Keep a Base64 fallback for legacy bookings created before QR files were persisted.
        if (dto.getQrCodeUrl() == null) {
            try {
                String qrText = "SMART_PARKING_PASS|" + booking.getBookingReference()
                        + "|" + booking.getVehicleNumber()
                        + "|" + booking.getParkingSlot().getSlotNumber();
                dto.setQrCodeBase64(QrCodeGenerator.generateQrCodeBase64(qrText, 250, 250));
            } catch (Exception e) {
                logger.error("QR Code generation error: ", e);
            }
        }

        model.addAttribute("booking", dto);
        model.addAttribute("user", currentUser);

        return "booking/confirmation";
    }

    /**
     * Securely stream the stored QR image for the signed-in owner/admin.
     */
    @GetMapping("/booking/qr/view/{bookingId}")
    public ResponseEntity<byte[]> viewQrCode(@PathVariable("bookingId") Long bookingId) {
        return qrResponse(bookingId, false);
    }

    /**
     * Download QR Code Pass PNG Image.
     */
    @GetMapping("/booking/qr/download/{bookingId}")
    public ResponseEntity<byte[]> downloadQrCode(@PathVariable("bookingId") Long bookingId) {
        return qrResponse(bookingId, true);
    }

    private ResponseEntity<byte[]> qrResponse(Long bookingId, boolean download) {
        User currentUser = userService.getAuthenticatedUser();
        Booking booking = bookingService.getBookingById(bookingId);

        boolean admin = currentUser.getRoles().stream().anyMatch(r -> r.getName() == com.example.parking.model.RoleName.ROLE_ADMIN);
        if (!admin && !booking.getUser().getId().equals(currentUser.getId())) {
            throw new BookingException("Unauthorized access to QR Code.");
        }

        try {
            byte[] qrBytes = null;
            if (booking.getQrCodePath() != null && !booking.getQrCodePath().isBlank()) {
                java.nio.file.Path path = java.nio.file.Paths.get(booking.getQrCodePath()).normalize();
                if (!path.isAbsolute()) {
                    path = java.nio.file.Paths.get(".").toAbsolutePath().normalize().resolve(path).normalize();
                }
                if (java.nio.file.Files.exists(path) && java.nio.file.Files.isRegularFile(path)) {
                    qrBytes = java.nio.file.Files.readAllBytes(path);
                }
            }
            if (qrBytes == null) {
                String qrText = "SMART_PARKING_PASS|" + booking.getBookingReference()
                        + "|" + booking.getVehicleNumber()
                        + "|" + booking.getParkingSlot().getSlotNumber();
                qrBytes = QrCodeGenerator.generateQrCodeBytes(qrText, 350, 350);
            }

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.IMAGE_PNG);
            if (download) {
                headers.setContentDispositionFormData("attachment", "qr-pass-" + booking.getBookingReference() + ".png");
            } else {
                headers.setCacheControl("no-store, max-age=0");
            }
            return ResponseEntity.ok().headers(headers).body(qrBytes);
        } catch (Exception e) {
            logger.error("QR Code response error for booking {}: ", bookingId, e);
            throw new BookingException("Failed to generate QR code image.");
        }
    }

    /** Cancel the signed-in user's active booking and release its slot. */
    @PostMapping("/user/cancel")
    public String cancelBooking(@RequestParam("bookingId") Long bookingId,
                                RedirectAttributes redirectAttributes) {
        try {
            User currentUser = userService.getAuthenticatedUser();
            bookingService.cancelBooking(bookingId, currentUser);
            redirectAttributes.addFlashAttribute("successMessage", "Booking cancelled successfully. The parking slot is available again.");
        } catch (Exception ex) {
            logger.warn("Booking cancellation failed: {}", ex.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", "We could not cancel this booking right now. Please try again.");
        }
        return "redirect:/user/dashboard";
    }

    /**
     * Handle Vehicle Checkout & Bill Processing (Increases Revenue ONLY on Checkout).
     */
    @PostMapping("/user/exit")
    public String handleCheckout(@RequestParam("bookingId") Long bookingId,
                                 RedirectAttributes redirectAttributes) {
        try {
            User currentUser = userService.getAuthenticatedUser();
            Bill bill = bookingService.processCheckout(bookingId, currentUser);
            redirectAttributes.addFlashAttribute("successMessage", "Vehicle Check-Out Successful! Bill Calculated.");
            redirectAttributes.addFlashAttribute("success", "Vehicle Check-Out Successful! Bill Calculated.");
            return "redirect:/booking/receipt/" + bookingId;
        } catch (Exception ex) {
            logger.error("Checkout failed: ", ex);
            redirectAttributes.addFlashAttribute("errorMessage", "Checkout could not be completed right now. Please try again.");
            return "redirect:/user/dashboard";
        }
    }

    /**
     * Display Calculated Bill Receipt View.
     */
    @GetMapping("/booking/receipt/{bookingId}")
    public String showReceipt(@PathVariable("bookingId") Long bookingId, Model model) {
        User currentUser = userService.getAuthenticatedUser();
        Booking booking = bookingService.getBookingById(bookingId);

        assertCanAccessBooking(booking, currentUser);

        Bill bill = billRepository.findByBooking(booking)
                .orElseThrow(() -> new BookingException("Bill not found for booking ID " + bookingId));

        BookingResponseDto dto = BookingMapper.toDto(booking, bill);
        model.addAttribute("booking", dto);
        model.addAttribute("bill", bill);
        model.addAttribute("user", currentUser);

        return "booking/receipt";
    }

    /**
     * Download Professional PDF Billing Receipt with Embedded QR Code.
     */
    @GetMapping("/booking/pdf/{bookingId}")
    public ResponseEntity<byte[]> downloadPdfReceipt(@PathVariable("bookingId") Long bookingId) {
        User currentUser = userService.getAuthenticatedUser();
        Booking booking = bookingService.getBookingById(bookingId);
        assertCanAccessBooking(booking, currentUser);

        Bill bill = billRepository.findByBooking(booking)
                .orElseThrow(() -> new BookingException("Bill not found for booking ID " + bookingId));

        try {
            String qrText = "SMART_PARKING_PASS|" + booking.getBookingReference() 
                    + "|" + booking.getVehicleNumber() 
                    + "|" + booking.getParkingSlot().getSlotNumber();
            byte[] qrBytes = QrCodeGenerator.generateQrCodeBytes(qrText, 150, 150);

            byte[] pdfBytes = PdfReceiptGenerator.generateBillPdf(booking, bill, qrBytes);

            java.nio.file.Path receiptDir = java.nio.file.Paths.get(receiptDirectory).toAbsolutePath().normalize();
            java.nio.file.Files.createDirectories(receiptDir);
            String receiptFileName = "receipt-" + booking.getBookingReference() + ".pdf";
            java.nio.file.Path receiptFile = receiptDir.resolve(receiptFileName).normalize();
            if (!receiptFile.startsWith(receiptDir)) {
                throw new BookingException("Invalid receipt file path.");
            }
            java.nio.file.Files.write(receiptFile, pdfBytes);
            booking.setReceiptPath("uploads/receipts/" + receiptFileName);
            bookingRepository.save(booking);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("attachment", "receipt-" + booking.getBookingReference() + ".pdf");
            headers.setCacheControl("must-revalidate, post-check=0, pre-check=0");

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(pdfBytes);
        } catch (Exception e) {
            logger.error("PDF generation error: ", e);
            throw new BookingException("Failed to generate PDF receipt document.");
        }
    }
    private void assertCanAccessBooking(Booking booking, User currentUser) {
        boolean admin = currentUser.getRoles().stream()
                .anyMatch(r -> r.getName() == com.example.parking.model.RoleName.ROLE_ADMIN);
        if (!admin && !booking.getUser().getId().equals(currentUser.getId())) {
            throw new BookingException("You are not authorized to access this booking.");
        }
    }

}
