package com.whereyouad.WhereYouAd.domains.user.persistence.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "refresh_tokens")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RefreshToken {

    @Id
    private String keyId; //유저의 이메일(Key)

    private String value; //Refresh Token 값(Value)

    //토큰 갱신 메서드
    public RefreshToken updateValue(String token) {
        this.value = token;
        return this;
    }
}
