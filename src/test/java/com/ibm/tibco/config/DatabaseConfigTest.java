package com.ibm.tibco.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import javax.sql.DataSource;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class DatabaseConfigTest {

    @InjectMocks
    private DatabaseConfig databaseConfig;

    @Test
    void shouldCreateDb1DataSource() {
        DataSource dataSource = databaseConfig.db1DataSource();
        assertNotNull(dataSource);
    }

    @Test
    void shouldCreateDb2DataSource() {
        DataSource dataSource = databaseConfig.db2DataSource();
        assertNotNull(dataSource);
    }

    @Test
    void shouldHaveDialectFields() {
        ReflectionTestUtils.setField(databaseConfig, "db1Dialect", "org.hibernate.dialect.MariaDBDialect");
        ReflectionTestUtils.setField(databaseConfig, "db1DdlAuto", "none");
        ReflectionTestUtils.setField(databaseConfig, "db1BatchSize", "50");
        ReflectionTestUtils.setField(databaseConfig, "db1OrderInserts", "true");
        ReflectionTestUtils.setField(databaseConfig, "db2Dialect", "org.hibernate.dialect.MariaDBDialect");
        ReflectionTestUtils.setField(databaseConfig, "db2DdlAuto", "none");

        String db1Dialect = (String) ReflectionTestUtils.getField(databaseConfig, "db1Dialect");
        String db2Dialect = (String) ReflectionTestUtils.getField(databaseConfig, "db2Dialect");

        assertEquals("org.hibernate.dialect.MariaDBDialect", db1Dialect);
        assertEquals("org.hibernate.dialect.MariaDBDialect", db2Dialect);
    }
}