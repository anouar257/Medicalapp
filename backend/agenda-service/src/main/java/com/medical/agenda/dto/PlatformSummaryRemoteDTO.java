package com.medical.agenda.dto;

/** Réponse JSON alignée sur le practitioner-service (champs camelCase). */
public class PlatformSummaryRemoteDTO {

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
