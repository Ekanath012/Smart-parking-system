package com.example.parking.security;

import com.example.parking.model.User;
import com.example.parking.repository.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * Custom UserDetailsService loading user authentication details from MySQL UserRepository.
 */
@Service
public class ParkingUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public ParkingUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmailWithRoles(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));
        return new ParkingUserDetails(user);
    }
}
