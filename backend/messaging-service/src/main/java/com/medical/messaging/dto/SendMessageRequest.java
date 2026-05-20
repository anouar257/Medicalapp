package com.medical.messaging.dto;

import com.medical.messaging.entity.Subject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.ArrayList;
import java.util.List;

public class SendMessageRequest {

  @NotNull private MessageDirection direction;

  @NotNull private Long concernedPersonId;

  @NotNull private Subject subject;

  @NotBlank
  @Size(max = 20_000)
  private String content;

  /** Requis si {@link #direction} = PATIENT_TO_PRACTITIONER. */
  private Long receiverPractitionerProfileId;

  /** Requis si {@link #direction} = PRACTITIONER_TO_PATIENT. */
  private Long receiverPatientId;

  private List<@NotNull @Valid AttachmentPayload> attachments = new ArrayList<>();

  public MessageDirection getDirection() {
    return direction;
  }

  public void setDirection(MessageDirection direction) {
    this.direction = direction;
  }

  public Long getConcernedPersonId() {
    return concernedPersonId;
  }

  public void setConcernedPersonId(Long concernedPersonId) {
    this.concernedPersonId = concernedPersonId;
  }

  public Subject getSubject() {
    return subject;
  }

  public void setSubject(Subject subject) {
    this.subject = subject;
  }

  public String getContent() {
    return content;
  }

  public void setContent(String content) {
    this.content = content;
  }

  public Long getReceiverPractitionerProfileId() {
    return receiverPractitionerProfileId;
  }

  public void setReceiverPractitionerProfileId(Long receiverPractitionerProfileId) {
    this.receiverPractitionerProfileId = receiverPractitionerProfileId;
  }

  public Long getReceiverPatientId() {
    return receiverPatientId;
  }

  public void setReceiverPatientId(Long receiverPatientId) {
    this.receiverPatientId = receiverPatientId;
  }

  public List<AttachmentPayload> getAttachments() {
    return attachments;
  }

  public void setAttachments(List<AttachmentPayload> attachments) {
    this.attachments = attachments;
  }
}
