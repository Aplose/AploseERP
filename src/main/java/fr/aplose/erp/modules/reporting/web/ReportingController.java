package fr.aplose.erp.modules.reporting.web;

import fr.aplose.erp.modules.reporting.service.ReportingExportService;
import fr.aplose.erp.modules.reporting.service.ReportingService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Controller
@RequestMapping("/reporting")
@RequiredArgsConstructor
public class ReportingController {

    private static final DateTimeFormatter FILE_DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final ReportingService reportingService;
    private final ReportingExportService reportingExportService;

    @GetMapping
    @PreAuthorize("hasAuthority('REPORT_READ')")
    public String index(Model model) {
        LocalDate now = LocalDate.now();
        LocalDate startOfMonth = now.withDayOfMonth(1);
        model.addAttribute("salesThisMonth", reportingService.getSalesRevenue(startOfMonth, now));
        model.addAttribute("overdueCount", reportingService.getOverdueInvoices().size());
        model.addAttribute("recentLeave", reportingService.getLeaveRequestsSummary(null, 5));
        return "modules/reporting/index";
    }

    @GetMapping("/sales")
    @PreAuthorize("hasAuthority('REPORT_READ')")
    public String salesReport(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                             @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                             Model model) {
        if (from == null) from = LocalDate.now().withDayOfMonth(1);
        if (to == null) to = LocalDate.now();
        model.addAttribute("from", from);
        model.addAttribute("to", to);
        model.addAttribute("revenue", reportingService.getSalesRevenue(from, to));
        return "modules/reporting/sales-report";
    }

    @GetMapping("/overdue-invoices")
    @PreAuthorize("hasAuthority('REPORT_READ')")
    public String overdueInvoices(Model model) {
        model.addAttribute("invoices", reportingService.getOverdueInvoices());
        return "modules/reporting/overdue-invoices";
    }

    @GetMapping("/leave")
    @PreAuthorize("hasAuthority('REPORT_READ')")
    public String leaveReport(@RequestParam(required = false) String status, Model model) {
        model.addAttribute("status", status);
        model.addAttribute("leaveRequests", reportingService.getLeaveRequestsSummary(status, 100));
        return "modules/reporting/leave-report";
    }

    // --- Export endpoints (REPORT_EXPORT) ---

    @GetMapping("/sales/export")
    @PreAuthorize("hasAuthority('REPORT_EXPORT')")
    public ResponseEntity<byte[]> exportSales(
            @RequestParam String format,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) throws Exception {
        if (from == null) from = LocalDate.now().withDayOfMonth(1);
        if (to == null) to = LocalDate.now();
        String baseName = "sales-report-" + from.format(FILE_DATE) + "-" + to.format(FILE_DATE);
        return switch (format.toLowerCase()) {
            case "csv" -> {
                byte[] data = reportingExportService.exportSalesCsv(from, to);
                yield ResponseEntity.ok()
                        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + baseName + ".csv\"")
                        .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                        .body(data);
            }
            case "excel", "xlsx" -> {
                byte[] data = reportingExportService.exportSalesExcel(from, to);
                yield ResponseEntity.ok()
                        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + baseName + ".xlsx\"")
                        .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                        .body(data);
            }
            case "pdf" -> {
                byte[] data = reportingExportService.exportSalesPdf(from, to);
                yield ResponseEntity.ok()
                        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + baseName + ".pdf\"")
                        .contentType(MediaType.APPLICATION_PDF)
                        .body(data);
            }
            default -> ResponseEntity.badRequest().build();
        };
    }

    @GetMapping("/overdue-invoices/export")
    @PreAuthorize("hasAuthority('REPORT_EXPORT')")
    public ResponseEntity<byte[]> exportOverdueInvoices(@RequestParam String format) throws Exception {
        String baseName = "overdue-invoices-" + LocalDate.now().format(FILE_DATE);
        return switch (format.toLowerCase()) {
            case "csv" -> {
                byte[] data = reportingExportService.exportOverdueInvoicesCsv();
                yield ResponseEntity.ok()
                        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + baseName + ".csv\"")
                        .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                        .body(data);
            }
            case "excel", "xlsx" -> {
                byte[] data = reportingExportService.exportOverdueInvoicesExcel();
                yield ResponseEntity.ok()
                        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + baseName + ".xlsx\"")
                        .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                        .body(data);
            }
            case "pdf" -> {
                byte[] data = reportingExportService.exportOverdueInvoicesPdf();
                yield ResponseEntity.ok()
                        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + baseName + ".pdf\"")
                        .contentType(MediaType.APPLICATION_PDF)
                        .body(data);
            }
            default -> ResponseEntity.badRequest().build();
        };
    }

    @GetMapping("/leave/export")
    @PreAuthorize("hasAuthority('REPORT_EXPORT')")
    public ResponseEntity<byte[]> exportLeave(
            @RequestParam String format,
            @RequestParam(required = false) String status) throws Exception {
        String baseName = "leave-report-" + LocalDate.now().format(FILE_DATE);
        return switch (format.toLowerCase()) {
            case "csv" -> {
                byte[] data = reportingExportService.exportLeaveCsv(status);
                yield ResponseEntity.ok()
                        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + baseName + ".csv\"")
                        .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                        .body(data);
            }
            case "excel", "xlsx" -> {
                byte[] data = reportingExportService.exportLeaveExcel(status);
                yield ResponseEntity.ok()
                        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + baseName + ".xlsx\"")
                        .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                        .body(data);
            }
            case "pdf" -> {
                byte[] data = reportingExportService.exportLeavePdf(status);
                yield ResponseEntity.ok()
                        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + baseName + ".pdf\"")
                        .contentType(MediaType.APPLICATION_PDF)
                        .body(data);
            }
            default -> ResponseEntity.badRequest().build();
        };
    }
}
