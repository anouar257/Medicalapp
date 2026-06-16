package com.medical.payment.repository;

import com.medical.payment.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {
  List<Payment> findByOrganizationIdOrderByPaymentDateDesc(Long organizationId);
  List<Payment> findByPatientIdAndOrganizationIdOrderByPaymentDateDesc(Long patientId, Long organizationId);
  java.util.Optional<Payment> findByAppointmentId(Long appointmentId);
}
