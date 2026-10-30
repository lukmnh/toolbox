package io.github.lukmnh.toolbox.productivity.calculator.domain;

import java.io.Serial;
import java.util.Objects;

/** Input kalkulator di luar rentang yang masuk akal. Bukan bug: pesan ditampilkan inline ke user. */
public final class CalculatorInputException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    private final InputProblem problem;

    public CalculatorInputException(InputProblem problem) {
        super("Invalid calculator input: " + problem);
        this.problem = Objects.requireNonNull(problem, "problem");
    }

    public InputProblem problem() {
        return problem;
    }
}
