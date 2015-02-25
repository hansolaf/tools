package com.github.hansolaf.tools.sql;

import org.h2.jdbcx.JdbcDataSource;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import static org.junit.Assert.assertEquals;

public class SQLTest {

    @Test
    public void basicUsage() throws IOException {
        try {
            JdbcDataSource ds = new JdbcDataSource();
            ds.setURL("jdbc:h2:./testdb");
            SQL.update(ds, "drop table if exists person");
            SQL.update(ds, "create table person (id integer, name varchar(30), age integer)");
            SQL.update(ds, "insert into person (id, name, age) values (?, ?, ?)", 1, "James", 29);
            assertEquals("James", SQL.selectFirst(ds, rs -> rs.getString("name"), "select * from person"));
        } finally {
            Files.deleteIfExists(new File("./testdb.mv.db").toPath());
        }
    }

}
