package com.medical.payment.service;

import com.medical.payment.dto.PaymentCreateRequestDTO;
import com.medical.payment.dto.PaymentDTO;
import com.medical.payment.entity.Payment;
import com.medical.payment.entity.PaymentStatus;
import com.medical.payment.repository.PaymentRepository;
import com.medical.payment.security.PaymentProPrincipal;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;

@Service
public class PaymentService {

  private final PaymentRepository repository;
  private final PaymentMapper mapper;

  public PaymentService(PaymentRepository repository, PaymentMapper mapper) {
    this.repository = repository;
    this.mapper = mapper;
  }

  @Transactional(readOnly = true)
  public List<PaymentDTO> getPaymentsForCabinet(PaymentProPrincipal principal) {
    if (principal.organizationId() == null) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Organization required");
    }
    return repository.findByOrganizationIdOrderByPaymentDateDesc(principal.organizationId())
        .stream()
        .map(mapper::toDto)
        .toList();
  }

  @Transactional
  public PaymentDTO createPayment(PaymentProPrincipal principal, PaymentCreateRequestDTO request) {
    if (principal.organizationId() == null) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Organization required");
    }

    Payment payment = new Payment();
    payment.setOrganizationId(principal.organizationId());
    payment.setPatientId(request.getPatientId());
    payment.setPatientName(request.getPatientName());
    payment.setAmount(request.getAmount());
    payment.setStatus(PaymentStatus.COMPLETED); // Default status for new cabinet payments
    payment.setMethod(request.getMethod());
    payment.setPaymentDate(request.getPaymentDate() != null ? request.getPaymentDate() : Instant.now());
    payment.setDescription(request.getDescription());
    payment.setAppointmentId(request.getAppointmentId());
    payment.setTotalAmount(request.getTotalAmount());
    payment.setDisease(request.getDisease());

    return mapper.toDto(repository.save(payment));
  }

  @Transactional
  public PaymentDTO updateStatus(PaymentProPrincipal principal, Long id, PaymentStatus status) {
    Payment payment = repository.findById(id)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Payment not found"));
    
    if (!payment.getOrganizationId().equals(principal.organizationId())) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
    }

    payment.setStatus(status);
    return mapper.toDto(repository.save(payment));
  }
}
