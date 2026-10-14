package com.kwikpay.interledgerpaymentservice.controller;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.ResponseStatus;

import com.kwikpay.interledgerpaymentservice.dto.CreatePaymentRequest;
import com.kwikpay.interledgerpaymentservice.entity.Payment;
import com.kwikpay.interledgerpaymentservice.service.PaymentService;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Payment createPayment(@Valid @RequestBody CreatePaymentRequest request) {
        return paymentService.createPayment(request);
    }

    @GetMapping("/{id}")
    public Payment getPayment(@PathVariable UUID id) {
        return paymentService.getPayment(id);
    }

    @GetMapping("/shop/{shopId}")
    public List<Payment> getPaymentsForShop(@PathVariable String shopId) {
        return paymentService.getPaymentsForShop(shopId);
    }
}
