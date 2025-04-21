package org.example.service;

import org.example.model.Customer;
import org.example.repository.CustomerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class CustomerService {

    private final CustomerRepository customerRepository;

    @Autowired
    public CustomerService(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    public List<Customer> findAll() {
        return customerRepository.findAll();
    }

    public Optional<Customer> findById(Long id) {
        return customerRepository.findById(id);
    }

    @Transactional
    public Customer save(Customer customer) {
        return customerRepository.save(customer);
    }

    @Transactional
    public void deleteById(Long id) {
        customerRepository.deleteById(id);
    }

    // Zwraca listę klientów, dla których nazwa zawiera podany fragment
    public List<Customer> findByNameContaining(String name) {
        return customerRepository.findAll().stream()
                .filter(customer -> customer.getName() != null &&
                        customer.getName().toLowerCase().contains(name.toLowerCase()))
                .toList();
    }

    // Zwraca listę klientów, dla których NIP zawiera podany fragment
    public List<Customer> findByNipContaining(String nip) {
        return customerRepository.findAll().stream()
                .filter(customer -> customer.getNip() != null &&
                        customer.getNip().contains(nip))
                .toList();
    }

    // Zwraca listę klientów posortowanych według nazwy
    public List<Customer> findAllSortedByName() {
        return customerRepository.findAll().stream()
                .sorted((c1, c2) -> {
                    if (c1.getName() == null) return 1;
                    if (c2.getName() == null) return -1;
                    return c1.getName().compareTo(c2.getName());
                })
                .toList();
    }

    // Sprawdza, czy klient o podanym NIP już istnieje
    public boolean existsByNip(String nip) {
        if (nip == null || nip.isEmpty()) {
            return false;
        }

        return customerRepository.findAll().stream()
                .anyMatch(customer -> nip.equals(customer.getNip()));
    }
}