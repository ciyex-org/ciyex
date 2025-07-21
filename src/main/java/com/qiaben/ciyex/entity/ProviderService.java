package com.qiaben.ciyex.entity;

import jakarta.persistence.*;
import lombok.*;
import java.util.List;

@Entity
@Table(name = "provider_service")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProviderService {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String serviceName;
    private String price;
    private String description;
    private Boolean isOnline;
    private String hospitalName;
    private String address;
    private String city;
    private String state;
    private String zipCode;
    private String status;
    private String mode;
    private String category;

    @ElementCollection
    private List<String> insuranceAccepted;
}

