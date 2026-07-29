package com.eventplatform.event.dto;

import com.eventplatform.event.model.Event;

import java.time.Instant;
import java.util.UUID;

public record EventResponse(
		UUID id,
		String title,
		String description,
		String location,
		Instant startTime,
		Instant endTime,
		Integer capacity,
		String organizerEmail,
		Instant createdAt,
		Instant updatedAt) {

	public static EventResponse from(Event event) {
		return new EventResponse(
				event.getId(),
				event.getTitle(),
				event.getDescription(),
				event.getLocation(),
				event.getStartTime(),
				event.getEndTime(),
				event.getCapacity(),
				event.getOrganizerEmail(),
				event.getCreatedAt(),
				event.getUpdatedAt());
	}
}