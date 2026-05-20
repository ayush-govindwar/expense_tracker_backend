package com.expensetracker.service;

import com.expensetracker.model.dto.response.CategorySummaryItemResponse;
import com.expensetracker.model.dto.response.CategorySummaryResponse;
import com.expensetracker.model.dto.response.DailyTrendItemResponse;
import com.expensetracker.model.dto.response.DailyTrendResponse;
import com.expensetracker.model.dto.response.MonthlySummaryItemResponse;
import com.expensetracker.model.entity.Budget;
import com.expensetracker.model.entity.Expense;
import com.expensetracker.repository.BudgetRepository;
import com.expensetracker.repository.ExpenseRepository;
import com.expensetracker.repository.UserRepository;
import com.expensetracker.repository.projection.CategorySpendSummary;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPageEventHelper;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfTemplate;
import com.lowagie.text.pdf.PdfWriter;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfContentByte;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.awt.Color;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class ReportService {

    private final ExpenseRepository expenseRepository;
    private final BudgetRepository budgetRepository;
    private final UserRepository userRepository;

    public ReportService(
            ExpenseRepository expenseRepository,
            BudgetRepository budgetRepository,
            UserRepository userRepository) {
        this.expenseRepository = expenseRepository;
        this.budgetRepository = budgetRepository;
        this.userRepository = userRepository;
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
        LocalDate start = LocalDate.of(year, month, 1);
        LocalDate end = start.withDayOfMonth(start.lengthOfMonth());

        String userName = userRepository.findById(userId)
                .map(u -> u.getName())
                .orElse("User");

        List<CategorySpendSummary> categorySpend = expenseRepository.sumByCategoryForUserAndDateRange(userId, start, end);
        BigDecimal totalSpend = categorySpend.stream()
                .map(CategorySpendSummary::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        long transactionCount = expenseRepository.countByUserIdAndExpenseDateBetween(userId, start, end);
        Optional<Expense> biggestExpense = expenseRepository.findBiggestExpensesForUserAndDateRange(userId, start, end)
                .stream()
                .findFirst();

        String topCategory = categorySpend.isEmpty()
                ? "-"
                : categorySpend.get(0).getCategoryName();

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            Document document = new Document(new Rectangle(595, 842), 36, 36, 72, 54);
            PdfWriter writer = PdfWriter.getInstance(document, out);
            writer.setPageEvent(new HeaderFooterEvent(
                    "Iauro Finance",
                    userName,
                    start,
                    end
            ));
            document.open();

            // Page 1 — Summary
            addSectionTitle(document, "Summary");
            document.add(kvLine("Month / Year", month + " / " + year));
            document.add(kvLine("Total spend", money(totalSpend)));
            document.add(kvLine("Number of transactions", String.valueOf(transactionCount)));
            document.add(kvLine("Biggest single expense", biggestExpense
                    .map(e -> money(e.getAmount()) + " (" + e.getExpenseDate() + ")")
                    .orElse("-")));
            document.add(kvLine("Top spending category", topCategory));

            document.newPage();

            // Page 2 — Category Breakdown
            addSectionTitle(document, "Category Breakdown");
            PdfPTable breakdown = new PdfPTable(4);
            breakdown.setWidthPercentage(100);
            breakdown.setWidths(new float[]{3.2f, 1.4f, 1.4f, 1.0f});
            breakdown.addCell(headerCell("Category"));
            breakdown.addCell(headerCell("Budget"));
            breakdown.addCell(headerCell("Spent"));
            breakdown.addCell(headerCell("% Used"));

            for (CategorySpendSummary row : categorySpend) {
                Budget budget = budgetRepository
                        .findByUserIdAndCategoryIdAndMonthAndYear(userId, row.getCategoryId(), month, year)
                        .orElse(null);

                breakdown.addCell(textCell(row.getCategoryName()));
                breakdown.addCell(textCell(budget != null ? money(budget.getAmount()) : "-"));
                breakdown.addCell(textCell(money(row.getTotalAmount())));
                breakdown.addCell(textCell(pctUsed(budget != null ? budget.getAmount() : null, row.getTotalAmount())));
            }
            document.add(breakdown);

            document.newPage();

            // Page 3 — Transactions
            addSectionTitle(document, "Transactions");
            List<Expense> expenses = expenseRepository.findForPdfTransactions(userId, start, end);

            PdfPTable tx = new PdfPTable(5);
            tx.setWidthPercentage(100);
            tx.setWidths(new float[]{1.2f, 1.8f, 1.0f, 1.2f, 2.8f});
            tx.addCell(headerCell("Date"));
            tx.addCell(headerCell("Category"));
            tx.addCell(headerCell("Amount"));
            tx.addCell(headerCell("Payment Method"));
            tx.addCell(headerCell("Comments"));

            for (Expense e : expenses) {
                tx.addCell(textCell(String.valueOf(e.getExpenseDate())));
                tx.addCell(textCell(e.getCategory() != null ? e.getCategory().getName() : "-"));
                tx.addCell(rightCell(money(e.getAmount())));
                tx.addCell(textCell(e.getPaymentMethod() != null ? e.getPaymentMethod() : "-"));
                tx.addCell(textCell(e.getComments() != null ? e.getComments() : ""));
            }
            document.add(tx);

            document.close();
            return out.toByteArray();
        } catch (DocumentException ex) {
            throw new IllegalStateException("Failed to generate PDF", ex);
        }
    }

    private static void addSectionTitle(Document document, String text) throws DocumentException {
        Font font = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14);
        Paragraph p = new Paragraph(text, font);
        p.setSpacingAfter(10);
        document.add(p);
    }

    private static Paragraph kvLine(String key, String value) {
        Font keyFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11);
        Font valueFont = FontFactory.getFont(FontFactory.HELVETICA, 11);
        Paragraph p = new Paragraph();
        p.add(new Phrase(key + ": ", keyFont));
        p.add(new Phrase(value, valueFont));
        p.setSpacingAfter(6);
        return p;
    }

    private static String money(BigDecimal amount) {
        if (amount == null) return "-";
        return amount.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    private static String pctUsed(BigDecimal budget, BigDecimal spent) {
        if (budget == null) return "-";
        if (budget.compareTo(BigDecimal.ZERO) == 0) return "0%";
        BigDecimal pct = spent.multiply(BigDecimal.valueOf(100))
                .divide(budget, 1, RoundingMode.HALF_UP);
        return pct.toPlainString() + "%";
    }

    private PdfPCell headerCell(String text) {
        PdfPCell cell = new PdfPCell(new Phrase(text, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11)));
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setBackgroundColor(new Color(240, 240, 240));
        return cell;
    }

    private static PdfPCell textCell(String text) {
        PdfPCell cell = new PdfPCell(new Phrase(text, FontFactory.getFont(FontFactory.HELVETICA, 10)));
        cell.setHorizontalAlignment(Element.ALIGN_LEFT);
        return cell;
    }

    private static PdfPCell rightCell(String text) {
        PdfPCell cell = new PdfPCell(new Phrase(text, FontFactory.getFont(FontFactory.HELVETICA, 10)));
        cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        return cell;
    }

    private static class HeaderFooterEvent extends PdfPageEventHelper {
        private final String appName;
        private final String userName;
        private final LocalDate start;
        private final LocalDate end;
        private PdfTemplate total;
        private BaseFont baseFont;

        private HeaderFooterEvent(String appName, String userName, LocalDate start, LocalDate end) {
            this.appName = appName;
            this.userName = userName;
            this.start = start;
            this.end = end;
        }

        @Override
        public void onOpenDocument(PdfWriter writer, Document document) {
            total = writer.getDirectContent().createTemplate(30, 16);
            try {
                baseFont = BaseFont.createFont(BaseFont.HELVETICA, BaseFont.WINANSI, BaseFont.NOT_EMBEDDED);
            } catch (Exception ignored) {
                baseFont = null;
            }
        }

        @Override
        public void onEndPage(PdfWriter writer, Document document) {
            PdfContentByte cb = writer.getDirectContent();

            String headerLeft = appName;
            String headerRight = "User: " + userName + " | Range: " + start + " to " + end;
            String footerLeft = "Generated by " + appName + " on " + LocalDate.now();

            int pageNumber = writer.getPageNumber();
            String pageText = "Page " + pageNumber + " of ";

            float left = document.left();
            float right = document.right();
            float top = document.top() + 30;
            float bottom = document.bottom() - 25;

            cb.beginText();
            cb.setFontAndSize(font(), 9);

            cb.showTextAligned(Element.ALIGN_LEFT, headerLeft, left, top, 0);
            cb.showTextAligned(Element.ALIGN_RIGHT, headerRight, right, top, 0);

            cb.showTextAligned(Element.ALIGN_LEFT, footerLeft, left, bottom, 0);
            cb.showTextAligned(Element.ALIGN_RIGHT, pageText, right - 30, bottom, 0);
            cb.endText();

            cb.addTemplate(total, right - 30 + width(pageText), bottom);
        }

        @Override
        public void onCloseDocument(PdfWriter writer, Document document) {
            total.beginText();
            total.setFontAndSize(font(), 9);
            total.setTextMatrix(0, 0);
            total.showText(String.valueOf(writer.getPageNumber() - 1));
            total.endText();
        }

        private BaseFont font() {
            if (baseFont != null) return baseFont;
            try {
                return BaseFont.createFont(BaseFont.HELVETICA, BaseFont.WINANSI, BaseFont.NOT_EMBEDDED);
            } catch (Exception e) {
                throw new IllegalStateException("Failed to init PDF font", e);
            }
        }

        private float width(String text) {
            return font().getWidthPoint(text, 9);
        }
    }
}
