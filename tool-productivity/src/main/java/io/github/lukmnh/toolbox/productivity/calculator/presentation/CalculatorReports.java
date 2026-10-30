package io.github.lukmnh.toolbox.productivity.calculator.presentation;

import io.github.lukmnh.toolbox.common.i18n.Messages;
import io.github.lukmnh.toolbox.productivity.calculator.domain.AmortizationRow;
import io.github.lukmnh.toolbox.productivity.calculator.domain.InstallmentRequest;
import io.github.lukmnh.toolbox.productivity.calculator.domain.InstallmentSchedule;
import io.github.lukmnh.toolbox.productivity.calculator.domain.Invoice;
import io.github.lukmnh.toolbox.productivity.calculator.domain.InvoiceLine;
import io.github.lukmnh.toolbox.productivity.calculator.domain.InvoiceParty;
import io.github.lukmnh.toolbox.productivity.calculator.domain.InvoiceTotals;
import io.github.lukmnh.toolbox.productivity.calculator.domain.ReportDocument;
import io.github.lukmnh.toolbox.productivity.shared.domain.RupiahFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.SequencedMap;
import java.util.stream.IntStream;

/** Susun isi PDF (teks sudah diformat) untuk simulasi cicilan dan invoice. */
final class CalculatorReports {

    private static final int QUANTITY_FRACTION_DIGITS = 3;

    private CalculatorReports() {}

    /** Ringkasan pinjaman + tabel amortisasi lengkap dengan kolom yang sama seperti di layar. */
    static ReportDocument installment(InstallmentSchedule schedule, InstallmentRequest request) {
        SequencedMap<String, String> summary = new LinkedHashMap<>();
        summary.put(Messages.get(InstallmentViewModel.PRINCIPAL_KEY), CalculatorMessages.money(schedule.principal()));
        summary.put(
                Messages.get(InstallmentViewModel.RATE_KEY), CalculatorMessages.percent(request.annualRatePercent()));
        summary.put(Messages.get(InstallmentViewModel.TENOR_KEY), String.valueOf(schedule.tenorMonths()));
        summary.put(
                Messages.get("productivity.calculator.installment.method"),
                CalculatorMessages.method(schedule.method()));
        summary.put(
                Messages.get("productivity.calculator.installment.feeAmount"),
                CalculatorMessages.money(schedule.fee()));
        summary.put(
                Messages.get(InstallmentViewModel.firstPaymentKey(schedule)),
                CalculatorMessages.money(schedule.firstPayment()));
        if (InstallmentViewModel.hasDifferentLastPayment(schedule)) {
            summary.put(
                    Messages.get("productivity.calculator.installment.lastPayment"),
                    CalculatorMessages.money(schedule.lastPayment()));
        }
        summary.put(
                Messages.get("productivity.calculator.installment.totalInterest"),
                CalculatorMessages.money(schedule.totalInterest()));
        summary.put(
                Messages.get("productivity.calculator.installment.totalCost"),
                CalculatorMessages.money(schedule.totalCost()));

        List<String> headers =
                InstallmentViewModel.COLUMN_KEYS.stream().map(Messages::get).toList();
        List<List<String>> rows =
                schedule.rows().stream().map(CalculatorReports::scheduleCells).toList();
        return ReportDocument.titled(Messages.get("productivity.calculator.installment.pdf.title"))
                .heading(Messages.get("productivity.calculator.result"))
                .keyValues(summary)
                .heading(Messages.get("productivity.calculator.schedule"))
                .table(headers, rows)
                .paragraph(Messages.get("productivity.calculator.installment.rounding"))
                .paragraph(Messages.get("productivity.calculator.installment.disclaimer"))
                .build();
    }

    private static List<String> scheduleCells(AmortizationRow row) {
        return List.of(
                String.valueOf(row.month()),
                CalculatorMessages.money(row.payment()),
                CalculatorMessages.money(row.principalPart()),
                CalculatorMessages.money(row.interest()),
                CalculatorMessages.money(row.remaining()));
    }

    static ReportDocument invoice(Invoice invoice, InvoiceTotals totals) {
        SequencedMap<String, String> header = new LinkedHashMap<>();
        header.put(Messages.get(InvoiceViewModel.NUMBER_KEY), invoice.number());
        header.put(Messages.get("productivity.calculator.invoice.date"), CalculatorMessages.date(invoice.date()));

        List<InvoiceLine> lines = invoice.pricing().lines();
        List<List<String>> rows = IntStream.range(0, lines.size())
                .mapToObj(index -> lineCells(index + 1, lines.get(index), totals))
                .toList();

        ReportDocument.Builder document = ReportDocument.titled(Messages.format(
                        "productivity.calculator.invoice.pdf.title",
                        CalculatorMessages.invoiceType(invoice.type()),
                        invoice.number()))
                .keyValues(header)
                .heading(Messages.get(InvoiceViewModel.SELLER_KEY))
                .paragraph(party(invoice.seller()));
        if (!invoice.customer().name().isEmpty()
                || !invoice.customer().details().isEmpty()) {
            document.heading(Messages.get(InvoiceViewModel.CUSTOMER_KEY)).paragraph(party(invoice.customer()));
        }
        document.heading(Messages.get("productivity.calculator.invoice.items"))
                .table(
                        InvoiceViewModel.PDF_COLUMN_KEYS.stream()
                                .map(Messages::get)
                                .toList(),
                        rows)
                .keyValues(totalRows(totals));
        if (!invoice.notes().isEmpty()) {
            document.heading(Messages.get("productivity.calculator.invoice.notes"))
                    .paragraph(invoice.notes());
        }
        return document.build();
    }

    /** Baris total yang sama dipakai di PDF dan ringkasan clipboard. Diskon 0% tidak ditampilkan. */
    static SequencedMap<String, String> totalRows(InvoiceTotals totals) {
        SequencedMap<String, String> rows = new LinkedHashMap<>();
        rows.put(Messages.get("productivity.calculator.invoice.subtotal"), CalculatorMessages.money(totals.subtotal()));
        if (totals.discountPercent().signum() > 0) {
            rows.put(
                    Messages.format(
                            "productivity.calculator.invoice.discountAmount",
                            CalculatorMessages.percent(totals.discountPercent())),
                    CalculatorMessages.money(totals.discount()));
        }
        rows.put(Messages.get("productivity.calculator.tax.base"), CalculatorMessages.money(totals.taxBase()));
        rows.put(
                Messages.format(
                        "productivity.calculator.invoice.taxAmount",
                        CalculatorMessages.percent(totals.tax().ratePercent())),
                CalculatorMessages.money(totals.tax().tax()));
        rows.put(
                Messages.get("productivity.calculator.invoice.grandTotal"),
                CalculatorMessages.money(totals.grandTotal()));
        return rows;
    }

    static String quantity(InvoiceLine line) {
        return RupiahFormat.plain(line.quantity(), QUANTITY_FRACTION_DIGITS);
    }

    private static List<String> lineCells(int number, InvoiceLine line, InvoiceTotals totals) {
        return List.of(
                String.valueOf(number),
                line.description(),
                quantity(line),
                CalculatorMessages.money(line.unitPrice()),
                CalculatorMessages.money(totals.lineAmounts().get(number - 1)));
    }

    private static String party(InvoiceParty party) {
        return party.details().isEmpty() ? party.name() : party.name() + "\n" + party.details();
    }
}
