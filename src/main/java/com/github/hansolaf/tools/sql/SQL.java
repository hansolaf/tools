package com.github.hansolaf.tools.sql;

import javax.sql.DataSource;
import java.sql.*;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.*;

public class SQL {

    public static interface Fn<T, R> {
        R apply(T t) throws SQLException;
    }

    public static <T> T selectFirst(DataSource ds, Fn<ResultSet, T> rowMapper, String sql, Object... args) {
        return select(ds, rowMapper, sql, args).stream().findFirst().orElse(null);
    }

    public static <T> List<T> select(DataSource ds, Fn<ResultSet, T> mapper, String sql, Object... args) {
        return doWithConnection(ds, connection -> {
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                try (ResultSet rs = prepare(statement, args).executeQuery()) {
                    List<T> results = new ArrayList<>();
                    while (rs.next())
                        results.add(mapper.apply(rs));
                    return results;
                }
            }
        });
    }

    public static int update(DataSource ds, String sql, Object... args) {
        return doWithConnection(ds, connection -> {
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                return prepare(statement, args).executeUpdate();
            }
        });
    }

    public static <T> T doWithConnection(DataSource ds, Fn<Connection, T> callback) {
        try (Connection connection = ds.getConnection()) {
            return callback.apply(connection);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private static PreparedStatement prepare(PreparedStatement prepStmt, Object... args) throws SQLException {
        for (int index = 0; index < args.length; index++) {
            if (args[index] == null) {
                prepStmt.setObject(index + 1, null);
            } else if (args[index] instanceof Enum) {
                prepStmt.setString(index + 1, ((Enum) args[index]).name());
            } else if (args[index] instanceof LocalDateTime) {
                prepStmt.setTimestamp(index + 1, Timestamp.valueOf((LocalDateTime) args[index]));
            } else if (args[index] instanceof Instant) {
                prepStmt.setTimestamp(index + 1, Timestamp.from((Instant) args[index]));
            } else {
                prepStmt.setObject(index + 1, args[index]);
            }
        }
        return prepStmt;
    }

}
