package org.example.service;

import org.example.model.Customer;
import org.example.repository.CustomerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Serwis zarządzający operacjami na klientach.
 * Zawiera logikę biznesową dotyczącą klientów w systemie.
 */
@Service
public class CustomerService {

    private final CustomerRepository customerRepository;

    @Autowired
    public CustomerService(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    /**
     * Pobiera wszystkich klientów z bazy danych.
     *
     * @return lista wszystkich klientów
     */
    public List<Customer> findAll() {
        return customerRepository.findAll();
    }

    /**
     * Pobiera klienta po identyfikatorze.
     *
     * @param id identyfikator klienta
     * @return opcjonalny klient (może nie istnieć)
     */
    public Optional<Customer> findById(Long id) {
        return customerRepository.findById(id);
    }

    /**
     * Zapisuje lub aktualizuje klienta w bazie danych.
     *
     * @param customer klient do zapisania
     * @return zapisany klient z nadanym identyfikatorem
     */
    @Transactional
    public Customer save(Customer customer) {
        return customerRepository.save(customer);
    }

    /**
     * Usuwa klienta o podanym identyfikatorze.
     *
     * @param id identyfikator klienta do usunięcia
     */
    @Transactional
    public void deleteById(Long id) {
        customerRepository.deleteById(id);
    }

    /**
     * Wyszukuje klientów, których nazwa zawiera podaną frazę.
     * Wyszukiwanie jest niewrażliwe na wielkość liter.
     *
     * @param name fraza do wyszukania w nazwie
     * @return lista klientów pasujących do kryterium
     */
    public List<Customer> findByNameContaining(String name) {
        return customerRepository.findAll().stream()
                .filter(customer -> customer.getName() != null &&
                        customer.getName().toLowerCase().contains(name.toLowerCase()))
                .toList();
    }

    /**
     * Wyszukuje klientów, których NIP zawiera podaną frazę.
     *
     * @param nip fraza do wyszukania w NIP
     * @return lista klientów pasujących do kryterium
     */
    public List<Customer> findByNipContaining(String nip) {
        return customerRepository.findAll().stream()
                .filter(customer -> customer.getNip() != null &&
                        customer.getNip().contains(nip))
                .toList();
    }

    /**
     * Pobiera wszystkich klientów posortowanych alfabetycznie po nazwie.
     *
     * @return lista klientów posortowana po nazwie
     */
    public List<Customer> findAllSortedByName() {
        return customerRepository.findAll().stream()
                .sorted((c1, c2) -> {
                    if (c1.getName() == null) return 1;
                    if (c2.getName() == null) return -1;
                    return c1.getName().compareTo(c2.getName());
                })
                .toList();
    }

    /**
     * Sprawdza czy istnieje klient o podanym NIP.
     *
     * @param nip NIP do sprawdzenia
     * @return true jeśli klient istnieje, false w przeciwnym przypadku
     */
    public boolean existsByNip(String nip) {
        if (nip == null || nip.isEmpty()) {
            return false;
        }

        return customerRepository.findAll().stream()
                .anyMatch(customer -> nip.equals(customer.getNip()));
    }
}