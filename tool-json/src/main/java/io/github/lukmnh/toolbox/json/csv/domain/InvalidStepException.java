package io.github.lukmnh.toolbox.json.csv.domain;

import java.io.Serial;

/**
 * Aturan sebuah langkah pipeline tidak valid terhadap kolom yang tersedia.
 *
 * @see StepProblem
 */
public final class InvalidStepException extends CsvToolException {

    @Serial
    private static final long serialVersionUID = 1L;

    private final int stepNumber;
    private final StepProblem problem;
    private final String detail;

    /**
     * @param stepNumber nomor langkah, mulai 1
     * @param detail nama kolom atau keterangan dari aturan user (bukan isi CSV); kosong bila tidak relevan
     */
    public InvalidStepException(int stepNumber, StepProblem problem, String detail) {
        super("Langkah " + stepNumber + " tidak valid: " + problem, null);
        this.stepNumber = stepNumber;
        this.problem = problem;
        this.detail = detail;
    }

    public int stepNumber() {
        return stepNumber;
    }

    public StepProblem problem() {
        return problem;
    }

    public String detail() {
        return detail;
    }
}
