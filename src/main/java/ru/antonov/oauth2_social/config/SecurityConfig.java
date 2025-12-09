package ru.antonov.oauth2_social.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import org.springframework.http.HttpMethod;

import org.springframework.security.authentication.AuthenticationProvider;

import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;

import org.springframework.security.config.http.SessionCreationPolicy;

import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import ru.antonov.oauth2_social.user.entity.Role;
import ru.antonov.oauth2_social.user.repository.UserRepository;

import java.util.List;

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

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration corsConfig = new CorsConfiguration();

        corsConfig.setAllowedOriginPatterns(List.of("*"));
        corsConfig.setAllowCredentials(true);
        corsConfig.setAllowedMethods(List.of("*"));
        corsConfig.setAllowedHeaders(List.of("*"));
        corsConfig.setExposedHeaders(List.of("*"));

        UrlBasedCorsConfigurationSource urlBasedConfig = new UrlBasedCorsConfigurationSource();
        urlBasedConfig.registerCorsConfiguration("/**", corsConfig);
        return urlBasedConfig;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
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
                                .requestMatchers(HttpMethod.DELETE, "/users/id")
                                .hasRole(Role.ADMIN.name())
                                .requestMatchers(HttpMethod.DELETE, "/users/email")
                                .hasRole(Role.ADMIN.name())

                                .requestMatchers(HttpMethod.POST, "/groups")
                                .hasAnyRole(Role.ADMIN.name())
                                .requestMatchers(HttpMethod.POST, "/groups/batch")
                                .hasAnyRole(Role.ADMIN.name())
                                .requestMatchers(HttpMethod.DELETE, "/groups/{groupId}")
                                .hasAnyRole(Role.ADMIN.name())

                                .requestMatchers(HttpMethod.POST, "/institutions")
                                .hasRole(Role.ADMIN.name())

                                .requestMatchers(HttpMethod.POST, "/courses/user-id-list")
                                .hasAnyRole(Role.TUTOR.name(), Role.ADMIN.name())
                                .requestMatchers(HttpMethod.POST, "/courses/group-id-list")
                                .hasAnyRole(Role.TUTOR.name(), Role.ADMIN.name())
                                .requestMatchers(HttpMethod.POST, "/courses/{courseId}/add-users/user-id-list")
                                .hasAnyRole(Role.TUTOR.name(), Role.ADMIN.name())
                                .requestMatchers(HttpMethod.POST, "/courses/{courseId}/add-users/group-id-list")
                                .hasAnyRole(Role.TUTOR.name(), Role.ADMIN.name())
                                .requestMatchers("/courses/{courseId}/remove-users/user-id-list")
                                .hasAnyRole(Role.TUTOR.name(), Role.ADMIN.name())
                                .requestMatchers(HttpMethod.DELETE, "/courses")
                                .hasAnyRole(Role.TUTOR.name(), Role.ADMIN.name())
                                .requestMatchers("/courses/{courseId}/edit-name")
                                .hasAnyRole(Role.TUTOR.name(), Role.ADMIN.name())
                                .requestMatchers(HttpMethod.GET, "/courses/institution/{institutionId}")
                                .hasRole(Role.ADMIN.name())
                                .requestMatchers(HttpMethod.POST, "/courses/{courseId}/materials")
                                .hasAnyRole(Role.TUTOR.name(), Role.ADMIN.name())
                                .requestMatchers(HttpMethod.DELETE, "/courses/materials/{materialId}")
                                .hasAnyRole(Role.TUTOR.name(), Role.ADMIN.name())
                                .requestMatchers(HttpMethod.PATCH, "/courses/materials/{materialId}")
                                .hasAnyRole(Role.TUTOR.name(), Role.ADMIN.name())
                                .requestMatchers(HttpMethod.GET, "/courses/{courseId}/users/not")
                                .hasAnyRole(Role.TUTOR.name(), Role.ADMIN.name())

                                .requestMatchers(HttpMethod.POST, "/tasks/**")
                                .hasAnyRole(Role.TUTOR.name(), Role.ADMIN.name())
                                .requestMatchers(HttpMethod.PATCH, "/tasks/**")
                                .hasAnyRole(Role.TUTOR.name(), Role.ADMIN.name())
                                .requestMatchers(HttpMethod.DELETE, "/tasks/**")
                                .hasAnyRole(Role.TUTOR.name(), Role.ADMIN.name())
                                .requestMatchers(HttpMethod.GET, "/tasks/course/{courseId}")
                                .hasAnyRole(Role.TUTOR.name(), Role.ADMIN.name())

                                .requestMatchers(HttpMethod.POST, "/solutions/task/{taskId}")
                                .hasRole(Role.STUDENT.name())
//                                .requestMatchers(HttpMethod.PATCH, "/solutions/{solutionId}/revoke")
//                                .hasRole(Role.STUDENT.name())
                                .requestMatchers(HttpMethod.PATCH, "/solutions/{solutionId}")
                                .hasRole(Role.STUDENT.name())
                                .requestMatchers(HttpMethod.DELETE, "/solutions/{solutionId}")
                                .hasRole(Role.STUDENT.name())
                                .requestMatchers(HttpMethod.PATCH, "/solutions/{solutionId}/review")
                                .hasAnyRole(Role.TUTOR.name(), Role.ADMIN.name())
                                .requestMatchers(HttpMethod.GET, "/solutions/task/{taskId}/batch")
                                .hasAnyRole(Role.ADMIN.name(), Role.TUTOR.name())
                                .requestMatchers(HttpMethod.GET, "/solutions/task/{taskId}/batch/unreviewed")
                                .hasAnyRole(Role.ADMIN.name(), Role.TUTOR.name())
                                .requestMatchers(HttpMethod.GET, "/solutions/task/{taskId}/batch/reviewed")
                                .hasAnyRole(Role.ADMIN.name(), Role.TUTOR.name())
                                .requestMatchers(HttpMethod.GET, "/solutions/course/{courseId}/batch")
                                .hasAnyRole(Role.ADMIN.name(), Role.TUTOR.name())
                                .requestMatchers(HttpMethod.GET, "/solutions/course/{courseId}/batch/unreviewed")
                                .hasAnyRole(Role.ADMIN.name(), Role.TUTOR.name())
                                .requestMatchers(HttpMethod.GET, "/solutions/course/{courseId}/batch/reviewed")
                                .hasAnyRole(Role.ADMIN.name(), Role.TUTOR.name())

                                .requestMatchers(WHITE_LIST_URL)
                                .permitAll()

                                .anyRequest()
                                .authenticated()
                )
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .authenticationProvider(authenticationProvider)
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}