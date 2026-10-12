package com.jobbridge.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration
public class JobPublicationMigration {
    private static final String MIGRATION_ID = "job-publication-v1";

    @Bean
    @Order(1)
    CommandLineRunner migrateExistingJobListings(JdbcTemplate jdbc) {
        return args -> {
            jdbc.execute("CREATE TABLE IF NOT EXISTS app_migration (id VARCHAR(100) PRIMARY KEY)");
            Integer applied = jdbc.queryForObject(
                    "SELECT COUNT(*) FROM app_migration WHERE id = ?", Integer.class, MIGRATION_ID);
            if (applied == null || applied == 0) {
                jdbc.update("UPDATE job SET published = TRUE WHERE published IS NULL OR published = FALSE");
                jdbc.update("INSERT INTO app_migration (id) VALUES (?)", MIGRATION_ID);
            }
        };
    }
}
