package com.holidayplanner.eventservice.repository;

import com.holidayplanner.shared.model.EventTerm;
import com.holidayplanner.shared.model.EventTermStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface EventTermRepository extends JpaRepository<EventTerm, UUID> {

    List<EventTerm> findByEvent_Id(UUID eventId);

    List<EventTerm> findByStatus(EventTermStatus status);
}
