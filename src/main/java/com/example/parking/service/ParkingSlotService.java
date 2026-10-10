package com.example.parking.service;

import com.example.parking.dto.ParkingSlotDto;
import com.example.parking.model.ParkingSlot;
import com.example.parking.model.SlotStatus;
import com.example.parking.model.SlotType;

import java.util.List;

/**
 * Service interface for Parking Slot management business logic.
 */
public interface ParkingSlotService {

    ParkingSlot addSlot(ParkingSlotDto slotDto);

    ParkingSlot updateSlot(Long slotId, ParkingSlotDto slotDto);

    void deleteSlot(Long slotId);

    ParkingSlot toggleMaintenance(Long slotId);

    List<ParkingSlot> getAllSlots();

    List<ParkingSlot> getAvailableSlots();

    List<ParkingSlot> getAvailableSlotsByType(SlotType slotType);

    ParkingSlot getSlotById(Long slotId);

    long countTotalSlots();

    long countSlotsByStatus(SlotStatus status);
}
