package com.holidayplanner.eventservice.repository;

import com.holidayplanner.shared.model.EventTerm;
import com.holidayplanner.shared.model.EventTermStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface EventTermRepository extends JpaRepository<EventTerm, UUID> {

    List<EventTerm> findByEvent_Id(UUID eventId);

    List<EventTerm> findByStatus(EventTermStatus status);

    @Query("SELECT t FROM EventTerm t JOIN FETCH t.event WHERE t.id = :id")
    Optional<EventTerm> findByIdWithEvent(@Param("id") UUID id);
}
