package com.ticket.payment.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PaymentController {

    @GetMapping
    public String getPayment(){
        return "hiPayment";
    }
}
