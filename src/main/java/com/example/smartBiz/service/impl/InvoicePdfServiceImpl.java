package com.example.smartBiz.service.impl;

import com.example.smartBiz.entity.Business;
import com.example.smartBiz.entity.Invoice;
import com.example.smartBiz.entity.InvoiceItem;
import com.example.smartBiz.exception.ResourceNotFoundException;
import com.example.smartBiz.repository.BusinessRepo;
import com.example.smartBiz.repository.InvoiceRepo;
import com.example.smartBiz.security.CustomUserPrincipal;
import com.example.smartBiz.service.InvoicePdfService;
import org.springframework.security.core.context.SecurityContextHolder;
import com.lowagie.text.*;
import com.lowagie.text.Image;
import com.lowagie.text.pdf.*;
import com.lowagie.text.pdf.draw.LineSeparator;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.time.format.DateTimeFormatter;

@Service
public class InvoicePdfServiceImpl implements InvoicePdfService {

    private final InvoiceRepo invoiceRepository;
    private final BusinessRepo businessRepo;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm");

    public InvoicePdfServiceImpl(
            InvoiceRepo invoiceRepository,
            BusinessRepo businessRepo) {
        this.invoiceRepository = invoiceRepository;
        this.businessRepo = businessRepo;
    }

    @Override
    public byte[] generateInvoicePdf(Long invoiceId) {

        CustomUserPrincipal principal = CustomUserPrincipal.getCurrent();
        if (principal == null)
            throw new ResourceNotFoundException("Unauthorized");

        Long businessId = principal.getBusinessId();

        // ADMIN bypasses ownership checks
        boolean isAdmin = SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        if (!isAdmin) {
            if (businessId == null)
                throw new ResourceNotFoundException("Business context missing (JWT required)");

            Business b = businessRepo.findById(businessId)
                    .orElseThrow(() -> new ResourceNotFoundException("Business not found"));

            if (Boolean.FALSE.equals(b.getActive()))
                throw new ResourceNotFoundException("Business account is disabled");
        }

        Invoice invoice = invoiceRepository.findInvoiceForPdf(invoiceId)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice not found"));

        if (!isAdmin && !businessId.equals(invoice.getBusinessId()))
            throw new ResourceNotFoundException("Access denied: invoice not in your business");

        // ── PDF generation ────────────────────────────────────────────────────
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {

            Document document = new Document(PageSize.A4, 36, 36, 36, 36);
            PdfWriter writer = PdfWriter.getInstance(document, baos);
            document.open();

            // Logo
            try {
                ClassPathResource resource = new ClassPathResource("static/mush.png");
                try (InputStream in = resource.getInputStream()) {
                    Image logo = Image.getInstance(in.readAllBytes());
                    logo.scaleToFit(90, 90);
                    logo.setAlignment(Image.ALIGN_RIGHT);
                    document.add(logo);
                }
            } catch (Exception ignored) {
                // Logo is optional — silently skip if missing
            }

            // ── 1. Company header ─────────────────────────────────────────────
            Font companyFont = new Font(Font.HELVETICA, 16, Font.BOLD);
            Font small = new Font(Font.HELVETICA, 10);

            Paragraph company = new Paragraph("SmartBiz", companyFont);
            company.setAlignment(Element.ALIGN_LEFT);

            Paragraph tagline = new Paragraph("AI-Powered Business Management Suite", small);
            tagline.setAlignment(Element.ALIGN_LEFT);

            document.add(company);
            document.add(tagline);
            document.add(new Paragraph(" "));
            addLine(document);

            // ── 2. Title + PAID stamp ─────────────────────────────────────────
            Font titleFont = new Font(Font.HELVETICA, 18, Font.BOLD);
            Paragraph title = new Paragraph("INVOICE", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            document.add(title);

            boolean isPaid = invoice.getStatus() != null
                    && "PAID".equalsIgnoreCase(invoice.getStatus().name());
            if (isPaid)
                addPaidStamp(document);

            document.add(new Paragraph(" "));

            // ── 3. Invoice details ────────────────────────────────────────────
            Font label = new Font(Font.HELVETICA, 10, Font.BOLD);
            Font value = new Font(Font.HELVETICA, 10);

            String customerName = safe(invoice.getCustomer().getName());
            String customerPhone = safe(invoice.getCustomer().getPhone());
            String customerAddress = safe(invoice.getCustomer().getAddress());

            // FIX: format invoiceDate properly instead of calling String.valueOf()
            String invoiceDate = invoice.getInvoiceDate() != null
                    ? invoice.getInvoiceDate().format(DATE_FMT)
                    : "N/A";

            // FIX: null-safe status — guard against null before calling .name()
            String statusText = invoice.getStatus() != null
                    ? invoice.getStatus().name()
                    : "UNKNOWN";

            PdfPTable infoTable = new PdfPTable(2);
            infoTable.setWidthPercentage(100);
            infoTable.setWidths(new float[] { 1f, 1f });

            PdfPCell left = new PdfPCell();
            left.setBorder(Rectangle.NO_BORDER);
            left.addElement(new Paragraph("Bill To:", label));
            left.addElement(new Paragraph(customerName, value));
            if (!customerPhone.isBlank())
                left.addElement(new Paragraph("Phone: " + customerPhone, value));
            if (!customerAddress.isBlank())
                left.addElement(new Paragraph("Address: " + customerAddress, value));

            PdfPCell right = new PdfPCell();
            right.setBorder(Rectangle.NO_BORDER);
            right.setHorizontalAlignment(Element.ALIGN_RIGHT);
            right.addElement(rightAligned("Invoice No: " + safe(invoice.getInvoiceNumber()), label));
            right.addElement(rightAligned("Date: " + invoiceDate, value));
            right.addElement(rightAligned("Status: " + statusText, value));

            infoTable.addCell(left);
            infoTable.addCell(right);

            document.add(infoTable);
            document.add(new Paragraph(" "));
            addLine(document);
            document.add(new Paragraph(" "));

            // ── 4. Items table ────────────────────────────────────────────────
            PdfPTable table = new PdfPTable(5);
            table.setWidthPercentage(100);
            table.setSpacingBefore(5);
            table.setWidths(new float[] { 1.2f, 4.8f, 1.2f, 2f, 2f });

            addHeaderCell(table, "No");
            addHeaderCell(table, "Product");
            addHeaderCell(table, "Qty");
            addHeaderCell(table, "Unit Price");
            addHeaderCell(table, "Line Total");

            int index = 1;
            for (InvoiceItem item : invoice.getItems()) {
                // FIX: null-safe product name — item.getProduct() could be null
                String productName = (item.getProduct() != null)
                        ? safe(item.getProduct().getName())
                        : "Unknown Product";

                table.addCell(bodyCell(String.valueOf(index++), Element.ALIGN_CENTER));
                table.addCell(bodyCell(productName, Element.ALIGN_LEFT));
                table.addCell(bodyCell(String.valueOf(item.getQuantity()), Element.ALIGN_CENTER));
                table.addCell(bodyCell(formatMoney(item.getUnitPrice()), Element.ALIGN_RIGHT));
                table.addCell(bodyCell(formatMoney(item.getLineTotal()), Element.ALIGN_RIGHT));
            }

            document.add(table);

            // ── 5. Grand total ────────────────────────────────────────────────
            document.add(new Paragraph(" "));

            PdfPTable totals = new PdfPTable(2);
            totals.setWidthPercentage(40);
            totals.setHorizontalAlignment(Element.ALIGN_RIGHT);
            totals.setWidths(new float[] { 1.5f, 1f });

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
            throw new ResourceNotFoundException("PDF generation failed: " + e.getMessage(), e);
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

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
        stamp.setSpacingBefore(-30);
        document.add(stamp);
    }

    private void addHeaderCell(PdfPTable table, String text) {
        Font headFont = new Font(Font.HELVETICA, 10, Font.BOLD);
        PdfPCell cell = new PdfPCell(new Phrase(text, headFont));
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setPadding(7);
        cell.setBackgroundColor(new java.awt.Color(230, 230, 230));
        table.addCell(cell);
    }

    private PdfPCell bodyCell(String text, int align) {
        PdfPCell cell = new PdfPCell(new Phrase(text, new Font(Font.HELVETICA, 10)));
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
        return v == null ? "0.00" : String.format("%.2f", v);
    }

    private String safe(String v) {
        return v == null ? "" : v.trim();
    }
}