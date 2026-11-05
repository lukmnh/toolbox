package io.github.lukmnh.toolbox.devutils.sql.domain;

import java.util.List;

/**
 * Referensi SQL: {@link #SYNTAX_GROUPS} untuk mode Cheatsheet, {@link #CONCEPT_GROUPS} untuk mode Concepts
 * (grup konsep punya intro di messages {@code devutils.sql.group.<group>.intro}). Urutan list = urutan tampil.
 * Contoh memakai PostgreSQL kecuali disebut lain di penjelasannya.
 */
public final class SqlCheatsheet {

    public static final List<String> SYNTAX_GROUPS =
            List.of("query", "join", "aggregate", "subquery", "window", "set", "dml", "ddl");

    public static final List<String> CONCEPT_GROUPS =
            List.of("acid", "isolation", "locking", "deadlock", "explain", "normalization", "partition", "sharding");

    public static final List<SqlSnippet> SNIPPETS = List.of(
            // ---------- Query ----------
            new SqlSnippet("query", "SELECT id, name FROM users WHERE active = true;", "select"),
            new SqlSnippet("query", "SELECT * FROM orders ORDER BY created_at DESC LIMIT 20 OFFSET 40;", "orderLimit"),
            new SqlSnippet("query", """
                    SELECT * FROM orders
                    WHERE (created_at, id) < (:last_created_at, :last_id)
                    ORDER BY created_at DESC, id DESC
                    LIMIT 20;""", "keyset"),
            new SqlSnippet("query", "SELECT DISTINCT country FROM customers;", "distinct"),
            new SqlSnippet(
                    "query",
                    "SELECT * FROM products WHERE price BETWEEN 10 AND 50 AND category_id IN (1, 2, 3);",
                    "betweenIn"),
            new SqlSnippet("query", "SELECT * FROM users WHERE email LIKE '%@example.com';", "like"),
            new SqlSnippet("query", "SELECT * FROM users WHERE deleted_at IS NULL;", "isNull"),
            new SqlSnippet("query", "SELECT name, COALESCE(phone, '-') AS phone FROM users;", "coalesce"),
            new SqlSnippet("query", """
                    SELECT id,
                           CASE WHEN total >= 1000 THEN 'big'
                                WHEN total >= 100 THEN 'medium'
                                ELSE 'small' END AS size
                    FROM orders;""", "case"),

            // ---------- Join ----------
            new SqlSnippet(
                    "join", "SELECT o.id, c.name FROM orders o JOIN customers c ON c.id = o.customer_id;", "innerJoin"),
            new SqlSnippet(
                    "join",
                    "SELECT c.name, o.id FROM customers c LEFT JOIN orders o ON o.customer_id = c.id;",
                    "leftJoin"),
            new SqlSnippet(
                    "join", "SELECT a.id, b.id FROM table_a a FULL OUTER JOIN table_b b ON b.key = a.key;", "fullJoin"),
            new SqlSnippet("join", "SELECT s.size, c.color FROM sizes s CROSS JOIN colors c;", "crossJoin"),
            new SqlSnippet(
                    "join",
                    "SELECT e.name, m.name AS manager FROM employees e LEFT JOIN employees m ON m.id = e.manager_id;",
                    "selfJoin"),
            new SqlSnippet("join", """
                    SELECT c.* FROM customers c
                    WHERE EXISTS (SELECT 1 FROM orders o WHERE o.customer_id = c.id);""", "semiJoin"),
            new SqlSnippet("join", """
                    SELECT c.* FROM customers c
                    WHERE NOT EXISTS (SELECT 1 FROM orders o WHERE o.customer_id = c.id);""", "antiJoin"),
            new SqlSnippet("join", """
                    SELECT c.name, last_order.created_at FROM customers c
                    CROSS JOIN LATERAL (
                      SELECT created_at FROM orders o
                      WHERE o.customer_id = c.id ORDER BY created_at DESC LIMIT 1
                    ) last_order;""", "lateral"),

            // ---------- Aggregate ----------
            new SqlSnippet(
                    "aggregate",
                    "SELECT customer_id, COUNT(*), SUM(total) FROM orders GROUP BY customer_id;",
                    "groupBy"),
            new SqlSnippet(
                    "aggregate",
                    "SELECT customer_id, COUNT(*) FROM orders GROUP BY customer_id HAVING COUNT(*) > 5;",
                    "having"),
            new SqlSnippet("aggregate", "SELECT COUNT(DISTINCT customer_id) FROM orders;", "countDistinct"),
            new SqlSnippet("aggregate", """
                    SELECT COUNT(*) FILTER (WHERE status = 'paid')     AS paid,
                           COUNT(*) FILTER (WHERE status = 'refunded') AS refunded
                    FROM orders;""", "filter"),
            new SqlSnippet(
                    "aggregate",
                    "SELECT region, product, SUM(total) FROM sales GROUP BY ROLLUP (region, product);",
                    "rollup"),

            // ---------- Subquery & CTE ----------
            new SqlSnippet(
                    "subquery",
                    "SELECT * FROM products WHERE price > (SELECT AVG(price) FROM products);",
                    "scalarSubquery"),
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
                    )
                    SELECT * FROM tree;""", "recursiveCte"),

            // ---------- Window ----------
            new SqlSnippet("window", """
                    SELECT * FROM (
                      SELECT o.*, ROW_NUMBER() OVER (PARTITION BY customer_id ORDER BY created_at DESC) AS rn
                      FROM orders o
                    ) x WHERE rn = 1;""", "rowNumber"),
            new SqlSnippet(
                    "window",
                    "SELECT name, score, RANK() OVER (ORDER BY score DESC), DENSE_RANK() OVER (ORDER BY score DESC) FROM players;",
                    "rank"),
            new SqlSnippet(
                    "window",
                    "SELECT day, revenue, revenue - LAG(revenue) OVER (ORDER BY day) AS diff FROM daily_sales;",
                    "lagLead"),
            new SqlSnippet("window", """
                    SELECT day, revenue,
                           SUM(revenue) OVER (ORDER BY day) AS running_total,
                           AVG(revenue) OVER (ORDER BY day ROWS BETWEEN 6 PRECEDING AND CURRENT ROW) AS avg_7d
                    FROM daily_sales;""", "runningTotal"),

            // ---------- Set ----------
            new SqlSnippet("set", "SELECT email FROM customers UNION SELECT email FROM leads;", "union"),
            new SqlSnippet("set", "SELECT email FROM customers UNION ALL SELECT email FROM leads;", "unionAll"),
            new SqlSnippet("set", "SELECT email FROM customers INTERSECT SELECT email FROM leads;", "intersect"),
            new SqlSnippet("set", "SELECT email FROM customers EXCEPT SELECT email FROM unsubscribed;", "except"),

            // ---------- DML ----------
            new SqlSnippet(
                    "dml",
                    "INSERT INTO users (name, email) VALUES ('Ana', 'ana@example.com'), ('Budi', 'budi@example.com');",
                    "insert"),
            new SqlSnippet(
                    "dml",
                    "INSERT INTO archived_orders SELECT * FROM orders WHERE created_at < '2024-01-01';",
                    "insertSelect"),
            new SqlSnippet("dml", "UPDATE products SET price = price * 1.1 WHERE category_id = 3;", "update"),
            new SqlSnippet("dml", """
                    UPDATE orders o SET status = 'vip'
                    FROM customers c
                    WHERE c.id = o.customer_id AND c.tier = 'gold';""", "updateJoin"),
            new SqlSnippet("dml", "DELETE FROM sessions WHERE expires_at < now();", "delete"),
            new SqlSnippet("dml", """
                    INSERT INTO stock (sku, qty) VALUES ('A1', 5)
                    ON CONFLICT (sku) DO UPDATE SET qty = stock.qty + EXCLUDED.qty;""", "upsertPg"),
            new SqlSnippet("dml", """
                    INSERT INTO stock (sku, qty) VALUES ('A1', 5)
                    ON DUPLICATE KEY UPDATE qty = qty + VALUES(qty);""", "upsertMysql"),
            new SqlSnippet("dml", """
                    MERGE INTO stock s
                    USING incoming i ON s.sku = i.sku
                    WHEN MATCHED THEN UPDATE SET qty = s.qty + i.qty
                    WHEN NOT MATCHED THEN INSERT (sku, qty) VALUES (i.sku, i.qty);""", "merge"),
            new SqlSnippet("dml", "INSERT INTO users (name) VALUES ('Ana') RETURNING id, created_at;", "returning"),

            // ---------- DDL ----------
            new SqlSnippet("ddl", """
                    CREATE TABLE orders (
                      id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                      customer_id BIGINT NOT NULL REFERENCES customers (id),
                      status      TEXT NOT NULL DEFAULT 'new' CHECK (status IN ('new', 'paid', 'cancelled')),
                      total       NUMERIC(12, 2) NOT NULL,
                      created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
                    );""", "createTable"),
            new SqlSnippet("ddl", "ALTER TABLE users ADD COLUMN phone TEXT;", "addColumn"),
            new SqlSnippet(
                    "ddl", "CREATE INDEX idx_orders_customer_created ON orders (customer_id, created_at);", "index"),
            new SqlSnippet(
                    "ddl", "CREATE INDEX CONCURRENTLY idx_users_email ON users (lower(email));", "indexConcurrently"),
            new SqlSnippet(
                    "ddl",
                    "CREATE INDEX idx_orders_unpaid ON orders (created_at) WHERE status = 'new';",
                    "partialIndex"),
            new SqlSnippet(
                    "ddl",
                    "CREATE UNIQUE INDEX uq_users_email ON users (email) WHERE deleted_at IS NULL;",
                    "uniqueIndex"),
            new SqlSnippet(
                    "ddl",
                    "CREATE VIEW active_users AS SELECT id, name, email FROM users WHERE deleted_at IS NULL;",
                    "view"),
            new SqlSnippet("ddl", "TRUNCATE TABLE staging_events;", "truncate"),
            new SqlSnippet("ddl", "DROP TABLE IF EXISTS tmp_import;", "drop"),

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
