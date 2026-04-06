package com.holidayplanner.organizationservice.service;

import com.holidayplanner.organizationservice.model.*;
import com.holidayplanner.organizationservice.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OrganizationService {

    private final OrganizationRepository organizationRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final SponsorRepository sponsorRepository;

    // --- Organization Operations ---

    public Organization createOrganization(String name, String bankAccount,
                                           LocalDateTime bookingStartTime) {
        if (organizationRepository.existsByName(name)) {
            throw new RuntimeException("Organization already exists: " + name);
        }
        Organization org = new Organization();
        org.setName(name);
        org.setBankAccount(bankAccount);
        org.setBookingStartTime(bookingStartTime);
        return organizationRepository.save(org);
    }

    public Organization updateOrganization(UUID organizationId, String bankAccount,
                                           LocalDateTime bookingStartTime) {
        Organization org = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new RuntimeException("Organization not found: " + organizationId));
        org.setBankAccount(bankAccount);
        org.setBookingStartTime(bookingStartTime);
        return organizationRepository.save(org);
    }

    public Organization getOrganization(UUID organizationId) {
        return organizationRepository.findById(organizationId)
                .orElseThrow(() -> new RuntimeException("Organization not found: " + organizationId));
    }

    public List<Organization> getAllOrganizations() {
        return organizationRepository.findAll();
    }

    // --- TeamMember Operations ---

    public TeamMember addTeamMember(UUID organizationId, UUID userId, String firstName,
                                    String lastName, String email, TeamMemberRole role) {
        Organization org = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new RuntimeException("Organization not found: " + organizationId));

        TeamMember member = new TeamMember();
        member.setOrganization(org);
        member.setUserId(userId);
        member.setFirstName(firstName);
        member.setLastName(lastName);
        member.setEmail(email);
        member.setRole(role);
        return teamMemberRepository.save(member);
    }

    public void removeTeamMember(UUID teamMemberId) {
        teamMemberRepository.deleteById(teamMemberId);
    }

    public List<TeamMember> getTeamMembers(UUID organizationId) {
        return teamMemberRepository.findByOrganization_Id(organizationId);
    }

    // --- Sponsor Operations ---

    public Sponsor addSponsor(UUID organizationId, String name, BigDecimal amount) {
        Organization org = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new RuntimeException("Organization not found: " + organizationId));

        Sponsor sponsor = new Sponsor();
        sponsor.setOrganization(org);
        sponsor.setName(name);
        sponsor.setAmount(amount);
        return sponsorRepository.save(sponsor);
    }

    public void removeSponsor(UUID sponsorId) {
        sponsorRepository.deleteById(sponsorId);
    }

    public List<Sponsor> getSponsors(UUID organizationId) {
        return sponsorRepository.findByOrganization_Id(organizationId);
    }
}
