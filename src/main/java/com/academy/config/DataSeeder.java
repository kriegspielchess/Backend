package com.academy.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.academy.entity.Course;
import com.academy.entity.Role;
import com.academy.entity.User;
import com.academy.repository.CourseRepository;
import com.academy.repository.UserRepository;

@Configuration
public class DataSeeder {

    @Bean
    CommandLineRunner seedData(CourseRepository courseRepository,
                               UserRepository userRepository,
                               PasswordEncoder passwordEncoder) {
        return args -> {
            if (courseRepository.count() == 0) {
                courseRepository.save(Course.builder()
                        .title("Beginner")
                        .description("Learn the rules, piece movement, basic tactics, and simple strategies with a friendly approach for newcomers.")
                        .instructor("Nathasha Sanali")
                        .durationHours(40)
                        .price(199.0)
                        .build());
                courseRepository.save(Course.builder()
                        .title("Intermediate")
                        .description("Build on fundamentals with opening principles, middlegame planning, and tactical patterns for improving players.")
                        .instructor("FI Gihan De Silva")
                        .durationHours(32)
                        .price(249.0)
                        .build());
                courseRepository.save(Course.builder()
                        .title("Advanced")
                        .description("Deep strategic concepts, complex endgames, and tournament preparation for serious, rated players.")
                        .instructor("FI Gihan De Silva")
                        .durationHours(28)
                        .price(179.0)
                        .build());
            }

            if (userRepository.count() == 0) {
                String password = passwordEncoder.encode("password");
                String adminPassword = passwordEncoder.encode("admin123");
                userRepository.save(User.builder()
                        .name("Student User")
                        .email("student@kriegspiel.com")
                        .password(password)
                        .role(Role.STUDENT)
                        .active(true)
                        .profileStatus(com.academy.entity.StudentProfileStatus.VERIFIED)
                        .className("Beginner")
                        .teacherName("Nathasha Sanali")
                        .build());
                userRepository.save(User.builder()
                        .name("Teacher User")
                        .email("teacher@kriegspiel.com")
                        .password(password)
                        .role(Role.TEACHER)
                        .active(true)
                        .build());
                userRepository.save(User.builder()
                        .name("Admin User")
                        .email("admin@kriegspiel.com")
                        .password(adminPassword)
                        .role(Role.ADMIN)
                        .active(true)
                        .build());
                userRepository.save(User.builder()
                        .name("Super Admin User")
                        .email("superadmin@kriegspiel.com")
                        .password(password)
                        .role(Role.SUPER_ADMIN)
                        .active(true)
                        .build());
            }
        };
    }
}
