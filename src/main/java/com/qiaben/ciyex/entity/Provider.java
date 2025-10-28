
package com.qiaben.ciyex.entity;
import com.qiaben.ciyex.dto.ProviderDto;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "provider")
public class Provider extends ProviderDto {


    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "org_id")
    private Long orgId;

    @Column(name = "npi")
    private String npi;

    @Column(name = "first_name")
    private String firstName;

    @Column(name = "last_name")
    private String lastName;

    @Column(name = "middle_name")
    private String middleName;

    @Column(name = "prefix")
    private String prefix;

    @Column(name = "suffix")
    private String suffix;

    @Column(name = "gender")
    private String gender;

    @Column(name = "date_of_birth")
    private String dateOfBirth;

    @Column(name = "photo")
    private String photo;

    @Column(name = "email")
    private String email;

    @Column(name = "phone_number")
    private String phoneNumber;

    @Column(name = "mobile_number")
    private String mobileNumber;

    @Column(name = "fax_number")
    private String faxNumber;

    @Column(name = "address")
    private String address;

    @Column(name = "specialty")
    private String specialty;

    @Column(name = "provider_type")
    private String providerType;

    @Column(name = "license_number")
    private String licenseNumber;

    @Column(name = "license_state")
    private String licenseState;

    @Column(name = "license_expiry")
    private String licenseExpiry;

    @Column(name = "external_id")
    private String externalId;

    @Column(name = "created_date")
    private String createdDate;

    @Column(name = "last_modified_date")
    private String lastModifiedDate;

    @Enumerated(EnumType.STRING)
    private ProviderStatus status;

    @PrePersist
    public void prePersist() {
        if (status == null) {
            status = ProviderStatus.ACTIVE;
        }
    }
}
