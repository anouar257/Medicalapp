package com.medical.payment.service;

import com.medical.payment.dto.PaymentDTO;
import com.medical.payment.entity.Payment;
import org.springframework.stereotype.Component;

@Component
public class PaymentMapper {
  
  public PaymentDTO toDto(Payment entity) {
    if (entity == null) return null;
    PaymentDTO dto = new PaymentDTO();
    dto.setId(entity.getId());
    dto.setOrganizationId(entity.getOrganizationId());
    dto.setPatientId(entity.getPatientId());
    dto.setPatientName(entity.getPatientName());
    dto.setAmount(entity.getAmount());
    dto.setStatus(entity.getStatus());
    dto.setMethod(entity.getMethod());
    dto.setPaymentDate(entity.getPaymentDate());
    dto.setDescription(entity.getDescription());
    dto.setAppointmentId(entity.getAppointmentId());
    dto.setTotalAmount(entity.getTotalAmount());
    dto.setDisease(entity.getDisease());
    return dto;
  }
}
