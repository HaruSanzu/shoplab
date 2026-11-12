package com.shoplab.tests.db;

import java.sql.SQLException;

/**
 * A database error, as an unchecked exception (tests don't need "throws SQLException" everywhere).
 * <p>
 * {@link #getSqlState()} is the standard 5-character error code of SQL databases. The useful ones here:
 * <ul>
 *   <li>{@code 23505} unique_violation: a duplicate value in a UNIQUE column</li>
 *   <li>{@code 23514} check_violation: a CHECK rule was broken, e.g. stock &lt; 0</li>
 *   <li>{@code 23503} foreign_key_violation: a reference to a row that does not exist</li>
 * </ul>
 */
public class DbException extends RuntimeException {

    public static final String UNIQUE_VIOLATION = "23505";
    public static final String CHECK_VIOLATION = "23514";
    public static final String FOREIGN_KEY_VIOLATION = "23503";

    private final String sqlState;

    public DbException(String sql, SQLException cause) {
        super(cause.getMessage() + System.lineSeparator() + "SQL: " + sql.strip(), cause);
        this.sqlState = cause.getSQLState();
    }

    public String getSqlState() {
        return sqlState;
    }
}
