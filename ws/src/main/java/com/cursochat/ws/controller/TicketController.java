package com.cursochat.ws.controller;

import com.cursochat.ws.services.TickectService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

@CrossOrigin
@RestController
@RequestMapping(path = "v1/ticket")
public class TicketController {

    @Autowired
    private TickectService tickectService;

    @PostMapping
    public Map<String, String> buildTicket(@RequestHeader(HttpHeaders.AUTHORIZATION) String authorization) {
        String token = Optional.ofNullable(authorization).map(it -> it.replace("Bearer ", "")).orElse("");

        String ticket = tickectService.buildAndSaveTicket(token);

        return Map.of("ticket", ticket);
    }
}
