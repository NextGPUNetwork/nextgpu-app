package ai.nextgpu.common.util;

import ai.nextgpu.common.model.BaseEntity;
import jakarta.persistence.PreUpdate;

import java.time.LocalDateTime;

public class AuditEntityListener {
    @PreUpdate
    public void onPreUpdate(Object entity){
        if (entity instanceof BaseEntity auditEntity ){
            if(Boolean.TRUE.equals(auditEntity.getVoided()) && auditEntity.getDateVoided() == null){
                auditEntity.setDateVoided(LocalDateTime.now());
            }
            auditEntity.setDateUpdated(LocalDateTime.now());
        }
    }
}
