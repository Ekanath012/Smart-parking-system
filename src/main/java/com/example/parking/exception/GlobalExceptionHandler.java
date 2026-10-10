package com.example.parking.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.ui.Model;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/**
 * Global Exception Handler for Thymeleaf MVC Application.
 * Intercepts application exceptions and provides clean, responsive user feedback.
 */
@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * Handles ResourceNotFoundException (HTTP 404).
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String handleResourceNotFound(ResourceNotFoundException ex, Model model) {
        logger.error("Resource not found: {}", ex.getMessage());
        model.addAttribute("status", 404);
        model.addAttribute("error", "Resource Not Found");
        model.addAttribute("message", ex.getMessage());
        return "error/error";
    }

    /**
     * Handles SlotNotAvailableException (HTTP 400).
     */
    @ExceptionHandler(SlotNotAvailableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public String handleSlotNotAvailable(SlotNotAvailableException ex, Model model) {
        logger.warn("Slot unavailable: {}", ex.getMessage());
        model.addAttribute("status", 400);
        model.addAttribute("error", "Slot Unavailable");
        model.addAttribute("message", ex.getMessage());
        return "error/error";
    }

    /**
     * Handles BookingException & IllegalArgumentException (HTTP 400).
     */
    @ExceptionHandler({BookingException.class, IllegalArgumentException.class, MethodArgumentTypeMismatchException.class, MissingServletRequestParameterException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public String handleBookingException(Exception ex, Model model) {
        logger.warn("Validation/Booking error: {}", ex.getMessage());
        model.addAttribute("status", 400);
        model.addAttribute("error", "Invalid Request");
        model.addAttribute("message", ex.getMessage());
        return "error/error";
    }

    /**
     * Handles DuplicateResourceException (HTTP 409).
     */
    @ExceptionHandler(DuplicateResourceException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public String handleDuplicateResource(DuplicateResourceException ex, Model model) {
        logger.warn("Duplicate resource: {}", ex.getMessage());
        model.addAttribute("status", 409);
        model.addAttribute("error", "Resource Already Exists");
        model.addAttribute("message", ex.getMessage());
        return "error/error";
    }

    /**
     * Handles UnauthorizedAccessException (HTTP 403).
     */
    @ExceptionHandler(UnauthorizedAccessException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public String handleUnauthorizedAccess(UnauthorizedAccessException ex, Model model) {
        logger.error("Unauthorized access attempt: {}", ex.getMessage());
        model.addAttribute("status", 403);
        model.addAttribute("error", "Access Denied");
        model.addAttribute("message", ex.getMessage());
        return "error/error";
    }

    /**
     * Catch-all for uncaught global exceptions (HTTP 500).
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public String handleGlobalException(Exception ex, Model model) {
        logger.error("Unhandled internal server error: ", ex);
        model.addAttribute("status", 500);
        model.addAttribute("error", "Internal Server Error");
        model.addAttribute("message", "Something went wrong while processing your request. Please try again. If the problem continues, contact support.");
        return "error/error";
    }
}
