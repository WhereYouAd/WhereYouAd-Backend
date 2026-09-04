package com.whereyouad.WhereYouAd.global.common;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

// 엔티티 삭제 오류 방지 : Organization/User 엔티티를 참조하는 스키마 구조가 바뀌면 실패
// 새 엔티티를 추가한 시점에 "이건 조직/회원 삭제 시 어떻게 정리되지?" 를 생각하게 만드는 목적.
// 검토, 처리 이후 아래 상수에 새 항목을 추가하고 삭제 로직도 함께 반영 필요
@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class DeletionSafetyGuardTest {

    @Autowired
    private EntityManager em;

    // organization 을 참조하는 FK 전체 - "테이블.컬럼:삭제규칙"
    private static final Set<String> EXPECTED_ORG_FK = Set.of(
            // 조직 Hard Delete 가 명시적으로 삭제
            "ai_insight_report.org_id:NO ACTION",   // aiInsightReportRepository.deleteByOrganizationId
            "org_invitation.org_id:NO ACTION",      // orgInvitationRepository.deleteByOrganizationId
            "org_member.org_id:NO ACTION",          // orgMemberRepository.deleteAll
            "timeline.org_id:NO ACTION",            // timelineRepository.deleteByOrganizationId

            // DB FK 의 ON DELETE CASCADE 가 정리
            "notification.org_id:CASCADE",
            "org_notification_setting.org_id:CASCADE",

            // "조직 삭제 전 광고 계정 연동이 해제되어 있다"는 전제에 의존
            // 연동 해제(PlatformDataCleanupExecutor.deleteAccountAndRelations)가 미리 지워준다
            "ad_campaign.org_id:NO ACTION",
            "platform_account.org_id:NO ACTION",
            "project.org_id:NO ACTION"
    );

    // users 를 참조하는 FK 전체
    private static final Set<String> EXPECTED_USER_FK = Set.of(
            // 회원 Hard Delete 가 명시적으로 삭제
            "auth_provider_account.user_id:NO ACTION",  // authProviderAccountRepository.deleteByUserId
            "org_member.user_id:NO ACTION",             // orgMemberRepository.deleteByUserId

            // DB FK 의 ON DELETE CASCADE 가 정리
            "user_notification.user_id:CASCADE",

            // 회원 삭제 전 스케줄러가 광고 계정 연동을 해제하며 정리
            // 정리 실패 시 UserDeleteScheduler 가 해당 회원 삭제를 보류한다
            "platform_connection.user_id:NO ACTION"
    );

    // org 를 가리키지만 FK 가 아닌 컬럼
    private static final Set<String> EXPECTED_ORG_PLAIN_REFERENCES = Set.of(
            "click_anomaly_event.org_id",   // 조직 Hard Delete 시 deleteByOrgId 로 직접 삭제
            "users.current_org_id"          // 조직 삭제 시 해당 멤버들의 값을 null 로 초기화
    );

    // user 를 가리키지만 FK 가 아닌 컬럼
    private static final Set<String> EXPECTED_USER_PLAIN_REFERENCES = Set.of(
            "organization.owner_user_id",   // 회원 Hard Delete 시 소유 조직을 함께 삭제
            "project.created_by",           // 감사 필드 - 회원 삭제 후에도 남는다 (의도된 잔존)
            "timeline.created_by"           // 감사 필드 - 위와 동일
    );


    @Test
    @DisplayName("조직을 참조하는 FK 목록이 변하지 않았는가 - 변했다면 조직 삭제 로직 검토 필요")
    void organizationReferencesUnchanged() {
        assertThat(foreignKeysReferencing("organization"))
                .as("organization 참조 FK 가 변경됨. 조직 Hard Delete 경로를 검토하고 기준선을 갱신할 것")
                .isEqualTo(EXPECTED_ORG_FK);
    }

    @Test
    @DisplayName("회원을 참조하는 FK 목록이 변하지 않았는가 - 변했다면 회원 삭제 로직 검토 필요")
    void userReferencesUnchanged() {
        assertThat(foreignKeysReferencing("users"))
                .as("users 참조 FK 가 변경됨. 회원 Hard Delete 경로를 검토하고 기준선을 갱신할 것")
                .isEqualTo(EXPECTED_USER_FK);
    }

    @Test
    @DisplayName("FK 없이 조직을 가리키는 컬럼 목록이 변하지 않았는가 - DB 가 정리해주지 않는 대상")
    void orgPlainReferencesUnchanged() {
        assertThat(plainReferenceColumns("c.COLUMN_NAME LIKE '%org_id%'"))
                .as("FK 없이 조직을 가리키는 컬럼이 추가됨. 조직 삭제 시 명시적 처리 코드가 반드시 필요하다")
                .isEqualTo(EXPECTED_ORG_PLAIN_REFERENCES);
    }

    @Test
    @DisplayName("FK 없이 회원을 가리키는 컬럼 목록이 변하지 않았는가 - DB 가 정리해주지 않는 대상")
    void userPlainReferencesUnchanged() {
        assertThat(plainReferenceColumns("c.COLUMN_NAME LIKE '%user_id%' OR c.COLUMN_NAME = 'created_by'"))
                .as("FK 없이 회원을 가리키는 컬럼이 추가됨. 회원 삭제 시 처리 방침을 결정할 것")
                .isEqualTo(EXPECTED_USER_PLAIN_REFERENCES);
    }

    @SuppressWarnings("unchecked")
    private Set<String> foreignKeysReferencing(String referencedTable) {
        List<Object[]> rows = em.createNativeQuery(
                        "SELECT kcu.TABLE_NAME, kcu.COLUMN_NAME, rc.DELETE_RULE " +
                                "FROM information_schema.REFERENTIAL_CONSTRAINTS rc " +
                                "JOIN information_schema.KEY_COLUMN_USAGE kcu " +
                                "  ON rc.CONSTRAINT_NAME = kcu.CONSTRAINT_NAME " +
                                " AND rc.CONSTRAINT_SCHEMA = kcu.CONSTRAINT_SCHEMA " +
                                "WHERE rc.CONSTRAINT_SCHEMA = DATABASE() " +
                                "  AND rc.REFERENCED_TABLE_NAME = :referencedTable")
                .setParameter("referencedTable", referencedTable)
                .getResultList();

        return rows.stream()
                .map(row -> row[0] + "." + row[1] + ":" + row[2])
                .collect(Collectors.toSet());
    }

    // FK 도 PK 도 아닌 참조성 컬럼을 찾기
    @SuppressWarnings("unchecked")
    private Set<String> plainReferenceColumns(String namePredicate) {
        List<Object[]> rows = em.createNativeQuery(
                        "SELECT c.TABLE_NAME, c.COLUMN_NAME FROM information_schema.COLUMNS c " +
                                "WHERE c.TABLE_SCHEMA = DATABASE() " +
                                "  AND (" + namePredicate + ") " +
                                "  AND NOT EXISTS (" +   // FK 컬럼 제외 - DB 가 알아서 정리하거나 막아준다
                                "    SELECT 1 FROM information_schema.KEY_COLUMN_USAGE k " +
                                "    WHERE k.TABLE_SCHEMA = c.TABLE_SCHEMA AND k.TABLE_NAME = c.TABLE_NAME " +
                                "      AND k.COLUMN_NAME = c.COLUMN_NAME AND k.REFERENCED_TABLE_NAME IS NOT NULL) " +
                                "  AND NOT EXISTS (" +   // PK 컬럼 제외 - organization.org_id, users.user_id 자기 자신
                                "    SELECT 1 FROM information_schema.KEY_COLUMN_USAGE k " +
                                "    WHERE k.TABLE_SCHEMA = c.TABLE_SCHEMA AND k.TABLE_NAME = c.TABLE_NAME " +
                                "      AND k.COLUMN_NAME = c.COLUMN_NAME AND k.CONSTRAINT_NAME = 'PRIMARY')")
                .getResultList();

        return rows.stream()
                .map(row -> row[0] + "." + row[1])
                .collect(Collectors.toSet());
    }
}
