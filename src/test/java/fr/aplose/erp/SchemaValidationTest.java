package fr.aplose.erp;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Production runs with ddl-auto=validate: Hibernate refuses to start when an entity does not match
 * the schema built by the Flyway migrations. This test starts the application the same way, so a
 * missing column is caught here rather than at deployment.
 */
@SpringBootTest(properties = "spring.jpa.hibernate.ddl-auto=validate")
@ActiveProfiles("test")
class SchemaValidationTest {

    @Test
    void entitiesMatchTheMigratedSchema() {
        // the context starting is the assertion
    }
}
