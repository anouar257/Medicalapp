package com.medical.messaging.dto;

import com.medical.messaging.entity.Subject;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class MessageResponse {

  private Long id;
  private Long senderPatientId;
  private String senderName;
  private Long senderPractitionerProfileId;
  private Long receiverPatientId;
  private String receiverName;
  private Long receiverPractitionerProfileId;
  private Long concernedPersonId;
  private String concernedPersonName;
  private String content;
  private Subject subject;
  private Instant sentAt;
  private boolean read;
  private List<AttachmentSummaryDto> attachments = new ArrayList<>();

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

  public String getSenderName() {
    return senderName;
  }

  public void setSenderName(String senderName) {
    this.senderName = senderName;
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

  public String getReceiverName() {
    return receiverName;
  }

  public void setReceiverName(String receiverName) {
    this.receiverName = receiverName;
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

  public String getConcernedPersonName() {
    return concernedPersonName;
  }

  public void setConcernedPersonName(String concernedPersonName) {
    this.concernedPersonName = concernedPersonName;
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

  public List<AttachmentSummaryDto> getAttachments() {
    return attachments;
  }

  public void setAttachments(List<AttachmentSummaryDto> attachments) {
    this.attachments = attachments;
  }
}
