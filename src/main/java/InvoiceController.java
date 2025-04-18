import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/invoices")
public class InvoiceController {

    @Autowired
    private InvoiceRepository invoiceRepository;

    @GetMapping
    public String getAll(@RequestParam(required = false) String status, Model model) {
        List<Invoice> invoices = (status == null || status.isEmpty()) ?
                invoiceRepository.findAll() :
                invoiceRepository.findByStatus(status);

        double total = invoices.stream().mapToDouble(Invoice::getTotal).sum();
        model.addAttribute("invoices", invoices);
        model.addAttribute("summary", total);
        model.addAttribute("statusFilter", status);
        return "invoice-list";
        }

        @GetMapping("/new")
        public String showForm(Model model) {
            model.addAttribute("invoice", new Invoice());
            return "invoice-form";
    }

        @PostMapping
        public String createOrUpdate(@ModelAttribute Invoice invoice) {
            invoiceRepository.save(invoice);
            return "redirect:/invoices";
    }

        @GetMapping("/edit/{id}")
        public String edit(@PathVariable Long id, Model model) {
            Invoice invoice = invoiceRepository.findById(id).orElseThrow();
            model.addAttribute("invoice", invoice);
            return "invoice-form";
    }
        @GetMapping("/delete/{id}")
        public String delete(@PathVariable Long id) {
            invoiceRepository.deleteById(id);
            return "redirect:/invoices";
    }
}
