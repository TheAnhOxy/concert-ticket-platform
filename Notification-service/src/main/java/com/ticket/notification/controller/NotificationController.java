package com.ticket.notification.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class NotificationController {

    @GetMapping
    public String getNotification(){
        return "hiNotification";
    }
}
