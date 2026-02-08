package org.ciyex.ehr.util;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class BCryptHasher {
    public static void main(String[] args) {


        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        String password = "Password@123";
        String hash = encoder.encode(password);
        System.out.println(password + " => " + hash);

    }
}