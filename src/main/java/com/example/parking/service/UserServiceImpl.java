package com.example.parking.service;

import com.example.parking.dto.UserRegisterDto;
import com.example.parking.exception.DuplicateResourceException;
import com.example.parking.exception.ResourceNotFoundException;
import com.example.parking.exception.UnauthorizedAccessException;
import com.example.parking.model.Role;
import com.example.parking.model.RoleName;
import com.example.parking.model.User;
import com.example.parking.repository.RoleRepository;
import com.example.parking.repository.UserRepository;
import com.example.parking.security.ParkingUserDetails;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Service implementation for managing user registration, authentication lookups, and security session retrieval.
 * Uses constructor injection for dependencies.
 */
@Service
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    public UserServiceImpl(UserRepository userRepository,
                           RoleRepository roleRepository,
                           PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public User registerUser(UserRegisterDto registerDto) {
        // Validate unique email constraint
        if (userRepository.existsByEmail(registerDto.getEmail())) {
            throw new DuplicateResourceException("Email address '" + registerDto.getEmail() + "' is already registered.");
        }

        // Validate password confirmation match
        if (!registerDto.getPassword().equals(registerDto.getConfirmPassword())) {
            throw new IllegalArgumentException("Password and Password Confirmation do not match.");
        }

        // Fetch ROLE_USER from database or initialize default
        Role userRole = roleRepository.findByName(RoleName.ROLE_USER)
                .orElseGet(() -> roleRepository.save(new Role(RoleName.ROLE_USER)));

        // Instantiate new user entity with BCrypt password encoding
        User user = new User();
        user.setName(registerDto.getName().trim());
        user.setEmail(registerDto.getEmail().trim().toLowerCase());
        user.setPassword(passwordEncoder.encode(registerDto.getPassword()));
        user.setPhone(registerDto.getPhone());
        user.setEnabled(true);
        user.addRole(userRole);

        return userRepository.save(user);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmailWithRoles(email);
    }

    @Override
    @Transactional(readOnly = true)
    public User getAuthenticatedUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || authentication.getPrincipal().equals("anonymousUser")) {
            throw new UnauthorizedAccessException("No authenticated user session found.");
        }

        Object principal = authentication.getPrincipal();
        if (principal instanceof ParkingUserDetails userDetails) {
            return userRepository.findById(userDetails.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("User", "id", userDetails.getId()));
        }

        String email = authentication.getName();
        return userRepository.findByEmailWithRoles(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));
    }
}
