package com.whereyouad.WhereYouAd.domains.user.domain.service;

import com.whereyouad.WhereYouAd.domains.user.application.dto.response.EmailSentResponse;
import com.whereyouad.WhereYouAd.domains.user.application.dto.response.PasswordResetResponse;
import com.whereyouad.WhereYouAd.domains.user.application.mapper.UserConverter;
import com.whereyouad.WhereYouAd.domains.user.domain.constant.Provider;
import com.whereyouad.WhereYouAd.domains.user.exception.handler.UserHandler;
import com.whereyouad.WhereYouAd.domains.user.exception.code.UserErrorCode;
import com.whereyouad.WhereYouAd.domains.user.persistence.entity.AuthProviderAccount;
import com.whereyouad.WhereYouAd.domains.user.persistence.entity.User;
import com.whereyouad.WhereYouAd.domains.user.persistence.repository.AuthProviderAccountRepository;
import com.whereyouad.WhereYouAd.domains.user.persistence.repository.UserRepository;
import com.whereyouad.WhereYouAd.global.utils.RedisUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Transactional
@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender emailSender;
    private final RedisUtil redisUtil;
    private final UserRepository userRepository;
    private final AuthProviderAccountRepository authProviderAccountRepository;

    // application.yml 적용 필요
    @Value("${spring.mail.username}")
    private String senderEmail;

    @Value("${spring.application.base-url}")
    private String baseUrl;

    // 인증코드 이메일 발송 로직 (최초 회원가입 시)
    public EmailSentResponse sendEmail(String toEmail) {
        if (userRepository.existsByEmail(toEmail)) { // 이미 해당 이메일로 생성한 계정이 있으면
            //해당 사용자 정보 조회
            User user = userRepository.findUserByEmail(toEmail)
                    .orElseThrow(() -> new UserHandler(UserErrorCode.USER_NOT_FOUND));

            //해당 사용자에 연관된 AuthProviderAccount 조회
            Optional<AuthProviderAccount> authProviderAccount = authProviderAccountRepository.findByUser(user);

            //만약 AuthProviderAccount 가 없으면
            if (authProviderAccount.isEmpty()) {
                //단순 이메일 회원가입에서 이메일 값이 중복인 것이므로 예외(기존 예외처리 로직)
                throw new UserHandler(UserErrorCode.USER_EMAIL_DUPLICATE);
            } else { //만약 AuthProviderAccount 가 있으면
                //해당 소셜 로그인 플랫폼 타입을 추출해서 반환
                Provider provider = authProviderAccount.get().getProvider();

                return UserConverter.toEmailSentResponseFail(toEmail, provider);
            }
        }

        //해당 이메일로 이미 생성된 계정 없으면 이메일 전송 진행
        String type = "회원가입";
        //템플릿 호출
        return (EmailSentResponse) emailSendTemplate(toEmail, type);
    }

    // 비밀번호 재설정을 위한 인증코드 이메일 발송 로직 (이미 회원가입 된 상태에서 비밀번호 재설정)
    public PasswordResetResponse sendEmailForPwd(String toEmail) {
        if (userRepository.existsByEmail(toEmail)) { // 이미 회원가입 되어있는 것이 확인되면
            String type = "비밀번호 재설정";
            return (PasswordResetResponse) emailSendTemplate(toEmail, type); // 정상적으로 이메일 발송
        } else { // 만약 회원가입 되어있지 않다면
            throw new UserHandler(UserErrorCode.USER_NOT_FOUND); // 예외발생
        }
    }

    // 조직 멤버 초대 이메일 발송
    public void sendEmailForOrgInvitation(String token, String toEmail, String orgName) {
        try {
            // 이메일 전송
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(toEmail);

            message.setSubject("[Where You Ad] 조직 " + orgName + "에 초대 되었습니다.");
            message.setText(baseUrl + "/api/org/invitations/" + token);
            message.setFrom(senderEmail);

            emailSender.send(message);
        } catch (MailException e) { // 예외 발생
            throw new UserHandler(UserErrorCode.USER_EMAIL_NOT_VALID); // 통합 응답 처리 예외로 반환
        }
    }

    //기존 이메일 발송 로직 템플릿 화
    //"회원가입 시 인증 이메일 발송" 과 "비밀번호 재설정 시 인증 이메일 발송" 에 대한 Response 분리 위해 반환값 Object로 변경 (fix/#39)
    private Object emailSendTemplate(String toEmail, String type) {

        // 인증코드 재전송 로직 -> 이미 Redis 에 해당 이메일 인증코드가 있을시 삭제
        String redisKey = "CODE:" + toEmail;
        if (redisUtil.getData(redisKey) != null) {
            redisUtil.deleteData(redisKey);
        }

        String authCode = createCode(); // 인증코드 생성

        if (isTestEmail(toEmail)) {
            // 테스트용 가짜 이메일은 서버 로그로만 인증코드를 보여주기
            // 실제 이메일 발송 X
            // 존재하지 않는 이메일 주소로 계속 이메일을 보내면 인증코드 전송용 이메일 계정이 스팸처리 될 가능성 존재하여 로그로 남기기
            log.warn("{} 이메일 -> 테스트 or 개발용 가짜 이메일 입니다.", toEmail);
            log.warn("[TEST 모드] 실제 발송을 건너뜁니다.");
            log.warn("[TEST 모드] 수신자: {}", toEmail);
            log.warn("[TEST 모드] 인증코드: {}", authCode);

        } else { // 실제 존재하는 이메일이라면

            try {
                // 실제 인증 코드가 담긴 이메일 전송
                SimpleMailMessage message = new SimpleMailMessage();
                message.setTo(toEmail);
                // 어떤 유형의 인증(최초 회원가입 or 비밀번호 재설정) 인지 구분하여 인증코드 발송
                message.setSubject("whereyouad " + type + " 인증번호");
                message.setText("[Where You Ad] " + type + "\n 인증 번호는 [" + authCode + "] 입니다.");
                message.setFrom(senderEmail);

                emailSender.send(message); // 만약 실제 존재하는 이메일인데 사용자가 오타를 냈다면
            } catch (MailException e) { // 예외 발생
                throw new UserHandler(UserErrorCode.USER_EMAIL_NOT_VALID); // 통합 응답 처리 예외로 반환
            }
        }

        // Redis에 저장 (Key: "CODE:이메일", Value: "123456", 유효시간: 180초(3분))
        // 테스트 계정도 인증은 해야하니 Redis 에 코드가 저장 되어야 함.
        // 테스트 계정의 인증은 서버 로그를 통해 인증코드를 얻어 입력.
        redisUtil.setDataExpire("CODE:" + toEmail, authCode, 60 * 3L);

        if (type.equals("회원가입")) { //해당 템플릿 메서드를 "회원가입을 위한 이메일 인증" 에서 호출한 경우,
            //해당 DTO 형식에 맞춰 반환
            return UserConverter.toEmailSentResponseSuccess(toEmail);
        } else { //"비밀번호 재설정" 에서 호출한 경우,
            //해당 DTO 형식에 맞춰 반환
            return UserConverter.toPasswordResetResponse(toEmail);
        }
    }

    // 인증코드 검증 메서드
    public void verifyEmailCode(String email, String inputCode) {
        // 해당 이메일 값으로 Redis 에서 조회
        String key = "CODE:" + email;
        String savedCode = redisUtil.getData(key);

        // 만약 인증코드가 없거나 잘못 입력했다면,
        if (savedCode == null || !savedCode.equals(inputCode)) {
            throw new UserHandler(UserErrorCode.USER_EMAIL_AUTH_INVALID); // 예외 발생(BAD_REQUEST)
        }

        // 정상적으로 인증코드를 입력했다면,
        // 기존 Redis 에 저장된 데이터를 지우고,
        redisUtil.deleteData(key);
        // "해당 이메일이 정상적으로 인증되었다" 는 값을 다시 Redis 에 저장 -> 이후 회원가입(signup) 내부 로직에서 활용
        redisUtil.setDataExpire("VERIFIED:" + email, "TRUE", 60 * 60L); // 1시간 뒤 Expire
    }

    // 테스트용 이메일은, "test" 로 시작하거나, "example.com" 으로 끝나야 한다.
    private boolean isTestEmail(String email) {
        return email.startsWith("test") || email.endsWith("example.com");
    }

    // 무작위 인증코드 값 생성
    private String createCode() {
        return String.valueOf((int) (Math.random() * (900000)) + 100000);
    }
}
