package com.medical.payment.security;

public record PaymentProPrincipal(Long organizationId, String role, String email) {}
