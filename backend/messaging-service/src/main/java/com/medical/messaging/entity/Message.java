package com.medical.messaging.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "messages")
public class Message {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  /** Compte patient expéditeur (null si l’expéditeur est un praticien). */
  @Column(name = "sender_patient_id")
  private Long senderPatientId;

  /** Profil praticien expéditeur ({@code PractitionerProfile.id}, aligné agenda). */
  @Column(name = "sender_practitioner_profile_id")
  private Long senderPractitionerProfileId;

  @Column(name = "receiver_patient_id")
  private Long receiverPatientId;

  @Column(name = "receiver_practitioner_profile_id")
  private Long receiverPractitionerProfileId;

  /** Patient ou proche concerné par le message. */
  @Column(name = "concerned_person_id", nullable = false)
  private Long concernedPersonId;

  @Column(nullable = false, length = 20_000)
  private String content;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 40)
  private Subject subject;

  @Column(name = "sent_at", nullable = false)
  private Instant sentAt = Instant.now();

  /** Lu par le destinataire (patient ou praticien selon le sens). */
  @Column(name = "read_flag", nullable = false)
  private boolean read;

  @OneToMany(
      mappedBy = "message",
      cascade = CascadeType.ALL,
      orphanRemoval = true,
      fetch = FetchType.LAZY)
  private List<Attachment> attachments = new ArrayList<>();

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public Long getSenderPatientId() {
    return senderPatientId;
  }

  public void setSenderPatientId(Long senderPatientId) {
    this.senderPatientId = senderPatientId;
  }

  public Long getSenderPractitionerProfileId() {
    return senderPractitionerProfileId;
  }

  public void setSenderPractitionerProfileId(Long senderPractitionerProfileId) {
    this.senderPractitionerProfileId = senderPractitionerProfileId;
  }

  public Long getReceiverPatientId() {
    return receiverPatientId;
  }

  public void setReceiverPatientId(Long receiverPatientId) {
    this.receiverPatientId = receiverPatientId;
  }

  public Long getReceiverPractitionerProfileId() {
    return receiverPractitionerProfileId;
  }

  public void setReceiverPractitionerProfileId(Long receiverPractitionerProfileId) {
    this.receiverPractitionerProfileId = receiverPractitionerProfileId;
  }

  public Long getConcernedPersonId() {
    return concernedPersonId;
  }

  public void setConcernedPersonId(Long concernedPersonId) {
    this.concernedPersonId = concernedPersonId;
  }

  public String getContent() {
    return content;
  }

  public void setContent(String content) {
    this.content = content;
  }

  public Subject getSubject() {
    return subject;
  }

  public void setSubject(Subject subject) {
    this.subject = subject;
  }

  public Instant getSentAt() {
    return sentAt;
  }

  public void setSentAt(Instant sentAt) {
    this.sentAt = sentAt;
  }

  public boolean isRead() {
    return read;
  }

  public void setRead(boolean read) {
    this.read = read;
  }

  public List<Attachment> getAttachments() {
    return attachments;
  }

  public void setAttachments(List<Attachment> attachments) {
    this.attachments = attachments;
  }

  public void addAttachment(Attachment a) {
    attachments.add(a);
    a.setMessage(this);
  }
}
