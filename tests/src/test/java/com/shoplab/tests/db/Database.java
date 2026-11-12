package com.shoplab.tests.db;

import com.shoplab.tests.config.TestConfig;
import io.qameta.allure.Allure;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * A tiny JDBC helper: run a SQL query, get Java objects back.
 * <p>
 * JDBC ("Java Database Connectivity") is the standard Java way to talk to any SQL database.
 * The steps are always the same:
 * <ol>
 *   <li>open a {@link Connection} (address, user, password);</li>
 *   <li>prepare a statement: SQL with "?" placeholders, then fill in the values;</li>
 *   <li>execute it and read the {@link ResultSet} row by row;</li>
 *   <li>close everything (try-with-resources does it automatically, even on errors).</li>
 * </ol>
 * This class writes those steps ONCE, so tests and {@link ShopDb} only write the SQL.
 */
public final class Database {

    private Database() {
    }

    /**
     * Turns the CURRENT row of a ResultSet into a Java object, e.g.
     * {@code rs -> new CartItemRow(rs.getLong("product_id"), rs.getInt("quantity"))}.
     * A "functional interface": it has one method, so it can be written as a lambda.
     */
    @FunctionalInterface
    public interface RowMapper<T> {
        T map(ResultSet rs) throws SQLException;
    }

    /** SELECT that returns many rows. */
    public static <T> List<T> query(String sql, RowMapper<T> mapper, Object... params) {
        try (Connection connection = connect();
             PreparedStatement statement = prepare(connection, sql, params);
             ResultSet rs = statement.executeQuery()) {

            List<T> rows = new ArrayList<>();
            while (rs.next()) {          // move to the next row; false when there are no more rows
                rows.add(mapper.map(rs));
            }
            report(sql, params, rows.size() + " row(s)");
            return rows;
        } catch (SQLException e) {
            throw new DbException(sql, e);
        }
    }

    /** SELECT that returns at most one row. Empty Optional = no such row. */
    public static <T> Optional<T> queryOne(String sql, RowMapper<T> mapper, Object... params) {
        List<T> rows = query(sql, mapper, params);
        if (rows.size() > 1) {
            throw new IllegalStateException("Expected at most 1 row but got " + rows.size() + " for: " + sql);
        }
        return rows.stream().findFirst();
    }

    /** SELECT count(*) ... : a single number. */
    public static long count(String sql, Object... params) {
        return queryOne(sql, rs -> rs.getLong(1), params).orElse(0L);
    }

    /**
     * INSERT / UPDATE / DELETE. Returns how many rows were changed.
     * Tests use it rarely: to prepare unusual data, or to check that the DATABASE itself rejects bad data.
     */
    public static int update(String sql, Object... params) {
        try (Connection connection = connect();
             PreparedStatement statement = prepare(connection, sql, params)) {
            int changed = statement.executeUpdate();
            report(sql, params, changed + " row(s) changed");
            return changed;
        } catch (SQLException e) {
            throw new DbException(sql, e);
        }
    }

    /** Fails fast with a clear message if the database cannot be reached. */
    public static void checkConnection() {
        try (Connection connection = connect()) {
            if (!connection.isValid(5)) {
                throw new IllegalStateException("Database connection is not valid: " + TestConfig.dbUrl());
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Cannot connect to the database at " + TestConfig.dbUrl()
                    + ". Is the shop running? Start it with: docker compose up -d", e);
        }
    }

    /**
     * A new connection for every query. Slower than a "connection pool", but simple and safe:
     * nothing stays open between tests. For a few hundred test queries the difference is tiny.
     */
    private static Connection connect() throws SQLException {
        return DriverManager.getConnection(TestConfig.dbUrl(), TestConfig.dbUser(), TestConfig.dbPassword());
    }

    /**
     * PreparedStatement with "?" placeholders, NEVER string concatenation:
     * <pre>
     *   BAD:  "SELECT * FROM users WHERE email = '" + email + "'"     // SQL injection!
     *   GOOD: "SELECT * FROM users WHERE email = ?"  + setObject(1, email)
     * </pre>
     * With "?", the value is sent separately from the SQL, so a value like {@code ' OR '1'='1}
     * is just a strange email, not a piece of SQL. Placeholders are numbered from 1.
     */
    private static PreparedStatement prepare(Connection connection, String sql, Object... params) throws SQLException {
        PreparedStatement statement = connection.prepareStatement(sql);
        for (int i = 0; i < params.length; i++) {
            statement.setObject(i + 1, params[i]);
        }
        return statement;
    }

    /** Shows each SQL query and its result in the Allure report, next to the HTTP requests. */
    private static void report(String sql, Object[] params, String result) {
        Allure.addAttachment("SQL: " + result, "text/plain",
                sql.strip() + System.lineSeparator() + "params: " + Arrays.toString(params));
    }
}
