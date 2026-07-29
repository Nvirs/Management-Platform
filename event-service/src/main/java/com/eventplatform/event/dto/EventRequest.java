package com.eventplatform.event.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.Instant;

public record EventRequest(

		@NotBlank String title,

		String description,

		String location,

		@NotNull @Future Instant startTime,

		@NotNull Instant endTime,

		@Positive Integer capacity) {
}