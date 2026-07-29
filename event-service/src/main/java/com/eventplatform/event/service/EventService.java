package com.eventplatform.event.service;

import com.eventplatform.event.dto.EventRequest;
import com.eventplatform.event.dto.EventResponse;
import com.eventplatform.event.exception.EventNotFoundException;
import com.eventplatform.event.exception.NotEventOwnerException;
import com.eventplatform.event.model.Event;
import com.eventplatform.event.repository.EventRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class EventService {

	private final EventRepository eventRepository;

	public EventService(EventRepository eventRepository) {
		this.eventRepository = eventRepository;
	}

	public EventResponse create(EventRequest request, String organizerEmail) {
		Event event = Event.builder()
				.title(request.title())
				.description(request.description())
				.location(request.location())
				.startTime(request.startTime())
				.endTime(request.endTime())
				.capacity(request.capacity())
				.organizerEmail(organizerEmail)
				.build();

		return EventResponse.from(eventRepository.save(event));
	}

	@Transactional(readOnly = true)
	public List<EventResponse> findAll() {
		return eventRepository.findAll().stream()
				.map(EventResponse::from)
				.toList();
	}

	@Transactional(readOnly = true)
	public EventResponse findById(UUID id) {
		return EventResponse.from(getOwnedOrThrow(id));
	}

	public EventResponse update(UUID id, EventRequest request, String requesterEmail) {
		Event event = getOwnedOrThrow(id);
		requireOwner(event, requesterEmail);

		event.setTitle(request.title());
		event.setDescription(request.description());
		event.setLocation(request.location());
		event.setStartTime(request.startTime());
		event.setEndTime(request.endTime());
		event.setCapacity(request.capacity());

		return EventResponse.from(eventRepository.save(event));
	}

	public void delete(UUID id, String requesterEmail) {
		Event event = getOwnedOrThrow(id);
		requireOwner(event, requesterEmail);
		eventRepository.delete(event);
	}

	private Event getOwnedOrThrow(UUID id) {
		return eventRepository.findById(id)
				.orElseThrow(() -> new EventNotFoundException(id));
	}

	private void requireOwner(Event event, String requesterEmail) {
		if (!event.getOrganizerEmail().equals(requesterEmail)) {
			throw new NotEventOwnerException();
		}
	}
}