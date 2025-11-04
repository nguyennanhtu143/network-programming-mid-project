package dev.p3ntest.zombies.service;

import dev.p3ntest.zombies.entity.User;
import dev.p3ntest.zombies.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    @Transactional
    public User upsertUserFromJwt(Jwt jwt) {
        String userId = jwt.getSubject();
        String scope = jwt.getClaimAsString("scope");
        List<String> scopePermissions = scope != null 
            ? Arrays.asList(scope.split(" ")) 
            : List.of();

        return userRepository.findById(userId)
            .map(user -> {
                user.setScopePermissions(scopePermissions);
                return userRepository.save(user);
            })
            .orElseGet(() -> {
                User newUser = new User();
                newUser.setId(userId);
                newUser.setName("Anonymous");
                newUser.setScopePermissions(scopePermissions);
                return userRepository.save(newUser);
            });
    }

    public User getUserFromJwt(Jwt jwt) {
        if (jwt == null) {
            return null;
        }
        return upsertUserFromJwt(jwt);
    }
}


