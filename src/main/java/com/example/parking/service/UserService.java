package com.example.parking.service;

import com.example.parking.dto.UserRegisterDto;
import com.example.parking.model.User;

import java.util.Optional;

/**
 * Service interface for User registration and profile management business logic.
 */
public interface UserService {

    User registerUser(UserRegisterDto registerDto);

    Optional<User> findByEmail(String email);

    User getAuthenticatedUser();
}
