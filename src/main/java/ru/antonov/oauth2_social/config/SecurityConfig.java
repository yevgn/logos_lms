package ru.antonov.oauth2_social.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;

import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import ru.antonov.oauth2_social.user.entity.Role;
import ru.antonov.oauth2_social.user.repository.UserRepository;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {
    private static final String[] WHITE_LIST_URL = {
            "/auth/**",
            "/swagger-ui/**",
            "/v3/api-docs/**"
    };

    private final UserRepository userRepository;

    @Bean
    public UserDetailsService userDetailsService() {
        return username -> userRepository.findByEmail(username)
                .orElseThrow(() -> new IllegalArgumentException(
                        String.format("Пользователь с email %s не найден", username)
                ));
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService());
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
//
//    @Bean
//    public CorsFilter corsFilter() {
//        final UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
//        final CorsConfiguration config = new CorsConfiguration();
//        config.setAllowCredentials(true);
//        config.setAllowedOrigins(Collections.singletonList("http://localhost:4200"));
//        config.setAllowedHeaders(Arrays.asList(
//                ORIGIN,
//                CONTENT_TYPE,
//                ACCEPT,
//                AUTHORIZATION
//        ));
//        config.setAllowedMethods(Arrays.asList(
//                GET.name(),
//                POST.name(),
//                DELETE.name(),
//                PUT.name(),
//                PATCH.name()
//        ));
//        source.registerCorsConfiguration("/**", config);
//        return new CorsFilter(source);
//
//    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .authorizeHttpRequests( req ->
                        req
                                .requestMatchers("/auth/logout")
                                .authenticated()
                                .requestMatchers("/auth/enable-tfa")
                                .authenticated()
                                .requestMatchers("/auth/disable-tfa")
                                .authenticated()

                                .requestMatchers("/users/csv/institution")
                                .hasAnyRole(Role.ADMIN.name())
                                .requestMatchers("/users/csv/group")
                                .hasAnyRole(Role.ADMIN.name())
                                .requestMatchers(HttpMethod.POST, "/users")
                                .hasAnyRole( Role.ADMIN.name())
                                .requestMatchers(HttpMethod.POST, "/users/batch")
                                .hasAnyRole( Role.ADMIN.name())

                                .requestMatchers(HttpMethod.GET, "/users/id")
                                .authenticated()
                                .requestMatchers(HttpMethod.DELETE, "/users/id")
                                .hasRole(Role.ADMIN.name())

                                .requestMatchers(HttpMethod.GET, "/users/email")
                                .authenticated()
                                .requestMatchers(HttpMethod.DELETE, "/users/email")
                                .hasRole(Role.ADMIN.name())

                                .requestMatchers(HttpMethod.GET, "/users/group")
                                .authenticated()
                                .requestMatchers(HttpMethod.GET, "/users/institution")
                                .authenticated()

                                .requestMatchers(HttpMethod.POST,"/groups")
                                .hasAnyRole( Role.ADMIN.name())
                                .requestMatchers(HttpMethod.POST,"/groups/batch")
                                .hasAnyRole( Role.ADMIN.name())

                                .requestMatchers(HttpMethod.DELETE,"/groups/id")
                                .hasAnyRole(Role.ADMIN.name())

                                .requestMatchers(WHITE_LIST_URL)
                                .permitAll()

                                .anyRequest()
                                .authenticated()
                );

        return http.build();
    }
}