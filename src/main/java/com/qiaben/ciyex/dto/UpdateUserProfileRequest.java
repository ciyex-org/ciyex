package com.qiaben.ciyex.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDate;

@Data
public class UpdateUserProfileRequest {
    private String firstName;
    private String lastName;
    private String email;
    private String phoneNumber;
    @JsonFormat(pattern = "yyyy-MM-dd") //  This tells Jackson how to parse it
    private LocalDate dateOfBirth;

}
