package com.medical.payment.controller;

import com.medical.payment.dto.PaymentCreateRequestDTO;
import com.medical.payment.dto.PaymentDTO;
import com.medical.payment.entity.PaymentStatus;
import com.medical.payment.security.PaymentProPrincipal;
import com.medical.payment.service.PaymentService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

  private final PaymentService paymentService;

  public PaymentController(PaymentService paymentService) {
    this.paymentService = paymentService;
  }

  private PaymentProPrincipal getPrincipal(Authentication auth) {
    if (auth == null || !(auth.getPrincipal() instanceof PaymentProPrincipal)) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid token");
    }
    return (PaymentProPrincipal) auth.getPrincipal();
  }

  @GetMapping("/cabinet")
  public List<PaymentDTO> getCabinetPayments(Authentication auth) {
    return paymentService.getPaymentsForCabinet(getPrincipal(auth));
  }

  @PostMapping
  public PaymentDTO createPayment(Authentication auth, @RequestBody PaymentCreateRequestDTO request) {
    return paymentService.createPayment(getPrincipal(auth), request);
  }

  @PatchMapping("/{id}/status")
  public PaymentDTO updateStatus(Authentication auth, @PathVariable Long id, @RequestBody StatusUpdateDTO statusUpdate) {
    return paymentService.updateStatus(getPrincipal(auth), id, statusUpdate.status());
  }

  public record StatusUpdateDTO(PaymentStatus status) {}
}
