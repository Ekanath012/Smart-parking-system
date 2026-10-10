package com.example.parking.controller;

import com.example.parking.repository.ParkingSlotRepository;
import com.example.parking.model.SlotStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Controller serving the landing page and public facility overview.
 */
@Controller
public class HomeController {

    private final ParkingSlotRepository slotRepository;

    public HomeController(ParkingSlotRepository slotRepository) {
        this.slotRepository = slotRepository;
    }

    /**
     * Home / Landing Page.
     */
    @GetMapping("/")
    public String showHomePage(Model model) {
        long totalSlots = slotRepository.count();
        long availableSlots = slotRepository.countByStatus(SlotStatus.AVAILABLE);
        long occupiedSlots = slotRepository.countByStatus(SlotStatus.OCCUPIED);

        model.addAttribute("totalSlots", totalSlots);
        model.addAttribute("availableSlots", availableSlots);
        model.addAttribute("occupiedSlots", occupiedSlots);

        return "index";
    }
}
