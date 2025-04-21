package org.example.service;

import org.example.exception.ResourceNotFoundException;
import org.example.model.Role;
import org.example.model.User;
import org.example.model.enums.RoleType;
import org.example.repository.RoleRepository;
import org.example.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Serwis obsługujący operacje na użytkownikach.
 */
@Service
public class UserService {

    private static final Logger logger = LoggerFactory.getLogger(UserService.class);

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

    /**
     * Pobiera listę wszystkich użytkowników.
     */
    public List<User> findAll() {
        return userRepository.findAll();
    }

    /**
     * Pobiera użytkownika po identyfikatorze.
     *
     * @throws ResourceNotFoundException gdy użytkownik nie istnieje
     */
    public User findById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Użytkownik", id));
    }

    /**
     * Pobiera użytkownika po nazwie użytkownika.
     *
     * @throws ResourceNotFoundException gdy użytkownik nie istnieje
     */
    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    /**
     * Sprawdza czy użytkownik o podanej nazwie istnieje.
     */
    public boolean existsByUsername(String username) {
        return userRepository.existsByUsername(username);
    }

    /**
     * Sprawdza czy użytkownik o podanym adresie email istnieje.
     */
    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }

    /**
     * Rejestruje nowego użytkownika.
     *
     * @throws IllegalArgumentException gdy nazwa użytkownika lub email już istnieją
     */
    @Transactional
    public User registerNewUser(User user) {
        // Sprawdź, czy username i email są unikalne
        if (userRepository.existsByUsername(user.getUsername())) {
            throw new IllegalArgumentException("Nazwa użytkownika już istnieje");
        }

        if (userRepository.existsByEmail(user.getEmail())) {
            throw new IllegalArgumentException("Email już istnieje");
        }

        // Kodowanie hasła
        user.setPassword(passwordEncoder.encode(user.getPassword()));

        // Przypisanie domyślnej roli użytkownika
        Set<Role> roles = new HashSet<>();
        Role userRole = roleRepository.findByName(RoleType.ROLE_USER)
                .orElseThrow(() -> new IllegalStateException("Rola USER nie znaleziona"));
        roles.add(userRole);
        user.setRoles(roles);

        // Aktywacja konta
        user.setActive(true);

        logger.info("Rejestracja nowego użytkownika: {}", user.getUsername());
        return userRepository.save(user);
    }

    /**
     * Aktualizuje dane użytkownika (bez zmiany nazwy użytkownika).
     *
     * @throws ResourceNotFoundException gdy użytkownik nie istnieje
     */
    @Transactional
    public User updateUser(User user) {
        // Sprawdź, czy użytkownik istnieje
        User existingUser = userRepository.findById(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Użytkownik", user.getId()));

        // Aktualizacja tylko dozwolonych pól (nie pozwalamy na zmianę nazwy użytkownika)
        existingUser.setEmail(user.getEmail());

        // Jeśli hasło zostało podane, zakoduj je
        if (user.getPassword() != null && !user.getPassword().isEmpty()) {
            existingUser.setPassword(passwordEncoder.encode(user.getPassword()));
        }

        // Aktualizacja statusu aktywności
        existingUser.setActive(user.isActive());

        logger.info("Aktualizacja użytkownika: {}", existingUser.getUsername());
        return userRepository.save(existingUser);
    }

    /**
     * Usuwa użytkownika.
     *
     * @throws ResourceNotFoundException gdy użytkownik nie istnieje
     */
    @Transactional
    public void deleteUser(Long id) {
        if (!userRepository.existsById(id)) {
            throw new ResourceNotFoundException("Użytkownik", id);
        }

        logger.info("Usuwanie użytkownika o ID: {}", id);
        userRepository.deleteById(id);
    }

    /**
     * Przełącza status aktywności użytkownika.
     *
     * @throws ResourceNotFoundException gdy użytkownik nie istnieje
     */
    @Transactional
    public void toggleUserActiveStatus(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Użytkownik", id));

        boolean newStatus = !user.isActive();
        user.setActive(newStatus);

        logger.info("Zmiana statusu aktywności użytkownika {}: {}", user.getUsername(), newStatus);
        userRepository.save(user);
    }

    /**
     * Nadaje uprawnienia administratora użytkownikowi.
     *
     * @throws ResourceNotFoundException gdy użytkownik lub rola nie istnieją
     */
    @Transactional
    public void grantAdminRole(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Użytkownik", userId));

        Role adminRole = roleRepository.findByName(RoleType.ROLE_ADMIN)
                .orElseThrow(() -> new IllegalStateException("Rola ADMIN nie znaleziona"));

        Set<Role> roles = user.getRoles();
        boolean hasAdminRole = roles.stream()
                .anyMatch(role -> role.getName() == RoleType.ROLE_ADMIN);

        if (!hasAdminRole) {
            roles.add(adminRole);
            user.setRoles(roles);
            logger.info("Nadanie uprawnień administratora użytkownikowi: {}", user.getUsername());
            userRepository.save(user);
        }
    }

    /**
     * Odbiera uprawnienia administratora użytkownikowi.
     *
     * @throws ResourceNotFoundException gdy użytkownik lub rola nie istnieją
     */
    @Transactional
    public void revokeAdminRole(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Użytkownik", userId));

        Set<Role> roles = user.getRoles();
        roles.removeIf(role -> role.getName() == RoleType.ROLE_ADMIN);

        Role userRole = roleRepository.findByName(RoleType.ROLE_USER)
                .orElseThrow(() -> new IllegalStateException("Rola USER nie znaleziona"));
        roles.add(userRole);

        user.setRoles(roles);
        logger.info("Odebranie uprawnień administratora użytkownikowi: {}", user.getUsername());
        userRepository.save(user);
    }

    /**
     * Zmienia hasło użytkownika.
     *
     * @throws ResourceNotFoundException gdy użytkownik nie istnieje
     */
    @Transactional
    public void changePassword(String username, String newPassword) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Użytkownik o nazwie " + username + " nie istnieje"));

        user.setPassword(passwordEncoder.encode(newPassword));
        user.setMustChangePassword(false);

        logger.info("Zmiana hasła dla użytkownika: {}", username);
        userRepository.save(user);
    }
}