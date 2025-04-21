package org.example.service;

import org.example.model.Company;
import org.example.repository.CompanyRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Serwis zarządzający danymi firmy wystawiającej faktury.
 */
@Service
public class CompanyService {

    private static final Logger logger = LoggerFactory.getLogger(CompanyService.class);

    private final CompanyRepository companyRepository;

    @Autowired
    public CompanyService(CompanyRepository companyRepository) {
        this.companyRepository = companyRepository;
    }

    /**
     * Pobiera informacje o firmie. Jeśli dane firmy nie istnieją, zwraca nowy pusty obiekt.
     *
     * @return informacje o firmie
     */
    public Company getCompanyInfo() {
        return companyRepository.getCompanyInfo();
    }

    /**
     * Zapisuje lub aktualizuje dane firmy.
     * W systemie powinien istnieć tylko jeden rekord firmy.
     *
     * @param company dane firmy do zapisania
     * @return zapisane dane firmy
     */
    @Transactional
    public Company saveCompanyInfo(Company company) {
        // Sprawdź, czy istnieje już rekord w bazie
        Company existingCompany = getCompanyInfo();

        if (existingCompany.getId() != null) {
            // Aktualizujemy istniejący rekord
            company.setId(existingCompany.getId());
            logger.info("Aktualizacja danych firmy o ID: {}", existingCompany.getId());
        } else {
            logger.info("Tworzenie nowego rekordu danych firmy");
        }

        return companyRepository.save(company);
    }

    /**
     * Inicjalizuje dane firmy domyślnymi wartościami, jeśli nie istnieją.
     * Używane przy pierwszym uruchomieniu aplikacji.
     */
    @Transactional
    public void initDefaultCompanyIfNotExists() {
        if (companyRepository.count() == 0) {
            logger.info("Inicjalizacja domyślnych danych firmy");

            Company company = new Company();
            company.setName("Twoja Firma Sp. z o.o.");
            company.setAddress("ul. Przykładowa 1, 00-000 Warszawa");
            company.setNip("1234567890");
            company.setEmail("kontakt@twojafirma.pl");
            company.setPhone("123-456-789");
            company.setBankName("Bank Przykładowy S.A.");
            company.setBankAccount("PL 12 3456 7890 1234 5678 9012 3456");

            companyRepository.save(company);
            logger.info("Utworzono domyślne dane firmy");
        }
    }
}