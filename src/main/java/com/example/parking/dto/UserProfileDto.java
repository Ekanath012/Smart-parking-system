package com.example.parking.dto;

import java.util.Set;

/**
 * Data Transfer Object representing authenticated user profile.
 */
public class UserProfileDto {

    private Long id;
    private String name;
    private String email;
    private String phone;
    private Set<String> roles;

    public UserProfileDto() {
    }

    public UserProfileDto(Long id, String name, String email, String phone, Set<String> roles) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.phone = phone;
        this.roles = roles;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public Set<String> getRoles() {
        return roles;
    }

    public void setRoles(Set<String> roles) {
        this.roles = roles;
    }
}
