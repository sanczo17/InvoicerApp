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
    @Transactional(readOnly = true)
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
    @Transactional
    public String restoreFromBackup(String backupFileName) throws IOException {
        logger.info("Rozpoczęcie przywracania danych z kopii zapasowej: {}", backupFileName);

        // Sprawdzenie, czy plik istnieje
        Path backupPath = Paths.get("backups", backupFileName);
        if (!Files.exists(backupPath)) {
            throw new IOException("Plik kopii zapasowej nie istnieje: " + backupPath);
        }

        // Utworzenie kopii zapasowej aktualnych danych przed przywróceniem
        createBackup();

        // Konfiguracja ObjectMapper do deserializacji JSON
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());

        // Wczytanie danych z pliku
        BackupData backupData = mapper.readValue(backupPath.toFile(), BackupData.class);

        try {
            // Czyszczenie bazy danych
            clearDatabase();

            // Przywracanie danych z kopii zapasowej
            restoreRoles(backupData.roles);
            restoreCompany(backupData.company);
            restoreCustomers(backupData.customers);
            restoreUsers(backupData.users);
            restoreInvoices(backupData.invoices);
            restoreLoginAudits(backupData.loginAudits);

            logger.info("Dane zostały pomyślnie przywrócone z kopii zapasowej");
            return "Dane zostały pomyślnie przywrócone z kopii zapasowej: " + backupFileName;
        } catch (Exception e) {
            logger.error("Błąd podczas przywracania danych: {}", e.getMessage(), e);
            throw new IOException("Błąd podczas przywracania danych: " + e.getMessage(), e);
        }
    }

    /**
     * Usuwa wszystkie dane z bazy danych.
     */
    private void clearDatabase() {
        logger.info("Czyszczenie bazy danych przed przywróceniem");
        invoiceRepository.deleteAll();
        customerRepository.deleteAll();
        loginAuditRepository.deleteAll();

        // Usuwamy powiązania użytkownik-rola
        List<User> users = userRepository.findAll();
        for (User user : users) {
            user.setRoles(new HashSet<>());
            userRepository.save(user);
        }

        userRepository.deleteAll();

        // Nie usuwamy ról, będą potrzebne do przywrócenia danych

        // Usuwamy dane firmy, ale zachowujemy ID = 1, jeśli istnieje
        Company company = companyRepository.getCompanyInfo();
        if (company != null && company.getId() != null) {
            // Czyszczenie pól company
            company.setName(null);
            company.setAddress(null);
            company.setNip(null);
            company.setRegon(null);
            company.setEmail(null);
            company.setPhone(null);
            company.setWebsite(null);
            company.setBankName(null);
            company.setBankAccount(null);
            company.setAdditionalInfo(null);
            company.setLogoPath(null);
            companyRepository.save(company);
        }
    }

    /**
     * Przywraca role z kopii zapasowej.
     */
    private void restoreRoles(List<Role> roles) {
        logger.info("Przywracanie ról");

        // Sprawdzamy, czy role już istnieją - jeśli nie, tworzymy je
        for (RoleType roleType : RoleType.values()) {
            if (roleRepository.findByName(roleType).isEmpty()) {
                Role role = new Role(roleType);
                roleRepository.save(role);
            }
        }
    }

    /**
     * Przywraca dane firmy z kopii zapasowej.
     */
    private void restoreCompany(Company company) {
        logger.info("Przywracanie danych firmy");
        if (company != null) {
            // Sprawdzamy, czy firma już istnieje
            Company existingCompany = companyRepository.getCompanyInfo();
            if (existingCompany != null && existingCompany.getId() != null) {
                // Aktualizujemy istniejący rekord
                company.setId(existingCompany.getId());
            }
            companyRepository.save(company);
        }
    }

    /**
     * Przywraca klientów z kopii zapasowej.
     */
    private void restoreCustomers(List<CustomerDTO> customerDTOs) {
        logger.info("Przywracanie klientów: {}", customerDTOs.size());

        Map<Long, Long> customerIdMapping = new HashMap<>(); // Mapowanie starych ID na nowe

        for (CustomerDTO dto : customerDTOs) {
            Customer customer = new Customer();
            customer.setName(dto.name);
            customer.setAddress(dto.address);
            customer.setNip(dto.nip);
            customer.setRegon(dto.regon);
            customer.setEmail(dto.email);
            customer.setPhone(dto.phone);

            Customer savedCustomer = customerRepository.save(customer);
            customerIdMapping.put(dto.id, savedCustomer.getId());
        }

        // Zapisujemy mapowanie ID do użycia przy przywracaniu faktur
        this.customerIdMapping = customerIdMapping;
    }

    private Map<Long, Long> customerIdMapping; // Przechowuje mapowanie ID klientów (stare->nowe)
    private Map<Long, Long> roleIdMapping = new HashMap<>(); // Przechowuje mapowanie ID ról (stare->nowe)

    /**
     * Przywraca użytkowników z kopii zapasowej.
     */
    private void restoreUsers(List<UserDTO> userDTOs) {
        logger.info("Przywracanie użytkowników: {}", userDTOs.size());

        // Tworzymy mapowanie ID ról
        List<Role> allRoles = roleRepository.findAll();
        for (Role role : allRoles) {
            roleIdMapping.put(role.getId(), role.getId());
        }

        for (UserDTO dto : userDTOs) {
            User user = new User();
            user.setUsername(dto.username);
            user.setEmail(dto.email);
            // Ustawiamy domyślne hasło (można zaimplementować specjalną logikę)
            // W praktyce powinno się wymagać zmiany hasła przy następnym logowaniu
            user.setPassword(passwordEncoder.encode("password"));
            user.setActive(dto.active);
            user.setMustChangePassword(true); // Wymuszamy zmianę hasła

            // Przypisanie ról
            Set<Role> roles = new HashSet<>();
            for (Long roleId : dto.roleIds) {
                roleRepository.findById(roleIdMapping.getOrDefault(roleId, roleId))
                        .ifPresent(roles::add);
            }
            user.setRoles(roles);

            userRepository.save(user);
        }
    }

    /**
     * Przywraca faktury z kopii zapasowej.
     */
    private void restoreInvoices(List<InvoiceDTO> invoiceDTOs) {
        logger.info("Przywracanie faktur: {}", invoiceDTOs.size());

        for (InvoiceDTO dto : invoiceDTOs) {
            Invoice invoice = new Invoice();
            invoice.setInvoiceNumber(dto.invoiceNumber);
            invoice.setIssueDate(dto.issueDate);
            invoice.setDueDate(dto.dueDate);
            invoice.setPaymentMethod(PaymentMethod.valueOf(dto.paymentMethod));
            invoice.setStatus(InvoiceStatus.valueOf(dto.status));
            invoice.setNotes(dto.notes);

            // Przypisanie klienta
            if (dto.customerId != null) {
                Long newCustomerId = customerIdMapping.getOrDefault(dto.customerId, dto.customerId);
                customerRepository.findById(newCustomerId).ifPresent(invoice::setCustomer);
            }

            // Zapis faktury, aby mieć ID
            Invoice savedInvoice = invoiceRepository.save(invoice);

            // Dodanie pozycji faktury
            for (InvoiceItemDTO itemDto : dto.items) {
                InvoiceItem item = new InvoiceItem();
                item.setProduct(itemDto.product);
                item.setQuantity(itemDto.quantity);
                item.setPrice(itemDto.price);
                item.setInvoice(savedInvoice);

                savedInvoice.getItems().add(item);
            }

            // Zapisanie faktury z pozycjami
            invoiceRepository.save(savedInvoice);
        }
    }

    /**
     * Przywraca logi logowań z kopii zapasowej.
     */
    private void restoreLoginAudits(List<LoginAudit> loginAudits) {
        logger.info("Przywracanie logów logowań: {}", loginAudits.size());
        loginAuditRepository.saveAll(loginAudits);
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