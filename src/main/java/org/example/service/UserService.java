package org.example.service;

import org.example.model.Role;
import org.example.model.User;
import org.example.model.enums.RoleType;
import org.example.repository.RoleRepository;
import org.example.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public UserService(UserRepository userRepository,
                       RoleRepository roleRepository,
                       PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public List<User> findAll() {
        return userRepository.findAll();
    }

    public Optional<User> findById(Long id) {
        return userRepository.findById(id);
    }

    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    public boolean existsByUsername(String username) {
        return userRepository.existsByUsername(username);
    }

    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }

    @Transactional
    public User registerNewUser(User user) {
        // Sprawdź, czy username i email są unikalne
        if (userRepository.existsByUsername(user.getUsername())) {
            throw new RuntimeException("Nazwa użytkownika już istnieje");
        }

        if (userRepository.existsByEmail(user.getEmail())) {
            throw new RuntimeException("Email już istnieje");
        }

        // Kodowanie hasła
        user.setPassword(passwordEncoder.encode(user.getPassword()));

        // Przypisanie domyślnej roli użytkownika
        Set<Role> roles = new HashSet<>();
        Role userRole = roleRepository.findByName(RoleType.ROLE_USER)
                .orElseThrow(() -> new RuntimeException("Rola USER nie znaleziona"));
        roles.add(userRole);
        user.setRoles(roles);

        // Aktywacja konta
        user.setActive(true);

        return userRepository.save(user);
    }

    @Transactional
    public User updateUser(User user) {
        // Sprawdź, czy użytkownik istnieje
        User existingUser = userRepository.findById(user.getId())
                .orElseThrow(() -> new RuntimeException("Użytkownik nie istnieje"));

        // Aktualizacja tylko dozwolonych pól (nie pozwalamy na zmianę nazwy użytkownika)
        existingUser.setEmail(user.getEmail());

        // Jeśli hasło zostało podane, zakoduj je
        if (user.getPassword() != null && !user.getPassword().isEmpty()) {
            existingUser.setPassword(passwordEncoder.encode(user.getPassword()));
        }

        // Aktualizacja statusu aktywności
        existingUser.setActive(user.isActive());

        // Zapisz zaktualizowane dane
        return userRepository.save(existingUser);
    }

    @Transactional
    public void deleteUser(Long id) {
        userRepository.deleteById(id);
    }

    @Transactional
    public void toggleUserActiveStatus(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Użytkownik nie istnieje"));

        user.setActive(!user.isActive());
        userRepository.save(user);
    }

    @Transactional
    public void grantAdminRole(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Użytkownik nie istnieje"));

        Role adminRole = roleRepository.findByName(RoleType.ROLE_ADMIN)
                .orElseThrow(() -> new RuntimeException("Rola ADMIN nie znaleziona"));

        Set<Role> roles = user.getRoles();
        boolean hasAdminRole = roles.stream()
                .anyMatch(role -> role.getName() == RoleType.ROLE_ADMIN);

        if (!hasAdminRole) {
            roles.add(adminRole);
            user.setRoles(roles);
            userRepository.save(user);
        }
    }

    @Transactional
    public void revokeAdminRole(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Użytkownik nie istnieje"));

        Set<Role> roles = user.getRoles();
        roles.removeIf(role -> role.getName() == RoleType.ROLE_ADMIN);

        Role userRole = roleRepository.findByName(RoleType.ROLE_USER)
                .orElseThrow(() -> new RuntimeException("Rola USER nie znaleziona"));
        roles.add(userRole);

        user.setRoles(roles);
        userRepository.save(user);
    }

    @Transactional
    public void changePassword(String username, String newPassword) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Użytkownik nie istnieje"));
        user.setPassword(passwordEncoder.encode(newPassword));
        user.setMustChangePassword(false);
        userRepository.save(user);
    }
}