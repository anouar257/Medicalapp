package com.medical.messaging.dto;

import java.util.Arrays;
import java.util.Objects;

/** Fichier prêt à être renvoyé en {@code ResponseEntity} (téléchargement). */
public record AttachmentDownload(String fileName, String fileType, byte[] data) {

    @Override
    public boolean equals(Object o) {
        return this == o || (o instanceof AttachmentDownload that &&
               Objects.equals(this.fileName, that.fileName) &&
               Objects.equals(this.fileType, that.fileType) &&
               Arrays.equals(this.data, that.data));
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(fileName, fileType);
        result = 31 * result + Arrays.hashCode(data);
        return result;
    }

    @Override
    public String toString() {
        return "AttachmentDownload[fileName=" + fileName + ", fileType=" + fileType + ", data=" + Arrays.toString(data) + "]";
    }
}
