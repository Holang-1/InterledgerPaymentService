package com.kwikpay.interledgerpaymentservice.service;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.kwikpay.interledgerpaymentservice.dto.CreatePaymentRequest;
import com.kwikpay.interledgerpaymentservice.entity.Payment;
import com.kwikpay.interledgerpaymentservice.repository.PaymentRepository;

import java.util.List;
import java.util.UUID;

@Service
public class PaymentService {

    private final PaymentRepository paymentRepository;

    public PaymentService(PaymentRepository paymentRepository) {
        this.paymentRepository = paymentRepository;
    }

    @Transactional
    public Payment createPayment(CreatePaymentRequest request) {
        Payment payment = new Payment(request.shopId(), request.amount(), request.currency());

        return paymentRepository.save(payment);
    }

    @Transactional(readOnly = true)
    public Payment getPayment(UUID id) {

        return paymentRepository.findById(id).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Payment not found"));
    }

    @Transactional(readOnly = true)
    public List<Payment> getPaymentsForShop(String shopId) {
        return paymentRepository.findByShopIdOrderByCreatedAtDesc(shopId);
    }
}