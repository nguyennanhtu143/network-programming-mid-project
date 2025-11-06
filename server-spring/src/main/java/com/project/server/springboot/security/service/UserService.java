package com.project.server.springboot.security.service;

import com.project.server.springboot.model.User;
import com.project.server.springboot.security.dto.AuthenticatedUserDto;
import com.project.server.springboot.security.dto.RegistrationRequest;
import com.project.server.springboot.security.dto.RegistrationResponse;

/**
 * Created on Ağustos, 2020
 *
 * @author Faruk
 */
public interface UserService {

	User findByUsername(String username);

	RegistrationResponse registration(RegistrationRequest registrationRequest);

	AuthenticatedUserDto findAuthenticatedUserByUsername(String username);

}
