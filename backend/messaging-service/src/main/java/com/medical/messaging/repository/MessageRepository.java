package com.medical.messaging.repository;

import com.medical.messaging.entity.Message;
import java.util.List;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MessageRepository extends JpaRepository<Message, Long> {

  @Query(
      """
      SELECT m FROM Message m
      WHERE m.senderPatientId = :pid OR m.receiverPatientId = :pid OR m.concernedPersonId = :pid
      ORDER BY m.sentAt DESC
      """)
  List<Message> findPatientMailbox(@Param("pid") Long patientUserId);

  @Query(
      """
      SELECT m FROM Message m
      WHERE m.senderPractitionerProfileId = :ppid OR m.receiverPractitionerProfileId = :ppid
      ORDER BY m.sentAt DESC
      """)
  List<Message> findPractitionerMailbox(@Param("ppid") Long practitionerProfileId);

  @Query(
      """
      SELECT m FROM Message m
      WHERE (m.senderPatientId = :patientId AND m.receiverPractitionerProfileId = :practitionerId)
         OR (m.senderPractitionerProfileId = :practitionerId AND m.receiverPatientId = :patientId)
      ORDER BY m.sentAt DESC
      """)
  List<Message> findConversation(
      @Param("patientId") Long patientId,
      @Param("practitionerId") Long practitionerId,
      Limit limit);
}

