package com.tiba.pts.config;

import com.tiba.pts.user.entity.Role;
import com.tiba.pts.user.entity.User;
import com.tiba.pts.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;

  @Override
  public void run(String... args) {

    if (userRepository.findByUsername("admin").isEmpty()) {

      User admin =
          User.builder()
              .username("admin")
              .password(passwordEncoder.encode("admin"))
              .role(Role.ROLE_ADMIN)
              .build();

      userRepository.save(admin);

      System.out.println("Admin user created.");
    }
  }
}
