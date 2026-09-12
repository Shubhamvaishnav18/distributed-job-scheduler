package com.jobscheduler.job_scheduler.service.impl;

import com.jobscheduler.job_scheduler.dto.request.CreateUserRequest;
import com.jobscheduler.job_scheduler.dto.request.LoginRequest;
import com.jobscheduler.job_scheduler.dto.response.AuthResponse;

import com.jobscheduler.job_scheduler.security.CustomUserDetailsService;
import com.jobscheduler.job_scheduler.security.JwtService;
import com.jobscheduler.job_scheduler.service.AuthService;
import com.jobscheduler.job_scheduler.service.UserService;
import lombok.RequiredArgsConstructor;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl
        implements AuthService {

    private final UserService userService;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final CustomUserDetailsService userDetailsService;

    @Override
    @Transactional
    public AuthResponse register(
            CreateUserRequest request
    ) {

        userService.createUser(request);

        UserDetails userDetails =
                userDetailsService.loadUserByUsername(
                        request.username()
                );

        return new AuthResponse(
                jwtService.generateToken(userDetails)
        );
    }

    @Override
    public AuthResponse login(
            LoginRequest request
    ) {

        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.username(),
                        request.password()
                )
        );

        UserDetails user =
                userDetailsService.loadUserByUsername(
                        request.username()
                );

        return new AuthResponse(
                jwtService.generateToken(user)
        );
    }
}
