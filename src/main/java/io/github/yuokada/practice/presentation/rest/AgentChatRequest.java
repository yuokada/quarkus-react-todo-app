package io.github.yuokada.practice.presentation.rest;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AgentChatRequest(
        @NotBlank @Size(max = 100) String sessionId,
        @NotBlank @Size(max = 2_000) String message) {}
