package com.ticket.booking.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class BookingController {

    @GetMapping
    public String getBooking(){
        return "hiBooking";
    }
}
