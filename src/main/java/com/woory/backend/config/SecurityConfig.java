package com.woory.backend.config;

import java.util.Collections;
import java.util.List;

import com.woory.backend.filter.JWTFilter;
import com.woory.backend.service.CustomOAuth2UserService;
import com.woory.backend.utils.JWTUtil;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import lombok.AllArgsConstructor;

@Configuration
// @EnableWebSecurity(debug = true)
@AllArgsConstructor
public class SecurityConfig {

	private final CustomOAuth2UserService oAuth2UserService;
	private final ClientRegistrationRepository clientRegistrationRepository;
	private final JWTUtil jwtUtil;
	private final CustomSuccessHandler successHandler;

	@Bean
	public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
		http
			.csrf(AbstractHttpConfigurer::disable)
			.formLogin(AbstractHttpConfigurer::disable)
			.httpBasic(AbstractHttpConfigurer::disable)
			.cors((cors) -> cors.configurationSource(getCorsConfiguration()))
			.addFilterBefore(new JWTFilter(jwtUtil), UsernamePasswordAuthenticationFilter.class)
			.oauth2Login((oauth2) -> oauth2
				.clientRegistrationRepository(clientRegistrationRepository)
				.userInfoEndpoint(userInfoEndpointConfig ->
					userInfoEndpointConfig.userService(oAuth2UserService))
				.successHandler(successHandler) // 로그인 성공 시에 응답에 토큰 넣어줌
				.failureUrl("https://woory.vercel.app")
			)
			.exceptionHandling(handler ->
				// 인증 없이 이용하려면 401
				handler.defaultAuthenticationEntryPointFor(
					new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED),
					new AntPathRequestMatcher("/v*/**")
				))
			.sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
			.authorizeHttpRequests((auth) -> auth
				.requestMatchers("/", "/error", "swagger-ui/**", "swagger/docs/**").permitAll()  //에러 페이지 허용
				.anyRequest().authenticated());

		return http.build();
	}

	public CorsConfigurationSource getCorsConfiguration() {

		CorsConfiguration configuration = new CorsConfiguration();

		configuration.setAllowedOrigins(List.of("http://localhost:3000", "https://woory.vercel.app"));
		configuration.setAllowedMethods(Collections.singletonList("*"));
		configuration.setAllowCredentials(true);
		configuration.setAllowedHeaders(Collections.singletonList("*"));
		configuration.setMaxAge(3600L);

		configuration.setExposedHeaders(Collections.singletonList("Set-Cookie"));
		configuration.setExposedHeaders(Collections.singletonList("AccessToken"));

		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		source.registerCorsConfiguration("/**", configuration);
		return source;
	}

}

