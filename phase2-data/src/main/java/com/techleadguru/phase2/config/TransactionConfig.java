package com.techleadguru.phase2.config;

import jakarta.persistence.EntityManagerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.orm.jpa.JpaTransactionManager;

import javax.sql.DataSource;

/**
 * DAY 24 — Transaction Configuration
 *
 * WHY JpaTransactionManager DOESN'T SUPPORT NESTED PROPAGATION:
 *   JpaTransactionManager uses JpaDialect. For NESTED to work, the JpaDialect.beginTransaction()
 *   must return an object implementing SavepointManager — neither DefaultJpaDialect nor
 *   HibernateJpaDialect does this in Spring 6.
 *
 * SOLUTION: Register a DataSourceTransactionManager alongside JpaTransactionManager.
 *   DataSourceTransactionManager works at raw JDBC level and fully supports savepoints.
 *   BatchImportService uses @Transactional(transactionManager = "dataSourceTransactionManager").
 *
 * We must also register JpaTransactionManager explicitly (as @Primary "transactionManager")
 * because Spring Boot's @ConditionalOnMissingBean(PlatformTransactionManager.class) will
 * skip auto-configuration once it sees our DataSourceTransactionManager bean.
 *
 * PRODUCTION NOTE:
 *   In real apps, use Spring's TransactionTemplate with DataSource directly, or configure
 *   ChainedTransactionManager for XA-style multi-resource coordination.
 */
@Configuration
public class TransactionConfig {

    /**
     * Primary JPA transaction manager — replaces Spring Boot's auto-configured one.
     * Required here because DataSourceTransactionManager below triggers
     * @ConditionalOnMissingBean(PlatformTransactionManager.class) and prevents
     * Spring Boot from auto-creating a JpaTransactionManager.
     */
    @Primary
    @Bean("transactionManager")
    public JpaTransactionManager transactionManager(EntityManagerFactory emf) {
        return new JpaTransactionManager(emf);
    }

    /**
     * JDBC-level transaction manager that supports NESTED propagation via JDBC savepoints.
     * Used by Day24's BatchImportService for partial-batch-recovery demonstration.
     */
    @Bean("dataSourceTransactionManager")
    public DataSourceTransactionManager dataSourceTransactionManager(DataSource dataSource) {
        return new DataSourceTransactionManager(dataSource);
    }
}
