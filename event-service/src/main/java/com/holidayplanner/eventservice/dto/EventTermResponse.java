package com.holidayplanner.eventservice.dto;

import com.holidayplanner.shared.model.EventTerm;
import com.holidayplanner.shared.model.EventTermStatus;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
public class EventTermResponse {
    private UUID id;
    private UUID eventId;
    private LocalDateTime startDateTime;
    private LocalDateTime endDateTime;
    private int minParticipants;
    private int maxParticipants;
    private EventTermStatus status;

    public static EventTermResponse from(EventTerm term) {
        EventTermResponse r = new EventTermResponse();
        r.id = term.getId();
        r.eventId = term.getEvent().getId();
        r.startDateTime = term.getStartDateTime();
        r.endDateTime = term.getEndDateTime();
        r.minParticipants = term.getMinParticipants();
        r.maxParticipants = term.getMaxParticipants();
        r.status = term.getStatus();
        return r;
    }
}
