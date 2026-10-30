package io.github.lukmnh.toolbox.productivity.calculator.application;

import io.github.lukmnh.toolbox.productivity.calculator.domain.InstallmentCalculator;
import io.github.lukmnh.toolbox.productivity.calculator.domain.InstallmentRequest;
import io.github.lukmnh.toolbox.productivity.calculator.domain.InstallmentSchedule;

/** Buat jadwal amortisasi + total bunga/bayar. Paling banyak 600 baris, jadi cukup dipanggil di FX thread. */
public final class SimulateInstallmentUseCase {

    public InstallmentSchedule execute(InstallmentRequest request) {
        return InstallmentCalculator.calculate(request);
    }
}
