package com.holidayplanner.identityservice.controller;

import com.holidayplanner.shared.model.Caregiver;
import com.holidayplanner.shared.model.FamilyMember;
import com.holidayplanner.shared.model.User;
import com.holidayplanner.identityservice.service.IdentityService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/identity")
@RequiredArgsConstructor
public class IdentityController {

    private final IdentityService identityService;

    // Hello World endpoint
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("IdentityService is running!");
    }

    // --- User Endpoints ---

    @PostMapping("/users/register")
    public ResponseEntity<User> register(
            @RequestParam("email") String email,
            @RequestParam("password") String password,
            @RequestParam("phoneNumber") String phoneNumber,
            @RequestParam("organizationId") UUID organizationId) {
        return ResponseEntity.ok(identityService.registerUser(email, password, phoneNumber, organizationId));
    }

    @GetMapping("/users/{userId}")
    public ResponseEntity<User> getUser(@PathVariable("userId") UUID userId) {
        return ResponseEntity.ok(identityService.getUserById(userId));
    }

    @PatchMapping("/users/{userId}/phone")
    public ResponseEntity<User> updatePhone(
            @PathVariable("userId") UUID userId,
            @RequestParam("phoneNumber") String phoneNumber) {
        return ResponseEntity.ok(identityService.updatePhoneNumber(userId, phoneNumber));
    }

    // --- FamilyMember Endpoints ---

    @GetMapping("/users/{userId}/family-members")
    public ResponseEntity<List<FamilyMember>> getFamilyMembers(@PathVariable("userId") UUID userId) {
        return ResponseEntity.ok(identityService.getFamilyMembers(userId));
    }

    @PostMapping("/users/{userId}/family-members")
    public ResponseEntity<FamilyMember> addFamilyMember(
            @PathVariable("userId") UUID userId,
            @RequestParam("firstName") String firstName,
            @RequestParam("lastName") String lastName,
            @RequestParam("birthDate") LocalDate birthDate,
            @RequestParam("zip") String zip) {
        return ResponseEntity.ok(identityService.addFamilyMember(userId, firstName, lastName, birthDate, zip));
    }

    @PutMapping("/family-members/{memberId}")
    public ResponseEntity<FamilyMember> updateFamilyMember(
            @PathVariable("memberId") UUID memberId,
            @RequestParam("firstName") String firstName,
            @RequestParam("lastName") String lastName,
            @RequestParam("birthDate") LocalDate birthDate,
            @RequestParam("zip") String zip) {
        return ResponseEntity.ok(identityService.updateFamilyMember(memberId, firstName, lastName, birthDate, zip));
    }

    @DeleteMapping("/family-members/{memberId}")
    public ResponseEntity<Void> removeFamilyMember(@PathVariable("memberId") UUID memberId) {
        identityService.removeFamilyMember(memberId);
        return ResponseEntity.noContent().build();
    }

    // --- Caregiver Endpoints ---

    @PostMapping("/caregivers")
    public ResponseEntity<Caregiver> createCaregiver(
            @RequestParam("firstName") String firstName,
            @RequestParam("lastName") String lastName,
            @RequestParam("email") String email,
            @RequestParam("phoneNumber") String phoneNumber) {
        return ResponseEntity.ok(identityService.createCaregiver(firstName, lastName, email, phoneNumber));
    }

    @GetMapping("/caregivers")
    public ResponseEntity<List<Caregiver>> getAllCaregivers() {
        return ResponseEntity.ok(identityService.getAllCaregivers());
    }

    @GetMapping("/caregivers/{caregiverId}")
    public ResponseEntity<Caregiver> getCaregiver(@PathVariable("caregiverId") UUID caregiverId) {
        return ResponseEntity.ok(identityService.getCaregiverById(caregiverId));
    }
}
