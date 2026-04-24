package com.holidayplanner.bookingservice.client;

import com.holidayplanner.bookingservice.exception.EventServiceException;
import com.holidayplanner.bookingservice.exception.EventTermNotFoundException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.UUID;

@Component
public class EventServiceClient {

    private final RestClient restClient;
    private final String eventServiceUrl;

    public EventServiceClient(
            RestClient.Builder restClientBuilder,
            @Value("${services.event-service.url:http://localhost:8081}") String eventServiceUrl) {
        this.restClient = restClientBuilder.build();
        this.eventServiceUrl = eventServiceUrl;
    }

    public EventTermDetails getEventTerm(UUID eventTermId) {
        String url = eventServiceUrl + "/api/event-terms/" + eventTermId;
        try {
            return restClient.get()
                    .uri(url)
                    .retrieve()
                    .onStatus(HttpStatusCode::is4xxClientError, (req, res) -> {
                        throw new EventTermNotFoundException(eventTermId);
                    })
                    .onStatus(HttpStatusCode::is5xxServerError, (req, res) -> {
                        throw new EventServiceException("Event service returned server error", null);
                    })
                    .body(EventTermDetails.class);
        } catch (EventTermNotFoundException | EventServiceException e) {
            throw e;
        } catch (ResourceAccessException e) {
            throw new EventServiceException("Event service unavailable", e);
        } catch (RestClientException e) {
            throw new EventServiceException("Event service error: " + e.getMessage(), e);
        }
    }
}
