package com.expensetracker.controller;

import com.expensetracker.model.dto.response.CategorySummaryResponse;
import com.expensetracker.model.dto.response.DailyTrendResponse;
import com.expensetracker.model.dto.response.MonthlySummaryItemResponse;
import com.expensetracker.security.UserPrincipal;
import com.expensetracker.service.ReportService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/reports")
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping("/category-summary")
    public CategorySummaryResponse categorySummary(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) Integer year) {
        LocalDate now = LocalDate.now();
        int m = month != null ? month : now.getMonthValue();
        int y = year != null ? year : now.getYear();
        return reportService.categorySummary(principal.getId(), m, y);
    }

    @GetMapping("/monthly-summary")
    public List<MonthlySummaryItemResponse> monthlySummary(
            @AuthenticationPrincipal UserPrincipal principal) {
        return reportService.monthlySummary(principal.getId());
    }

    @GetMapping("/daily-trend")
    public DailyTrendResponse dailyTrend(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) Integer year) {
        LocalDate now = LocalDate.now();
        int m = month != null ? month : now.getMonthValue();
        int y = year != null ? year : now.getYear();
        return reportService.dailyTrend(principal.getId(), m, y);
    }

    @GetMapping("/export/pdf")
    public ResponseEntity<byte[]> exportPdf(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) Integer year) {
        LocalDate now = LocalDate.now();
        int m = month != null ? month : now.getMonthValue();
        int y = year != null ? year : now.getYear();

        byte[] pdf = reportService.exportPdf(principal.getId(), m, y);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=expense-report.pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }
}
