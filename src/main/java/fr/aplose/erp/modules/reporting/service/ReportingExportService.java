package fr.aplose.erp.modules.reporting.service;

import fr.aplose.erp.modules.commerce.entity.Invoice;
import fr.aplose.erp.modules.commerce.repository.InvoiceRepository;
import fr.aplose.erp.modules.leave.entity.LeaveRequest;
import fr.aplose.erp.modules.leave.repository.LeaveRequestRepository;
import fr.aplose.erp.tenant.context.TenantContext;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ReportingExportService {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ISO_LOCAL_DATE;

    private final InvoiceRepository invoiceRepository;
    private final LeaveRequestRepository leaveRequestRepository;

    // --- Sales report ---

    @Transactional(readOnly = true)
    public byte[] exportSalesCsv(LocalDate from, LocalDate to) {
        BigDecimal revenue = invoiceRepository.sumTotalAmountByTenantAndTypeAndDateRange(
                TenantContext.getCurrentTenantId(), "SALES", from, to);
        revenue = revenue != null ? revenue : BigDecimal.ZERO;
        String header = "from,to,revenue_eur";
        String row = csvEscape(from.format(DATE_FMT)) + "," + csvEscape(to.format(DATE_FMT)) + "," + csvEscape(revenue.toPlainString());
        return (header + "\n" + row).getBytes(StandardCharsets.UTF_8);
    }

    @Transactional(readOnly = true)
    public byte[] exportSalesExcel(LocalDate from, LocalDate to) throws Exception {
        BigDecimal revenue = invoiceRepository.sumTotalAmountByTenantAndTypeAndDateRange(
                TenantContext.getCurrentTenantId(), "SALES", from, to);
        revenue = revenue != null ? revenue : BigDecimal.ZERO;
        try (Workbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("Sales");
            Row headerRow = sheet.createRow(0);
            headerRow.createCell(0).setCellValue("From");
            headerRow.createCell(1).setCellValue("To");
            headerRow.createCell(2).setCellValue("Revenue (EUR)");
            Row dataRow = sheet.createRow(1);
            dataRow.createCell(0).setCellValue(from.format(DATE_FMT));
            dataRow.createCell(1).setCellValue(to.format(DATE_FMT));
            dataRow.createCell(2).setCellValue(revenue.doubleValue());
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            wb.write(out);
            return out.toByteArray();
        }
    }

    @Transactional(readOnly = true)
    public byte[] exportSalesPdf(LocalDate from, LocalDate to) throws DocumentException {
        BigDecimal revenue = invoiceRepository.sumTotalAmountByTenantAndTypeAndDateRange(
                TenantContext.getCurrentTenantId(), "SALES", from, to);
        revenue = revenue != null ? revenue : BigDecimal.ZERO;
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document doc = new Document(PageSize.A4);
        PdfWriter.getInstance(doc, out);
        doc.open();
        doc.add(new Paragraph("Sales revenue by period", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14)));
        doc.add(new Paragraph(" "));
        doc.add(new Paragraph("From: " + from.format(DATE_FMT) + " — To: " + to.format(DATE_FMT)));
        doc.add(new Paragraph("Total revenue (EUR): " + revenue.toPlainString()));
        doc.close();
        return out.toByteArray();
    }

    // --- Overdue invoices ---

    @Transactional(readOnly = true)
    public byte[] exportOverdueInvoicesCsv() {
        List<Invoice> list = invoiceRepository.findOverdue(TenantContext.getCurrentTenantId(), LocalDate.now());
        StringBuilder sb = new StringBuilder();
        sb.append("reference,third_party,date_issued,date_due,total_amount,amount_remaining,status\n");
        for (Invoice inv : list) {
            sb.append(csvEscape(inv.getReference())).append(",")
              .append(csvEscape(inv.getThirdParty() != null ? inv.getThirdParty().getName() : "")).append(",")
              .append(csvEscape(inv.getDateIssued() != null ? inv.getDateIssued().format(DATE_FMT) : "")).append(",")
              .append(csvEscape(inv.getDateDue() != null ? inv.getDateDue().format(DATE_FMT) : "")).append(",")
              .append(csvEscape(inv.getTotalAmount() != null ? inv.getTotalAmount().toPlainString() : "")).append(",")
              .append(csvEscape(inv.getAmountRemaining() != null ? inv.getAmountRemaining().toPlainString() : "")).append(",")
              .append(csvEscape(inv.getStatus())).append("\n");
        }
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    @Transactional(readOnly = true)
    public byte[] exportOverdueInvoicesExcel() throws Exception {
        List<Invoice> list = invoiceRepository.findOverdue(TenantContext.getCurrentTenantId(), LocalDate.now());
        try (Workbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("Overdue invoices");
            Row headerRow = sheet.createRow(0);
            String[] headers = {"Reference", "Third party", "Date issued", "Date due", "Total", "Remaining", "Status"};
            for (int i = 0; i < headers.length; i++) headerRow.createCell(i).setCellValue(headers[i]);
            int rowNum = 1;
            for (Invoice inv : list) {
                Row r = sheet.createRow(rowNum++);
                r.createCell(0).setCellValue(inv.getReference());
                r.createCell(1).setCellValue(inv.getThirdParty() != null ? inv.getThirdParty().getName() : "");
                r.createCell(2).setCellValue(inv.getDateIssued() != null ? inv.getDateIssued().format(DATE_FMT) : "");
                r.createCell(3).setCellValue(inv.getDateDue() != null ? inv.getDateDue().format(DATE_FMT) : "");
                r.createCell(4).setCellValue(inv.getTotalAmount() != null ? inv.getTotalAmount().doubleValue() : 0);
                r.createCell(5).setCellValue(inv.getAmountRemaining() != null ? inv.getAmountRemaining().doubleValue() : 0);
                r.createCell(6).setCellValue(inv.getStatus());
            }
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            wb.write(out);
            return out.toByteArray();
        }
    }

    @Transactional(readOnly = true)
    public byte[] exportOverdueInvoicesPdf() throws DocumentException {
        List<Invoice> list = invoiceRepository.findOverdue(TenantContext.getCurrentTenantId(), LocalDate.now());
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document doc = new Document(PageSize.A4.rotate());
        PdfWriter.getInstance(doc, out);
        doc.open();
        doc.add(new Paragraph("Overdue invoices", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14)));
        doc.add(new Paragraph(" "));
        PdfPTable table = new PdfPTable(7);
        table.setWidths(new float[]{1.2f, 2f, 1f, 1f, 1f, 1f, 0.8f});
        table.addCell(new PdfPCell(new Phrase("Reference", FontFactory.getFont(FontFactory.HELVETICA_BOLD))));
        table.addCell(new PdfPCell(new Phrase("Third party", FontFactory.getFont(FontFactory.HELVETICA_BOLD))));
        table.addCell(new PdfPCell(new Phrase("Issued", FontFactory.getFont(FontFactory.HELVETICA_BOLD))));
        table.addCell(new PdfPCell(new Phrase("Due", FontFactory.getFont(FontFactory.HELVETICA_BOLD))));
        table.addCell(new PdfPCell(new Phrase("Total", FontFactory.getFont(FontFactory.HELVETICA_BOLD))));
        table.addCell(new PdfPCell(new Phrase("Remaining", FontFactory.getFont(FontFactory.HELVETICA_BOLD))));
        table.addCell(new PdfPCell(new Phrase("Status", FontFactory.getFont(FontFactory.HELVETICA_BOLD))));
        for (Invoice inv : list) {
            table.addCell(inv.getReference());
            table.addCell(inv.getThirdParty() != null ? inv.getThirdParty().getName() : "");
            table.addCell(inv.getDateIssued() != null ? inv.getDateIssued().format(DATE_FMT) : "");
            table.addCell(inv.getDateDue() != null ? inv.getDateDue().format(DATE_FMT) : "");
            table.addCell(inv.getTotalAmount() != null ? inv.getTotalAmount().toPlainString() : "");
            table.addCell(inv.getAmountRemaining() != null ? inv.getAmountRemaining().toPlainString() : "");
            table.addCell(inv.getStatus());
        }
        doc.add(table);
        doc.close();
        return out.toByteArray();
    }

    // --- Leave report ---

    @Transactional(readOnly = true)
    public byte[] exportLeaveCsv(String status) {
        List<LeaveRequest> list = getLeaveList(status);
        StringBuilder sb = new StringBuilder();
        sb.append("requester,leave_type,date_start,date_end,status\n");
        for (LeaveRequest lr : list) {
            sb.append(csvEscape(lr.getRequester() != null ? lr.getRequester().getDisplayName() : "")).append(",")
              .append(csvEscape(lr.getLeaveType() != null ? lr.getLeaveType().getLabel() : "")).append(",")
              .append(csvEscape(lr.getDateStart() != null ? lr.getDateStart().format(DATE_FMT) : "")).append(",")
              .append(csvEscape(lr.getDateEnd() != null ? lr.getDateEnd().format(DATE_FMT) : "")).append(",")
              .append(csvEscape(lr.getStatus())).append("\n");
        }
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    @Transactional(readOnly = true)
    public byte[] exportLeaveExcel(String status) throws Exception {
        List<LeaveRequest> list = getLeaveList(status);
        try (Workbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("Leave requests");
            Row headerRow = sheet.createRow(0);
            String[] headers = {"Requester", "Leave type", "Date start", "Date end", "Status"};
            for (int i = 0; i < headers.length; i++) headerRow.createCell(i).setCellValue(headers[i]);
            int rowNum = 1;
            for (LeaveRequest lr : list) {
                Row r = sheet.createRow(rowNum++);
                r.createCell(0).setCellValue(lr.getRequester() != null ? lr.getRequester().getDisplayName() : "");
                r.createCell(1).setCellValue(lr.getLeaveType() != null ? lr.getLeaveType().getLabel() : "");
                r.createCell(2).setCellValue(lr.getDateStart() != null ? lr.getDateStart().format(DATE_FMT) : "");
                r.createCell(3).setCellValue(lr.getDateEnd() != null ? lr.getDateEnd().format(DATE_FMT) : "");
                r.createCell(4).setCellValue(lr.getStatus());
            }
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            wb.write(out);
            return out.toByteArray();
        }
    }

    @Transactional(readOnly = true)
    public byte[] exportLeavePdf(String status) throws DocumentException {
        List<LeaveRequest> list = getLeaveList(status);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document doc = new Document(PageSize.A4.rotate());
        PdfWriter.getInstance(doc, out);
        doc.open();
        doc.add(new Paragraph("Leave requests report", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14)));
        doc.add(new Paragraph(" "));
        PdfPTable table = new PdfPTable(5);
        table.setWidths(new float[]{2f, 1.5f, 1f, 1f, 1f});
        table.addCell(new PdfPCell(new Phrase("Requester", FontFactory.getFont(FontFactory.HELVETICA_BOLD))));
        table.addCell(new PdfPCell(new Phrase("Leave type", FontFactory.getFont(FontFactory.HELVETICA_BOLD))));
        table.addCell(new PdfPCell(new Phrase("Start", FontFactory.getFont(FontFactory.HELVETICA_BOLD))));
        table.addCell(new PdfPCell(new Phrase("End", FontFactory.getFont(FontFactory.HELVETICA_BOLD))));
        table.addCell(new PdfPCell(new Phrase("Status", FontFactory.getFont(FontFactory.HELVETICA_BOLD))));
        for (LeaveRequest lr : list) {
            table.addCell(lr.getRequester() != null ? lr.getRequester().getDisplayName() : "");
            table.addCell(lr.getLeaveType() != null ? lr.getLeaveType().getLabel() : "");
            table.addCell(lr.getDateStart() != null ? lr.getDateStart().format(DATE_FMT) : "");
            table.addCell(lr.getDateEnd() != null ? lr.getDateEnd().format(DATE_FMT) : "");
            table.addCell(lr.getStatus());
        }
        doc.add(table);
        doc.close();
        return out.toByteArray();
    }

    private List<LeaveRequest> getLeaveList(String status) {
        var pageable = PageRequest.of(0, 2000, Sort.by(Sort.Direction.DESC, "dateStart"));
        if (status != null && !status.isBlank()) {
            return leaveRequestRepository.findByTenantIdAndStatusOrderByCreatedAtDesc(
                    TenantContext.getCurrentTenantId(), status, pageable).getContent();
        }
        return leaveRequestRepository.findByTenantIdOrderByCreatedAtDesc(
                TenantContext.getCurrentTenantId(), pageable).getContent();
    }

    private static String csvEscape(String s) {
        if (s == null) return "\"\"";
        if (s.contains(",") || s.contains("\"") || s.contains("\n")) {
            return "\"" + s.replace("\"", "\"\"") + "\"";
        }
        return s;
    }
}
