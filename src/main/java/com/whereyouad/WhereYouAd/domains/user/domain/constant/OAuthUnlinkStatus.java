package com.whereyouad.WhereYouAd.domains.user.domain.constant;

public enum OAuthUnlinkStatus {
    LINKED,         // 소셜 계정과 정상적으로 연동된 상태
    UNLINKING,      // 소셜 제공자에 연동 해제를 요청 중인 상태
    UNLINKED,       // 소셜 제공자 연동 해제가 완료된 상태
    UNLINK_FAILED   // 연동 해제에 실패하여 재시도가 필요한 상태
}
