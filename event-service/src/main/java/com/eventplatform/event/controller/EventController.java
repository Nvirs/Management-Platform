package com.eventplatform.event.controller;

import com.eventplatform.event.dto.EventRequest;
import com.eventplatform.event.dto.EventResponse;
import com.eventplatform.event.service.EventService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/events")
public class EventController {

	private final EventService eventService;

	public EventController(EventService eventService) {
		this.eventService = eventService;
	}

	@PostMapping
	public ResponseEntity<EventResponse> create(
			@Valid @RequestBody EventRequest request, Authentication authentication) {
		return ResponseEntity.status(HttpStatus.CREATED)
				.body(eventService.create(request, authentication.getName()));
	}

	@GetMapping
	public ResponseEntity<List<EventResponse>> findAll() {
		return ResponseEntity.ok(eventService.findAll());
	}

	@GetMapping("/{id}")
	public ResponseEntity<EventResponse> findById(@PathVariable UUID id) {
		return ResponseEntity.ok(eventService.findById(id));
	}

	@PutMapping("/{id}")
	public ResponseEntity<EventResponse> update(
			@PathVariable UUID id, @Valid @RequestBody EventRequest request, Authentication authentication) {
		return ResponseEntity.ok(eventService.update(id, request, authentication.getName()));
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<Void> delete(@PathVariable UUID id, Authentication authentication) {
		eventService.delete(id, authentication.getName());
		return ResponseEntity.noContent().build();
	}
}