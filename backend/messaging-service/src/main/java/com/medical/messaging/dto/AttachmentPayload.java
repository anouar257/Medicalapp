package com.medical.messaging.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class AttachmentPayload {

  @NotBlank
  @Size(max = 512)
  private String fileName;

  @NotBlank
  @Size(max = 255)
  private String fileType;

  /** Contenu encodé Base64 (standard ou URL-safe). */
  @NotBlank private String base64Data;

  public String getFileName() {
    return fileName;
  }

  public void setFileName(String fileName) {
    this.fileName = fileName;
  }

  public String getFileType() {
    return fileType;
  }

  public void setFileType(String fileType) {
    this.fileType = fileType;
  }

  public String getBase64Data() {
    return base64Data;
  }

  public void setBase64Data(String base64Data) {
    this.base64Data = base64Data;
  }
}
