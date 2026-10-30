import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Locale;
import java.util.Random;
import java.util.UUID;

/**
 * Generator data dummy untuk uji performa JSON Viewer (single-file program, tanpa build).
 *
 * <pre>
 * java tools/GenerateSample.java 400000 sample.json     # JSON array (~200 MB)
 * java tools/GenerateSample.java 400000 sample.jsonl    # JSON Lines
 * </pre>
 *
 * Seed tetap, sehingga file yang dihasilkan selalu sama.
 */
public final class GenerateSample {

    private static final long SEED = 42L;
    private static final String[] NAMES = {
        "Ani", "Budi Santoso", "Citra Lestari", "Dodi", "Eka Putri", "Fajar", "Gita Ayu", "Hadi", "Intan Nuraini",
        "Joko Widodo", "Siti Nurhaliza", "Ayu Ting Ting", "Rafi \\\"Raffi\\\" Ahmad", "Zoë Ünal", "Dimas 😀"
    };
    private static final String[] CITIES = {"Jakarta", "Bandung", "Surabaya", "Medan", "Makassar", "Yogyakarta", "Bekasi"};
    private static final String[] STATUSES = {"active", "inactive", "pending", "banned"};
    private static final String[] TAGS = {"vip", "baru", "promo", "loyal", "b2b", "reseller"};
    private static final long START_EPOCH_SECONDS = 1_600_000_000L;
    private static final int EPOCH_RANGE_SECONDS = 150_000_000;

    private GenerateSample() {}

    public static void main(String[] args) throws IOException {
        if (args.length != 2) {
            System.err.println("Pemakaian: java tools/GenerateSample.java <jumlah-record> <file.json|file.jsonl>");
            System.exit(1);
        }
        int count = Integer.parseInt(args[0]);
        Path output = Path.of(args[1]);
        boolean jsonLines = output.getFileName().toString().toLowerCase(Locale.ROOT).matches(".*\\.(jsonl|ndjson)");
        Random random = new Random(SEED);
        try (BufferedWriter out = Files.newBufferedWriter(output, StandardCharsets.UTF_8)) {
            if (!jsonLines) {
                out.write("[\n");
            }
            for (int id = 1; id <= count; id++) {
                out.write(jsonLines ? "" : "  ");
                out.write(record(id, random));
                out.write(!jsonLines && id < count ? ",\n" : "\n");
            }
            if (!jsonLines) {
                out.write("]\n");
            }
        }
        System.out.printf("%,d record → %s (%,d byte)%n", count, output, Files.size(output));
    }

    private static String record(int id, Random random) {
        String name = pick(NAMES, random);
        String city = pick(CITIES, random);
        boolean deleted = random.nextInt(20) == 0;
        return String.format(
                Locale.ROOT,
                "{\"id\": %d, \"uuid\": \"%s\", \"nama\": \"%s\", \"email\": \"user%d@contoh.id\", \"status\": \"%s\","
                        + " \"umur\": %s, \"saldo\": %.2f, \"aktif\": %b, \"createdAt\": \"%s\","
                        + " \"tags\": [\"%s\", \"%s\"],"
                        + " \"address\": {\"city\": \"%s\", \"zip\": \"%05d\", \"geo\": {\"lat\": %.6f, \"lon\": %.6f}},"
                        + " \"items\": [{\"sku\": \"PRD-%04d\", \"qty\": %d, \"price\": %d},"
                        + " {\"sku\": \"PRD-%04d\", \"qty\": %d, \"price\": %d}],"
                        + " \"deletedAt\": %s}",
                id,
                new UUID(random.nextLong(), random.nextLong()),
                name,
                id,
                pick(STATUSES, random),
                random.nextInt(10) == 0 ? "null" : String.valueOf(18 + random.nextInt(60)),
                random.nextDouble() * 10_000_000,
                random.nextBoolean(),
                Instant.ofEpochSecond(START_EPOCH_SECONDS + random.nextInt(EPOCH_RANGE_SECONDS)),
                pick(TAGS, random),
                pick(TAGS, random),
                city,
                random.nextInt(100_000),
                -11 + random.nextDouble() * 17,
                95 + random.nextDouble() * 46,
                random.nextInt(10_000),
                1 + random.nextInt(9),
                1_000 * (1 + random.nextInt(500)),
                random.nextInt(10_000),
                1 + random.nextInt(9),
                1_000 * (1 + random.nextInt(500)),
                deleted ? "\"" + Instant.ofEpochSecond(START_EPOCH_SECONDS + random.nextInt(EPOCH_RANGE_SECONDS)) + "\"" : "null");
    }

    private static String pick(String[] values, Random random) {
        return values[random.nextInt(values.length)];
    }
}
