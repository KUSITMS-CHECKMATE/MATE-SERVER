package server.MATE.global.security.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import server.MATE.domain.auth.jwt.JwtProvider;
import server.MATE.domain.users.repository.UsersRepository;
import server.MATE.global.security.filter.JwtAuthenticationFilter;
import server.MATE.global.security.handler.CustomAccessDeniedHandler;
import server.MATE.global.security.handler.CustomAuthenticationEntryPoint;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final CustomAuthenticationEntryPoint customAuthenticationEntryPoint;
    private final CustomAccessDeniedHandler customAccessDeniedHandler;
    private final JwtProvider jwtProvider;
    private final UsersRepository usersRepository;

    private static final String[] PUBLIC_WHITELIST = {
            "/",
            "/api/v1/auth/toss/login", "/api/v1/auth/reissue", "/api/v1/auth/toss/login/unlink/callback",
            "/api/v1/auth/local-test-token",
            "/swagger-ui/**", "/swagger-ui.html", "/api-docs/**", "/v3/api-docs/**", "/docs/error-codes"
    };

    public static final String[] INFRA_WHITELIST = {
    };

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable())
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(PUBLIC_WHITELIST).permitAll()
                        .requestMatchers(INFRA_WHITELIST).permitAll()
                        .anyRequest().authenticated()
                )
                .addFilterBefore(
                        new JwtAuthenticationFilter(jwtProvider, usersRepository, customAuthenticationEntryPoint),
                        UsernamePasswordAuthenticationFilter.class
                )
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(customAuthenticationEntryPoint)
                        .accessDeniedHandler(customAccessDeniedHandler)
                );

        return http.build();
    }
}
