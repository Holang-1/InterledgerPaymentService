package com.kwikpay.interledgerpaymentservice.repository;


import org.springframework.data.jpa.repository.JpaRepository;

import com.kwikpay.interledgerpaymentservice.entity.Payment;

import java.util.List;
import java.util.UUID;

public interface PaymentRepository extends JpaRepository<Payment, UUID> {

    List<Payment> findByShopIdOrderByCreatedAtDesc(String shopId);
}
