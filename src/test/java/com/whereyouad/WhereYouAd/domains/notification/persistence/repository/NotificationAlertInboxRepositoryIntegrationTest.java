package com.whereyouad.WhereYouAd.domains.notification.persistence.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:notification-inbox;MODE=MySQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class NotificationAlertInboxRepositoryIntegrationTest {

    @Autowired
    private NotificationAlertInboxRepository repository;

    @Test
    void failedClaimCanBeReclaimedAndCompletedClaimCannot() {
        assertThat(repository.insertProcessingIfAbsent("event-1")).isEqualTo(1);
        assertThat(repository.insertProcessingIfAbsent("event-1")).isZero();
        assertThat(repository.markFailed("event-1")).isEqualTo(1);
        assertThat(repository.reclaimFailedOrStale("event-1", LocalDateTime.now())).isEqualTo(1);
        assertThat(repository.markCompleted("event-1")).isEqualTo(1);
        assertThat(repository.reclaimFailedOrStale("event-1", LocalDateTime.now().plusMinutes(1))).isZero();
    }
}
