package com.cursochat.ws.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.logging.Logger;

@RestController
public class HealthCheckController {

    private final static Logger logger = Logger.getLogger(HealthCheckController.class.getName());

    @GetMapping
    void heathCheck() {
        logger.info("HEALTH CHECK");
    }
}
