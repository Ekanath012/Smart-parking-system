package com.example.parking.service;

import com.example.parking.model.*;
import com.example.parking.repository.BookingRepository;
import com.example.parking.repository.ParkingSlotRepository;
import com.example.parking.repository.RoleRepository;
import com.example.parking.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Component that guarantees default database roles, admin account, user account, 
 * initial slots, and sample bookings on application startup.
 */
@Component
public class DataInitializer {

    private static final Logger logger = LoggerFactory.getLogger(DataInitializer.class);

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final ParkingSlotRepository slotRepository;
    private final BookingRepository bookingRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.admin.email:}")
    private String configuredAdminEmail;

    @Value("${app.admin.initial-password:}")
    private String configuredAdminPassword;

    public DataInitializer(RoleRepository roleRepository,
                           UserRepository userRepository,
                           ParkingSlotRepository slotRepository,
                           BookingRepository bookingRepository,
                           PasswordEncoder passwordEncoder) {
        this.roleRepository = roleRepository;
        this.userRepository = userRepository;
        this.slotRepository = slotRepository;
        this.bookingRepository = bookingRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void initData() {
        logger.info("Initializing system default roles, users, slots, and bookings...");

        // 1. Initialize Roles
        Role adminRole = roleRepository.findByName(RoleName.ROLE_ADMIN)
                .orElseGet(() -> roleRepository.save(new Role(RoleName.ROLE_ADMIN)));

        Role userRole = roleRepository.findByName(RoleName.ROLE_USER)
                .orElseGet(() -> roleRepository.save(new Role(RoleName.ROLE_USER)));

        // 2. Initialize the administrator only when explicit bootstrap credentials are configured.
        // Existing admin passwords are NEVER reset on application startup. This prevents a public/default
        // credential from silently taking over an existing administrator account.
        if (configuredAdminEmail != null && !configuredAdminEmail.isBlank()
                && configuredAdminPassword != null && !configuredAdminPassword.isBlank()) {
            Optional<User> adminOpt = userRepository.findByEmail(configuredAdminEmail.trim().toLowerCase());
            if (adminOpt.isEmpty()) {
                User admin = new User();
                admin.setName("System Administrator");
                admin.setEmail(configuredAdminEmail.trim().toLowerCase());
                admin.setPassword(passwordEncoder.encode(configuredAdminPassword));
                admin.setPhone(null);
                admin.setEnabled(true);
                admin.addRole(adminRole);
                userRepository.save(admin);
                logger.info("Created the configured administrator account.");
            } else {
                User admin = adminOpt.get();
                admin.setEnabled(true);
                if (admin.getRoles().stream().noneMatch(r -> r.getName() == RoleName.ROLE_ADMIN)) {
                    admin.addRole(adminRole);
                    userRepository.save(admin);
                }
                logger.info("Administrator account already exists; its password was not reset.");
            }
        } else {
            logger.info("Admin bootstrap credentials are not configured. Existing administrator accounts are unchanged.");
        }

        // 3. Initialize/Update Driver User (user@parking.com / user123)
        User user;
        Optional<User> userOpt = userRepository.findByEmail("user@parking.com");
        if (userOpt.isEmpty()) {
            user = new User();
            user.setName("John Doe");
            user.setEmail("user@parking.com");
            user.setPassword(passwordEncoder.encode("user123"));
            user.setPhone("9876543211");
            user.setEnabled(true);
            user.addRole(userRole);
            user = userRepository.save(user);
            logger.info("Created default User account: user@parking.com / user123");
        } else {
            user = userOpt.get();
            user.setPassword(passwordEncoder.encode("user123"));
            user.setEnabled(true);
            if (user.getRoles().isEmpty()) {
                user.addRole(userRole);
            }
            user = userRepository.save(user);
        }

        // 4. Initialize Parking Slots with default Rates (₹20/hr CAR, ₹10/hr BIKE, ₹30/hr EV)
        ParkingSlot slotA1, slotA2;
        if (slotRepository.count() == 0) {
            slotA1 = slotRepository.save(new ParkingSlot("A-01", SlotType.CAR, 20.0, "Ground"));
            slotA2 = slotRepository.save(new ParkingSlot("A-02", SlotType.CAR, 20.0, "Ground"));
            slotRepository.save(new ParkingSlot("A-03", SlotType.CAR, 20.0, "Ground"));
            slotRepository.save(new ParkingSlot("B-01", SlotType.BIKE, 10.0, "Ground"));
            slotRepository.save(new ParkingSlot("B-02", SlotType.BIKE, 10.0, "Ground"));
            slotRepository.save(new ParkingSlot("C-01", SlotType.CAR, 20.0, "1st Floor"));
            slotRepository.save(new ParkingSlot("E-01", SlotType.EV, 30.0, "1st Floor"));
            logger.info("Created default initial parking slots with rates.");
        } else {
            slotA1 = slotRepository.findBySlotNumber("A-01").orElseGet(() -> slotRepository.save(new ParkingSlot("A-01", SlotType.CAR, 20.0, "Ground")));
            slotA2 = slotRepository.findBySlotNumber("A-02").orElseGet(() -> slotRepository.save(new ParkingSlot("A-02", SlotType.CAR, 20.0, "Ground")));
            
            slotRepository.findAll().forEach(slot -> {
                if (slot.getHourlyRate() == null || slot.getHourlyRate() <= 0) {
                    slot.setHourlyRate(20.0);
                    slotRepository.save(slot);
                }
            });
        }

        // 5. Do not seed a sample active booking.
        // Active bookings must be created only by real customers. This prevents the
        // default demo user from starting the application with a slot already occupied.
        // Also clean up the legacy demo booking from earlier project versions, if it exists.
        Optional<Booking> legacyDemoBooking = bookingRepository.findByBookingReference("SPK-889922-1042");
        if (legacyDemoBooking.isPresent()) {
            Booking demoBooking = legacyDemoBooking.get();
            if (demoBooking.getStatus() != BookingStatus.COMPLETED
                    && demoBooking.getStatus() != BookingStatus.CANCELLED) {
                ParkingSlot demoSlot = demoBooking.getParkingSlot();
                demoBooking.setStatus(BookingStatus.CANCELLED);
                demoBooking.setExitTime(LocalDateTime.now());
                bookingRepository.save(demoBooking);
                if (demoSlot != null && demoSlot.getStatus() != SlotStatus.MAINTENANCE) {
                    demoSlot.setStatus(SlotStatus.AVAILABLE);
                    slotRepository.save(demoSlot);
                }
                logger.info("Removed legacy demo parking booking {} and released its slot.",
                        demoBooking.getBookingReference());
            }
        }
    }
}
