package com.qiaben.ciyex.entity;



import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@Entity
@Table(name = "patient_code_lists")
@EqualsAndHashCode(callSuper = true)
public class PatientCodeList extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    

    @Column(nullable = false, length = 120)
    private String title;

    @Column(name = "order_index")
    private Integer orderIndex = 0;

    @Column(name = "is_default")
    private boolean isDefault = false;

    @Column(nullable = false)
    private boolean active = true;

    @Column(columnDefinition = "text")
    private String notes;

    // Comma-separated codes as stored representation
    @Column(columnDefinition = "text")
    private String codes;

    // audit fields provided by AuditableEntity (stored as LocalDateTime)

    // Maintain public API as OffsetDateTime for compatibility by delegating to AuditableEntity
    public OffsetDateTime getCreatedAt() {
        if (getCreatedDate() == null) return null;
        return getCreatedDate().atOffset(ZoneOffset.UTC);
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        if (createdAt == null) setCreatedDate(null);
        else setCreatedDate(createdAt.toLocalDateTime());
    }

    public OffsetDateTime getUpdatedAt() {
        if (getLastModifiedDate() == null) return null;
        return getLastModifiedDate().atOffset(ZoneOffset.UTC);
    }

    public void setUpdatedAt(OffsetDateTime updatedAt) {
        if (updatedAt == null) setLastModifiedDate(null);
        else setLastModifiedDate(updatedAt.toLocalDateTime());
    }

    // Getters & Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public Integer getOrderIndex() { return orderIndex; }
    public void setOrderIndex(Integer orderIndex) { this.orderIndex = orderIndex; }

    public boolean isDefault() { return isDefault; }
    public void setDefault(boolean aDefault) { isDefault = aDefault; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public String getCodes() { return codes; }
    public void setCodes(String codes) { this.codes = codes; }

    
}
