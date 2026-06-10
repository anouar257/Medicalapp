package com.medical.payment.dto;

import com.medical.payment.entity.PaymentMethod;
import com.medical.payment.entity.PaymentStatus;
import java.time.Instant;

public class PaymentDTO {
  private Long id;
  private Long organizationId;
  private Long patientId;
  private String patientName;
  private Double amount;
  private PaymentStatus status;
  private PaymentMethod method;
  private Instant paymentDate;
  private String description;
  private Long appointmentId;
  private Double totalAmount;
  private String disease;

  // Getters and Setters
  public Long getId() { return id; }
  public void setId(Long id) { this.id = id; }
  
  public Long getOrganizationId() { return organizationId; }
  public void setOrganizationId(Long organizationId) { this.organizationId = organizationId; }
  
  public Long getPatientId() { return patientId; }
  public void setPatientId(Long patientId) { this.patientId = patientId; }
  
  public String getPatientName() { return patientName; }
  public void setPatientName(String patientName) { this.patientName = patientName; }
  
  public Double getAmount() { return amount; }
  public void setAmount(Double amount) { this.amount = amount; }
  
  public PaymentStatus getStatus() { return status; }
  public void setStatus(PaymentStatus status) { this.status = status; }
  
  public PaymentMethod getMethod() { return method; }
  public void setMethod(PaymentMethod method) { this.method = method; }
  
  public Instant getPaymentDate() { return paymentDate; }
  public void setPaymentDate(Instant paymentDate) { this.paymentDate = paymentDate; }
  
  public String getDescription() { return description; }
  public void setDescription(String description) { this.description = description; }
  
  public Long getAppointmentId() { return appointmentId; }
  public void setAppointmentId(Long appointmentId) { this.appointmentId = appointmentId; }
  
  public Double getTotalAmount() { return totalAmount; }
  public void setTotalAmount(Double totalAmount) { this.totalAmount = totalAmount; }
  
  public String getDisease() { return disease; }
  public void setDisease(String disease) { this.disease = disease; }
}
