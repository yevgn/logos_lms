package ru.antonov.oauth2_social.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import org.springframework.http.HttpMethod;

import org.springframework.security.authentication.AuthenticationProvider;

import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;

import org.springframework.security.config.http.SessionCreationPolicy;

import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import ru.antonov.oauth2_social.user.entity.Role;
import ru.antonov.oauth2_social.user.repository.UserRepository;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
@EnableMethodSecurity
public class SecurityConfig {
    private static final String[] WHITE_LIST_URL = {
            "/auth/**",
            "/swagger-ui/**",
            "/v3/api-docs/**"
    };

    private final JwtAuthenticationFilter jwtAuthFilter;
    private final AuthenticationProvider authenticationProvider;
    private final UserRepository userRepository;


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
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                //   .cors(Customizer.withDefaults())
                .authorizeHttpRequests(req ->
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
                                .hasAnyRole(Role.ADMIN.name())
                                .requestMatchers(HttpMethod.POST, "/users/batch")
                                .hasAnyRole(Role.ADMIN.name())

                                .requestMatchers(HttpMethod.GET, "/users/id")
                                .authenticated()
                                .requestMatchers(HttpMethod.DELETE, "/users/id")
                                .hasRole(Role.ADMIN.name())

                                .requestMatchers(HttpMethod.GET, "/users/email")
                                .authenticated()
                                .requestMatchers(HttpMethod.DELETE, "/users/email")
                                .hasRole(Role.ADMIN.name())

                                .requestMatchers(HttpMethod.POST, "/groups")
                                .hasAnyRole(Role.ADMIN.name())
                                .requestMatchers(HttpMethod.POST, "/groups/batch")
                                .hasAnyRole(Role.ADMIN.name())

                                .requestMatchers(HttpMethod.DELETE, "/groups/id")
                                .hasAnyRole(Role.ADMIN.name())

                                .requestMatchers(HttpMethod.POST, "/institutions")
                                .hasRole(Role.ADMIN.name())
                                .requestMatchers(HttpMethod.GET, "/institutions")
                                .authenticated()
                                .requestMatchers("/institutions/users")
                                .authenticated()
                                .requestMatchers("/institutions/users/group")
                                .authenticated()

                                .requestMatchers(HttpMethod.POST, "/courses/user-id-list")
                                .hasRole(Role.TUTOR.name())
                                .requestMatchers(HttpMethod.POST, "/courses/group-id-list")
                                .hasRole(Role.TUTOR.name())
                                .requestMatchers(HttpMethod.POST, "/courses/*/add-users/user-id-list")
                                .hasRole(Role.TUTOR.name())
                                .requestMatchers(HttpMethod.POST, "/courses/*/add-users/group-id-list")
                                .hasRole(Role.TUTOR.name())
                                .requestMatchers("/courses/*/remove-users/user-id-list")
                                .hasRole(Role.TUTOR.name())
                                .requestMatchers(HttpMethod.DELETE, "/courses")
                                .hasRole(Role.TUTOR.name())
                                .requestMatchers("/courses/*/edit-name")
                                .hasRole(Role.TUTOR.name())
                                .requestMatchers(HttpMethod.GET, "/courses/institution")
                                .hasRole(Role.ADMIN.name())
                                .requestMatchers(HttpMethod.POST, "/courses/*/materials")
                                .hasRole(Role.TUTOR.name())

                                .requestMatchers(WHITE_LIST_URL)
                                .permitAll()

                                .anyRequest()
                                .authenticated()
                )
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .authenticationProvider(authenticationProvider)
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);;

        return http.build();
    }
}