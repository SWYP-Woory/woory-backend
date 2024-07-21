package com.woory.backend.config;

import java.io.IOException;
import java.util.stream.Collectors;

import org.springframework.http.ResponseCookie;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import com.woory.backend.dto.CustomOAuth2User;
import com.woory.backend.utils.CookieUtil;
import com.woory.backend.utils.JWTUtil;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class CustomSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {
	private final JWTUtil jwtUtil;

	public CustomSuccessHandler(JWTUtil jwtUtil) {
		this.jwtUtil = jwtUtil;
	}

	@Override
	public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
		Authentication authentication) throws IOException, ServletException {
		CustomOAuth2User user = (CustomOAuth2User)authentication.getPrincipal();

		Long userId = Long.valueOf(user.getName());
		String authorities = authentication.getAuthorities().stream()
			.map(GrantedAuthority::getAuthority)
			.collect(Collectors.joining(","));

		String accessToken = jwtUtil.generateAccessToken(userId, authorities);
		ResponseCookie cookie = CookieUtil.createAccessTokenCookie(accessToken, jwtUtil.getAccTokenExpireTime());
		log.info("accessToken = {}", accessToken);
		response.setHeader("Set-Cookie", cookie.toString());
		response.sendRedirect("http://localhost:3000");
		response.setStatus(HttpServletResponse.SC_OK);
	}
}
