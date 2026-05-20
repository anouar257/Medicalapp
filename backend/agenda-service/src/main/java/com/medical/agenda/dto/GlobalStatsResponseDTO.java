package com.medical.agenda.dto;

/** Statistiques globales plateforme (super-admin) — practitioner + agenda. */
public class GlobalStatsResponseDTO {

  private long totalActiveCabinets;
  private long totalPractitionerProfiles;
  private long totalPatients;
  private long totalDoctorsInAgenda;
  private long totalSyncedDoctorsInAgenda;
  private long totalAppointments;

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

  public long getTotalDoctorsInAgenda() {
    return totalDoctorsInAgenda;
  }

  public void setTotalDoctorsInAgenda(long totalDoctorsInAgenda) {
    this.totalDoctorsInAgenda = totalDoctorsInAgenda;
  }

  public long getTotalSyncedDoctorsInAgenda() {
    return totalSyncedDoctorsInAgenda;
  }

  public void setTotalSyncedDoctorsInAgenda(long totalSyncedDoctorsInAgenda) {
    this.totalSyncedDoctorsInAgenda = totalSyncedDoctorsInAgenda;
  }

  public long getTotalAppointments() {
    return totalAppointments;
  }

  public void setTotalAppointments(long totalAppointments) {
    this.totalAppointments = totalAppointments;
  }
}
