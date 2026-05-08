package com.medical.agenda.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "agenda.doctor-photos")
public class AgendaProperties {

  /**
   * Répertoire sur disque pour les portraits uploadés (créé au démarrage si absent). En prod,
   * pointez vers un volume persistant.
   */
  private String directory = System.getProperty("user.home") + "/.agenda/doctor-photos";

  public String getDirectory() {
    return directory;
  }

  public void setDirectory(String directory) {
    this.directory = directory;
  }
}
