package com.example.parking.service;

import com.example.parking.dto.ParkingSlotDto;
import com.example.parking.exception.BookingException;
import com.example.parking.exception.DuplicateResourceException;
import com.example.parking.exception.ResourceNotFoundException;
import com.example.parking.model.ParkingSlot;
import com.example.parking.model.SlotStatus;
import com.example.parking.model.SlotType;
import com.example.parking.repository.ParkingSlotRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Service implementation for managing physical parking slots.
 * Uses constructor injection.
 */
@Service
public class ParkingSlotServiceImpl implements ParkingSlotService {

    private final ParkingSlotRepository slotRepository;

    public ParkingSlotServiceImpl(ParkingSlotRepository slotRepository) {
        this.slotRepository = slotRepository;
    }

    @Override
    @Transactional
    public ParkingSlot addSlot(ParkingSlotDto slotDto) {
        String formattedSlotNumber = slotDto.getSlotNumber().trim().toUpperCase();
        if (slotRepository.existsBySlotNumber(formattedSlotNumber)) {
            throw new DuplicateResourceException("Parking slot '" + formattedSlotNumber + "' already exists.");
        }

        ParkingSlot slot = new ParkingSlot();
        slot.setSlotNumber(formattedSlotNumber);
        slot.setSlotType(slotDto.getSlotType());
        slot.setHourlyRate(slotDto.getHourlyRate());
        slot.setFloorLevel(slotDto.getFloorLevel().trim());
        slot.setStatus(SlotStatus.AVAILABLE);

        return slotRepository.save(slot);
    }

    @Override
    @Transactional
    public ParkingSlot updateSlot(Long slotId, ParkingSlotDto slotDto) {
        ParkingSlot slot = getSlotById(slotId);

        String newSlotNumber = slotDto.getSlotNumber().trim().toUpperCase();
        if (!slot.getSlotNumber().equalsIgnoreCase(newSlotNumber) && slotRepository.existsBySlotNumber(newSlotNumber)) {
            throw new DuplicateResourceException("Parking slot '" + newSlotNumber + "' already exists.");
        }

        slot.setSlotNumber(newSlotNumber);
        slot.setSlotType(slotDto.getSlotType());
        slot.setHourlyRate(slotDto.getHourlyRate());
        slot.setFloorLevel(slotDto.getFloorLevel().trim());

        return slotRepository.save(slot);
    }

    @Override
    @Transactional
    public void deleteSlot(Long slotId) {
        ParkingSlot slot = getSlotById(slotId);
        if (slot.getStatus() == SlotStatus.OCCUPIED) {
            throw new BookingException("Cannot delete slot '" + slot.getSlotNumber() + "' because it is currently occupied by a parked vehicle.");
        }
        slotRepository.delete(slot);
    }

    @Override
    @Transactional
    public ParkingSlot toggleMaintenance(Long slotId) {
        ParkingSlot slot = getSlotById(slotId);
        if (slot.getStatus() == SlotStatus.OCCUPIED) {
            throw new BookingException("Cannot set slot '" + slot.getSlotNumber() + "' to maintenance while occupied.");
        }

        if (slot.getStatus() == SlotStatus.MAINTENANCE) {
            slot.setStatus(SlotStatus.AVAILABLE);
        } else {
            slot.setStatus(SlotStatus.MAINTENANCE);
        }

        return slotRepository.save(slot);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ParkingSlot> getAllSlots() {
        return slotRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ParkingSlot> getAvailableSlots() {
        return slotRepository.findByStatus(SlotStatus.AVAILABLE);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ParkingSlot> getAvailableSlotsByType(SlotType slotType) {
        return slotRepository.findBySlotTypeAndStatus(slotType, SlotStatus.AVAILABLE);
    }

    @Override
    @Transactional(readOnly = true)
    public ParkingSlot getSlotById(Long slotId) {
        return slotRepository.findById(slotId)
                .orElseThrow(() -> new ResourceNotFoundException("ParkingSlot", "id", slotId));
    }

    @Override
    @Transactional(readOnly = true)
    public long countTotalSlots() {
        return slotRepository.count();
    }

    @Override
    @Transactional(readOnly = true)
    public long countSlotsByStatus(SlotStatus status) {
        return slotRepository.countByStatus(status);
    }
}
