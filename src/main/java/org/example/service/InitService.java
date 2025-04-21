package org.example.service;

import org.example.model.Customer;
import org.example.model.Role;
import org.example.model.User;
import org.example.model.enums.RoleType;
import org.example.repository.CustomerRepository;
import org.example.repository.RoleRepository;
import org.example.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

/**
 * Serwis inicjalizujący dane początkowe aplikacji.
 * Tworzy role, domyślne konto administratora i przykładowych klientów.
 */
@Service
public class InitService {

    private static final Logger logger = LoggerFactory.getLogger(InitService.class);

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final CustomerRepository customerRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.admin.username:admin}")
    private String adminUsername;

    @Value("${app.admin.email:admin@example.com}")
    private String adminEmail;

    @Value("${app.admin.default-password:admin}")
    private String adminDefaultPassword;

    @Value("${app.admin.force-password-change:true}")
    private boolean adminForcePasswordChange;

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

    /**
     * Inicjalizuje dane początkowe po uruchomieniu aplikacji.
     */
    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void init() {
        logger.info("Inicjalizacja danych początkowych aplikacji...");

        initRoles();
        initAdminAccount();
        initSampleCustomers();

        logger.info("Inicjalizacja danych zakończona pomyślnie");
    }

    /**
     * Tworzy role użytkowników, jeśli nie istnieją.
     */
    private void initRoles() {
        logger.debug("Inicjalizacja ról użytkowników...");

        if (roleRepository.findByName(RoleType.ROLE_ADMIN).isEmpty()) {
            logger.info("Tworzenie roli ADMIN");
            roleRepository.save(new Role(RoleType.ROLE_ADMIN));
        }

        if (roleRepository.findByName(RoleType.ROLE_USER).isEmpty()) {
            logger.info("Tworzenie roli USER");
            roleRepository.save(new Role(RoleType.ROLE_USER));
        }
    }

    /**
     * Tworzy konto administratora, jeśli nie istnieje.
     */
    private void initAdminAccount() {
        logger.debug("Sprawdzanie konta administratora...");

        // Sprawdzamy, czy konto admina już istnieje
        Optional<User> existingAdmin = userRepository.findByUsername(adminUsername);

        if (existingAdmin.isPresent()) {
            logger.info("Konto administratora {} już istnieje", adminUsername);
            return;
        }

        // Jeśli konto nie istnieje, tworzymy je
        logger.info("Tworzenie konta administratora: {}", adminUsername);

        User admin = new User();
        admin.setUsername(adminUsername);
        admin.setEmail(adminEmail);
        admin.setPassword(passwordEncoder.encode(adminDefaultPassword));
        admin.setActive(true);
        admin.setMustChangePassword(adminForcePasswordChange);

        Set<Role> roles = new HashSet<>();
        Role adminRole = roleRepository.findByName(RoleType.ROLE_ADMIN)
                .orElseThrow(() -> new IllegalStateException("Rola ADMIN nie znaleziona"));
        Role userRole = roleRepository.findByName(RoleType.ROLE_USER)
                .orElseThrow(() -> new IllegalStateException("Rola USER nie znaleziona"));

        roles.add(adminRole);
        roles.add(userRole);
        admin.setRoles(roles);

        userRepository.save(admin);

        logger.info("=======================================================");
        logger.info("Utworzono domyślne konto administratora:");
        logger.info("Użytkownik: {}", adminUsername);
        logger.info("Hasło: {}", adminDefaultPassword);
        if (adminForcePasswordChange) {
            logger.info("Przy pierwszym logowaniu należy zmienić hasło.");
        }
        logger.info("=======================================================");
    }

    /**
     * Tworzy przykładowych klientów, jeśli nie istnieją w bazie.
     */
    private void initSampleCustomers() {
        logger.debug("Sprawdzanie przykładowych klientów...");

        // Sprawdzamy, czy są już klienci w bazie
        if (customerRepository.count() > 0) {
            logger.info("Baza zawiera już klientów. Pomijam inicjalizację przykładowych klientów.");
            return;
        }

        logger.info("Inicjalizacja przykładowych klientów...");

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

        logger.info("Dodano 2 przykładowych klientów");
    }
}