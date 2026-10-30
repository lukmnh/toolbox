package io.github.lukmnh.toolbox.app.guide;

import io.github.lukmnh.toolbox.common.report.PdfReport;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.commonmark.node.BlockQuote;
import org.commonmark.node.BulletList;
import org.commonmark.node.Code;
import org.commonmark.node.Document;
import org.commonmark.node.FencedCodeBlock;
import org.commonmark.node.HardLineBreak;
import org.commonmark.node.Heading;
import org.commonmark.node.HtmlBlock;
import org.commonmark.node.HtmlInline;
import org.commonmark.node.IndentedCodeBlock;
import org.commonmark.node.Link;
import org.commonmark.node.ListBlock;
import org.commonmark.node.ListItem;
import org.commonmark.node.Node;
import org.commonmark.node.OrderedList;
import org.commonmark.node.Paragraph;
import org.commonmark.node.SoftLineBreak;
import org.commonmark.node.Text;
import org.commonmark.parser.Parser;

/**
 * Markdown → {@link PdfReport}: heading level 1 pertama menjadi judul dokumen, heading lain menjadi heading, item list
 * menjadi paragraf berawalan {@code "• "} atau {@code "1. "}, dan blok kode menjadi paragraf apa adanya.
 *
 * <p>Tabel tidak dipetakan karena parser inti commonmark (tanpa extension GFM) tidak mengenal sintaks tabel.
 */
public final class GuidePdf {

    private static final String BULLET = "• ";
    private static final String INDENT = "    ";

    private final Parser parser = Parser.builder().build();

    public void write(String markdown, String fallbackTitle, Path target) throws IOException {
        toReport(markdown, fallbackTitle).writeTo(target);
    }

    PdfReport toReport(String markdown, String fallbackTitle) {
        Document document = (Document) parser.parse(markdown);
        Heading titleHeading = firstTitle(document);
        PdfReport report = PdfReport.titled(titleHeading == null ? fallbackTitle : inline(titleHeading));
        for (Node block = document.getFirstChild(); block != null; block = block.getNext()) {
            if (block != titleHeading) {
                appendBlock(report, block, 0);
            }
        }
        return report;
    }

    /** Hanya bila dokumen diawali heading level 1; heading level 1 di tengah dokumen tetap menjadi heading biasa. */
    private static Heading firstTitle(Document document) {
        return document.getFirstChild() instanceof Heading heading && heading.getLevel() == 1 ? heading : null;
    }

    private static void appendBlock(PdfReport report, Node block, int depth) {
        switch (block) {
            case Heading heading -> report.heading(inline(heading));
            case Paragraph paragraph -> report.paragraph(INDENT.repeat(depth) + inline(paragraph));
            case BulletList list -> appendList(report, list, depth);
            case OrderedList list -> appendList(report, list, depth);
            case FencedCodeBlock code -> report.paragraph(code.getLiteral().stripTrailing());
            case IndentedCodeBlock code -> report.paragraph(code.getLiteral().stripTrailing());
            case HtmlBlock html -> report.paragraph(html.getLiteral().stripTrailing());
            case BlockQuote quote -> children(quote).forEach(child -> appendBlock(report, child, depth));
            // ThematicBreak dan blok lain tidak punya padanan di PdfReport
            default -> {}
        }
    }

    private static void appendList(PdfReport report, ListBlock list, int depth) {
        int number = list instanceof OrderedList ordered ? startNumber(ordered) : 0;
        for (Node item : children(list)) {
            if (!(item instanceof ListItem listItem)) {
                continue;
            }
            String marker = list instanceof OrderedList ? number++ + ". " : BULLET;
            List<String> texts = new ArrayList<>();
            List<Node> nested = new ArrayList<>();
            for (Node child : children(listItem)) {
                if (child instanceof Paragraph paragraph) {
                    texts.add(inline(paragraph));
                } else {
                    nested.add(child);
                }
            }
            report.paragraph(INDENT.repeat(depth) + marker + String.join(" ", texts));
            nested.forEach(child -> appendBlock(report, child, depth + 1));
        }
    }

    private static int startNumber(OrderedList list) {
        Integer start = list.getMarkerStartNumber();
        return start == null ? 1 : start;
    }

    /** Teks polos dari inline node: format (bold, italic, code) dibuang, link ditulis "teks (url)". */
    static String inline(Node node) {
        StringBuilder builder = new StringBuilder();
        appendInline(builder, node);
        return builder.toString().strip();
    }

    private static void appendInline(StringBuilder builder, Node node) {
        for (Node child : children(node)) {
            switch (child) {
                case Text text -> builder.append(text.getLiteral());
                case Code code -> builder.append(code.getLiteral());
                case HtmlInline html -> builder.append(html.getLiteral());
                case SoftLineBreak softBreak -> builder.append(' ');
                case HardLineBreak hardBreak -> builder.append('\n');
                case Link link -> {
                    int start = builder.length();
                    appendInline(builder, link);
                    String label = builder.substring(start);
                    if (!label.equals(link.getDestination())) {
                        builder.append(" (").append(link.getDestination()).append(')');
                    }
                }
                default -> appendInline(builder, child);
            }
        }
    }

    private static List<Node> children(Node parent) {
        List<Node> children = new ArrayList<>();
        for (Node child = parent.getFirstChild(); child != null; child = child.getNext()) {
            children.add(child);
        }
        return children;
    }
}
