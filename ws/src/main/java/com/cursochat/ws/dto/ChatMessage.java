package com.cursochat.ws.dto;

import com.cursochat.ws.data.User;

public record ChatMessage(User from, User to, String text) {
}
