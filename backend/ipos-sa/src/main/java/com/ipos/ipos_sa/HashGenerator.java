package com.ipos.ipos_sa;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class HashGenerator {
  public static void main(String[] args) {
    BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
    System.out.println("Admin1234!: " + encoder.encode("Admin1234!"));
    System.out.println("Director1234!: " + encoder.encode("Director1234!"));
  }
}
