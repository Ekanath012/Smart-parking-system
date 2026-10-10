package com.example.parking.controller;

import com.example.parking.model.SlotStatus;
import com.example.parking.model.User;
import com.example.parking.repository.BookingRepository;
import com.example.parking.repository.ParkingSlotRepository;
import com.example.parking.service.UserService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Controller handling User Dashboard and User views.
 */
@Controller
@RequestMapping("/user")
public class UserController {

    private final UserService userService;
    private final ParkingSlotRepository slotRepository;
    private final BookingRepository bookingRepository;

    public UserController(UserService userService,
                          ParkingSlotRepository slotRepository,
                          BookingRepository bookingRepository) {
        this.userService = userService;
        this.slotRepository = slotRepository;
        this.bookingRepository = bookingRepository;
    }

    /**
     * User Dashboard Overview.
     */
    @GetMapping("/dashboard")
    public String showUserDashboard(Model model) {
        User currentUser = userService.getAuthenticatedUser();
        model.addAttribute("user", currentUser);

        model.addAttribute("availableSlots", slotRepository.findByStatus(SlotStatus.AVAILABLE));
        model.addAttribute("activeBooking", bookingRepository.findActiveBookingByUserId(currentUser.getId()).orElse(null));
        model.addAttribute("bookingHistory", bookingRepository.findByUserWithDetailsOrderByCreatedAtDesc(currentUser));

        return "user/dashboard";
    }
}
