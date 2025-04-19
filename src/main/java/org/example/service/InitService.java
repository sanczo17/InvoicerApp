package org.example.service;

import org.example.model.Role;
import org.example.model.enums.RoleType;
import org.example.model.User;
import org.example.repository.RoleRepository;
import org.example.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Set;

@Service
public class InitService {

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @EventListener(ApplicationReadyEvent.class)
    public void initRolesAndAdmin() {
        initRoles();

        if (!userRepository.existsByUsername("admin")) {
            createAdminAccount();
        }
    }

    private void initRoles() {
        if (roleRepository.findByName(RoleType.ROLE_ADMIN).isEmpty()) {
            roleRepository.save(new Role(RoleType.ROLE_ADMIN));
        }

        if (roleRepository.findByName(RoleType.ROLE_USER).isEmpty()) {
            roleRepository.save(new Role(RoleType.ROLE_USER));
        }
    }

    private void createAdminAccount() {
        User admin = new User();
        admin.setUsername("admin");
        admin.setEmail("admin@example.com");
        admin.setPassword(passwordEncoder.encode("admin"));
        admin.setActive(true);

        Set<Role> roles = new HashSet<>();
        roles.add(roleRepository.findByName(RoleType.ROLE_ADMIN).get());
        admin.setRoles(roles);

        userRepository.save(admin);
    }
}