package com.holidayplanner.identityservice.controller;

import com.holidayplanner.shared.model.Caregiver;
import com.holidayplanner.shared.model.FamilyMember;
import com.holidayplanner.shared.model.User;
import com.holidayplanner.identityservice.command.IdentityCommandService;
import com.holidayplanner.identityservice.composition.IdentityCompositionService;
import com.holidayplanner.identityservice.dto.UserProfileEnrichedResponse;
import com.holidayplanner.identityservice.query.IdentityQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Identity Service Controller.
 * 
 * Routes requests to appropriate services following CQRS pattern:
 * - POST/PATCH/DELETE endpoints → IdentityCommandService (write operations)
 * - GET endpoints → IdentityQueryService (read operations)
 * - Composition GET endpoints → IdentityCompositionService (enriched reads)
 */
@RestController
@RequestMapping("/api/identity")
@RequiredArgsConstructor
public class IdentityController {

    private final IdentityCommandService commandService;
    private final IdentityQueryService queryService;
    private final IdentityCompositionService compositionService;

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
        return ResponseEntity.ok(commandService.registerUser(email, password, phoneNumber, organizationId));
    }

    @GetMapping("/users/{userId}")
    public ResponseEntity<User> getUser(@PathVariable("userId") UUID userId) {
        return ResponseEntity.ok(queryService.getUserById(userId));
    }

    @GetMapping("/users/{userId}/profile")
    public ResponseEntity<UserProfileEnrichedResponse> getUserProfile(@PathVariable("userId") UUID userId) {
        return ResponseEntity.ok(compositionService.getUserProfileEnriched(userId));
    }

    @PatchMapping("/users/{userId}/phone")
    public ResponseEntity<User> updatePhone(
            @PathVariable("userId") UUID userId,
            @RequestParam("phoneNumber") String phoneNumber) {
        return ResponseEntity.ok(commandService.updatePhoneNumber(userId, phoneNumber));
    }

    // --- FamilyMember Endpoints ---

    @GetMapping("/users/{userId}/family-members")
    public ResponseEntity<List<FamilyMember>> getFamilyMembers(@PathVariable("userId") UUID userId) {
        return ResponseEntity.ok(queryService.getFamilyMembers(userId));
    }

    @PostMapping("/users/{userId}/family-members")
    public ResponseEntity<FamilyMember> addFamilyMember(
            @PathVariable("userId") UUID userId,
            @RequestParam("firstName") String firstName,
            @RequestParam("lastName") String lastName,
            @RequestParam("birthDate") LocalDate birthDate,
            @RequestParam("zip") String zip) {
        return ResponseEntity.ok(commandService.addFamilyMember(userId, firstName, lastName, birthDate, zip));
    }

    @PutMapping("/family-members/{memberId}")
    public ResponseEntity<FamilyMember> updateFamilyMember(
            @PathVariable("memberId") UUID memberId,
            @RequestParam("firstName") String firstName,
            @RequestParam("lastName") String lastName,
            @RequestParam("birthDate") LocalDate birthDate,
            @RequestParam("zip") String zip) {
        return ResponseEntity.ok(commandService.updateFamilyMember(memberId, firstName, lastName, birthDate, zip));
    }

    @DeleteMapping("/family-members/{memberId}")
    public ResponseEntity<Void> removeFamilyMember(@PathVariable("memberId") UUID memberId) {
        commandService.removeFamilyMember(memberId);
        return ResponseEntity.noContent().build();
    }

    // --- Caregiver Endpoints ---

    @PostMapping("/caregivers")
    public ResponseEntity<Caregiver> createCaregiver(
            @RequestParam("firstName") String firstName,
            @RequestParam("lastName") String lastName,
            @RequestParam("email") String email,
            @RequestParam("phoneNumber") String phoneNumber) {
        return ResponseEntity.ok(commandService.createCaregiver(firstName, lastName, email, phoneNumber));
    }

    @GetMapping("/caregivers")
    public ResponseEntity<List<Caregiver>> getAllCaregivers() {
        return ResponseEntity.ok(queryService.getAllCaregivers());
    }

    @GetMapping("/caregivers/{caregiverId}")
    public ResponseEntity<Caregiver> getCaregiver(@PathVariable("caregiverId") UUID caregiverId) {
        return ResponseEntity.ok(queryService.getCaregiverById(caregiverId));
    }
}
