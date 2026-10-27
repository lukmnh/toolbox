package io.github.lukmnh.toolbox.pdf.merge.domain;

/** Port: menggabungkan PDF. Use case mengendalikan urutan, progress, dan pembatalan per file. */
public interface PdfMerger {

    MergeSession openSession();
}
