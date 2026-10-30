package io.github.lukmnh.toolbox.productivity.rename.domain;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;

/** Menentukan status tiap nama baru: valid, bentrok di batch, atau bentrok dengan file lain di folder. */
public final class RenameChecker {

    private RenameChecker() {}

    /**
     * @param originalNames nama sekarang file batch
     * @param targetNames nama baru, indeks sama dengan {@code originalNames}
     * @param folderNames semua entry di folder (termasuk file batch)
     * @param caseInsensitive file system tidak membedakan huruf besar-kecil (Windows, macOS)
     * @return satu hasil per file, indeks sama dengan input
     */
    public static List<NameCheck> check(
            List<String> originalNames, List<String> targetNames, Set<String> folderNames, boolean caseInsensitive) {
        if (originalNames.size() != targetNames.size()) {
            throw new IllegalArgumentException("Old and new name counts differ");
        }
        UnaryOperator<String> key = caseInsensitive ? name -> name.toLowerCase(Locale.ROOT) : name -> name;
        Map<String, Integer> targetCounts = new HashMap<>();
        targetNames.forEach(name -> targetCounts.merge(key.apply(name), 1, Integer::sum));
        Set<String> batchKeys = originalNames.stream().map(key).collect(Collectors.toSet());
        // File batch akan pindah lewat nama sementara, jadi hanya entry di luar batch yang bisa bentrok
        Set<String> outsiders = folderNames.stream()
                .map(key)
                .filter(name -> !batchKeys.contains(name))
                .collect(Collectors.toSet());
        List<NameCheck> checks = new ArrayList<>(targetNames.size());
        for (int index = 0; index < targetNames.size(); index++) {
            String target = targetNames.get(index);
            boolean duplicate = targetCounts.get(key.apply(target)) > 1;
            boolean taken = outsiders.contains(key.apply(target));
            checks.add(checkOne(originalNames.get(index), target, duplicate, taken));
        }
        return List.copyOf(checks);
    }

    private static NameCheck checkOne(String original, String target, boolean duplicate, boolean taken) {
        // File yang tidak berubah tidak disentuh, jadi nama lamanya tidak divalidasi ulang
        if (target.equals(original)) {
            return duplicate
                    ? new NameCheck(RenameStatus.CONFLICT, NameIssue.DUPLICATE_IN_BATCH)
                    : new NameCheck(RenameStatus.UNCHANGED, NameIssue.NONE);
        }
        NameIssue issue = NameValidator.check(target);
        if (issue != NameIssue.NONE) {
            return new NameCheck(RenameStatus.INVALID, issue);
        }
        if (duplicate) {
            return new NameCheck(RenameStatus.CONFLICT, NameIssue.DUPLICATE_IN_BATCH);
        }
        if (taken) {
            return new NameCheck(RenameStatus.CONFLICT, NameIssue.EXISTS_IN_FOLDER);
        }
        return new NameCheck(RenameStatus.OK, NameIssue.NONE);
    }
}
