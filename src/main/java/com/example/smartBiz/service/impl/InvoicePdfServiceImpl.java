package com.example.smartBiz.service.impl;

import com.example.smartBiz.entity.Business;
import com.example.smartBiz.entity.Invoice;
import com.example.smartBiz.entity.InvoiceItem;
import com.example.smartBiz.entity.BusinessProfile;
import com.example.smartBiz.exception.ResourceNotFoundException;
import com.example.smartBiz.repository.BusinessProfileRepo;
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
import java.util.Base64;

@Service
public class InvoicePdfServiceImpl implements InvoicePdfService {

    private final InvoiceRepo invoiceRepository;
    private final BusinessRepo businessRepo;
    private final BusinessProfileRepo profileRepo;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm");

    public InvoicePdfServiceImpl(
            InvoiceRepo invoiceRepository,
            BusinessRepo businessRepo,
            BusinessProfileRepo profileRepo) {
        this.invoiceRepository = invoiceRepository;
        this.businessRepo = businessRepo;
        this.profileRepo = profileRepo;
    }

    /**
     * Page event handler for adding page numbers to each page.
     */
    private static class PageNumberEvent extends PdfPageEventHelper {
        private final Font footerFont = new Font(Font.HELVETICA, 9, Font.NORMAL);
        private final String businessName;

        public PageNumberEvent(String businessName) {
            this.businessName = businessName;
        }

        @Override
        public void onEndPage(PdfWriter writer, Document document) {
            PdfContentByte cb = writer.getDirectContent();
            Phrase footer = new Phrase(
                String.format("%s | Page %d", businessName, writer.getPageNumber()),
                footerFont
            );
            ColumnText.showTextAligned(cb, Element.ALIGN_CENTER,
                footer,
                (document.right() + document.left()) / 2,
                document.bottom() - 20,
                0);
        }
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

        // Fetch Business Profile
        BusinessProfile profile = profileRepo.findByBusinessId(invoice.getBusinessId())
                .orElse(null);

        String businessName = (profile != null && profile.getBusinessName() != null) ? profile.getBusinessName() : "SmartBiz";
        String ownerName = (profile != null && profile.getOwnerName() != null) ? profile.getOwnerName() : "";
        String taglineStr = (profile != null && profile.getBrandTagline() != null) ? profile.getBrandTagline() : "AI-Powered Business Management Suite";
        String businessAddress = (profile != null && profile.getAddress() != null) ? profile.getAddress() : "";
        String businessPhone = (profile != null && profile.getPhone() != null) ? profile.getPhone() : "";
        String currency = (profile != null && profile.getCurrency() != null && !profile.getCurrency().isBlank()) ? profile.getCurrency() : "";
        java.awt.Color brandColor = (profile != null && profile.getBrandColor() != null) 
                ? java.awt.Color.decode(profile.getBrandColor().startsWith("#") ? profile.getBrandColor() : "#" + profile.getBrandColor()) 
                : new java.awt.Color(230, 230, 230);

        // ── PDF generation ────────────────────────────────────────────────────
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {

            Document document = new Document(PageSize.A4, 36, 36, 36, 36);
            PdfWriter writer = PdfWriter.getInstance(document, baos);
            document.open();

            // Logo
            try {
                if (profile != null && profile.getLogo() != null && !profile.getLogo().isBlank()) {
                    String logoData = profile.getLogo();
                    if (logoData.contains(",")) {
                        logoData = logoData.split(",")[1];
                    }
                    byte[] imageBytes = Base64.getDecoder().decode(logoData);
                    Image logo = Image.getInstance(imageBytes);
                    logo.scaleToFit(90, 90);
                    logo.setAlignment(Image.ALIGN_RIGHT);
                    document.add(logo);
                } else {
                    ClassPathResource resource = new ClassPathResource("static/mush.png");
                    try (InputStream in = resource.getInputStream()) {
                        Image logo = Image.getInstance(in.readAllBytes());
                        logo.scaleToFit(90, 90);
                        logo.setAlignment(Image.ALIGN_RIGHT);
                        document.add(logo);
                    }
                }
            } catch (Exception ignored) {
            }

            // ── 1. Company header ─────────────────────────────────────────────
            Font companyFont = new Font(Font.HELVETICA, 16, Font.BOLD);
            Font ownerFont = new Font(Font.HELVETICA, 11, Font.ITALIC);
            Font small = new Font(Font.HELVETICA, 10);

            Paragraph company = new Paragraph(businessName, companyFont);
            company.setAlignment(Element.ALIGN_LEFT);
            document.add(company);

            if (!ownerName.isBlank()) {
                Paragraph owner = new Paragraph("Owner: " + ownerName, ownerFont);
                owner.setAlignment(Element.ALIGN_LEFT);
                document.add(owner);
            }

            Paragraph tagline = new Paragraph(taglineStr, small);
            tagline.setAlignment(Element.ALIGN_LEFT);
            document.add(tagline);

            if (!businessAddress.isBlank() || !businessPhone.isBlank()) {
                StringBuilder contactInfo = new StringBuilder();
                if (!businessAddress.isBlank()) contactInfo.append(businessAddress);
                if (!businessAddress.isBlank() && !businessPhone.isBlank()) contactInfo.append(" | ");
                if (!businessPhone.isBlank()) contactInfo.append("Tel: ").append(businessPhone);
                
                Paragraph contact = new Paragraph(contactInfo.toString(), small);
                contact.setAlignment(Element.ALIGN_LEFT);
                document.add(contact);
            }

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
            PdfPTable table = new PdfPTable(6);
            table.setWidthPercentage(100);
            table.setSpacingBefore(5);
            table.setWidths(new float[] { 1f, 4f, 1f, 1.5f, 1.5f, 2f });

            addHeaderCell(table, "No", brandColor);
            addHeaderCell(table, "Product", brandColor);
            addHeaderCell(table, "Qty", brandColor);
            addHeaderCell(table, "Unit Price", brandColor);
            addHeaderCell(table, "Discount", brandColor);
            addHeaderCell(table, "Line Total", brandColor);

            int index = 1;
            double subtotalVal = 0.0;
            for (InvoiceItem item : invoice.getItems()) {
                String productName = (item.getProduct() != null)
                        ? safe(item.getProduct().getName())
                        : "Unknown Product";

                double itemSubtotal = (item.getUnitPrice() != null ? item.getUnitPrice() : 0.0) * (item.getQuantity() != null ? item.getQuantity() : 0);
                subtotalVal += itemSubtotal;

                table.addCell(bodyCell(String.valueOf(index++), Element.ALIGN_CENTER));
                table.addCell(bodyCell(productName, Element.ALIGN_LEFT));
                table.addCell(bodyCell(String.valueOf(item.getQuantity()), Element.ALIGN_CENTER));
                table.addCell(bodyCell(formatMoney(item.getUnitPrice(), currency), Element.ALIGN_RIGHT));
                table.addCell(bodyCell(formatMoney(item.getDiscountAmount(), currency), Element.ALIGN_RIGHT));
                table.addCell(bodyCell(formatMoney(item.getLineTotal(), currency), Element.ALIGN_RIGHT));
            }

            document.add(table);

            // ── 5. Totals breakdown ───────────────────────────────────────────
            document.add(new Paragraph(" "));

            PdfPTable totals = new PdfPTable(2);
            totals.setWidthPercentage(45);
            totals.setHorizontalAlignment(Element.ALIGN_RIGHT);
            totals.setWidths(new float[] { 1.5f, 1f });

            totals.addCell(totalsLabel("Subtotal", value));
            totals.addCell(totalsValue(formatMoney(subtotalVal, currency), value));

            if (invoice.getTotalDiscount() != null && invoice.getTotalDiscount() > 0) {
                totals.addCell(totalsLabel("Total Discount", value));
                totals.addCell(totalsValue("-" + formatMoney(invoice.getTotalDiscount(), currency), value));
            }

            totals.addCell(totalsLabel("Grand Total", label));
            totals.addCell(totalsValue(formatMoney(invoice.getTotalAmount(), currency), label));

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

    @Override
    public byte[] generateInvoicePdfForPrint(Long invoiceId) {
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

        return generatePrintOptimizedPdf(invoice);
    }

    private byte[] generatePrintOptimizedPdf(Invoice invoice) {
        // Fetch Business Profile
        BusinessProfile profile = profileRepo.findByBusinessId(invoice.getBusinessId())
                .orElse(null);

        String businessName = (profile != null && profile.getBusinessName() != null) ? profile.getBusinessName() : "SmartBiz";
        String ownerName = (profile != null && profile.getOwnerName() != null) ? profile.getOwnerName() : "";
        String taglineStr = (profile != null && profile.getBrandTagline() != null) ? profile.getBrandTagline() : "AI-Powered Business Management Suite";
        String businessAddress = (profile != null && profile.getAddress() != null) ? profile.getAddress() : "";
        String businessPhone = (profile != null && profile.getPhone() != null) ? profile.getPhone() : "";
        String currency = (profile != null && profile.getCurrency() != null && !profile.getCurrency().isBlank()) ? profile.getCurrency() : "";
        java.awt.Color brandColor = (profile != null && profile.getBrandColor() != null)
                ? java.awt.Color.decode(profile.getBrandColor().startsWith("#") ? profile.getBrandColor() : "#" + profile.getBrandColor())
                : new java.awt.Color(230, 230, 230);

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {

            Document document = new Document(PageSize.A4, 36, 36, 36, 36);
            PdfWriter writer = PdfWriter.getInstance(document, baos);
            writer.setPageEvent(new PageNumberEvent(businessName));
            document.open();

            // Logo
            addLogo(document, profile);

            // ── 1. Company header ─────────────────────────────────────────────
            addCompanyHeader(document, businessName, ownerName, taglineStr, businessAddress, businessPhone);

            // ── 2. Title + PAID stamp ─────────────────────────────────────────
            addInvoiceTitle(document, invoice);

            // ── 3. Invoice details ────────────────────────────────────────────
            addInvoiceDetails(document, invoice);

            // ── 4. Items table (ENHANCED with discount details) ───────────────
            double subtotalBeforeDiscounts = addEnhancedItemsTable(document, invoice, brandColor, currency);

            // ── 5. Totals breakdown (ENHANCED with discount summary) ────────────
            addEnhancedTotals(document, invoice, subtotalBeforeDiscounts, currency);

            // ── 6. Footer ──────────────────────────────────────────────────────
            addPrintFooter(document);

            document.close();
            writer.close();

            return baos.toByteArray();

        } catch (Exception e) {
            throw new ResourceNotFoundException("PDF generation failed: " + e.getMessage(), e);
        }
    }

    private void addLogo(Document document, BusinessProfile profile) throws Exception {
        try {
            if (profile != null && profile.getLogo() != null && !profile.getLogo().isBlank()) {
                String logoData = profile.getLogo();
                if (logoData.contains(",")) {
                    logoData = logoData.split(",")[1];
                }
                byte[] imageBytes = Base64.getDecoder().decode(logoData);
                Image logo = Image.getInstance(imageBytes);
                logo.scaleToFit(90, 90);
                logo.setAlignment(Image.ALIGN_RIGHT);
                document.add(logo);
            } else {
                ClassPathResource resource = new ClassPathResource("static/mush.png");
                try (InputStream in = resource.getInputStream()) {
                    Image logo = Image.getInstance(in.readAllBytes());
                    logo.scaleToFit(90, 90);
                    logo.setAlignment(Image.ALIGN_RIGHT);
                    document.add(logo);
                }
            }
        } catch (Exception ignored) {
        }
    }

    private void addCompanyHeader(Document document, String businessName, String ownerName,
                                   String taglineStr, String businessAddress, String businessPhone) throws DocumentException {
        Font companyFont = new Font(Font.HELVETICA, 16, Font.BOLD);
        Font ownerFont = new Font(Font.HELVETICA, 11, Font.ITALIC);
        Font small = new Font(Font.HELVETICA, 10);

        Paragraph company = new Paragraph(businessName, companyFont);
        company.setAlignment(Element.ALIGN_LEFT);
        document.add(company);

        if (!ownerName.isBlank()) {
            Paragraph owner = new Paragraph("Owner: " + ownerName, ownerFont);
            owner.setAlignment(Element.ALIGN_LEFT);
            document.add(owner);
        }

        Paragraph tagline = new Paragraph(taglineStr, small);
        tagline.setAlignment(Element.ALIGN_LEFT);
        document.add(tagline);

        if (!businessAddress.isBlank() || !businessPhone.isBlank()) {
            StringBuilder contactInfo = new StringBuilder();
            if (!businessAddress.isBlank()) contactInfo.append(businessAddress);
            if (!businessAddress.isBlank() && !businessPhone.isBlank()) contactInfo.append(" | ");
            if (!businessPhone.isBlank()) contactInfo.append("Tel: ").append(businessPhone);

            Paragraph contact = new Paragraph(contactInfo.toString(), small);
            contact.setAlignment(Element.ALIGN_LEFT);
            document.add(contact);
        }

        document.add(new Paragraph(" "));
        addLine(document);
    }

    private void addInvoiceTitle(Document document, Invoice invoice) throws DocumentException {
        Font titleFont = new Font(Font.HELVETICA, 18, Font.BOLD);
        Paragraph title = new Paragraph("INVOICE", titleFont);
        title.setAlignment(Element.ALIGN_CENTER);
        document.add(title);

        boolean isPaid = invoice.getStatus() != null
                && "PAID".equalsIgnoreCase(invoice.getStatus().name());
        if (isPaid)
            addPaidStamp(document);

        document.add(new Paragraph(" "));
    }

    private void addInvoiceDetails(Document document, Invoice invoice) throws DocumentException {
        Font label = new Font(Font.HELVETICA, 10, Font.BOLD);
        Font value = new Font(Font.HELVETICA, 10);

        String customerName = safe(invoice.getCustomer().getName());
        String customerPhone = safe(invoice.getCustomer().getPhone());
        String customerAddress = safe(invoice.getCustomer().getAddress());

        String invoiceDate = invoice.getInvoiceDate() != null
                ? invoice.getInvoiceDate().format(DATE_FMT)
                : "N/A";
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
    }

    private double addEnhancedItemsTable(Document document, Invoice invoice, java.awt.Color brandColor, String currency) throws DocumentException {
        // Table with 8 columns: No, Product, Qty, Unit Price, Discount %, Discount Amt, Final Price, Line Total
        PdfPTable table = new PdfPTable(8);
        table.setWidthPercentage(100);
        table.setSpacingBefore(5);
        table.setWidths(new float[] { 0.6f, 3f, 0.8f, 1.5f, 1.2f, 1.5f, 1.5f, 1.5f });

        addEnhancedHeaderCell(table, "No", brandColor);
        addEnhancedHeaderCell(table, "Product", brandColor);
        addEnhancedHeaderCell(table, "Qty", brandColor);
        addEnhancedHeaderCell(table, "Unit Price", brandColor);
        addEnhancedHeaderCell(table, "Disc %", brandColor);
        addEnhancedHeaderCell(table, "Disc Amt", brandColor);
        addEnhancedHeaderCell(table, "Final Price", brandColor);
        addEnhancedHeaderCell(table, "Line Total", brandColor);

        int index = 1;
        double subtotalBeforeDiscounts = 0.0;

        for (InvoiceItem item : invoice.getItems()) {
            String productName = (item.getProduct() != null)
                    ? safe(item.getProduct().getName())
                    : "Unknown Product";

            double qty = item.getQuantity() != null ? item.getQuantity() : 0;
            double unitPrice = item.getUnitPrice() != null ? item.getUnitPrice() : 0.0;
            double discountPct = item.getDiscountPercentage() != null ? item.getDiscountPercentage() : 0.0;
            double discountAmt = item.getDiscountAmount() != null ? item.getDiscountAmount() : 0.0;
            double lineTotal = item.getLineTotal() != null ? item.getLineTotal() : 0.0;
            double finalUnitPrice = lineTotal / (qty > 0 ? qty : 1);

            double itemSubtotal = unitPrice * qty;
            subtotalBeforeDiscounts += itemSubtotal;

            table.addCell(bodyCell(String.valueOf(index++), Element.ALIGN_CENTER));
            table.addCell(bodyCell(productName, Element.ALIGN_LEFT));
            table.addCell(bodyCell(String.valueOf((int) qty), Element.ALIGN_CENTER));
            table.addCell(bodyCell(formatMoney(unitPrice, currency), Element.ALIGN_RIGHT));
            table.addCell(bodyCell(discountPct > 0 ? formatMoney(discountPct, "") + "%" : "-", Element.ALIGN_CENTER));
            table.addCell(bodyCell(discountAmt > 0 ? "-" + formatMoney(discountAmt, currency) : "-", Element.ALIGN_RIGHT));
            table.addCell(bodyCell(formatMoney(finalUnitPrice, currency), Element.ALIGN_RIGHT));
            table.addCell(bodyCell(formatMoney(lineTotal, currency), Element.ALIGN_RIGHT));
        }

        document.add(table);
        return subtotalBeforeDiscounts;
    }

    private void addEnhancedHeaderCell(PdfPTable table, String text, java.awt.Color bgColor) {
        Font headFont = new Font(Font.HELVETICA, 9, Font.BOLD);
        PdfPCell cell = new PdfPCell(new Phrase(text, headFont));
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setPadding(6);
        cell.setBackgroundColor(bgColor);
        table.addCell(cell);
    }

    private void addEnhancedTotals(Document document, Invoice invoice, double subtotalBeforeDiscounts, String currency) throws DocumentException {
        Font label = new Font(Font.HELVETICA, 10, Font.BOLD);
        Font value = new Font(Font.HELVETICA, 10);
        Font highlight = new Font(Font.HELVETICA, 11, Font.BOLD);

        document.add(new Paragraph(" "));

        // Discount Summary Box
        if (invoice.getTotalDiscount() != null && invoice.getTotalDiscount() > 0) {
            PdfPTable discountSummary = new PdfPTable(2);
            discountSummary.setWidthPercentage(60);
            discountSummary.setHorizontalAlignment(Element.ALIGN_RIGHT);
            discountSummary.setSpacingBefore(10);
            discountSummary.setSpacingAfter(10);
            discountSummary.setWidths(new float[] { 2f, 1f });

            // Title
            PdfPCell titleCell = new PdfPCell(new Phrase("DISCOUNT SUMMARY", new Font(Font.HELVETICA, 11, Font.BOLD)));
            titleCell.setColspan(2);
            titleCell.setHorizontalAlignment(Element.ALIGN_CENTER);
            titleCell.setBackgroundColor(new java.awt.Color(240, 240, 240));
            titleCell.setPadding(6);
            discountSummary.addCell(titleCell);

            discountSummary.addCell(totalsLabel("Original Subtotal", value));
            discountSummary.addCell(totalsValue(formatMoney(subtotalBeforeDiscounts, currency), value));

            discountSummary.addCell(totalsLabel("Total Discounts", value));
            discountSummary.addCell(totalsValue("-" + formatMoney(invoice.getTotalDiscount(), currency), value));

            document.add(discountSummary);
        }

        // Grand Total
        PdfPTable totals = new PdfPTable(2);
        totals.setWidthPercentage(45);
        totals.setHorizontalAlignment(Element.ALIGN_RIGHT);
        totals.setWidths(new float[] { 1.5f, 1f });

        totals.addCell(totalsLabel("Grand Total", highlight));
        totals.addCell(totalsValue(formatMoney(invoice.getTotalAmount(), currency), highlight));

        document.add(totals);
    }

    private void addPrintFooter(Document document) throws DocumentException {
        Font small = new Font(Font.HELVETICA, 9, Font.ITALIC);
        document.add(new Paragraph(" "));
        addLine(document);
        Paragraph thankYou = new Paragraph("Thank you for your business!", small);
        thankYou.setAlignment(Element.ALIGN_CENTER);
        document.add(thankYou);
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

    private void addHeaderCell(PdfPTable table, String text, java.awt.Color bgColor) {
        Font headFont = new Font(Font.HELVETICA, 10, Font.BOLD);
        PdfPCell cell = new PdfPCell(new Phrase(text, headFont));
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setPadding(7);
        cell.setBackgroundColor(bgColor);
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

    private String formatMoney(Double v, String currency) {
        String amount = v == null ? "0.00" : String.format("%.2f", v);
        return (currency != null && !currency.isBlank()) ? currency + " " + amount : amount;
    }

    private String safe(String v) {
        return v == null ? "" : v.trim();
    }
}