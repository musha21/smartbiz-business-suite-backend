package com.example.smartBiz.service.impl;

import com.example.smartBiz.entity.Invoice;
import com.example.smartBiz.entity.InvoiceItem;
import com.example.smartBiz.exception.ResourceNotFoundException;
import com.example.smartBiz.repository.InvoiceRepo;
import com.example.smartBiz.service.InvoicePdfService;
import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.lowagie.text.pdf.draw.LineSeparator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.lowagie.text.Image;
import org.springframework.core.io.ClassPathResource;
import java.io.InputStream;

import java.io.ByteArrayOutputStream;

@Service
public class InvoicePdfServiceImpl implements InvoicePdfService {

    private final InvoiceRepo invoiceRepository;

    @Autowired
    public InvoicePdfServiceImpl(InvoiceRepo invoiceRepository) {
        this.invoiceRepository = invoiceRepository;
    }


    @Override
    public byte[] generateInvoicePdf(Long invoiceId) {
        // ✅ Best if you use a fetch-join repo method to avoid LazyInitializationException
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new RuntimeException("Invoice not found"));

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {

            Document document = new Document(PageSize.A4, 36, 36, 36, 36);
            PdfWriter writer = PdfWriter.getInstance(document, baos);
            document.open();
            // --- LOGO (Classpath) ---
            try {
                ClassPathResource resource = new ClassPathResource("static/mush.png");
                try (InputStream in = resource.getInputStream()) {
                    byte[] bytes = in.readAllBytes();
                    Image logo = Image.getInstance(bytes);

                    // Size control (change values as needed)
                    logo.scaleToFit(90, 90);

                    // Alignment: LEFT / CENTER / RIGHT
                    logo.setAlignment(Image.ALIGN_RIGHT);

                    document.add(logo);
                }
            } catch (Exception ignored) {
                // if logo missing, PDF will still generate
            }


            // ----------------------------
            // 1) Company Header (Text)
            // ----------------------------
            String companyName = "SmartBiz"; // change this to your brand
            Font companyFont = new Font(Font.HELVETICA, 16, Font.BOLD);
            Font small = new Font(Font.HELVETICA, 10);

            Paragraph company = new Paragraph(companyName, companyFont);
            company.setAlignment(Element.ALIGN_LEFT);

            Paragraph tagline = new Paragraph("AI-Powered Business Management Suite", small);
            tagline.setAlignment(Element.ALIGN_LEFT);

            document.add(company);
            document.add(tagline);
            document.add(new Paragraph(" "));
            addLine(document);

            // ----------------------------
            // 2) Title + Paid Stamp
            // ----------------------------
            Font titleFont = new Font(Font.HELVETICA, 18, Font.BOLD);
            Paragraph title = new Paragraph("INVOICE", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            document.add(title);

            // PAID Stamp (only if PAID)
            if (invoice.getStatus() != null && invoice.getStatus().name().equalsIgnoreCase("PAID")) {
                addPaidStamp(document);
            }

            document.add(new Paragraph(" "));

            // ----------------------------
            // 3) Invoice Details (Left/Right table)
            // ----------------------------
            PdfPTable infoTable = new PdfPTable(2);
            infoTable.setWidthPercentage(100);
            infoTable.setWidths(new float[]{1f, 1f});

            Font label = new Font(Font.HELVETICA, 10, Font.BOLD);
            Font value = new Font(Font.HELVETICA, 10);

            // Left side: customer
            String customerName = safe(invoice.getCustomer().getName());
            String customerPhone = safe(invoice.getCustomer().getPhone());
            String customerAddress = safe(invoice.getCustomer().getAddress());

            PdfPCell left = new PdfPCell();
            left.setBorder(Rectangle.NO_BORDER);
            left.addElement(new Paragraph("Bill To:", label));
            left.addElement(new Paragraph(customerName, value));
            if (!customerPhone.isBlank()) left.addElement(new Paragraph("Phone: " + customerPhone, value));
            if (!customerAddress.isBlank()) left.addElement(new Paragraph("Address: " + customerAddress, value));

            // Right side: invoice meta
            PdfPCell right = new PdfPCell();
            right.setBorder(Rectangle.NO_BORDER);
            right.setHorizontalAlignment(Element.ALIGN_RIGHT);

            right.addElement(rightAligned("Invoice No: " + safe(invoice.getInvoiceNumber()), label));
            right.addElement(rightAligned("Date: " + String.valueOf(invoice.getInvoiceDate()), value));
            right.addElement(rightAligned("Status: " + invoice.getStatus().name(), value));

            infoTable.addCell(left);
            infoTable.addCell(right);

            document.add(infoTable);
            document.add(new Paragraph(" "));
            addLine(document);
            document.add(new Paragraph(" "));

            // ----------------------------
            // 4) Items Table (Styled)
            // ----------------------------
            PdfPTable table = new PdfPTable(5);
            table.setWidthPercentage(100);
            table.setSpacingBefore(5);
            table.setWidths(new float[]{1.2f, 4.8f, 1.2f, 2f, 2f});

            addHeaderCell(table, "No");
            addHeaderCell(table, "Product");
            addHeaderCell(table, "Qty");
            addHeaderCell(table, "Unit Price");
            addHeaderCell(table, "Line Total");

            int index = 1;
            for (InvoiceItem item : invoice.getItems()) {
                table.addCell(bodyCell(String.valueOf(index++), Element.ALIGN_CENTER));
                table.addCell(bodyCell(safe(item.getProduct().getName()), Element.ALIGN_LEFT));
                table.addCell(bodyCell(String.valueOf(item.getQuantity()), Element.ALIGN_CENTER));
                table.addCell(bodyCell(formatMoney(item.getUnitPrice()), Element.ALIGN_RIGHT));
                table.addCell(bodyCell(formatMoney(item.getLineTotal()), Element.ALIGN_RIGHT));
            }

            document.add(table);

            // ----------------------------
            // 5) Totals
            // ----------------------------
            document.add(new Paragraph(" "));

            PdfPTable totals = new PdfPTable(2);
            totals.setWidthPercentage(40);
            totals.setHorizontalAlignment(Element.ALIGN_RIGHT);
            totals.setWidths(new float[]{1.5f, 1f});

            totals.addCell(totalsLabel("Grand Total", label));
            totals.addCell(totalsValue(formatMoney(invoice.getTotalAmount()), label));

            document.add(totals);

            document.add(new Paragraph(" "));
            addLine(document);
            document.add(new Paragraph("Thank you!", small));

            document.close();
            writer.close();

            return baos.toByteArray();

        } catch (Exception e) {
            throw new RuntimeException("PDF generation failed: " + e.getMessage());
        }
    }

    private void addLine(Document document) throws DocumentException {
        LineSeparator line = new LineSeparator();
        line.setLineWidth(0.8f);
        document.add(line);
    }

    private Paragraph rightAligned(String text, Font font) {
        Paragraph p = new Paragraph(text, font);
        p.setAlignment(Element.ALIGN_RIGHT);
        return p;
    }

    private void addPaidStamp(Document document) throws DocumentException {
        Font stampFont = new Font(Font.HELVETICA, 28, Font.BOLD);
        Paragraph stamp = new Paragraph("PAID", stampFont);
        stamp.setAlignment(Element.ALIGN_RIGHT);
        stamp.setSpacingBefore(-30); // pulls it closer to title
        document.add(stamp);
    }

    private void addHeaderCell(PdfPTable table, String text) {
        Font headFont = new Font(Font.HELVETICA, 10, Font.BOLD);
        PdfPCell cell = new PdfPCell(new Phrase(text, headFont));
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setPadding(7);
        cell.setBackgroundColor(new java.awt.Color(230, 230, 230)); // light gray
        table.addCell(cell);
    }

    private PdfPCell bodyCell(String text, int align) {
        Font bodyFont = new Font(Font.HELVETICA, 10);
        PdfPCell cell = new PdfPCell(new Phrase(text, bodyFont));
        cell.setHorizontalAlignment(align);
        cell.setPadding(7);
        return cell;
    }

    private PdfPCell totalsLabel(String text, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setBorder(Rectangle.BOX);
        cell.setPadding(7);
        return cell;
    }

    private PdfPCell totalsValue(String text, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        cell.setBorder(Rectangle.BOX);
        cell.setPadding(7);
        return cell;
    }

    private String formatMoney(Double v) {
        if (v == null) return "0.00";
        return String.format("%.2f", v);
    }

    private String safe(String v) {
        return v == null ? "" : v.trim();
    }


}
