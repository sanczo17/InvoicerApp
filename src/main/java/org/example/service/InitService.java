package org.example.service;

import org.example.model.Customer;
import org.example.model.Role;
import org.example.model.User;
import org.example.model.enums.RoleType;
import org.example.repository.CustomerRepository;
import org.example.repository.RoleRepository;
import org.example.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;

@Service
public class InitService {

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final CustomerRepository customerRepository;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public InitService(RoleRepository roleRepository,
                       UserRepository userRepository,
                       CustomerRepository customerRepository,
                       PasswordEncoder passwordEncoder) {
        this.roleRepository = roleRepository;
        this.userRepository = userRepository;
        this.customerRepository = customerRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void init() {
        initRoles();

        if (!userRepository.existsByUsername("admin")) {
            createAdminAccount();
        }

        if (customerRepository.count() == 0) {
            createSampleCustomers();
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
        roles.add(roleRepository.findByName(RoleType.ROLE_ADMIN)
                .orElseThrow(() -> new RuntimeException("Rola ADMIN nie znaleziona")));
        admin.setRoles(roles);

        userRepository.save(admin);
    }

    private void createSampleCustomers() {
        // Tworzymy przykładowych klientów
        Customer customer1 = new Customer();
        customer1.setName("Firma ABC Sp. z o.o.");
        customer1.setAddress("ul. Przykładowa 1, 00-001 Warszawa");
        customer1.setNip("1234567890");
        customer1.setEmail("kontakt@firmaabc.pl");
        customer1.setPhone("123-456-789");
        customerRepository.save(customer1);

        Customer customer2 = new Customer();
        customer2.setName("Jan Kowalski - Usługi Informatyczne");
        customer2.setAddress("ul. Programistów 5, 50-001 Wrocław");
        customer2.setNip("0987654321");
        customer2.setEmail("jan.kowalski@example.com");
        customer2.setPhone("987-654-321");
        customerRepository.save(customer2);
    }
}