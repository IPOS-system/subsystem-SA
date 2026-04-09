package com.ipos.ipos_sa;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class IposSaApplication {

  public static void main(String[] args) {
    SpringApplication.run(IposSaApplication.class, args);

    // TEMP: Generate Bcrypt hashes for seed passwords
    org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder encoder =
        new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder();
    System.out.println("Admin1234!: " + encoder.encode("Admin1234!"));
    System.out.println("Director1234!: " + encoder.encode("Director1234!"));
  }
}
