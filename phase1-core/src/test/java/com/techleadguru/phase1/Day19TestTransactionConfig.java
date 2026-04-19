package com.techleadguru.phase1;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Provides a minimal no-op PlatformTransactionManager so that
 * @Transactional test methods in phase1-core can be managed even though
 * phase1-core has no DataSource / JPA infrastructure.
 *
 * The only purpose is to satisfy Spring's TransactionTestExecutionListener
 * requirement; the manager correctly marks transactions as active/inactive
 * via TransactionSynchronizationManager so that Day19's assertions work.
 */
@TestConfiguration
public class Day19TestTransactionConfig {

    @Bean
    public PlatformTransactionManager noopTransactionManager() {
        return new AbstractPlatformTransactionManager() {

            @Override
            protected Object doGetTransaction() {
                return new Object();
            }

            @Override
            protected void doBegin(Object transaction, TransactionDefinition definition) {
                TransactionSynchronizationManager.setActualTransactionActive(true);
                if (definition.getName() != null) {
                    TransactionSynchronizationManager.setCurrentTransactionName(definition.getName());
                }
                TransactionSynchronizationManager.initSynchronization();
            }

            @Override
            protected void doCommit(DefaultTransactionStatus status) {
                TransactionSynchronizationManager.setActualTransactionActive(false);
            }

            @Override
            protected void doRollback(DefaultTransactionStatus status) {
                TransactionSynchronizationManager.setActualTransactionActive(false);
            }

            @Override
            protected void doCleanupAfterCompletion(Object transaction) {
                if (TransactionSynchronizationManager.isSynchronizationActive()) {
                    TransactionSynchronizationManager.clearSynchronization();
                }
                TransactionSynchronizationManager.setActualTransactionActive(false);
                TransactionSynchronizationManager.setCurrentTransactionName(null);
            }
        };
    }
}
