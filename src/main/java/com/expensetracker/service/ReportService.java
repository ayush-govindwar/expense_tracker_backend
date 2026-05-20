package com.expensetracker.service;

import com.expensetracker.model.dto.response.CategorySummaryItemResponse;
import com.expensetracker.model.dto.response.CategorySummaryResponse;
import com.expensetracker.model.dto.response.DailyTrendItemResponse;
import com.expensetracker.model.dto.response.DailyTrendResponse;
import com.expensetracker.model.dto.response.MonthlySummaryItemResponse;
import com.expensetracker.repository.ExpenseRepository;
import com.expensetracker.repository.projection.CategorySpendSummary;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
public class ReportService {

    private final ExpenseRepository expenseRepository;

    public ReportService(ExpenseRepository expenseRepository) {
        this.expenseRepository = expenseRepository;
    }

    public CategorySummaryResponse categorySummary(Long userId, int month, int year) {
        LocalDate start = LocalDate.of(year, month, 1);
        LocalDate end = start.withDayOfMonth(start.lengthOfMonth());

        List<CategorySpendSummary> rows =
                expenseRepository.sumByCategoryForUserAndDateRange(userId, start, end);

        BigDecimal total = rows.stream()
                .map(CategorySpendSummary::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        CategorySummaryResponse response = new CategorySummaryResponse();
        response.setMonth(month);
        response.setYear(year);
        response.setTotalSpend(total);

        List<CategorySummaryItemResponse> items = new ArrayList<>();
        for (CategorySpendSummary row : rows) {
            double pct = total.compareTo(BigDecimal.ZERO) == 0
                    ? 0
                    : row.getTotalAmount()
                            .multiply(BigDecimal.valueOf(100))
                            .divide(total, 1, RoundingMode.HALF_UP)
                            .doubleValue();
            items.add(new CategorySummaryItemResponse(
                    row.getCategoryName(),
                    row.getCategoryIcon(),
                    row.getTotalAmount(),
                    pct));
        }
        response.setCategories(items);
        return response;
    }

    public List<MonthlySummaryItemResponse> monthlySummary(Long userId) {
        return expenseRepository.sumMonthlyForUser(userId).stream()
                .map(row -> new MonthlySummaryItemResponse(
                        row.getMonth(), row.getYear(), row.getTotalAmount()))
                .toList();
    }

    public DailyTrendResponse dailyTrend(Long userId, int month, int year) {
        LocalDate start = LocalDate.of(year, month, 1);
        LocalDate end = start.withDayOfMonth(start.lengthOfMonth());

        List<DailyTrendItemResponse> days = expenseRepository
                .sumDailyForUserAndDateRange(userId, start, end)
                .stream()
                .map(row -> new DailyTrendItemResponse(row.getExpenseDate(), row.getTotalAmount()))
                .toList();

        DailyTrendResponse response = new DailyTrendResponse();
        response.setMonth(month);
        response.setYear(year);
        response.setDays(days);
        return response;
    }

    public byte[] exportPdf(Long userId, int month, int year) {
        CategorySummaryResponse summary = categorySummary(userId, month, year);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            Document document = new Document();
            PdfWriter.getInstance(document, out);
            document.open();

            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16);
            document.add(new Paragraph("Expense Report — " + month + "/" + year, titleFont));
            document.add(new Paragraph("Total spend: " + summary.getTotalSpend()));
            document.add(new Paragraph(" "));

            PdfPTable table = new PdfPTable(3);
            table.setWidthPercentage(100);
            table.addCell(headerCell("Category"));
            table.addCell(headerCell("Amount"));
            table.addCell(headerCell("%"));

            for (CategorySummaryItemResponse item : summary.getCategories()) {
                table.addCell(item.getCategoryName());
                table.addCell(item.getAmount().toPlainString());
                table.addCell(String.valueOf(item.getPercentage()));
            }

            document.add(table);
            document.close();
            return out.toByteArray();
        } catch (DocumentException ex) {
            throw new IllegalStateException("Failed to generate PDF", ex);
        }
    }

    private PdfPCell headerCell(String text) {
        PdfPCell cell = new PdfPCell(new Phrase(text, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11)));
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        return cell;
    }
}
