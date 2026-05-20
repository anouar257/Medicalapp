package com.medical.messaging.dto;

/** Fichier prêt à être renvoyé en {@code ResponseEntity} (téléchargement). */
public record AttachmentDownload(String fileName, String fileType, byte[] data) {}
