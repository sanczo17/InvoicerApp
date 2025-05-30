package org.example.service;

import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;
import org.example.model.Company;
import org.example.model.Invoice;
import org.example.model.InvoiceItem;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Serwis do generowania dokumentów PDF dla faktur.
 * Używa biblioteki iText do tworzenia dokumentów.
 */
@Service
public class PdfService {

    private final CompanyService companyService;

    @Autowired
    public PdfService(CompanyService companyService) {
        this.companyService = companyService;
    }

    // Definiujemy czcionki z obsługą polskich znaków
    private static final Font HEADER_FONT;
    private static final Font TITLE_FONT;
    private static final Font NORMAL_FONT;
    private static final Font SMALL_FONT;

    // Inicjalizacja statyczna czcionek
    static {
        try {
            // Używamy kodowania CP1250 dla języka polskiego
            BaseFont baseFont = BaseFont.createFont(BaseFont.HELVETICA, BaseFont.CP1250, BaseFont.EMBEDDED);
            HEADER_FONT = new Font(baseFont, 18, Font.BOLD);
            TITLE_FONT = new Font(baseFont, 12, Font.BOLD);
            NORMAL_FONT = new Font(baseFont, 10, Font.NORMAL);
            SMALL_FONT = new Font(baseFont, 8, Font.NORMAL);
        } catch (DocumentException | IOException e) {
            throw new RuntimeException("Błąd podczas inicjalizacji czcionek PDF", e);
        }
    }

    /**
     * Generuje dokument PDF dla podanej faktury.
     *
     * @param invoice faktura, dla której generowany jest PDF
     * @return tablica bajtów zawierająca dokument PDF
     * @throws DocumentException w przypadku błędu podczas generowania dokumentu
     */
    public byte[] generateInvoicePdf(Invoice invoice) throws DocumentException {
        Document document = new Document(PageSize.A4);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        try {
            PdfWriter.getInstance(document, baos);
            document.open();

            // Pobieramy dane firmy z bazy
            Company company = companyService.getCompanyInfo();

            // Dodaj logo lub nagłówek
            Paragraph header = new Paragraph("FAKTURA", HEADER_FONT);
            header.setAlignment(Element.ALIGN_CENTER);
            document.add(header);
            document.add(Chunk.NEWLINE);

            // Informacje o fakturze
            document.add(new Paragraph("Numer faktury: " + invoice.getInvoiceNumber(), TITLE_FONT));

            DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy", new Locale("pl"));
            document.add(new Paragraph("Data wystawienia: " + invoice.getIssueDate().format(dateFormatter), NORMAL_FONT));
            document.add(new Paragraph("Termin płatności: " + invoice.getDueDate().format(dateFormatter), NORMAL_FONT));
            document.add(new Paragraph("Metoda płatności: " + invoice.getPaymentMethod(), NORMAL_FONT));
            document.add(Chunk.NEWLINE);

            // Dane sprzedawcy - teraz z bazy danych
            document.add(new Paragraph("Sprzedawca:", TITLE_FONT));
            document.add(new Paragraph(company.getName(), NORMAL_FONT));
            if (company.getAddress() != null && !company.getAddress().isEmpty()) {
                document.add(new Paragraph(company.getAddress(), NORMAL_FONT));
            }
            if (company.getNip() != null && !company.getNip().isEmpty()) {
                document.add(new Paragraph("NIP: " + company.getNip(), NORMAL_FONT));
            }
            if (company.getEmail() != null && !company.getEmail().isEmpty()) {
                document.add(new Paragraph("Email: " + company.getEmail(), NORMAL_FONT));
            }
            if (company.getPhone() != null && !company.getPhone().isEmpty()) {
                document.add(new Paragraph("Telefon: " + company.getPhone(), NORMAL_FONT));
            }
            if (company.getBankAccount() != null && !company.getBankAccount().isEmpty()) {
                document.add(new Paragraph("Konto: " + company.getBankAccount(), NORMAL_FONT));
                if (company.getBankName() != null && !company.getBankName().isEmpty()) {
                    document.add(new Paragraph("Bank: " + company.getBankName(), NORMAL_FONT));
                }
            }
            document.add(Chunk.NEWLINE);

            // Dane nabywcy
            document.add(new Paragraph("Nabywca:", TITLE_FONT));
            if (invoice.getCustomer() != null) {
                document.add(new Paragraph(invoice.getCustomer().getName(), NORMAL_FONT));
                if (invoice.getCustomer().getAddress() != null) {
                    document.add(new Paragraph(invoice.getCustomer().getAddress(), NORMAL_FONT));
                }
                if (invoice.getCustomer().getNip() != null) {
                    document.add(new Paragraph("NIP: " + invoice.getCustomer().getNip(), NORMAL_FONT));
                }
            }
            document.add(Chunk.NEWLINE);

            // Tabela pozycji faktury
            PdfPTable table = new PdfPTable(5); // 5 kolumn
            table.setWidthPercentage(100);
            float[] columnWidths = {10f, 40f, 15f, 15f, 20f};
            table.setWidths(columnWidths);

            // Nagłówki tabeli
            table.addCell(createHeaderCell("Lp."));
            table.addCell(createHeaderCell("Nazwa"));
            table.addCell(createHeaderCell("Ilość"));
            table.addCell(createHeaderCell("Cena jedn."));
            table.addCell(createHeaderCell("Wartość"));

            // Pozycje faktury
            int i = 1;
            for (InvoiceItem item : invoice.getItems()) {
                table.addCell(createCell(String.valueOf(i++)));
                table.addCell(createCell(item.getProduct()));
                table.addCell(createCell(String.valueOf(item.getQuantity())));
                table.addCell(createCell(String.format("%.2f zł", item.getPrice())));
                table.addCell(createCell(String.format("%.2f zł", item.getTotal())));
            }

            document.add(table);
            document.add(Chunk.NEWLINE);

            // Podsumowanie
            Paragraph summaryParagraph = new Paragraph("Razem do zapłaty: " + String.format("%.2f zł", invoice.getTotal()), TITLE_FONT);
            summaryParagraph.setAlignment(Element.ALIGN_RIGHT);
            document.add(summaryParagraph);

            // Status
            Paragraph statusParagraph = new Paragraph("Status: " + invoice.getStatus().getDisplayName(), NORMAL_FONT);
            statusParagraph.setAlignment(Element.ALIGN_RIGHT);
            document.add(statusParagraph);

            // Dodaj uwagi, jeśli istnieją
            if (invoice.getNotes() != null && !invoice.getNotes().isEmpty()) {
                document.add(Chunk.NEWLINE);
                document.add(new Paragraph("Uwagi:", TITLE_FONT));
                document.add(new Paragraph(invoice.getNotes(), NORMAL_FONT));
            }

            // Dodaj dodatkowe informacje z danych firmy
            if (company.getAdditionalInfo() != null && !company.getAdditionalInfo().isEmpty()) {
                document.add(Chunk.NEWLINE);
                document.add(new Paragraph(company.getAdditionalInfo(), SMALL_FONT));
            }

            // Dodaj stopkę
            document.add(Chunk.NEWLINE);
            document.add(Chunk.NEWLINE);
            Paragraph footer = new Paragraph("Dokument wygenerowany elektronicznie przez system InvoicerApp.", SMALL_FONT);
            footer.setAlignment(Element.ALIGN_CENTER);
            document.add(footer);

            document.close();
            return baos.toByteArray();

        } catch (Exception e) {
            document.close();
            throw new DocumentException("Błąd podczas generowania PDF: " + e.getMessage());
        }
    }

    /**
     * Tworzy komórkę nagłówka tabeli z określonym tekstem.
     */
    private PdfPCell createHeaderCell(String text) {
        PdfPCell cell = new PdfPCell(new Phrase(text, TITLE_FONT));
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setBackgroundColor(BaseColor.LIGHT_GRAY);
        cell.setPadding(5);
        return cell;
    }

    /**
     * Tworzy standardową komórkę tabeli z określonym tekstem.
     */
    private PdfPCell createCell(String text) {
        PdfPCell cell = new PdfPCell(new Phrase(text, NORMAL_FONT));
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setPadding(5);
        return cell;
    }
}