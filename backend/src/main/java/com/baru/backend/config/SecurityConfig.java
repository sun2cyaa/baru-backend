package com.baru.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // API 서버라서 보통 끔 (POST 테스트할 때 특히)
                .csrf(csrf -> csrf.disable())

                // 프론트 연결용: /api/** 는 인증 없이 접근 허용
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/**").permitAll()
                        .anyRequest().authenticated()
                )

                // 로그인 페이지로 리다이렉트되는 걸 막고(프론트/백 분리 시 불편)
                .formLogin(form -> form.disable())
                .httpBasic(Customizer.withDefaults());

        return http.build();
    }
}

