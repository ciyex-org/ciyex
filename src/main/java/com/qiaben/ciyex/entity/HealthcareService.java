package com.qiaben.ciyex.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data // Lombok will generate the getters, setters, equals, hashCode, and toString methods for you.
public class HealthcareService {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String description;
    private String type;
    private String location;

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }



    // Manually add getters and setters if not using Lombok
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }





    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }
}
