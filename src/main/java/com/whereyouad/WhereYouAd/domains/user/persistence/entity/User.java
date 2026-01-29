package com.whereyouad.WhereYouAd.domains.user.persistence.entity;

import com.whereyouad.WhereYouAd.domains.user.domain.constant.UserStatus;
import com.whereyouad.WhereYouAd.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.ColumnDefault;

@Entity
@Table(name = "users")
@Getter
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
public class User extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long id;

    @Column(nullable = false, name = "email", length = 320)
    private String email;

    @Column(name = "password", length = 255)
    private String password;

    @Column(nullable = false, name = "name", length = 50)
    private String name;

    @Column(name = "profile_image_url", length = 1024)
    private String profileImageUrl;

    @Column(nullable = false, name = "phone_number", length = 32)
    private String phoneNumber;

    @Column(nullable = false, name = "is_email_verified")
    @ColumnDefault("false")  //기본값 false
    private boolean isEmailVerified;  //회원 가입시 이메일 인증 여부

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, name = "status")
    @ColumnDefault("'ACTIVE'")  //기본값 ACTIVE
    private UserStatus status;  //ACTIVE, SUSPENDED, DELETED

    public void resetPassword(String newPassword) {
        this.password = newPassword;
    }
}
