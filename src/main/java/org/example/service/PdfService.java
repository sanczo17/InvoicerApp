package org.example.service;

import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;
import org.example.model.Invoice;
import org.example.model.InvoiceItem;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Service
public class PdfService {

    // Dodajemy czcionki z obsługą polskich znaków
    private static final Font HEADER_FONT;
    private static final Font TITLE_FONT;
    private static final Font NORMAL_FONT;
    private static final Font SMALL_FONT;

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

    public byte[] generateInvoicePdf(Invoice invoice) throws DocumentException {
        Document document = new Document(PageSize.A4);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        try {
            PdfWriter.getInstance(document, baos);
            document.open();

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

            // Dane sprzedawcy
            document.add(new Paragraph("Sprzedawca:", TITLE_FONT));
            document.add(new Paragraph("Twoja Firma Sp. z o.o.", NORMAL_FONT));
            document.add(new Paragraph("ul. Przykładowa 1, 00-000 Warszawa", NORMAL_FONT));
            document.add(new Paragraph("NIP: 1234567890", NORMAL_FONT));
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

    private PdfPCell createHeaderCell(String text) {
        PdfPCell cell = new PdfPCell(new Phrase(text, TITLE_FONT));
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setBackgroundColor(BaseColor.LIGHT_GRAY);
        cell.setPadding(5);
        return cell;
    }

    private PdfPCell createCell(String text) {
        PdfPCell cell = new PdfPCell(new Phrase(text, NORMAL_FONT));
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setPadding(5);
        return cell;
    }
}