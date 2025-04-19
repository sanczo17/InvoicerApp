package org.example.controller;

import org.example.model.Invoice;
import org.example.model.enums.InvoiceStatus;
import org.example.repository.InvoiceRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.time.Month;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
public class DashboardController {

    @Autowired
    private InvoiceRepository invoiceRepository;

    @GetMapping("/")
    public String dashboard(Model model) {
        List<Invoice> allInvoices = invoiceRepository.findAll();

        double paidTotal = allInvoices.stream()
                .filter(invoice -> invoice.getStatus() == InvoiceStatus.OPLACONA)
                .mapToDouble(Invoice::getTotal)
                .sum();

        double unpaidTotal = allInvoices.stream()
                .filter(invoice -> invoice.getStatus() == InvoiceStatus.NIEOPLACONA)
                .mapToDouble(Invoice::getTotal)
                .sum();

        Map<String, Double> monthlyData = new HashMap<>();
        double monthlyAverage = allInvoices.stream().mapToDouble(Invoice::getTotal).sum() / 12;

        for (Month month : Month.values()) {
            monthlyData.put(month.toString(), monthlyAverage);
        }

        model.addAttribute("paidTotal", paidTotal);
        model.addAttribute("unpaidTotal", unpaidTotal);
        model.addAttribute("invoiceCount", allInvoices.size());
        model.addAttribute("monthlyData", monthlyData);

        return "dashboard";
    }
}