package com.brenogomes.pm.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
                        .anyRequest().permitAll()

                ).csrf(csrf -> csrf
                        .disable()  // ← desabilita CSRF completamente para desenvolvimento
                )
                .headers(headers -> headers
                        .frameOptions(frame -> frame.disable())  // ← libera frames do H2
                );

        return http.build();
    }
}
