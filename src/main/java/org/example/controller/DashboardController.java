package org.example.controller;

import org.example.model.Invoice;
import org.example.model.enums.InvoiceStatus;
import org.example.service.InvoiceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.time.LocalDate;
import java.time.Month;
import java.time.format.TextStyle;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
public class DashboardController {

    private final InvoiceService invoiceService;

    @Autowired
    public DashboardController(InvoiceService invoiceService) {
        this.invoiceService = invoiceService;
    }

    @GetMapping("/")
    public String dashboard(Model model) {
        List<Invoice> allInvoices = invoiceService.findAll();

        // Obliczanie sum faktur opłaconych i nieopłaconych
        double paidTotal = allInvoices.stream()
                .filter(invoice -> invoice.getStatus() == InvoiceStatus.OPLACONA)
                .mapToDouble(Invoice::getTotal)
                .sum();

        double unpaidTotal = allInvoices.stream()
                .filter(invoice -> invoice.getStatus() == InvoiceStatus.NIEOPLACONA)
                .mapToDouble(Invoice::getTotal)
                .sum();

        // Przygotowanie danych miesięcznych dla wykresu
        Map<String, Double> monthlyData = new LinkedHashMap<>();
        LocalDate now = LocalDate.now();
        int currentYear = now.getYear();

        // Inicjalizacja wszystkich miesięcy z zerami w kolejności chronologicznej
        for (int i = 1; i <= 12; i++) {
            Month month = Month.of(i);
            String monthName = month.getDisplayName(TextStyle.FULL, new Locale("pl"));
            monthlyData.put(monthName, 0.0);
        }

        // Pogrupowanie faktur według miesięcy i obliczenie sum
        Map<Month, Double> monthlyTotals = allInvoices.stream()
                .filter(invoice -> invoice.getIssueDate() != null &&
                        invoice.getIssueDate().getYear() == currentYear)
                .collect(Collectors.groupingBy(
                        invoice -> invoice.getIssueDate().getMonth(),
                        Collectors.summingDouble(Invoice::getTotal)
                ));

        // Aktualizacja mapy rzeczywistymi danymi
        monthlyTotals.forEach((month, total) -> {
            String monthName = month.getDisplayName(TextStyle.FULL, new Locale("pl"));
            monthlyData.put(monthName, total);
        });

        model.addAttribute("paidTotal", paidTotal);
        model.addAttribute("unpaidTotal", unpaidTotal);
        model.addAttribute("invoiceCount", allInvoices.size());
        model.addAttribute("monthlyData", monthlyData);

        return "dashboard";
    }
}