package com.medical.agenda.dto;

public class DoctorReviewRequestDTO {

  private Long appointmentId;
  private Integer rating;
  private Integer punctualityRating;
  private String comment;

  public Long getAppointmentId() {
    return appointmentId;
  }

  public void setAppointmentId(Long appointmentId) {
    this.appointmentId = appointmentId;
  }

  public Integer getRating() {
    return rating;
  }

  public void setRating(Integer rating) {
    this.rating = rating;
  }

  public Integer getPunctualityRating() {
    return punctualityRating;
  }

  public void setPunctualityRating(Integer punctualityRating) {
    this.punctualityRating = punctualityRating;
  }

  public String getComment() {
    return comment;
  }

  public void setComment(String comment) {
    this.comment = comment;
  }
}
