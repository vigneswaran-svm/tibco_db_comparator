package com.ibm.tibco.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.boot.orm.jpa.EntityManagerFactoryBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableTransactionManagement
public class DatabaseConfig {

    @Value("${db1.jpa.hibernate.dialect}")
    private String db1Dialect;

    @Value("${db1.jpa.hibernate.ddl-auto}")
    private String db1DdlAuto;

    @Value("${db2.jpa.hibernate.dialect}")
    private String db2Dialect;

    @Value("${db2.jpa.hibernate.ddl-auto}")
    private String db2DdlAuto;

    @Primary
    @Bean(name = "db1DataSource")
    @ConfigurationProperties(prefix = "db1.datasource")
    public DataSource db1DataSource() {
        return DataSourceBuilder.create().build();
    }

    @Primary
    @Bean(name = "db1EntityManagerFactory")
    public LocalContainerEntityManagerFactoryBean db1EntityManagerFactory(
            EntityManagerFactoryBuilder builder,
            @Qualifier("db1DataSource") DataSource dataSource) {

        Map<String, Object> properties = new HashMap<>();
        properties.put("hibernate.dialect", db1Dialect);
        properties.put("hibernate.hbm2ddl.auto", db1DdlAuto);

        return builder
                .dataSource(dataSource)
                .packages("com.ibm.tibco.model.db1")
                .persistenceUnit("db1")
                .properties(properties)
                .build();
    }

    @Primary
    @Bean(name = "db1TransactionManager")
    public PlatformTransactionManager db1TransactionManager(
            @Qualifier("db1EntityManagerFactory") LocalContainerEntityManagerFactoryBean db1EntityManagerFactory) {
        return new JpaTransactionManager(db1EntityManagerFactory.getObject());
    }

    @Bean(name = "db2DataSource")
    @ConfigurationProperties(prefix = "db2.datasource")
    public DataSource db2DataSource() {
        return DataSourceBuilder.create().build();
    }

    @Bean(name = "db2EntityManagerFactory")
    public LocalContainerEntityManagerFactoryBean db2EntityManagerFactory(
            EntityManagerFactoryBuilder builder,
            @Qualifier("db2DataSource") DataSource dataSource) {

        Map<String, Object> properties = new HashMap<>();
        properties.put("hibernate.dialect", db2Dialect);
        properties.put("hibernate.hbm2ddl.auto", db2DdlAuto);

        return builder
                .dataSource(dataSource)
                .packages("com.ibm.tibco.model.db2")
                .persistenceUnit("db2")
                .properties(properties)
                .build();
    }

    @Bean(name = "db2TransactionManager")
    public PlatformTransactionManager db2TransactionManager(
            @Qualifier("db2EntityManagerFactory") LocalContainerEntityManagerFactoryBean db2EntityManagerFactory) {
        return new JpaTransactionManager(db2EntityManagerFactory.getObject());
    }
}