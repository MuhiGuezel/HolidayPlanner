package com.holidayplanner.organizationservice.controller;

import com.holidayplanner.shared.model.*;
import com.holidayplanner.organizationservice.service.OrganizationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/organizations")
@RequiredArgsConstructor
public class OrganizationController {

    private final OrganizationService organizationService;

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("OrganizationService is running!");
    }

    // --- Organization Endpoints ---

    @PostMapping
    public ResponseEntity<Organization> createOrganization(
            @RequestParam("name") String name,
            @RequestParam("bankAccount") String bankAccount,
            @RequestParam(value = "bookingStartTime", required = false) LocalDateTime bookingStartTime) {
        return ResponseEntity.ok(organizationService.createOrganization(name, bankAccount, bookingStartTime));
    }

    @GetMapping
    public ResponseEntity<List<Organization>> getAllOrganizations() {
        return ResponseEntity.ok(organizationService.getAllOrganizations());
    }

    @GetMapping("/{organizationId}")
    public ResponseEntity<Organization> getOrganization(@PathVariable("organizationId") UUID organizationId) {
        return ResponseEntity.ok(organizationService.getOrganization(organizationId));
    }

    @PutMapping("/{organizationId}")
    public ResponseEntity<Organization> updateOrganization(
            @PathVariable("organizationId") UUID organizationId,
            @RequestParam("bankAccount") String bankAccount,
            @RequestParam(value = "bookingStartTime", required = false) LocalDateTime bookingStartTime) {
        return ResponseEntity.ok(organizationService.updateOrganization(organizationId, bankAccount, bookingStartTime));
    }

    // --- TeamMember Endpoints ---

    @GetMapping("/{organizationId}/team-members")
    public ResponseEntity<List<TeamMember>> getTeamMembers(@PathVariable("organizationId") UUID organizationId) {
        return ResponseEntity.ok(organizationService.getTeamMembers(organizationId));
    }

    @PostMapping("/{organizationId}/team-members")
    public ResponseEntity<TeamMember> addTeamMember(
            @PathVariable("organizationId") UUID organizationId,
            @RequestParam("userId") UUID userId,
            @RequestParam("firstName") String firstName,
            @RequestParam("lastName") String lastName,
            @RequestParam("email") String email,
            @RequestParam(value = "role", defaultValue = "TEAM_MEMBER") TeamMemberRole role) {
        return ResponseEntity.ok(organizationService.addTeamMember(
                organizationId, userId, firstName, lastName, email, role));
    }

    @DeleteMapping("/team-members/{teamMemberId}")
    public ResponseEntity<Void> removeTeamMember(@PathVariable("teamMemberId") UUID teamMemberId) {
        organizationService.removeTeamMember(teamMemberId);
        return ResponseEntity.noContent().build();
    }

    // --- Sponsor Endpoints ---

    @GetMapping("/{organizationId}/sponsors")
    public ResponseEntity<List<Sponsor>> getSponsors(@PathVariable("organizationId") UUID organizationId) {
        return ResponseEntity.ok(organizationService.getSponsors(organizationId));
    }

    @PostMapping("/{organizationId}/sponsors")
    public ResponseEntity<Sponsor> addSponsor(
            @PathVariable("organizationId") UUID organizationId,
            @RequestParam("name") String name,
            @RequestParam(value = "amount", required = false) BigDecimal amount) {
        return ResponseEntity.ok(organizationService.addSponsor(organizationId, name, amount));
    }

    @DeleteMapping("/sponsors/{sponsorId}")
    public ResponseEntity<Void> removeSponsor(@PathVariable("sponsorId") UUID sponsorId) {
        organizationService.removeSponsor(sponsorId);
        return ResponseEntity.noContent().build();
    }
}
