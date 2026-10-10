package com.example.parking.controller;

import com.example.parking.dto.ParkingSlotDto;
import com.example.parking.exception.BookingException;
import com.example.parking.exception.DuplicateResourceException;
import com.example.parking.model.SlotStatus;
import com.example.parking.repository.BillRepository;
import com.example.parking.repository.BookingRepository;
import com.example.parking.service.ParkingSlotService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Controller managing Admin operations: Overview Dashboard, Live Vehicle Bookings with Number Plates, 
 * Parking Slot Inventory CRUD, Maintenance Management, and Revenue Tracking.
 */
@Controller
@RequestMapping("/admin")
public class AdminController {

    private final ParkingSlotService slotService;
    private final BillRepository billRepository;
    private final BookingRepository bookingRepository;

    public AdminController(ParkingSlotService slotService,
                           BillRepository billRepository,
                           BookingRepository bookingRepository) {
        this.slotService = slotService;
        this.billRepository = billRepository;
        this.bookingRepository = bookingRepository;
    }

    /**
     * Admin Dashboard Overview displaying Stats, Slot Inventory, and Live Vehicle Bookings with Number Plates.
     */
    @GetMapping("/dashboard")
    public String showAdminDashboard(Model model) {
        long totalSlots = slotService.countTotalSlots();
        long availableSlots = slotService.countSlotsByStatus(SlotStatus.AVAILABLE);
        long occupiedSlots = slotService.countSlotsByStatus(SlotStatus.OCCUPIED);
        long maintenanceSlots = slotService.countSlotsByStatus(SlotStatus.MAINTENANCE);
        Double totalRevenue = billRepository.calculateTotalRevenue();

        model.addAttribute("totalSlots", totalSlots);
        model.addAttribute("availableSlots", availableSlots);
        model.addAttribute("occupiedSlots", occupiedSlots);
        model.addAttribute("maintenanceSlots", maintenanceSlots);
        model.addAttribute("totalRevenue", totalRevenue != null ? totalRevenue : 0.0);
        model.addAttribute("slots", slotService.getAllSlots());
        model.addAttribute("allBookings", bookingRepository.findAllWithDetails());

        if (!model.containsAttribute("slotDto")) {
            model.addAttribute("slotDto", new ParkingSlotDto());
        }

        return "admin/dashboard";
    }

    /**
     * Handle New Parking Slot Creation Form.
     */
    @PostMapping("/slots/add")
    public String addParkingSlot(@Valid @ModelAttribute("slotDto") ParkingSlotDto slotDto,
                                 BindingResult bindingResult,
                                 RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("org.springframework.validation.BindingResult.slotDto", bindingResult);
            redirectAttributes.addFlashAttribute("slotDto", slotDto);
            redirectAttributes.addFlashAttribute("errorMessage", "Invalid slot input details. Please check the form.");
            return "redirect:/admin/dashboard";
        }

        try {
            slotService.addSlot(slotDto);
            redirectAttributes.addFlashAttribute("successMessage", "Parking slot '" + slotDto.getSlotNumber() + "' added successfully!");
        } catch (DuplicateResourceException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }

        return "redirect:/admin/dashboard";
    }

    /**
     * Toggle Maintenance Status for a Parking Slot.
     */
    @PostMapping("/slots/toggle/{id}")
    public String toggleSlotMaintenance(@PathVariable("id") Long slotId, RedirectAttributes redirectAttributes) {
        try {
            slotService.toggleMaintenance(slotId);
            redirectAttributes.addFlashAttribute("successMessage", "Slot status toggled successfully!");
        } catch (BookingException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/admin/dashboard";
    }

    /**
     * Delete a Parking Slot.
     */
    @PostMapping("/slots/delete/{id}")
    public String deleteParkingSlot(@PathVariable("id") Long slotId, RedirectAttributes redirectAttributes) {
        try {
            slotService.deleteSlot(slotId);
            redirectAttributes.addFlashAttribute("successMessage", "Parking slot deleted successfully!");
        } catch (BookingException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/admin/dashboard";
    }
}
