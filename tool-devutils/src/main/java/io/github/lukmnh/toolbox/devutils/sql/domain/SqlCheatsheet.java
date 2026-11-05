package io.github.lukmnh.toolbox.devutils.sql.domain;

import java.util.List;

/**
 * Referensi SQL: {@link #SYNTAX_GROUPS} untuk mode Cheatsheet, {@link #CONCEPT_GROUPS} untuk mode Concepts
 * (grup konsep punya intro di messages {@code devutils.sql.group.<group>.intro}). Urutan list = urutan tampil.
 * Contoh memakai PostgreSQL kecuali disebut lain; padanan MySQL/SQL Server ditulis sebagai komentar di kode.
 */
public final class SqlCheatsheet {

    public static final List<String> SYNTAX_GROUPS = List.of(
            "query",
            "conditional",
            "join",
            "subquery",
            "aggregate",
            "window",
            "set",
            "insert",
            "update",
            "delete",
            "date",
            "string",
            "json",
            "ddl",
            "index",
            "ops",
            "security");

    public static final List<String> CONCEPT_GROUPS =
            List.of("acid", "isolation", "locking", "deadlock", "explain", "normalization", "partition", "sharding");

    public static final List<SqlSnippet> SNIPPETS = List.of(
            // ---------- Query basics ----------
            new SqlSnippet("query", "SELECT id, name, email FROM users WHERE active = true;", "select"),
            new SqlSnippet("query", """
                    SELECT * FROM orders
                    WHERE status <> 'cancelled'
                      AND (total >= 100 OR priority = 'high')
                      AND NOT is_test;""", "whereOperators"),
            new SqlSnippet(
                    "query",
                    "SELECT u.name AS customer_name, o.total * 1.11 AS total_with_tax\nFROM users u JOIN orders o ON o.user_id = u.id;",
                    "alias"),
            new SqlSnippet("query", "SELECT * FROM orders ORDER BY created_at DESC LIMIT 20 OFFSET 40;", "orderLimit"),
            new SqlSnippet("query", """
                    SELECT * FROM orders
                    ORDER BY created_at DESC
                    OFFSET 40 ROWS FETCH FIRST 20 ROWS ONLY;
                    -- SQL Server (old style): SELECT TOP 20 * FROM orders ORDER BY created_at DESC;""", "fetchFirst"),
            new SqlSnippet("query", """
                    SELECT * FROM orders
                    WHERE (created_at, id) < (:last_created_at, :last_id)
                    ORDER BY created_at DESC, id DESC
                    LIMIT 20;""", "keyset"),
            new SqlSnippet("query", "SELECT DISTINCT country FROM customers;", "distinct"),
            new SqlSnippet("query", """
                    SELECT DISTINCT ON (customer_id) customer_id, id, created_at
                    FROM orders
                    ORDER BY customer_id, created_at DESC;""", "distinctOn"),
            new SqlSnippet(
                    "query",
                    "SELECT * FROM products WHERE price BETWEEN 10 AND 50 AND category_id IN (1, 2, 3);",
                    "betweenIn"),
            new SqlSnippet(
                    "query",
                    "SELECT * FROM users WHERE email LIKE '%@example.com';\nSELECT * FROM users WHERE name ILIKE 'ana%';   -- PostgreSQL, case-insensitive",
                    "like"),
            new SqlSnippet(
                    "query",
                    "SELECT * FROM users WHERE deleted_at IS NULL;\nSELECT * FROM users WHERE phone IS NOT NULL;",
                    "isNull"),
            new SqlSnippet(
                    "query",
                    "SELECT * FROM tasks ORDER BY due_date ASC NULLS LAST;\n-- MySQL: ORDER BY due_date IS NULL, due_date",
                    "nullsOrder"),
            new SqlSnippet("query", """
                    SELECT CAST(price AS INTEGER), CAST(id AS TEXT), '2026-11-05'::date
                    FROM products;
                    -- MySQL: CAST(id AS CHAR), CAST(price AS SIGNED), CAST('2026-11-05' AS DATE)""", "cast"),
            new SqlSnippet("query", """
                    SELECT ROUND(total, 2), CEIL(total), FLOOR(total),
                           7 / 2      AS int_division,   -- 3 in PostgreSQL and SQL Server
                           7 / 2.0    AS real_division,  -- 3.5
                           total / NULLIF(qty, 0) AS unit_price
                    FROM orders;""", "math"),

            // ---------- CASE & NULL handling ----------
            new SqlSnippet("conditional", """
                    SELECT id,
                           CASE WHEN total >= 1000 THEN 'big'
                                WHEN total >= 100 THEN 'medium'
                                ELSE 'small' END AS size
                    FROM orders;""", "case"),
            new SqlSnippet("conditional", """
                    SELECT id,
                           CASE status WHEN 'new'  THEN 'Pending'
                                       WHEN 'paid' THEN 'Paid'
                                       ELSE 'Other' END AS label
                    FROM orders;""", "caseSimple"),
            new SqlSnippet("conditional", """
                    SELECT * FROM tickets
                    ORDER BY CASE priority WHEN 'high' THEN 1 WHEN 'medium' THEN 2 ELSE 3 END,
                             created_at;""", "caseOrderBy"),
            new SqlSnippet("conditional", """
                    SELECT customer_id,
                           SUM(CASE WHEN status = 'paid' THEN total ELSE 0 END) AS paid_total,
                           COUNT(CASE WHEN status = 'cancelled' THEN 1 END)     AS cancelled_count
                    FROM orders
                    GROUP BY customer_id;""", "casePivot"),
            new SqlSnippet(
                    "conditional",
                    "SELECT * FROM products\nWHERE CASE WHEN :category_id IS NULL THEN true ELSE category_id = :category_id END;",
                    "caseWhere"),
            new SqlSnippet(
                    "conditional", "SELECT name, COALESCE(phone, mobile, '-') AS contact FROM users;", "coalesce"),
            new SqlSnippet(
                    "conditional",
                    "SELECT revenue / NULLIF(visits, 0) AS revenue_per_visit FROM daily_stats;",
                    "nullif"),
            new SqlSnippet(
                    "conditional",
                    "SELECT GREATEST(price - discount, 0) AS final_price, LEAST(stock, 10) AS max_per_order\nFROM products;",
                    "greatestLeast"),
            new SqlSnippet(
                    "conditional",
                    "SELECT * FROM users WHERE email IS DISTINCT FROM :old_email;\n-- MySQL: NOT (email <=> :old_email)",
                    "isDistinctFrom"),

            // ---------- Joins ----------
            new SqlSnippet(
                    "join",
                    "SELECT o.id, c.name\nFROM orders o\nJOIN customers c ON c.id = o.customer_id;",
                    "innerJoin"),
            new SqlSnippet(
                    "join",
                    "SELECT c.name, o.id\nFROM customers c\nLEFT JOIN orders o ON o.customer_id = c.id;",
                    "leftJoin"),
            new SqlSnippet("join", """
                    SELECT c.name, o.id
                    FROM customers c
                    LEFT JOIN orders o ON o.customer_id = c.id AND o.status = 'paid';""", "leftJoinFilter"),
            new SqlSnippet("join", """
                    SELECT c.*
                    FROM customers c
                    LEFT JOIN orders o ON o.customer_id = c.id
                    WHERE o.id IS NULL;""", "leftJoinNull"),
            new SqlSnippet(
                    "join",
                    "SELECT c.name, o.id\nFROM customers c\nRIGHT JOIN orders o ON o.customer_id = c.id;",
                    "rightJoin"),
            new SqlSnippet("join", """
                    SELECT COALESCE(a.sku, b.sku) AS sku, a.qty AS system_qty, b.qty AS counted_qty
                    FROM stock a
                    FULL OUTER JOIN stock_count b ON b.sku = a.sku
                    WHERE a.qty IS DISTINCT FROM b.qty;""", "fullJoin"),
            new SqlSnippet("join", "SELECT s.size, c.color FROM sizes s CROSS JOIN colors c;", "crossJoin"),
            new SqlSnippet("join", """
                    SELECT e.name, m.name AS manager
                    FROM employees e
                    LEFT JOIN employees m ON m.id = e.manager_id;""", "selfJoin"),
            new SqlSnippet("join", """
                    SELECT a.id, b.id AS duplicate_id, a.email
                    FROM users a
                    JOIN users b ON b.email = a.email AND b.id > a.id;""", "selfJoinDuplicates"),
            new SqlSnippet("join", """
                    SELECT o.id, c.name, p.name AS product, oi.qty
                    FROM orders o
                    JOIN customers c    ON c.id = o.customer_id
                    JOIN order_items oi ON oi.order_id = o.id
                    JOIN products p     ON p.id = oi.product_id
                    WHERE o.created_at >= now() - INTERVAL '7 days';""", "multiJoin"),
            new SqlSnippet("join", "SELECT * FROM orders JOIN customers USING (customer_id);", "usingJoin"),
            new SqlSnippet("join", """
                    SELECT c.name, last_order.created_at
                    FROM customers c
                    CROSS JOIN LATERAL (
                      SELECT created_at FROM orders o
                      WHERE o.customer_id = c.id ORDER BY created_at DESC LIMIT 1
                    ) last_order;""", "lateral"),

            // ---------- Subquery, EXISTS, ANY/ALL, CTE ----------
            new SqlSnippet(
                    "subquery",
                    "SELECT * FROM products WHERE price > (SELECT AVG(price) FROM products);",
                    "scalarSubquery"),
            new SqlSnippet(
                    "subquery",
                    "SELECT * FROM products\nWHERE category_id IN (SELECT id FROM categories WHERE active);",
                    "inSubquery"),
            new SqlSnippet("subquery", """
                    SELECT c.* FROM customers c
                    WHERE EXISTS (
                      SELECT 1 FROM orders o
                      WHERE o.customer_id = c.id AND o.created_at >= now() - INTERVAL '30 days'
                    );""", "exists"),
            new SqlSnippet("subquery", """
                    SELECT c.* FROM customers c
                    WHERE NOT EXISTS (SELECT 1 FROM orders o WHERE o.customer_id = c.id);""", "notExists"),
            new SqlSnippet("subquery", """
                    -- returns no rows at all if any manager_id is NULL
                    SELECT * FROM users WHERE id NOT IN (SELECT manager_id FROM teams);
                    -- safe version
                    SELECT * FROM users u
                    WHERE NOT EXISTS (SELECT 1 FROM teams t WHERE t.manager_id = u.id);""", "notInNull"),
            new SqlSnippet(
                    "subquery",
                    "SELECT * FROM products\nWHERE price > ANY (SELECT price FROM products WHERE category_id = 3);",
                    "any"),
            new SqlSnippet(
                    "subquery",
                    "SELECT * FROM users WHERE id = ANY(:ids);   -- :ids bound as an array, e.g. {1,2,3}",
                    "anyArray"),
            new SqlSnippet(
                    "subquery",
                    "SELECT * FROM products\nWHERE price > ALL (SELECT price FROM products WHERE category_id = 3);",
                    "all"),
            new SqlSnippet("subquery", """
                    SELECT p.name, p.price,
                           (SELECT AVG(x.price) FROM products x WHERE x.category_id = p.category_id) AS category_avg
                    FROM products p;""", "correlated"),
            new SqlSnippet("subquery", """
                    SELECT t.customer_id, t.total_spent
                    FROM (
                      SELECT customer_id, SUM(total) AS total_spent FROM orders GROUP BY customer_id
                    ) t
                    WHERE t.total_spent > 1000;""", "derivedTable"),
            new SqlSnippet("subquery", """
                    WITH big_orders AS (
                      SELECT * FROM orders WHERE total > 1000
                    )
                    SELECT customer_id, COUNT(*) FROM big_orders GROUP BY customer_id;""", "cte"),
            new SqlSnippet("subquery", """
                    WITH RECURSIVE tree AS (
                      SELECT id, parent_id, name, 1 AS depth FROM categories WHERE parent_id IS NULL
                      UNION ALL
                      SELECT c.id, c.parent_id, c.name, t.depth + 1
                      FROM categories c JOIN tree t ON c.parent_id = t.id
                      WHERE t.depth < 10
                    )
                    SELECT * FROM tree;""", "recursiveCte"),

            // ---------- Aggregation ----------
            new SqlSnippet(
                    "aggregate",
                    "SELECT customer_id, COUNT(*), SUM(total), AVG(total), MAX(created_at)\nFROM orders\nGROUP BY customer_id;",
                    "groupBy"),
            new SqlSnippet("aggregate", """
                    SELECT customer_id, SUM(total) AS spent
                    FROM orders
                    WHERE status = 'paid'
                    GROUP BY customer_id
                    HAVING SUM(total) > 1000
                    ORDER BY spent DESC;""", "having"),
            new SqlSnippet(
                    "aggregate",
                    "SELECT COUNT(*), COUNT(phone), COUNT(DISTINCT city) FROM customers;",
                    "countDistinct"),
            new SqlSnippet("aggregate", """
                    SELECT COUNT(*) FILTER (WHERE status = 'paid')     AS paid,
                           COUNT(*) FILTER (WHERE status = 'refunded') AS refunded
                    FROM orders;""", "filter"),
            new SqlSnippet("aggregate", """
                    SELECT order_id, STRING_AGG(sku, ', ' ORDER BY sku) AS skus
                    FROM order_items
                    GROUP BY order_id;
                    -- MySQL: GROUP_CONCAT(sku ORDER BY sku SEPARATOR ', ')""", "stringAgg"),
            new SqlSnippet(
                    "aggregate",
                    "SELECT region, product, SUM(total)\nFROM sales\nGROUP BY ROLLUP (region, product);",
                    "rollup"),

            // ---------- Window functions ----------
            new SqlSnippet("window", """
                    SELECT * FROM (
                      SELECT o.*, ROW_NUMBER() OVER (PARTITION BY customer_id ORDER BY created_at DESC) AS rn
                      FROM orders o
                    ) x
                    WHERE rn = 1;""", "rowNumber"),
            new SqlSnippet(
                    "window",
                    "SELECT name, score,\n       RANK() OVER (ORDER BY score DESC),\n       DENSE_RANK() OVER (ORDER BY score DESC)\nFROM players;",
                    "rank"),
            new SqlSnippet(
                    "window",
                    "SELECT day, revenue, revenue - LAG(revenue) OVER (ORDER BY day) AS diff\nFROM daily_sales;",
                    "lagLead"),
            new SqlSnippet("window", """
                    SELECT day, revenue,
                           SUM(revenue) OVER (ORDER BY day) AS running_total,
                           AVG(revenue) OVER (ORDER BY day ROWS BETWEEN 6 PRECEDING AND CURRENT ROW) AS avg_7d
                    FROM daily_sales;""", "runningTotal"),
            new SqlSnippet(
                    "window",
                    "SELECT product_id, revenue,\n       ROUND(100.0 * revenue / SUM(revenue) OVER (), 2) AS pct_of_total\nFROM product_sales;",
                    "percentOfTotal"),

            // ---------- UNION & set operations ----------
            new SqlSnippet("set", "SELECT email FROM customers\nUNION\nSELECT email FROM leads;", "union"),
            new SqlSnippet("set", "SELECT email FROM customers\nUNION ALL\nSELECT email FROM leads;", "unionAll"),
            new SqlSnippet("set", """
                    SELECT name, created_at, 'customer' AS source FROM customers
                    UNION ALL
                    SELECT name, created_at, 'lead' FROM leads
                    ORDER BY created_at DESC
                    LIMIT 50;""", "unionOrder"),
            new SqlSnippet("set", "SELECT email FROM customers\nINTERSECT\nSELECT email FROM leads;", "intersect"),
            new SqlSnippet("set", "SELECT email FROM customers\nEXCEPT\nSELECT email FROM unsubscribed;", "except"),

            // ---------- INSERT & UPSERT ----------
            new SqlSnippet(
                    "insert",
                    "INSERT INTO users (name, email)\nVALUES ('Ana', 'ana@example.com'), ('Budi', 'budi@example.com');",
                    "insert"),
            new SqlSnippet(
                    "insert",
                    "INSERT INTO archived_orders (id, customer_id, total)\nSELECT id, customer_id, total FROM orders WHERE created_at < '2024-01-01';",
                    "insertSelect"),
            new SqlSnippet("insert", "INSERT INTO users (name) VALUES ('Ana') RETURNING id, created_at;", "returning"),
            new SqlSnippet("insert", """
                    INSERT INTO stock (sku, qty) VALUES ('A1', 5)
                    ON CONFLICT (sku) DO UPDATE SET qty = stock.qty + EXCLUDED.qty;""", "upsertPg"),
            new SqlSnippet("insert", """
                    INSERT INTO stock (sku, qty) VALUES ('A1', 5) AS new
                    ON DUPLICATE KEY UPDATE qty = qty + new.qty;""", "upsertMysql"),
            new SqlSnippet("insert", """
                    INSERT INTO tags (name) VALUES ('sql') ON CONFLICT (name) DO NOTHING;   -- PostgreSQL
                    INSERT IGNORE INTO tags (name) VALUES ('sql');                          -- MySQL""", "insertIgnore"),
            new SqlSnippet("insert", """
                    MERGE INTO stock s
                    USING incoming i ON s.sku = i.sku
                    WHEN MATCHED THEN UPDATE SET qty = s.qty + i.qty
                    WHEN NOT MATCHED THEN INSERT (sku, qty) VALUES (i.sku, i.qty);""", "merge"),
            new SqlSnippet("insert", """
                    COPY users (name, email) FROM '/data/users.csv' WITH (FORMAT csv, HEADER true);
                    -- psql, file on your machine: \\copy users (name, email) FROM 'users.csv' WITH (FORMAT csv, HEADER true)
                    -- MySQL: LOAD DATA LOCAL INFILE 'users.csv' INTO TABLE users FIELDS TERMINATED BY ',' IGNORE 1 LINES;""", "bulkLoad"),

            // ---------- UPDATE ----------
            new SqlSnippet(
                    "update",
                    "UPDATE products SET price = price * 1.1, updated_at = now()\nWHERE category_id = 3;",
                    "update"),
            new SqlSnippet("update", """
                    UPDATE customers c
                    SET total_spent = s.spent
                    FROM (
                      SELECT customer_id, SUM(total) AS spent FROM orders GROUP BY customer_id
                    ) s
                    WHERE s.customer_id = c.id;""", "updateFromSelect"),
            new SqlSnippet("update", """
                    UPDATE orders o
                    SET status = 'vip'
                    FROM customers c
                    WHERE c.id = o.customer_id AND c.tier = 'gold';""", "updateJoin"),
            new SqlSnippet("update", """
                    UPDATE orders o
                    JOIN customers c ON c.id = o.customer_id
                    SET o.status = 'vip'
                    WHERE c.tier = 'gold';""", "updateJoinMysql"),
            new SqlSnippet("update", """
                    UPDATE products p
                    SET last_sold_at = (
                      SELECT MAX(o.created_at)
                      FROM order_items oi JOIN orders o ON o.id = oi.order_id
                      WHERE oi.product_id = p.id
                    );""", "updateSubquery"),
            new SqlSnippet("update", """
                    UPDATE users SET status = 'inactive'
                    WHERE id IN (
                      SELECT user_id FROM sessions
                      GROUP BY user_id
                      HAVING MAX(last_seen) < now() - INTERVAL '1 year'
                    );""", "updateWhereIn"),
            new SqlSnippet("update", """
                    UPDATE products
                    SET price = CASE id WHEN 1 THEN 9.99
                                        WHEN 2 THEN 19.99
                                        ELSE price END
                    WHERE id IN (1, 2);""", "updateCase"),
            new SqlSnippet("update", """
                    UPDATE products p
                    SET price = v.price
                    FROM (VALUES (1, 9.99), (2, 19.99)) AS v (id, price)
                    WHERE p.id = v.id;""", "updateValues"),
            new SqlSnippet("update", """
                    UPDATE jobs SET status = 'running', started_at = now()
                    WHERE id = :id AND status = 'queued'
                    RETURNING *;""", "updateReturning"),
            new SqlSnippet("update", """
                    BEGIN;
                    SELECT stock FROM products WHERE id = 5 FOR UPDATE;
                    -- the application checks the stock here
                    UPDATE products SET stock = stock - 1 WHERE id = 5;
                    COMMIT;""", "selectForUpdate"),
            new SqlSnippet("update", """
                    UPDATE orders SET archived = true
                    WHERE id IN (
                      SELECT id FROM orders
                      WHERE archived = false AND created_at < '2024-01-01'
                      LIMIT 5000
                    );
                    -- repeat until 0 rows. MySQL: UPDATE orders SET archived = 1 WHERE ... LIMIT 5000;""", "updateBatch"),

            // ---------- DELETE ----------
            new SqlSnippet("delete", "DELETE FROM sessions WHERE expires_at < now();", "delete"),
            new SqlSnippet(
                    "delete",
                    "DELETE FROM order_items\nWHERE order_id IN (SELECT id FROM orders WHERE status = 'cancelled');",
                    "deleteSubquery"),
            new SqlSnippet(
                    "delete",
                    "DELETE FROM sessions s\nWHERE NOT EXISTS (SELECT 1 FROM users u WHERE u.id = s.user_id);",
                    "deleteExists"),
            new SqlSnippet("delete", """
                    DELETE FROM order_items oi
                    USING orders o
                    WHERE o.id = oi.order_id AND o.status = 'cancelled';""", "deleteUsing"),
            new SqlSnippet("delete", """
                    DELETE oi
                    FROM order_items oi
                    JOIN orders o ON o.id = oi.order_id
                    WHERE o.status = 'cancelled';""", "deleteJoinMysql"),
            new SqlSnippet("delete", """
                    DELETE FROM users
                    WHERE id IN (
                      SELECT id FROM (
                        SELECT id, ROW_NUMBER() OVER (PARTITION BY email ORDER BY id) AS rn FROM users
                      ) d
                      WHERE d.rn > 1
                    );""", "deleteDuplicates"),
            new SqlSnippet("delete", """
                    DELETE FROM events
                    WHERE id IN (
                      SELECT id FROM events WHERE created_at < now() - INTERVAL '90 days' LIMIT 10000
                    );
                    -- repeat until 0 rows. MySQL: DELETE FROM events WHERE created_at < NOW() - INTERVAL 90 DAY LIMIT 10000;""", "deleteBatch"),
            new SqlSnippet("delete", """
                    WITH moved AS (
                      DELETE FROM orders WHERE created_at < '2024-01-01' RETURNING *
                    )
                    INSERT INTO archived_orders SELECT * FROM moved;""", "archiveMove"),
            new SqlSnippet(
                    "delete",
                    "UPDATE users SET deleted_at = now() WHERE id = :id;\nSELECT * FROM users WHERE deleted_at IS NULL;",
                    "softDelete"),
            new SqlSnippet("delete", "TRUNCATE TABLE staging_events RESTART IDENTITY;", "truncate"),

            // ---------- Dates & time ----------
            new SqlSnippet("date", """
                    SELECT now(), CURRENT_DATE, CURRENT_TIMESTAMP, clock_timestamp();
                    -- MySQL: NOW(), CURDATE()   SQL Server: SYSDATETIME(), CAST(GETDATE() AS date)""", "currentDateTime"),
            new SqlSnippet("date", """
                    CREATE TABLE events (
                      happened_at TIMESTAMPTZ NOT NULL,  -- a moment in time, stored as UTC
                      local_start TIMESTAMP,             -- wall-clock time without a zone
                      birthday    DATE,                  -- a calendar day
                      opens_at    TIME                   -- time of day
                    );""", "dateTypes"),
            new SqlSnippet("date", """
                    SELECT * FROM orders
                    WHERE created_at >= '2026-11-01'
                      AND created_at <  '2026-12-01';""", "dateRange"),
            new SqlSnippet("date", """
                    SELECT * FROM orders WHERE created_at >= now() - INTERVAL '30 days';
                    SELECT * FROM orders WHERE created_at >= CURRENT_DATE;   -- today
                    -- MySQL: created_at >= NOW() - INTERVAL 30 DAY""", "lastNDays"),
            new SqlSnippet("date", """
                    SELECT now() + INTERVAL '1 day',
                           now() - INTERVAL '2 hours',
                           CURRENT_DATE + 7,
                           started_at + INTERVAL '1 month' AS renews_at
                    FROM subscriptions;
                    -- MySQL: NOW() + INTERVAL 1 DAY, DATE_ADD(started_at, INTERVAL 1 MONTH)""", "dateArithmetic"),
            new SqlSnippet("date", """
                    SELECT date_trunc('month', created_at) AS month, COUNT(*), SUM(total)
                    FROM orders
                    GROUP BY 1
                    ORDER BY 1;
                    -- MySQL: GROUP BY DATE_FORMAT(created_at, '%Y-%m-01')""", "dateTrunc"),
            new SqlSnippet("date", """
                    SELECT EXTRACT(YEAR FROM created_at)  AS year,
                           EXTRACT(MONTH FROM created_at) AS month,
                           EXTRACT(DOW FROM created_at)   AS weekday   -- 0 = Sunday
                    FROM orders;
                    -- MySQL: YEAR(created_at), MONTH(created_at), DAYOFWEEK(created_at) (1 = Sunday)""", "extract"),
            new SqlSnippet("date", """
                    SELECT ended_at - started_at                            AS duration,      -- interval
                           EXTRACT(EPOCH FROM (ended_at - started_at)) / 60 AS minutes,
                           CURRENT_DATE - due_date                          AS days_overdue   -- integer
                    FROM tasks;
                    -- MySQL: TIMESTAMPDIFF(MINUTE, started_at, ended_at), DATEDIFF(CURDATE(), due_date)""", "dateDiff"),
            new SqlSnippet("date", """
                    SELECT name, EXTRACT(YEAR FROM age(CURRENT_DATE, birth_date)) AS age
                    FROM people;
                    -- MySQL: TIMESTAMPDIFF(YEAR, birth_date, CURDATE())""", "age"),
            new SqlSnippet("date", """
                    SELECT to_char(created_at, 'YYYY-MM-DD HH24:MI') FROM orders;   -- PostgreSQL, Oracle
                    -- MySQL: DATE_FORMAT(created_at, '%Y-%m-%d %H:%i')
                    -- SQL Server: FORMAT(created_at, 'yyyy-MM-dd HH:mm')""", "formatDate"),
            new SqlSnippet("date", """
                    SELECT to_date('05/11/2026', 'DD/MM/YYYY'),
                           to_timestamp('2026-11-05 14:30', 'YYYY-MM-DD HH24:MI');
                    -- MySQL: STR_TO_DATE('05/11/2026', '%d/%m/%Y')""", "parseDate"),
            new SqlSnippet("date", """
                    SELECT created_at AT TIME ZONE 'Asia/Jakarta' AS local_time FROM orders;
                    SET TIME ZONE 'UTC';
                    -- MySQL: CONVERT_TZ(created_at, 'UTC', 'Asia/Jakarta')""", "timeZone"),
            new SqlSnippet("date", """
                    SELECT date_trunc('month', CURRENT_DATE)::date                             AS first_day,
                           (date_trunc('month', CURRENT_DATE) + INTERVAL '1 month - 1 day')::date AS last_day;
                    -- MySQL: DATE_FORMAT(CURDATE(), '%Y-%m-01'), LAST_DAY(CURDATE())""", "monthBounds"),
            new SqlSnippet("date", """
                    SELECT d::date AS day, COUNT(o.id) AS orders
                    FROM generate_series('2026-11-01'::date, '2026-11-30'::date, INTERVAL '1 day') AS d
                    LEFT JOIN orders o ON o.created_at >= d AND o.created_at < d + INTERVAL '1 day'
                    GROUP BY d
                    ORDER BY d;""", "generateSeries"),
            new SqlSnippet("date", """
                    SELECT EXTRACT(EPOCH FROM now())::bigint AS unix_seconds,
                           to_timestamp(1762300800)          AS from_unix;
                    -- MySQL: UNIX_TIMESTAMP(), FROM_UNIXTIME(1762300800)""", "epoch"),
            new SqlSnippet("date", """
                    SELECT * FROM bookings
                    WHERE room_id = 7
                      AND starts_at < :new_end
                      AND ends_at   > :new_start;""", "overlap"),

            // ---------- Strings ----------
            new SqlSnippet("string", """
                    SELECT first_name || ' ' || last_name     AS full_name,   -- NULL if any part is NULL
                           CONCAT_WS(' ', first_name, last_name) AS safe_name  -- skips NULLs
                    FROM users;""", "concat"),
            new SqlSnippet(
                    "string",
                    "SELECT LOWER(email), UPPER(code), TRIM(name), LENGTH(name), CHAR_LENGTH(name)\nFROM users;",
                    "caseTrim"),
            new SqlSnippet(
                    "string",
                    "SELECT LEFT(sku, 3), RIGHT(sku, 2), SUBSTRING(sku, 4, 3), POSITION('-' IN sku)\nFROM products;",
                    "substring"),
            new SqlSnippet("string", """
                    SELECT REPLACE(phone, '-', '')     AS digits,
                           SPLIT_PART(email, '@', 2)   AS domain   -- MySQL: SUBSTRING_INDEX(email, '@', -1)
                    FROM users;""", "replaceSplit"),
            new SqlSnippet("string", """
                    SELECT * FROM users WHERE email ~* '^[a-z0-9._%+-]+@example\\.com$';
                    SELECT regexp_replace(phone, '[^0-9]', '', 'g') FROM users;
                    -- MySQL: WHERE email REGEXP '...', REGEXP_REPLACE(phone, '[^0-9]', '')""", "regex"),
            new SqlSnippet("string", "SELECT LPAD(id::text, 8, '0') AS invoice_no FROM invoices;   -- 00000042", "pad"),
            new SqlSnippet("string", """
                    SELECT * FROM articles
                    WHERE to_tsvector('english', title || ' ' || body) @@ plainto_tsquery('english', 'database locking');
                    -- MySQL: WHERE MATCH(title, body) AGAINST ('database locking')  (needs a FULLTEXT index)""", "fullText"),

            // ---------- JSON & JSONB (PostgreSQL) ----------
            new SqlSnippet("json", """
                    CREATE TABLE events (
                      id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                      payload    JSONB NOT NULL DEFAULT '{}',
                      created_at TIMESTAMPTZ NOT NULL DEFAULT now()
                    );
                    INSERT INTO events (payload) VALUES ('{"type": "signup", "email": "a@example.com"}');
                    -- JDBC: VALUES (?::jsonb) or CAST(? AS jsonb), then setString(1, json)""", "jsonType"),
            new SqlSnippet("json", """
                    SELECT payload->>'email'            AS email,      -- text
                           payload->'address'->>'city'  AS city,       -- nested object
                           payload->'items'->0->>'sku'  AS first_sku,  -- array element, 0-based
                           (payload->>'age')::int       AS age         -- cast before comparing
                    FROM events;
                    -- MySQL: payload->>'$.email', payload->>'$.items[0].sku'""", "jsonRead"),
            new SqlSnippet("json", """
                    SELECT payload #>> '{address,city}'  AS city,        -- path as text[], returns text
                           payload #>  '{items,0}'       AS first_item,  -- returns jsonb
                           payload['address']['city']    AS city_json    -- PostgreSQL 14+ subscript, returns jsonb
                    FROM events;""", "jsonPath"),
            new SqlSnippet("json", """
                    SELECT id, payload->>'status' AS status, (payload->>'amount')::numeric AS amount
                    FROM events
                    WHERE payload->>'status' = 'paid'
                      AND (payload->>'amount')::numeric >= 100
                      AND (payload->>'paid_at')::timestamptz >= now() - interval '7 days'
                    ORDER BY (payload->>'paid_at')::timestamptz DESC;""", "jsonFilterValue"),
            new SqlSnippet("json", """
                    SELECT * FROM events WHERE payload @> '{"type": "signup"}';
                    SELECT * FROM events WHERE payload @> '{"tags": ["vip"]}';                 -- array contains "vip"
                    SELECT * FROM events WHERE payload @> '{"address": {"city": "Jakarta"}}';  -- nested
                    -- MySQL: WHERE JSON_CONTAINS(payload, '"signup"', '$.type')""", "jsonFilter"),
            new SqlSnippet("json", """
                    SELECT * FROM events WHERE payload ? 'coupon';                     -- key exists
                    SELECT * FROM events WHERE payload ?| array['coupon', 'voucher'];  -- any of these keys
                    SELECT * FROM events WHERE payload ?& array['email', 'phone'];     -- all of these keys
                    -- JDBC reads ? as a bind parameter: write ?? (pgjdbc) or payload @? '$.coupon'
                    -- MySQL: JSON_CONTAINS_PATH(payload, 'one', '$.coupon')""", "jsonKeyExists"),
            new SqlSnippet("json", """
                    SELECT * FROM orders WHERE payload @? '$.items[*] ? (@.qty > 10)';  -- any item with qty > 10
                    SELECT * FROM orders WHERE payload @@ '$.total > 1000';             -- path predicate is true
                    SELECT jsonb_path_query_array(payload, '$.items[*].sku') AS skus FROM orders;
                    SELECT * FROM orders
                    WHERE jsonb_path_exists(payload, '$.items[*] ? (@.sku == $sku)', jsonb_build_object('sku', 'A1'));
                    -- values go in the vars argument, never concatenated into the path""", "jsonPathQuery"),
            new SqlSnippet("json", """
                    SELECT JSON_VALUE(payload, '$.address.city')                        AS city,
                           JSON_VALUE(payload, '$.total' RETURNING numeric)             AS total,
                           JSON_VALUE(payload, '$.qty' RETURNING int DEFAULT 0 ON EMPTY) AS qty,
                           JSON_QUERY(payload, '$.items')                               AS items,
                           JSON_EXISTS(payload, '$.coupon')                             AS has_coupon
                    FROM orders;
                    -- PostgreSQL 17+. Oracle has the same functions; MySQL 8 and SQL Server have JSON_VALUE""", "jsonSqlStandard"),
            new SqlSnippet("json", """
                    SELECT id, jsonb_array_elements_text(payload->'tags') AS tag FROM events;
                    SELECT tag, count(*) FROM events, jsonb_array_elements_text(payload->'tags') AS tag
                    GROUP BY tag ORDER BY count(*) DESC;
                    SELECT id, jsonb_array_length(payload->'tags') AS tag_count FROM events;
                    -- MySQL: JSON_TABLE(payload, '$.tags[*]' COLUMNS (tag VARCHAR(50) PATH '$')) AS t""", "jsonArray"),
            new SqlSnippet("json", """
                    SELECT o.id, i.sku, i.qty, i.price, i.qty * i.price AS line_total
                    FROM orders o
                    CROSS JOIN LATERAL jsonb_to_recordset(o.payload->'items') AS i(sku text, qty int, price numeric);
                    -- PostgreSQL 17+ (MySQL 8 is similar):
                    -- JSON_TABLE(o.payload, '$.items[*]' COLUMNS (sku varchar(50) PATH '$.sku', qty int PATH '$.qty')) AS i""", "jsonToRecordset"),
            new SqlSnippet("json", """
                    SELECT key, value FROM users, jsonb_each_text(settings) WHERE id = 1;
                    SELECT key, count(*) AS rows_with_key
                    FROM events, jsonb_object_keys(payload) AS key
                    GROUP BY key ORDER BY rows_with_key DESC;""", "jsonEach"),
            new SqlSnippet("json", """
                    SELECT payload->>'type'                   AS type,
                           count(*)                           AS event_count,
                           sum((payload->>'amount')::numeric) AS amount
                    FROM events
                    WHERE created_at >= date_trunc('month', now())
                    GROUP BY payload->>'type'
                    ORDER BY event_count DESC;""", "jsonGroupBy"),
            new SqlSnippet("json", """
                    SELECT c.id,
                           COALESCE(jsonb_agg(jsonb_build_object('id', o.id, 'total', o.total) ORDER BY o.id)
                                      FILTER (WHERE o.id IS NOT NULL), '[]') AS orders
                    FROM customers c
                    LEFT JOIN orders o ON o.customer_id = c.id
                    GROUP BY c.id;
                    -- MySQL: JSON_ARRAYAGG(JSON_OBJECT('id', o.id, 'total', o.total))""", "jsonBuild"),
            new SqlSnippet("json", """
                    SELECT user_id, jsonb_object_agg(name, value) AS settings   -- {"theme": "dark", "lang": "id"}
                    FROM user_settings
                    GROUP BY user_id;
                    -- MySQL: JSON_OBJECTAGG(name, value)""", "jsonObjectAgg"),
            new SqlSnippet("json", """
                    SELECT to_jsonb(u) - 'password_hash' AS user_json FROM users u WHERE id = 1;
                    SELECT jsonb_strip_nulls(to_jsonb(p)) FROM products p;   -- drop keys whose value is null
                    SELECT jsonb_pretty(payload) FROM events WHERE id = 1;   -- indented, for reading""", "jsonRowToJson"),
            new SqlSnippet("json", """
                    UPDATE users SET settings = jsonb_set(settings, '{theme}', '"dark"') WHERE id = 1;
                    UPDATE users SET settings = jsonb_set(settings, '{notify,email}', to_jsonb(false)) WHERE id = 1;
                    UPDATE users SET settings['theme'] = '"dark"' WHERE id = 1;   -- PostgreSQL 14+, creates missing parents
                    -- MySQL: SET settings = JSON_SET(settings, '$.theme', 'dark')""", "jsonUpdate"),
            new SqlSnippet("json", """
                    UPDATE users
                    SET settings = COALESCE(settings, '{}') || '{"theme": "dark", "lang": "id"}'
                    WHERE id = 1;
                    -- MySQL: JSON_MERGE_PATCH(settings, '{"theme": "dark"}'), which also merges nested objects""", "jsonMerge"),
            new SqlSnippet("json", """
                    UPDATE users SET settings = settings - 'legacy_flag' WHERE id = 1;     -- one key
                    UPDATE users SET settings = settings - '{a,b}'::text[] WHERE id = 1;   -- several keys
                    UPDATE users SET settings = settings #- '{address,zip}' WHERE id = 1;  -- nested path
                    UPDATE events SET payload = jsonb_set(payload, '{tags}', (payload->'tags') - 'vip')
                    WHERE payload @> '{"tags": ["vip"]}';                                -- remove "vip" from an array
                    -- MySQL: JSON_REMOVE(settings, '$.legacy_flag')""", "jsonRemove"),
            new SqlSnippet("json", """
                    UPDATE events
                    SET payload = jsonb_set(payload, '{tags}', COALESCE(payload->'tags', '[]') || '["vip"]')
                    WHERE id = 1
                      AND NOT payload @> '{"tags": ["vip"]}';   -- skip if already there
                    -- At a position: jsonb_insert(payload, '{tags,0}', '"first"')
                    -- MySQL: JSON_ARRAY_APPEND(payload, '$.tags', 'vip')""", "jsonArrayAppend"),
            new SqlSnippet("json", """
                    UPDATE orders
                    SET payload = jsonb_set(payload, '{items}', COALESCE((
                          SELECT jsonb_agg(CASE WHEN item->>'sku' = 'A1'
                                                THEN item || '{"qty": 5}'
                                                ELSE item END ORDER BY idx)
                          FROM jsonb_array_elements(payload->'items') WITH ORDINALITY AS e(item, idx)
                        ), '[]'))
                    WHERE id = 42;""", "jsonArrayItemUpdate"),
            new SqlSnippet("json", """
                    -- Trap: when :nickname is NULL, to_jsonb() and jsonb_set() return NULL and settings is wiped
                    UPDATE users SET settings = jsonb_set(settings, '{nickname}', to_jsonb(CAST(:nickname AS text)))
                    WHERE id = 1;

                    -- Safe (PostgreSQL 13+): NULL becomes JSON null, or 'delete_key' removes the key
                    UPDATE users SET settings = jsonb_set_lax(settings, '{nickname}', to_jsonb(CAST(:nickname AS text)))
                    WHERE id = 1;
                    UPDATE users
                    SET settings = jsonb_set_lax(settings, '{nickname}', to_jsonb(CAST(:nickname AS text)), true, 'delete_key')
                    WHERE id = 1;""", "jsonNullTrap"),
            new SqlSnippet("json", """
                    INSERT INTO products (sku, name, price)
                    SELECT sku, name, price
                    FROM jsonb_to_recordset(CAST(:items AS jsonb)) AS x(sku text, name text, price numeric)
                    ON CONFLICT (sku) DO UPDATE SET name = EXCLUDED.name, price = EXCLUDED.price;
                    -- :items = '[{"sku": "A1", "name": "Pen", "price": 2.5}, ...]'""", "jsonImport"),
            new SqlSnippet("json", """
                    ALTER TABLE events
                      ADD CONSTRAINT payload_is_object CHECK (jsonb_typeof(payload) = 'object'),
                      ADD CONSTRAINT payload_has_type  CHECK (payload->>'type' IS NOT NULL);
                    -- PostgreSQL 16+: find text that is not a valid JSON object before casting it
                    SELECT id FROM raw_imports WHERE body IS NOT JSON OBJECT;""", "jsonValidate"),
            new SqlSnippet("json", """
                    CREATE INDEX CONCURRENTLY events_payload_path ON events USING GIN (payload jsonb_path_ops);  -- @>, @?, @@
                    CREATE INDEX CONCURRENTLY events_payload_all  ON events USING GIN (payload);  -- also ?, ?|, ?&
                    CREATE INDEX CONCURRENTLY events_email ON events ((payload->>'email'));       -- =, range, ORDER BY""", "jsonIndex"),
            new SqlSnippet("json", """
                    ALTER TABLE events
                      ADD COLUMN event_type TEXT GENERATED ALWAYS AS (payload->>'type') STORED;
                    CREATE INDEX CONCURRENTLY events_event_type ON events (event_type);
                    SELECT * FROM events WHERE event_type = 'signup';""", "jsonGenerated"),

            // ---------- Tables & ALTER TABLE ----------
            new SqlSnippet("ddl", """
                    CREATE TABLE orders (
                      id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                      customer_id BIGINT NOT NULL REFERENCES customers (id),
                      status      TEXT NOT NULL DEFAULT 'new' CHECK (status IN ('new', 'paid', 'cancelled')),
                      total       NUMERIC(12, 2) NOT NULL,
                      created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
                    );""", "createTable"),
            new SqlSnippet(
                    "ddl",
                    "CREATE TABLE orders_backup_20261105 AS SELECT * FROM orders;\n-- structure only (MySQL): CREATE TABLE orders_copy LIKE orders;",
                    "createTableAs"),
            new SqlSnippet("ddl", "ALTER TABLE users ADD COLUMN phone TEXT;", "addColumn"),
            new SqlSnippet("ddl", """
                    ALTER TABLE users ADD COLUMN status TEXT;                      -- 1. nullable: instant
                    UPDATE users SET status = 'active' WHERE status IS NULL;       -- 2. backfill in batches
                    ALTER TABLE users ALTER COLUMN status SET DEFAULT 'active';
                    ALTER TABLE users ALTER COLUMN status SET NOT NULL;            -- 3. enforce
                    -- MySQL: ALTER TABLE users MODIFY COLUMN status VARCHAR(20) NOT NULL DEFAULT 'active';""", "addColumnNotNull"),
            new SqlSnippet("ddl", "ALTER TABLE users DROP COLUMN IF EXISTS legacy_code;", "dropColumn"),
            new SqlSnippet(
                    "ddl",
                    "ALTER TABLE users RENAME COLUMN fullname TO full_name;\nALTER TABLE customer RENAME TO customers;   -- MySQL: RENAME TABLE customer TO customers;",
                    "rename"),
            new SqlSnippet("ddl", """
                    ALTER TABLE orders ALTER COLUMN total TYPE NUMERIC(14, 2);
                    ALTER TABLE events ALTER COLUMN code TYPE INTEGER USING code::integer;
                    -- MySQL: ALTER TABLE orders MODIFY COLUMN total DECIMAL(14, 2) NOT NULL;""", "alterType"),
            new SqlSnippet("ddl", """
                    ALTER TABLE orders ALTER COLUMN status SET DEFAULT 'new';
                    ALTER TABLE orders ALTER COLUMN status DROP DEFAULT;
                    ALTER TABLE orders ALTER COLUMN note DROP NOT NULL;""", "alterDefault"),
            new SqlSnippet("ddl", """
                    ALTER TABLE orders
                      ADD CONSTRAINT fk_orders_customer FOREIGN KEY (customer_id)
                      REFERENCES customers (id) ON DELETE RESTRICT NOT VALID;
                    ALTER TABLE orders VALIDATE CONSTRAINT fk_orders_customer;""", "addForeignKey"),
            new SqlSnippet("ddl", """
                    ALTER TABLE products ADD CONSTRAINT chk_price_positive CHECK (price > 0);
                    ALTER TABLE users ADD CONSTRAINT uq_users_email UNIQUE (email);""", "addConstraint"),
            new SqlSnippet(
                    "ddl",
                    "ALTER TABLE orders DROP CONSTRAINT IF EXISTS fk_orders_customer;\n-- MySQL: ALTER TABLE orders DROP FOREIGN KEY fk_orders_customer;",
                    "dropConstraint"),
            new SqlSnippet(
                    "ddl",
                    "CREATE OR REPLACE VIEW active_users AS\nSELECT id, name, email FROM users WHERE deleted_at IS NULL;",
                    "view"),
            new SqlSnippet(
                    "ddl",
                    "COMMENT ON COLUMN orders.total IS 'Gross amount incl. tax, in IDR';\n-- MySQL: total DECIMAL(12, 2) COMMENT 'Gross amount incl. tax' in the column definition",
                    "comment"),
            new SqlSnippet("ddl", """
                    SELECT setval(pg_get_serial_sequence('orders', 'id'), (SELECT MAX(id) FROM orders));
                    -- MySQL: ALTER TABLE orders AUTO_INCREMENT = 100001;""", "resetSequence"),
            new SqlSnippet("ddl", "DROP TABLE IF EXISTS tmp_import;", "drop"),

            // ---------- Indexes ----------
            new SqlSnippet(
                    "index", "CREATE INDEX idx_orders_customer_created ON orders (customer_id, created_at);", "index"),
            new SqlSnippet(
                    "index", "CREATE INDEX CONCURRENTLY idx_orders_status ON orders (status);", "indexConcurrently"),
            new SqlSnippet(
                    "index",
                    "CREATE UNIQUE INDEX uq_users_email_active ON users (email) WHERE deleted_at IS NULL;",
                    "uniqueIndex"),
            new SqlSnippet(
                    "index",
                    "CREATE INDEX idx_orders_unpaid ON orders (created_at) WHERE status = 'new';",
                    "partialIndex"),
            new SqlSnippet(
                    "index",
                    "CREATE INDEX idx_users_lower_email ON users (lower(email));\nSELECT * FROM users WHERE lower(email) = lower(:email);",
                    "expressionIndex"),
            new SqlSnippet(
                    "index",
                    "CREATE INDEX idx_orders_customer_inc ON orders (customer_id) INCLUDE (total, status);",
                    "coveringIndex"),
            new SqlSnippet("index", "CREATE INDEX idx_events_payload ON events USING GIN (payload);", "ginIndex"),
            new SqlSnippet("index", """
                    SELECT indexname, indexdef FROM pg_indexes WHERE tablename = 'orders';
                    DROP INDEX CONCURRENTLY IF EXISTS idx_orders_status;
                    -- MySQL: SHOW INDEX FROM orders;  DROP INDEX idx_orders_status ON orders;""", "listDropIndex"),

            // ---------- Production operations ----------
            new SqlSnippet("ops", """
                    BEGIN;
                    SELECT id, status FROM orders WHERE id = 1001;          -- 1. check the WHERE first
                    UPDATE orders SET status = 'cancelled' WHERE id = 1001;  -- 2. expect "UPDATE 1"
                    SELECT id, status FROM orders WHERE id = 1001;          -- 3. verify
                    COMMIT;                                                 -- or ROLLBACK""", "safeManualFix"),
            new SqlSnippet("ops", """
                    SELECT column_name, data_type, is_nullable, column_default
                    FROM information_schema.columns
                    WHERE table_name = 'orders'
                    ORDER BY ordinal_position;
                    -- psql: \\d orders   MySQL: SHOW CREATE TABLE orders;""", "describeTable"),
            new SqlSnippet("ops", """
                    SELECT pid, now() - query_start AS duration, state, wait_event_type, query
                    FROM pg_stat_activity
                    WHERE state <> 'idle'
                    ORDER BY duration DESC;
                    -- MySQL: SHOW FULL PROCESSLIST;""", "activeQueries"),
            new SqlSnippet("ops", """
                    SELECT pg_cancel_backend(12345);      -- stop the query, keep the connection
                    SELECT pg_terminate_backend(12345);   -- close the connection
                    -- MySQL: KILL QUERY 12345;  KILL 12345;""", "cancelQuery"),
            new SqlSnippet("ops", """
                    SET statement_timeout = '30s';                        -- this session
                    ALTER ROLE app_user SET statement_timeout = '30s';    -- default for a role
                    -- MySQL: SET SESSION max_execution_time = 30000;  (SELECT only, milliseconds)""", "statementTimeout"),
            new SqlSnippet("ops", """
                    SELECT calls, round(mean_exec_time::numeric, 1) AS avg_ms,
                           round(total_exec_time::numeric) AS total_ms, query
                    FROM pg_stat_statements
                    ORDER BY total_exec_time DESC
                    LIMIT 10;""", "slowQueries"),
            new SqlSnippet("ops", """
                    SELECT relname AS table_name,
                           pg_size_pretty(pg_total_relation_size(relid)) AS total_size,
                           n_live_tup AS approx_rows
                    FROM pg_stat_user_tables
                    ORDER BY pg_total_relation_size(relid) DESC
                    LIMIT 20;""", "tableSizes"),
            new SqlSnippet("ops", """
                    SELECT reltuples::bigint AS approx_rows FROM pg_class WHERE relname = 'events';
                    -- MySQL: SELECT TABLE_ROWS FROM information_schema.TABLES WHERE TABLE_NAME = 'events';""", "approxCount"),
            new SqlSnippet("ops", """
                    SELECT relname AS table_name, indexrelname AS index_name, idx_scan,
                           pg_size_pretty(pg_relation_size(indexrelid)) AS size
                    FROM pg_stat_user_indexes
                    WHERE idx_scan = 0
                    ORDER BY pg_relation_size(indexrelid) DESC;""", "unusedIndexes"),
            new SqlSnippet("ops", "VACUUM (ANALYZE, VERBOSE) orders;\n-- MySQL: OPTIMIZE TABLE orders;", "vacuum"),
            new SqlSnippet("ops", """
                    CREATE ROLE report_reader LOGIN PASSWORD 'change-me';
                    GRANT CONNECT ON DATABASE shop TO report_reader;
                    GRANT USAGE ON SCHEMA public TO report_reader;
                    GRANT SELECT ON ALL TABLES IN SCHEMA public TO report_reader;
                    ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT SELECT ON TABLES TO report_reader;
                    -- MySQL: CREATE USER 'report'@'%' IDENTIFIED BY '...'; GRANT SELECT ON shop.* TO 'report'@'%';""", "readOnlyUser"),

            // ---------- Security ----------
            new SqlSnippet("security", """
                    -- never: "SELECT * FROM users WHERE email = '" + email + "'"
                    SELECT * FROM users WHERE email = ?;    -- JDBC PreparedStatement, MySQL drivers
                    SELECT * FROM users WHERE email = $1;   -- PostgreSQL drivers""", "parameterized"),
            new SqlSnippet("security", """
                    PREPARE find_user (text) AS SELECT * FROM users WHERE email = $1;
                    EXECUTE find_user('ana@example.com');
                    DEALLOCATE find_user;""", "prepare"),
            new SqlSnippet("security", """
                    -- table and column names cannot be bound as parameters:
                    -- map user input to a fixed list in code (sort=name -> ORDER BY name)
                    SELECT format('SELECT * FROM %I ORDER BY %I', 'orders', 'created_at');   -- PostgreSQL quoting""", "dynamicIdentifier"),
            new SqlSnippet("security", """
                    ALTER TABLE orders ENABLE ROW LEVEL SECURITY;
                    CREATE POLICY tenant_isolation ON orders
                      USING (tenant_id = current_setting('app.tenant_id')::bigint);
                    SET app.tenant_id = '42';   -- per request, inside the transaction""", "rowLevelSecurity"),

            // ---------- ACID ----------
            new SqlSnippet("acid", """
                    BEGIN;
                    UPDATE accounts SET balance = balance - 100 WHERE id = 1;
                    UPDATE accounts SET balance = balance + 100 WHERE id = 2;
                    COMMIT;""", "transaction"),
            new SqlSnippet("acid", """
                    BEGIN;
                    DELETE FROM orders WHERE customer_id = 42;
                    -- something looks wrong
                    ROLLBACK;""", "rollback"),
            new SqlSnippet("acid", """
                    BEGIN;
                    INSERT INTO orders (customer_id, total) VALUES (1, 50);
                    SAVEPOINT before_items;
                    INSERT INTO order_items (order_id, sku) VALUES (1, 'X');
                    ROLLBACK TO SAVEPOINT before_items;
                    COMMIT;""", "savepoint"),
            new SqlSnippet(
                    "acid",
                    "ALTER TABLE accounts ADD CONSTRAINT balance_not_negative CHECK (balance >= 0);",
                    "consistency"),

            // ---------- Isolation ----------
            new SqlSnippet("isolation", "SET TRANSACTION ISOLATION LEVEL READ UNCOMMITTED;", "readUncommitted"),
            new SqlSnippet("isolation", "BEGIN ISOLATION LEVEL READ COMMITTED;", "readCommitted"),
            new SqlSnippet("isolation", "BEGIN ISOLATION LEVEL REPEATABLE READ;", "repeatableRead"),
            new SqlSnippet("isolation", "BEGIN ISOLATION LEVEL SERIALIZABLE;", "serializable"),
            new SqlSnippet(
                    "isolation",
                    "SHOW transaction_isolation;          -- PostgreSQL\nSELECT @@transaction_isolation;   -- MySQL",
                    "showIsolation"),

            // ---------- Locking ----------
            new SqlSnippet("locking", """
                    BEGIN;
                    SELECT balance FROM accounts WHERE id = 1 FOR UPDATE;
                    UPDATE accounts SET balance = balance - 100 WHERE id = 1;
                    COMMIT;""", "forUpdate"),
            new SqlSnippet("locking", "SELECT * FROM customers WHERE id = 7 FOR SHARE;", "forShare"),
            new SqlSnippet("locking", "SELECT * FROM seats WHERE id = 12 FOR UPDATE NOWAIT;", "nowait"),
            new SqlSnippet("locking", """
                    SELECT id FROM jobs WHERE status = 'queued'
                    ORDER BY id LIMIT 10
                    FOR UPDATE SKIP LOCKED;""", "skipLocked"),
            new SqlSnippet("locking", """
                    UPDATE products SET stock = stock - 1, version = version + 1
                    WHERE id = 5 AND version = :version_read;""", "optimistic"),
            new SqlSnippet(
                    "locking", "UPDATE products SET stock = stock - 1 WHERE id = 5 AND stock > 0;", "atomicUpdate"),
            new SqlSnippet("locking", "LOCK TABLE inventory IN SHARE ROW EXCLUSIVE MODE;", "lockTable"),
            new SqlSnippet("locking", "SELECT pg_advisory_xact_lock(hashtext('monthly-report'));", "advisoryLock"),

            // ---------- Deadlock ----------
            new SqlSnippet("deadlock", """
                    -- Tx A: UPDATE accounts SET balance = balance - 10 WHERE id = 1;
                    -- Tx B: UPDATE accounts SET balance = balance - 10 WHERE id = 2;
                    -- Tx A: UPDATE accounts SET balance = balance + 10 WHERE id = 2;  -- waits for B
                    -- Tx B: UPDATE accounts SET balance = balance + 10 WHERE id = 1;  -- waits for A: deadlock""", "deadlockExample"),
            new SqlSnippet("deadlock", """
                    SELECT * FROM accounts WHERE id IN (1, 2)
                    ORDER BY id
                    FOR UPDATE;""", "lockOrder"),
            new SqlSnippet("deadlock", "SET lock_timeout = '3s';\nSET deadlock_timeout = '1s';", "lockTimeout"),
            new SqlSnippet("deadlock", """
                    SELECT pid, pg_blocking_pids(pid) AS blocked_by, state, query
                    FROM pg_stat_activity
                    WHERE cardinality(pg_blocking_pids(pid)) > 0;""", "findBlocking"),
            new SqlSnippet("deadlock", "SHOW ENGINE INNODB STATUS;", "innodbStatus"),

            // ---------- EXPLAIN ----------
            new SqlSnippet("explain", "EXPLAIN SELECT * FROM orders WHERE customer_id = 42;", "explainPlan"),
            new SqlSnippet("explain", "EXPLAIN ANALYZE SELECT * FROM orders WHERE customer_id = 42;", "explainAnalyze"),
            new SqlSnippet(
                    "explain",
                    "EXPLAIN (ANALYZE, BUFFERS, FORMAT TEXT)\nSELECT * FROM orders WHERE customer_id = 42;",
                    "explainBuffers"),
            new SqlSnippet("explain", """
                    BEGIN;
                    EXPLAIN ANALYZE DELETE FROM orders WHERE created_at < '2020-01-01';
                    ROLLBACK;""", "explainDml"),
            new SqlSnippet("explain", """
                    Index Scan using idx_orders_customer on orders
                      (cost=0.43..8.45 rows=1 width=64) (actual time=0.021..0.023 rows=3 loops=1)
                      Index Cond: (customer_id = 42)
                    Planning Time: 0.110 ms
                    Execution Time: 0.045 ms""", "readPlan"),
            new SqlSnippet(
                    "explain", "EXPLAIN FORMAT=TREE SELECT * FROM orders WHERE customer_id = 42;", "explainMysql"),
            new SqlSnippet("explain", "ANALYZE orders;", "analyzeStats"),

            // ---------- Normalization ----------
            new SqlSnippet("normalization", """
                    -- Not 1NF: several values in one column
                    -- orders(id, products = 'pen,book,bag')
                    CREATE TABLE order_items (
                      order_id BIGINT REFERENCES orders (id),
                      product_id BIGINT REFERENCES products (id),
                      qty INT NOT NULL,
                      PRIMARY KEY (order_id, product_id)
                    );""", "firstNf"),
            new SqlSnippet("normalization", """
                    -- Not 2NF: product_name depends only on product_id, not the whole key
                    -- order_items(order_id, product_id, product_name, qty)
                    -- Fix: keep product_name in products, join when needed""", "secondNf"),
            new SqlSnippet("normalization", """
                    -- Not 3NF: city_name depends on city_id, not on the customer
                    -- customers(id, name, city_id, city_name)
                    CREATE TABLE cities (id BIGINT PRIMARY KEY, name TEXT NOT NULL);
                    ALTER TABLE customers DROP COLUMN city_name;""", "thirdNf"),
            new SqlSnippet("normalization", """
                    ALTER TABLE posts ADD COLUMN comment_count INT NOT NULL DEFAULT 0;
                    -- keep it in the same transaction as the insert
                    UPDATE posts SET comment_count = comment_count + 1 WHERE id = :post_id;""", "denormCounter"),
            new SqlSnippet("normalization", """
                    ALTER TABLE order_items ADD COLUMN unit_price NUMERIC(12, 2) NOT NULL;
                    -- price copied at purchase time, later price changes do not touch old orders""", "denormSnapshot"),
            new SqlSnippet("normalization", """
                    CREATE MATERIALIZED VIEW daily_revenue AS
                    SELECT date_trunc('day', created_at) AS day, SUM(total) AS revenue
                    FROM orders GROUP BY 1;

                    REFRESH MATERIALIZED VIEW CONCURRENTLY daily_revenue;""", "materializedView"),

            // ---------- Partition ----------
            new SqlSnippet("partition", """
                    CREATE TABLE events (
                      id BIGINT NOT NULL,
                      created_at TIMESTAMPTZ NOT NULL,
                      payload JSONB,
                      PRIMARY KEY (id, created_at)
                    ) PARTITION BY RANGE (created_at);

                    CREATE TABLE events_2026_11 PARTITION OF events
                      FOR VALUES FROM ('2026-11-01') TO ('2026-12-01');""", "rangePartition"),
            new SqlSnippet("partition", """
                    CREATE TABLE customers (id BIGINT, region TEXT NOT NULL) PARTITION BY LIST (region);
                    CREATE TABLE customers_asia PARTITION OF customers FOR VALUES IN ('ID', 'SG', 'MY');""", "listPartition"),
            new SqlSnippet("partition", """
                    CREATE TABLE sessions (user_id BIGINT NOT NULL, data JSONB) PARTITION BY HASH (user_id);
                    CREATE TABLE sessions_p0 PARTITION OF sessions FOR VALUES WITH (MODULUS 4, REMAINDER 0);""", "hashPartition"),
            new SqlSnippet("partition", "CREATE TABLE events_default PARTITION OF events DEFAULT;", "defaultPartition"),
            new SqlSnippet("partition", """
                    ALTER TABLE events DETACH PARTITION events_2025_01 CONCURRENTLY;
                    DROP TABLE events_2025_01;""", "dropPartition"),
            new SqlSnippet(
                    "partition",
                    "EXPLAIN SELECT * FROM events WHERE created_at >= '2026-11-01' AND created_at < '2026-11-08';",
                    "pruning"),

            // ---------- Sharding ----------
            new SqlSnippet("sharding", """
                    -- every table that is sharded carries the shard key
                    CREATE TABLE orders (
                      tenant_id BIGINT NOT NULL,
                      id BIGINT NOT NULL,
                      total NUMERIC(12, 2),
                      PRIMARY KEY (tenant_id, id)
                    );""", "shardKey"),
            new SqlSnippet(
                    "sharding",
                    "-- application side: shard = hash(tenant_id) % shard_count\nSELECT abs(hashtext('42')) % 4 AS shard;",
                    "hashRouting"),
            new SqlSnippet("sharding", "SELECT * FROM orders WHERE tenant_id = 42 AND id = 1001;", "singleShardQuery"),
            new SqlSnippet("sharding", "SELECT status, COUNT(*) FROM orders GROUP BY status;", "crossShardQuery"),
            new SqlSnippet("sharding", """
                    -- Citus extension (PostgreSQL)
                    SELECT create_distributed_table('orders', 'tenant_id');
                    SELECT create_reference_table('countries');""", "citus"));

    private SqlCheatsheet() {}
}
