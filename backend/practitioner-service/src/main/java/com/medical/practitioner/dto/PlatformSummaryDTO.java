package com.medical.practitioner.dto;

/** Agrégats plateforme (cabinets actifs, praticiens, patients) — exposé au super-admin et à l'agenda. */
public class PlatformSummaryDTO {

  private long totalActiveCabinets;
  private long totalPractitionerProfiles;
  private long totalPatients;

  public long getTotalActiveCabinets() {
    return totalActiveCabinets;
  }

  public void setTotalActiveCabinets(long totalActiveCabinets) {
    this.totalActiveCabinets = totalActiveCabinets;
  }

  public long getTotalPractitionerProfiles() {
    return totalPractitionerProfiles;
  }

  public void setTotalPractitionerProfiles(long totalPractitionerProfiles) {
    this.totalPractitionerProfiles = totalPractitionerProfiles;
  }

  public long getTotalPatients() {
    return totalPatients;
  }

  public void setTotalPatients(long totalPatients) {
    this.totalPatients = totalPatients;
  }
}
