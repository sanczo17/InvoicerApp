package org.example.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.example.model.*;
import org.example.model.enums.InvoiceStatus;
import org.example.model.enums.PaymentMethod;
import org.example.model.enums.RoleType;
import org.example.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.*;

/**
 * Serwis odpowiedzialny za tworzenie kopii zapasowych danych systemu i ich przywracanie.
 * Eksportuje dane w formie uproszczonych DTO do pliku JSON.
 */
@Service
public class BackupService {

    private static final Logger logger = LoggerFactory.getLogger(BackupService.class);

    private final UserRepository userRepository;
    private final CustomerRepository customerRepository;
    private final InvoiceRepository invoiceRepository;
    private final CompanyRepository companyRepository;
    private final RoleRepository roleRepository;
    private final LoginAuditRepository loginAuditRepository;
    private final PasswordEncoder passwordEncoder;

    private Map<Long, Long> customerIdMapping; // Przechowuje mapowanie ID klientów (stare->nowe)
    private Map<Long, Long> roleIdMapping = new HashMap<>(); // Przechowuje mapowanie ID ról (stare->nowe)

    @Autowired
    public BackupService(UserRepository userRepository,
                         CustomerRepository customerRepository,
                         InvoiceRepository invoiceRepository,
                         CompanyRepository companyRepository,
                         RoleRepository roleRepository,
                         LoginAuditRepository loginAuditRepository,
                         PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.customerRepository = customerRepository;
        this.invoiceRepository = invoiceRepository;
        this.companyRepository = companyRepository;
        this.roleRepository = roleRepository;
        this.loginAuditRepository = loginAuditRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Tworzy kopię zapasową wszystkich danych systemu.
     * Używa obiektów DTO bez cyklicznych referencji.
     *
     * @return ścieżka do utworzonego pliku kopii zapasowej
     * @throws IOException w przypadku błędu podczas zapisu pliku
     */
    @Transactional
    public String createBackup() throws IOException {
        logger.info("Rozpoczęcie tworzenia kopii zapasowej danych systemu...");

        // Konfiguracja ObjectMapper do serializacji JSON
        ObjectMapper mapper = new ObjectMapper();
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        // Przygotowanie danych do kopii
        BackupData backupData = new BackupData();

        // Pobieranie i konwersja danych z repozytoriów
        backupData.company = companyRepository.getCompanyInfo();
        backupData.customers = convertCustomers(customerRepository.findAll());
        backupData.invoices = convertInvoices(invoiceRepository.findAll());
        backupData.users = convertUsers(userRepository.findAll());
        backupData.roles = roleRepository.findAll();
        backupData.loginAudits = loginAuditRepository.findAll();
        backupData.timestamp = new Date().toString();
        backupData.version = "1.0";

        // Utworzenie katalogu backups, jeśli nie istnieje
        Path backupDir = Paths.get("backups");
        if (!Files.exists(backupDir)) {
            Files.createDirectories(backupDir);
        }

        // Generowanie nazwy pliku z datą i czasem
        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String fileName = "backup_" + timestamp + ".json";
        File backupFile = new File(backupDir.toFile(), fileName);

        // Zapis danych do pliku
        mapper.writeValue(backupFile, backupData);

        logger.info("Kopia zapasowa została utworzona: {}", backupFile.getAbsolutePath());
        return backupFile.getAbsolutePath();
    }

    /**
     * Przywraca dane z pliku kopii zapasowej.
     *
     * @param backupFileName nazwa pliku kopii zapasowej
     * @return informacja o powodzeniu operacji
     * @throws IOException w przypadku błędu podczas odczytu pliku
     */
    @Transactional(propagation = Propagation.NEVER) // Wyłączamy zarządzanie transakcjami na poziomie metody
    public String restoreFromBackup(String backupFileName) throws IOException {
        logger.info("Rozpoczęcie przywracania danych z kopii zapasowej: {}", backupFileName);

        // Sprawdzenie, czy plik istnieje
        Path backupPath = Paths.get("backups", backupFileName);
        if (!Files.exists(backupPath)) {
            throw new IOException("Plik kopii zapasowej nie istnieje: " + backupPath);
        }

        // Konfiguracja ObjectMapper do deserializacji JSON
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());

        try {
            // Wczytanie danych z pliku
            BackupData backupData = mapper.readValue(backupPath.toFile(), BackupData.class);

            // Przywracamy każdy typ danych oddzielnie, pomijając Company
            boolean success = true;

            // 1. Przywróć role
            try {
                restoreRolesInTransaction(backupData.roles);
            } catch (Exception e) {
                logger.error("Błąd podczas przywracania ról: {}", e.getMessage(), e);
                success = false;
            }

            // 2. Przywróć klientów
            try {
                restoreCustomersInTransaction(backupData.customers);
            } catch (Exception e) {
                logger.error("Błąd podczas przywracania klientów: {}", e.getMessage(), e);
                success = false;
            }

            // 3. Przywróć użytkowników
            try {
                restoreUsersInTransaction(backupData.users);
            } catch (Exception e) {
                logger.error("Błąd podczas przywracania użytkowników: {}", e.getMessage(), e);
                success = false;
            }

            // 4. Przywróć faktury
            try {
                restoreInvoicesInTransaction(backupData.invoices);
            } catch (Exception e) {
                logger.error("Błąd podczas przywracania faktur: {}", e.getMessage(), e);
                success = false;
            }

            // 5. Przywróć logi logowań
            try {
                restoreLoginAuditsInTransaction(backupData.loginAudits);
            } catch (Exception e) {
                logger.error("Błąd podczas przywracania logów logowań: {}", e.getMessage(), e);
                success = false;
            }

            if (success) {
                return "Dane zostały pomyślnie przywrócone z kopii zapasowej: " + backupFileName;
            } else {
                return "Dane zostały częściowo przywrócone z kopii zapasowej. Sprawdź logi, aby uzyskać więcej informacji.";
            }
        } catch (Exception e) {
            logger.error("Błąd podczas przywracania danych: {}", e.getMessage(), e);
            throw new IOException("Błąd podczas przywracania danych: " + e.getMessage(), e);
        }
    }

    /**
     * Pomocnicza metoda do przywracania ról w osobnej transakcji.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void restoreRolesInTransaction(List<Role> roles) {
        logger.info("Przywracanie ról...");

        // Najpierw usuń istniejące dane
        try {
            roleRepository.deleteAll();
            roleRepository.flush();
        } catch (Exception e) {
            logger.warn("Nie można usunąć istniejących ról: {}", e.getMessage());
            // Kontynuujemy, próbując tylko dodać role
        }

        // Teraz przywróć dane z kopii
        for (RoleType roleType : RoleType.values()) {
            try {
                // Sprawdź, czy rola już istnieje
                Optional<Role> existingRole = roleRepository.findByName(roleType);
                if (existingRole.isEmpty()) {
                    Role role = new Role(roleType);
                    Role savedRole = roleRepository.save(role);
                    logger.info("Zapisano rolę: {}", savedRole.getName());
                } else {
                    logger.info("Rola już istnieje: {}", roleType);
                }
            } catch (Exception e) {
                logger.error("Błąd podczas zapisywania roli {}: {}", roleType, e.getMessage());
                throw e;
            }
        }
    }

    /**
     * Pomocnicza metoda do przywracania klientów w osobnej transakcji.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void restoreCustomersInTransaction(List<CustomerDTO> customerDTOs) {
        logger.info("Przywracanie klientów: {}", customerDTOs.size());

        // Najpierw usuń istniejące dane
        try {
            customerRepository.deleteAll();
            customerRepository.flush();
        } catch (Exception e) {
            logger.warn("Nie można usunąć istniejących klientów: {}", e.getMessage());
            // Kontynuujemy, próbując tylko dodać klientów
        }

        // Teraz przywróć dane z kopii
        Map<Long, Long> newCustomerIdMapping = new HashMap<>();
        for (CustomerDTO dto : customerDTOs) {
            try {
                Customer customer = new Customer();
                customer.setName(dto.name);
                customer.setAddress(dto.address);
                customer.setNip(dto.nip);
                customer.setRegon(dto.regon);
                customer.setEmail(dto.email);
                customer.setPhone(dto.phone);

                Customer savedCustomer = customerRepository.save(customer);
                newCustomerIdMapping.put(dto.id, savedCustomer.getId());
                logger.info("Zapisano klienta: {} (ID: {})", savedCustomer.getName(), savedCustomer.getId());
            } catch (Exception e) {
                logger.error("Błąd podczas zapisywania klienta {}: {}", dto.name, e.getMessage());
                throw e;
            }
        }

        // Zapisz mapowanie ID do wykorzystania później
        this.customerIdMapping = newCustomerIdMapping;
    }

    /**
     * Pomocnicza metoda do przywracania użytkowników w osobnej transakcji.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void restoreUsersInTransaction(List<UserDTO> userDTOs) {
        logger.info("Przywracanie użytkowników: {}", userDTOs.size());

        // Najpierw usuń istniejące dane
        try {
            userRepository.deleteAll();
            userRepository.flush();
        } catch (Exception e) {
            logger.warn("Nie można usunąć istniejących użytkowników: {}", e.getMessage());
            // Kontynuujemy, próbując tylko dodać użytkowników
        }

        // Przygotuj mapowanie ról
        Map<Long, Role> rolesMap = new HashMap<>();
        for (Role role : roleRepository.findAll()) {
            rolesMap.put(role.getId(), role);
            // Zapisz mapowanie dla roleName->Role
            if (role.getName() == RoleType.ROLE_ADMIN) {
                rolesMap.put(1L, role); // Przypisz ID 1 do ADMIN dla kopii zapasowych
            } else if (role.getName() == RoleType.ROLE_USER) {
                rolesMap.put(2L, role); // Przypisz ID 2 do USER dla kopii zapasowych
            }
        }

        // Teraz przywróć dane z kopii
        for (UserDTO dto : userDTOs) {
            try {
                User user = new User();
                user.setUsername(dto.username);
                user.setEmail(dto.email);
                user.setPassword(passwordEncoder.encode("password")); // Domyślne hasło
                user.setActive(dto.active);
                user.setMustChangePassword(true);

                // Przypisanie ról
                Set<Role> userRoles = new HashSet<>();
                for (Long roleId : dto.roleIds) {
                    Role role = rolesMap.get(roleId);
                    if (role != null) {
                        userRoles.add(role);
                    } else {
                        logger.warn("Nie znaleziono roli o ID: {}", roleId);
                    }
                }

                if (userRoles.isEmpty()) {
                    // Jeśli nie znaleziono ról, przypisz domyślną rolę USER
                    Role userRole = rolesMap.values().stream()
                            .filter(r -> r.getName() == RoleType.ROLE_USER)
                            .findFirst()
                            .orElse(null);

                    if (userRole != null) {
                        userRoles.add(userRole);
                    }
                }

                user.setRoles(userRoles);

                User savedUser = userRepository.save(user);
                logger.info("Zapisano użytkownika: {} (ID: {})", savedUser.getUsername(), savedUser.getId());
            } catch (Exception e) {
                logger.error("Błąd podczas zapisywania użytkownika {}: {}", dto.username, e.getMessage());
                throw e;
            }
        }
    }

    /**
     * Pomocnicza metoda do przywracania faktur w osobnej transakcji.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void restoreInvoicesInTransaction(List<InvoiceDTO> invoiceDTOs) {
        logger.info("Przywracanie faktur: {}", invoiceDTOs.size());

        // Najpierw usuń istniejące dane
        try {
            invoiceRepository.deleteAll();
            invoiceRepository.flush();
        } catch (Exception e) {
            logger.warn("Nie można usunąć istniejących faktur: {}", e.getMessage());
        }

        // Sprawdź, czy mamy mapowanie ID klientów
        if (customerIdMapping == null || customerIdMapping.isEmpty()) {
            logger.warn("Brak mapowania ID klientów - faktury mogą zostać przywrócone niepoprawnie!");
            customerIdMapping = new HashMap<>();
        }

        // Teraz przywróć dane z kopii
        for (InvoiceDTO dto : invoiceDTOs) {
            try {
                Invoice invoice = new Invoice();
                invoice.setInvoiceNumber(dto.invoiceNumber);
                invoice.setIssueDate(dto.issueDate);
                invoice.setDueDate(dto.dueDate);

                // Bezpieczne ustawienie metody płatności
                try {
                    invoice.setPaymentMethod(PaymentMethod.valueOf(dto.paymentMethod));
                } catch (Exception e) {
                    logger.warn("Nieprawidłowa metoda płatności: {}. Ustawiam domyślną.", dto.paymentMethod);
                    invoice.setPaymentMethod(PaymentMethod.PRZELEW);
                }

                // Bezpieczne ustawienie statusu
                try {
                    invoice.setStatus(InvoiceStatus.valueOf(dto.status));
                } catch (Exception e) {
                    logger.warn("Nieprawidłowy status faktury: {}. Ustawiam domyślny.", dto.status);
                    invoice.setStatus(InvoiceStatus.NIEOPLACONA);
                }

                invoice.setNotes(dto.notes);

                // WAŻNA ZMIANA - zapisujemy fakturę BEZ klienta najpierw
                Invoice savedInvoice = invoiceRepository.save(invoice);

                // Przypisanie klienta - teraz w odrębnym kroku
                if (dto.customerId != null) {
                    Long newCustomerId = customerIdMapping.get(dto.customerId);
                    if (newCustomerId != null) {
                        try {
                            // Pobierz klienta w obecnej transakcji i przypisz go do faktury
                            Optional<Customer> customer = customerRepository.findById(newCustomerId);
                            if (customer.isPresent()) {
                                savedInvoice.setCustomer(customer.get());
                                // Zapisz fakturę ponownie, tym razem z klientem
                                savedInvoice = invoiceRepository.save(savedInvoice);
                                logger.info("Przypisano klienta ID: {} do faktury ID: {}",
                                        newCustomerId, savedInvoice.getId());
                            } else {
                                logger.warn("Nie znaleziono klienta o ID: {} (stare ID: {})",
                                        newCustomerId, dto.customerId);
                            }
                        } catch (Exception e) {
                            logger.error("Błąd podczas przypisywania klienta do faktury: {}", e.getMessage());
                            // Kontynuuj mimo błędu, aby przynajmniej faktura była zapisana
                        }
                    } else {
                        logger.warn("Brak mapowania dla ID klienta: {}", dto.customerId);
                    }
                }

                // Dodanie pozycji faktury
                for (InvoiceItemDTO itemDto : dto.items) {
                    try {
                        InvoiceItem item = new InvoiceItem();
                        item.setProduct(itemDto.product);
                        item.setQuantity(itemDto.quantity);
                        item.setPrice(itemDto.price);
                        item.setInvoice(savedInvoice);

                        savedInvoice.getItems().add(item);
                    } catch (Exception e) {
                        logger.error("Błąd podczas dodawania pozycji do faktury: {}", e.getMessage());
                        // Kontynuuj z pozostałymi pozycjami
                    }
                }

                // Zapisanie faktury z pozycjami
                try {
                    invoiceRepository.save(savedInvoice);
                    logger.info("Zapisano fakturę: {} (ID: {})", savedInvoice.getInvoiceNumber(), savedInvoice.getId());
                } catch (Exception e) {
                    logger.error("Błąd podczas zapisywania pozycji faktury: {}", e.getMessage());
                    // Faktura główna jest już zapisana, więc nie zatrzymujemy procesu
                }
            } catch (Exception e) {
                logger.error("Błąd podczas zapisywania faktury {}: {}", dto.invoiceNumber, e.getMessage());
                // Kontynuuj z kolejnymi fakturami
            }
        }
    }

    /**
     * Pomocnicza metoda do przywracania logów logowań w osobnej transakcji.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void restoreLoginAuditsInTransaction(List<LoginAudit> loginAudits) {
        logger.info("Przywracanie logów logowań: {}", loginAudits.size());

        // Najpierw usuń istniejące dane
        try {
            loginAuditRepository.deleteAll();
            loginAuditRepository.flush();
        } catch (Exception e) {
            logger.warn("Nie można usunąć istniejących logów logowań: {}", e.getMessage());
            // Kontynuujemy, próbując tylko dodać logi
        }

        // Teraz przywróć dane z kopii
        if (loginAudits != null && !loginAudits.isEmpty()) {
            try {
                loginAuditRepository.saveAll(loginAudits);
                logger.info("Zapisano {} logów logowań", loginAudits.size());
            } catch (Exception e) {
                logger.error("Błąd podczas zapisywania logów logowań: {}", e.getMessage());
                throw e;
            }
        }
    }

    /**
     * Konwertuje listę użytkowników do prostych DTO bez cyklicznych referencji.
     */
    private List<UserDTO> convertUsers(List<User> users) {
        List<UserDTO> result = new ArrayList<>();
        for (User user : users) {
            UserDTO dto = new UserDTO();
            dto.id = user.getId();
            dto.username = user.getUsername();
            dto.email = user.getEmail();
            dto.active = user.isActive();
            dto.mustChangePassword = user.isMustChangePassword();
            dto.roleIds = new ArrayList<>();
            for (Role role : user.getRoles()) {
                dto.roleIds.add(role.getId());
            }
            result.add(dto);
        }
        return result;
    }

    /**
     * Konwertuje listę klientów do prostych DTO bez cyklicznych referencji.
     */
    private List<CustomerDTO> convertCustomers(List<Customer> customers) {
        List<CustomerDTO> result = new ArrayList<>();
        for (Customer customer : customers) {
            CustomerDTO dto = new CustomerDTO();
            dto.id = customer.getId();
            dto.name = customer.getName();
            dto.address = customer.getAddress();
            dto.nip = customer.getNip();
            dto.regon = customer.getRegon();
            dto.email = customer.getEmail();
            dto.phone = customer.getPhone();
            result.add(dto);
        }
        return result;
    }

    /**
     * Konwertuje listę faktur do prostych DTO bez cyklicznych referencji.
     */
    private List<InvoiceDTO> convertInvoices(List<Invoice> invoices) {
        List<InvoiceDTO> result = new ArrayList<>();
        for (Invoice invoice : invoices) {
            InvoiceDTO dto = new InvoiceDTO();
            dto.id = invoice.getId();
            dto.invoiceNumber = invoice.getInvoiceNumber();
            dto.issueDate = invoice.getIssueDate();
            dto.dueDate = invoice.getDueDate();
            dto.paymentMethod = invoice.getPaymentMethod().name();
            dto.status = invoice.getStatus().name();
            dto.notes = invoice.getNotes();

            if (invoice.getCustomer() != null) {
                dto.customerId = invoice.getCustomer().getId();
                dto.customerName = invoice.getCustomer().getName();
            }

            dto.items = new ArrayList<>();
            for (InvoiceItem item : invoice.getItems()) {
                InvoiceItemDTO itemDto = new InvoiceItemDTO();
                itemDto.id = item.getId();
                itemDto.product = item.getProduct();
                itemDto.quantity = item.getQuantity();
                itemDto.price = item.getPrice();
                dto.items.add(itemDto);
            }

            result.add(dto);
        }
        return result;
    }

    /**
     * Klasa zawierająca wszystkie dane kopii zapasowej.
     */
    public static class BackupData {
        public Company company;
        public List<CustomerDTO> customers;
        public List<InvoiceDTO> invoices;
        public List<UserDTO> users;
        public List<Role> roles;
        public List<LoginAudit> loginAudits;
        public String timestamp;
        public String version;
    }

    /**
     * DTO dla użytkownika bez cyklicznych referencji.
     */
    public static class UserDTO {
        public Long id;
        public String username;
        public String email;
        public boolean active;
        public boolean mustChangePassword;
        public List<Long> roleIds;
    }

    /**
     * DTO dla klienta bez cyklicznych referencji.
     */
    public static class CustomerDTO {
        public Long id;
        public String name;
        public String address;
        public String nip;
        public String regon;
        public String email;
        public String phone;
    }

    /**
     * DTO dla faktury bez cyklicznych referencji.
     */
    public static class InvoiceDTO {
        public Long id;
        public String invoiceNumber;
        public LocalDate issueDate;
        public LocalDate dueDate;
        public String paymentMethod;
        public Long customerId;
        public String customerName;
        public String status;
        public String notes;
        public List<InvoiceItemDTO> items;
    }

    /**
     * DTO dla pozycji faktury.
     */
    public static class InvoiceItemDTO {
        public Long id;
        public String product;
        public int quantity;
        public double price;
    }
}