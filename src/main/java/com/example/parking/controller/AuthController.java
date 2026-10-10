package com.example.parking.controller;

import com.example.parking.dto.UserRegisterDto;
import com.example.parking.exception.DuplicateResourceException;
import com.example.parking.service.UserService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

/**
 * Controller handling user authentication routes: Login & Registration.
 */
@Controller
public class AuthController {

    private final UserService userService;
    private final com.example.parking.service.PasswordResetService passwordResetService;

    public AuthController(UserService userService, com.example.parking.service.PasswordResetService passwordResetService) {
        this.userService = userService;
        this.passwordResetService = passwordResetService;
    }

    /**
     * Display Custom Login Page.
     */
    @GetMapping("/login")
    public String showLoginPage(@RequestParam(value = "error", required = false) String error,
                                @RequestParam(value = "logout", required = false) String logout,
                                @RequestParam(value = "registered", required = false) String registered,
                                Model model) {
        if (error != null) {
            model.addAttribute("errorMessage", "Invalid email address or password. Please try again.");
        }
        if (logout != null) {
            model.addAttribute("infoMessage", "You have been logged out successfully.");
        }
        if (registered != null) {
            model.addAttribute("successMessage", "Registration successful! Please sign in with your credentials.");
        }
        return "login";
    }

    @GetMapping("/forgot-password")
    public String showForgotPasswordPage() {
        return "forgot-password";
    }

    @PostMapping("/forgot-password")
    public String requestPasswordReset(@RequestParam("email") String email, RedirectAttributes redirectAttributes) {
        String baseUrl = ServletUriComponentsBuilder.fromCurrentContextPath().build().toUriString();
        boolean sent = passwordResetService.requestReset(email, baseUrl);
        // Deliberately do not reveal whether an email address is registered.
        if (sent) {
            redirectAttributes.addFlashAttribute("successMessage", "If an account exists for that email, a password reset link has been sent.");
        } else {
            redirectAttributes.addFlashAttribute("infoMessage", "If an account exists for that email and email delivery is configured, a password reset link will be sent.");
        }
        return "redirect:/forgot-password";
    }

    @GetMapping("/reset-password")
    public String showResetPasswordPage(@RequestParam("token") String token, Model model) {
        model.addAttribute("token", token);
        model.addAttribute("tokenValid", passwordResetService.isValid(token));
        return "reset-password";
    }

    @PostMapping("/reset-password")
    public String resetPassword(@RequestParam("token") String token,
                                @RequestParam("password") String password,
                                @RequestParam("confirmPassword") String confirmPassword,
                                RedirectAttributes redirectAttributes) {
        if (password == null || password.length() < 6) {
            redirectAttributes.addFlashAttribute("errorMessage", "Password must contain at least 6 characters.");
            return "redirect:/reset-password?token=" + java.net.URLEncoder.encode(token, java.nio.charset.StandardCharsets.UTF_8);
        }
        if (!password.equals(confirmPassword)) {
            redirectAttributes.addFlashAttribute("errorMessage", "Passwords do not match.");
            return "redirect:/reset-password?token=" + java.net.URLEncoder.encode(token, java.nio.charset.StandardCharsets.UTF_8);
        }
        if (!passwordResetService.resetPassword(token, password, confirmPassword)) {
            redirectAttributes.addFlashAttribute("errorMessage", "This reset link is invalid or has expired. Please request a new link.");
            return "redirect:/forgot-password";
        }
        redirectAttributes.addFlashAttribute("successMessage", "Password reset successfully. You can now sign in with your new password.");
        return "redirect:/login";
    }

    /**
     * Display Registration Page.
     */
    @GetMapping("/register")
    public String showRegistrationPage(Model model) {
        model.addAttribute("userDto", new UserRegisterDto());
        return "register";
    }

    /**
     * Process User Registration Form Submission with Validation.
     */
    @PostMapping("/register")
    public String handleUserRegistration(@Valid @ModelAttribute("userDto") UserRegisterDto registerDto,
                                         BindingResult bindingResult,
                                         Model model) {
        if (bindingResult.hasErrors()) {
            return "register";
        }

        try {
            userService.registerUser(registerDto);
            return "redirect:/login?registered=true";
        } catch (DuplicateResourceException | IllegalArgumentException ex) {
            model.addAttribute("errorMessage", ex.getMessage());
            return "register";
        }
    }
}
