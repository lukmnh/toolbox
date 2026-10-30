package io.github.lukmnh.toolbox.productivity.budget.domain;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.UnaryOperator;

/** Operasi list immutable berbasis id: hasilnya selalu list baru, list asal tidak berubah. */
final class ListEdits {

    private ListEdits() {}

    static <T> List<T> replace(List<T> list, Function<T, String> id, String targetId, UnaryOperator<T> change) {
        return list.stream()
                .map(element -> id.apply(element).equals(targetId) ? change.apply(element) : element)
                .toList();
    }

    static <T> List<T> removeIf(List<T> list, Function<T, String> id, String targetId) {
        return list.stream()
                .filter(element -> !id.apply(element).equals(targetId))
                .toList();
    }

    static <T> List<T> append(List<T> list, T element) {
        List<T> copy = new ArrayList<>(list);
        copy.add(element);
        return List.copyOf(copy);
    }

    /** Sisipkan {@code element} di {@code index}; index di luar batas dijepit ke awal/akhir list. */
    static <T> List<T> insert(List<T> list, int index, T element) {
        List<T> copy = new ArrayList<>(list);
        copy.add(Math.clamp(index, 0, copy.size()), element);
        return List.copyOf(copy);
    }

    /** Geser elemen ber-id {@code targetId} sejauh {@code delta}; di tepi list tidak terjadi apa-apa. */
    static <T> List<T> move(List<T> list, Function<T, String> id, String targetId, int delta) {
        int from = indexOf(list, id, targetId);
        int to = from + delta;
        if (from < 0 || to < 0 || to >= list.size()) {
            return list;
        }
        List<T> copy = new ArrayList<>(list);
        copy.add(to, copy.remove(from));
        return List.copyOf(copy);
    }

    static <T> int indexOf(List<T> list, Function<T, String> id, String targetId) {
        for (int index = 0; index < list.size(); index++) {
            if (id.apply(list.get(index)).equals(targetId)) {
                return index;
            }
        }
        return -1;
    }
}
