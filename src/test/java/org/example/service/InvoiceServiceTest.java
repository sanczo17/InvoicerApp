package org.example.service;

import org.example.exception.ResourceNotFoundException;
import org.example.model.Customer;
import org.example.model.Invoice;
import org.example.model.InvoiceItem;
import org.example.model.enums.InvoiceStatus;
import org.example.repository.CustomerRepository;
import org.example.repository.InvoiceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Testy jednostkowe dla klasy InvoiceService.
 */
@ExtendWith(MockitoExtension.class)
public class InvoiceServiceTest {

    @Mock
    private InvoiceRepository invoiceRepository;

    @Mock
    private CustomerRepository customerRepository;

    @InjectMocks
    private InvoiceService invoiceService;

    private Invoice testInvoice;
    private Customer testCustomer;

    @BeforeEach
    void setup() {
        // Przygotowanie danych testowych
        testCustomer = new Customer();
        testCustomer.setId(1L);
        testCustomer.setName("Testowa Firma");
        testCustomer.setNip("1234567890");

        testInvoice = new Invoice();
        testInvoice.setId(1L);
        testInvoice.setInvoiceNumber("FV/2023/01/01");
        testInvoice.setIssueDate(LocalDate.now());
        testInvoice.setDueDate(LocalDate.now().plusDays(14));
        testInvoice.setStatus(InvoiceStatus.NIEOPLACONA);
        testInvoice.setCustomer(testCustomer);

        InvoiceItem item = new InvoiceItem();
        item.setId(1L);
        item.setProduct("Usługa testowa");
        item.setQuantity(1);
        item.setPrice(100.0);
        item.setInvoice(testInvoice);

        testInvoice.getItems().add(item);
    }

    @Test
    void findById_WhenInvoiceExists_ShouldReturnInvoice() {
        // Given
        when(invoiceRepository.findById(1L)).thenReturn(Optional.of(testInvoice));

        // When
        Invoice result = invoiceService.findById(1L);

        // Then
        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("FV/2023/01/01", result.getInvoiceNumber());
    }

    @Test
    void findById_WhenInvoiceDoesNotExist_ShouldThrowException() {
        // Given
        when(invoiceRepository.findById(99L)).thenReturn(Optional.empty());

        // When/Then
        assertThrows(ResourceNotFoundException.class, () -> {
            invoiceService.findById(99L);
        });
    }

    @Test
    void save_WhenCustomerExists_ShouldSaveInvoice() {
        // Given
        Invoice newInvoice = new Invoice();
        newInvoice.setIssueDate(LocalDate.now());
        newInvoice.setDueDate(LocalDate.now().plusDays(14));

        Customer customer = new Customer();
        customer.setId(1L);
        newInvoice.setCustomer(customer);

        InvoiceItem item = new InvoiceItem();
        item.setProduct("Produkt testowy");
        item.setQuantity(1);
        item.setPrice(100.0);
        newInvoice.getItems().add(item);

        when(customerRepository.findById(1L)).thenReturn(Optional.of(testCustomer));
        when(invoiceRepository.save(any(Invoice.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(invoiceRepository.findAll()).thenReturn(Collections.emptyList());

        // When
        Invoice savedInvoice = invoiceService.save(newInvoice);

        // Then
        assertNotNull(savedInvoice);
        assertNotNull(savedInvoice.getInvoiceNumber());
        assertEquals(testCustomer, savedInvoice.getCustomer());
        assertEquals(newInvoice.getItems().get(0).getInvoice(), savedInvoice);

        verify(invoiceRepository).save(newInvoice);
    }

    @Test
    void save_WhenCustomerDoesNotExist_ShouldThrowException() {
        // Given
        Invoice newInvoice = new Invoice();
        newInvoice.setIssueDate(LocalDate.now());
        newInvoice.setDueDate(LocalDate.now().plusDays(14));

        Customer customer = new Customer();
        customer.setId(99L); // Nieistniejący klient
        newInvoice.setCustomer(customer);

        InvoiceItem item = new InvoiceItem();
        item.setProduct("Produkt testowy");
        item.setQuantity(1);
        item.setPrice(100.0);
        newInvoice.getItems().add(item);

        when(customerRepository.findById(99L)).thenReturn(Optional.empty());

        // When/Then
        assertThrows(ResourceNotFoundException.class, () -> {
            invoiceService.save(newInvoice);
        });

        verify(invoiceRepository, never()).save(any(Invoice.class));
    }

    @Test
    void save_WhenInvoiceHasInvalidDates_ShouldThrowException() {
        // Given
        Invoice newInvoice = new Invoice();
        newInvoice.setIssueDate(LocalDate.now());
        newInvoice.setDueDate(LocalDate.now().minusDays(1)); // Data płatności wcześniejsza niż data wystawienia

        InvoiceItem item = new InvoiceItem();
        item.setProduct("Produkt testowy");
        item.setQuantity(1);
        item.setPrice(100.0);
        newInvoice.getItems().add(item);

        // When/Then
        assertThrows(IllegalArgumentException.class, () -> {
            invoiceService.save(newInvoice);
        });

        verify(invoiceRepository, never()).save(any(Invoice.class));
    }

    @Test
    void findByStatus_ShouldReturnFilteredInvoices() {
        // Given
        Invoice invoice1 = new Invoice();
        invoice1.setId(1L);
        invoice1.setStatus(InvoiceStatus.OPLACONA);

        Invoice invoice2 = new Invoice();
        invoice2.setId(2L);
        invoice2.setStatus(InvoiceStatus.NIEOPLACONA);

        when(invoiceRepository.findByStatus(InvoiceStatus.OPLACONA)).thenReturn(Collections.singletonList(invoice1));

        // When
        List<Invoice> result = invoiceService.findByStatus(InvoiceStatus.OPLACONA);

        // Then
        assertEquals(1, result.size());
        assertEquals(InvoiceStatus.OPLACONA, result.get(0).getStatus());
    }

    @Test
    void findAll_ShouldReturnAllInvoices() {
        // Given
        when(invoiceRepository.findAll()).thenReturn(Arrays.asList(testInvoice));

        // When
        List<Invoice> result = invoiceService.findAll();

        // Then
        assertEquals(1, result.size());
        assertEquals(testInvoice, result.get(0));
    }

    @Test
    void deleteById_WhenInvoiceExists_ShouldDeleteInvoice() {
        // Given
        when(invoiceRepository.existsById(1L)).thenReturn(true);
        doNothing().when(invoiceRepository).deleteById(1L);

        // When
        invoiceService.deleteById(1L);

        // Then
        verify(invoiceRepository).deleteById(1L);
    }

    @Test
    void deleteById_WhenInvoiceDoesNotExist_ShouldThrowException() {
        // Given
        when(invoiceRepository.existsById(99L)).thenReturn(false);

        // When/Then
        assertThrows(ResourceNotFoundException.class, () -> {
            invoiceService.deleteById(99L);
        });

        verify(invoiceRepository, never()).deleteById(any());
    }
}