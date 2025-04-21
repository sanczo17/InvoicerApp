package org.example.service;

import org.example.exception.ResourceNotFoundException;
import org.example.model.Role;
import org.example.model.User;
import org.example.model.enums.RoleType;
import org.example.repository.RoleRepository;
import org.example.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Testy jednostkowe dla klasy UserService.
 */
@ExtendWith(MockitoExtension.class)
public class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    private User testUser;
    private Role userRole;
    private Role adminRole;

    @BeforeEach
    void setup() {
        // Przygotowanie danych testowych
        userRole = new Role(RoleType.ROLE_USER);
        userRole.setId(1L);

        adminRole = new Role(RoleType.ROLE_ADMIN);
        adminRole.setId(2L);

        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");
        testUser.setPassword("encodedPassword");
        testUser.setActive(true);

        Set<Role> roles = new HashSet<>();
        roles.add(userRole);
        testUser.setRoles(roles);
    }

    @Test
    void findById_WhenUserExists_ShouldReturnUser() {
        // Given
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        // When
        User result = userService.findById(1L);

        // Then
        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("testuser", result.getUsername());
    }

    @Test
    void findById_WhenUserDoesNotExist_ShouldThrowException() {
        // Given
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        // When/Then
        assertThrows(ResourceNotFoundException.class, () -> {
            userService.findById(99L);
        });
    }

    @Test
    void registerNewUser_WhenUsernameAndEmailAreUnique_ShouldRegisterUser() {
        // Given
        User newUser = new User();
        newUser.setUsername("newuser");
        newUser.setEmail("new@example.com");
        newUser.setPassword("password");

        when(userRepository.existsByUsername("newuser")).thenReturn(false);
        when(userRepository.existsByEmail("new@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password")).thenReturn("encodedPassword");
        when(roleRepository.findByName(RoleType.ROLE_USER)).thenReturn(Optional.of(userRole));
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

        // When
        User registeredUser = userService.registerNewUser(newUser);

        // Then
        assertNotNull(registeredUser);
        assertEquals("newuser", registeredUser.getUsername());
        assertEquals("encodedPassword", registeredUser.getPassword());
        assertTrue(registeredUser.isActive());
        assertEquals(1, registeredUser.getRoles().size());
        assertTrue(registeredUser.getRoles().contains(userRole));

        verify(userRepository).save(newUser);
    }

    @Test
    void registerNewUser_WhenUsernameExists_ShouldThrowException() {
        // Given
        User newUser = new User();
        newUser.setUsername("existinguser");
        newUser.setEmail("new@example.com");

        when(userRepository.existsByUsername("existinguser")).thenReturn(true);

        // When/Then
        assertThrows(IllegalArgumentException.class, () -> {
            userService.registerNewUser(newUser);
        });

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void registerNewUser_WhenEmailExists_ShouldThrowException() {
        // Given
        User newUser = new User();
        newUser.setUsername("newuser");
        newUser.setEmail("existing@example.com");

        when(userRepository.existsByUsername("newuser")).thenReturn(false);
        when(userRepository.existsByEmail("existing@example.com")).thenReturn(true);

        // When/Then
        assertThrows(IllegalArgumentException.class, () -> {
            userService.registerNewUser(newUser);
        });

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void updateUser_WhenUserExists_ShouldUpdateUser() {
        // Given
        User updatedUser = new User();
        updatedUser.setId(1L);
        updatedUser.setEmail("updated@example.com");
        updatedUser.setActive(false);

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

        // When
        User result = userService.updateUser(updatedUser);

        // Then
        assertNotNull(result);
        assertEquals("updated@example.com", result.getEmail());
        assertFalse(result.isActive());
        assertEquals("testuser", result.getUsername()); // Nazwa użytkownika nie powinna się zmienić

        verify(userRepository).save(testUser);
    }

    @Test
    void updateUser_WithNewPassword_ShouldEncodePassword() {
        // Given
        User updatedUser = new User();
        updatedUser.setId(1L);
        updatedUser.setPassword("newPassword");

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(passwordEncoder.encode("newPassword")).thenReturn("newEncodedPassword");
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

        // When
        User result = userService.updateUser(updatedUser);

        // Then
        assertEquals("newEncodedPassword", result.getPassword());

        verify(passwordEncoder).encode("newPassword");
        verify(userRepository).save(testUser);
    }

    @Test
    void grantAdminRole_WhenUserDoesNotHaveAdminRole_ShouldAddRole() {
        // Given
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(roleRepository.findByName(RoleType.ROLE_ADMIN)).thenReturn(Optional.of(adminRole));
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

        // When
        userService.grantAdminRole(1L);

        // Then
        assertEquals(2, testUser.getRoles().size());
        assertTrue(testUser.getRoles().contains(adminRole));

        verify(userRepository).save(testUser);
    }

    @Test
    void revokeAdminRole_WhenUserHasAdminRole_ShouldRemoveRole() {
        // Given
        testUser.getRoles().add(adminRole); // Dodajemy rolę admina

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(roleRepository.findByName(RoleType.ROLE_USER)).thenReturn(Optional.of(userRole));
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

        // When
        userService.revokeAdminRole(1L);

        // Then
        assertEquals(1, testUser.getRoles().size());
        assertFalse(testUser.getRoles().contains(adminRole));
        assertTrue(testUser.getRoles().contains(userRole));

        verify(userRepository).save(testUser);
    }

    @Test
    void changePassword_ShouldEncodeAndSaveNewPassword() {
        // Given
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.encode("newPassword")).thenReturn("newEncodedPassword");
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

        // When
        userService.changePassword("testuser", "newPassword");

        // Then
        assertEquals("newEncodedPassword", testUser.getPassword());
        assertFalse(testUser.isMustChangePassword());

        verify(passwordEncoder).encode("newPassword");
        verify(userRepository).save(testUser);
    }

    @Test
    void changePassword_WhenUserDoesNotExist_ShouldThrowException() {
        // Given
        when(userRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());

        // When/Then
        assertThrows(ResourceNotFoundException.class, () -> {
            userService.changePassword("nonexistent", "newPassword");
        });

        verify(passwordEncoder, never()).encode(anyString());
        verify(userRepository, never()).save(any(User.class));
    }
}