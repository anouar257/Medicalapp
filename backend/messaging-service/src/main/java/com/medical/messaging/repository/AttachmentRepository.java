package com.medical.messaging.repository;

import com.medical.messaging.entity.Attachment;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AttachmentRepository extends JpaRepository<Attachment, Long> {

  Optional<Attachment> findByIdAndMessageId(Long attachmentId, Long messageId);
}
