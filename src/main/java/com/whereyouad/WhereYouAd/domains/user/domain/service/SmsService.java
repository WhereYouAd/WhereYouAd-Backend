package com.whereyouad.WhereYouAd.domains.user.domain.service;

import com.whereyouad.WhereYouAd.domains.user.exception.code.UserErrorCode;
import com.whereyouad.WhereYouAd.domains.user.exception.handler.UserHandler;
import com.whereyouad.WhereYouAd.domains.user.persistence.entity.User;
import com.whereyouad.WhereYouAd.domains.user.persistence.repository.UserRepository;
import net.nurigo.sdk.NurigoApp;
import net.nurigo.sdk.message.model.Message;
import com.whereyouad.WhereYouAd.domains.user.application.dto.response.SmsResponse;
import com.whereyouad.WhereYouAd.global.utils.RedisUtil;
import net.nurigo.sdk.message.request.SingleMessageSendingRequest;
import net.nurigo.sdk.message.service.DefaultMessageService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Random;

@Service
@Transactional
public class SmsService {
    private final RedisUtil redisUtil;
    private final UserRepository userRepository;
    private final DefaultMessageService defaultMessageService;
    private final String senderNumber;

    public SmsService(RedisUtil redisUtil, UserRepository userRepository,
            @Value("${coolSms.apiKey}") String smsApiKey,
            @Value("${coolSms.secretKey}") String smsSecretKey,
            @Value("${coolSms.senderNumber}") String senderNumber) {
        this.redisUtil = redisUtil;
        this.userRepository = userRepository;
        this.senderNumber = senderNumber;
        this.defaultMessageService = NurigoApp.INSTANCE.initialize(smsApiKey, smsSecretKey,
                "https://api.coolsms.co.kr");
    }

    // SMS 전송
    public SmsResponse.SmsSentResponse sendSms(String phoneNumber) {
        // 이메일 찾기 기능: 가입된 유저인지 확인
        if (!userRepository.existsByPhoneNumber(phoneNumber)) {
            throw new UserHandler(UserErrorCode.USER_NOT_FOUND_BY_PHONE);
        }

        Message message = new Message();
        String verificationCode = generateCode();

        message.setFrom(senderNumber); // 발신 번호
        message.setTo(phoneNumber);
        message.setText("[Where You Ad] 이메일 찾기\n 휴대폰 인증 번호는 [" + verificationCode + "] 입니다.");

        // redis 저장
        redisUtil.setDataExpire(phoneNumber, verificationCode, 180); // 유효 기간 3분

        try {
            this.defaultMessageService.sendOne(new SingleMessageSendingRequest(message));
        } catch (Exception e) {
            e.printStackTrace();
            throw new UserHandler(UserErrorCode.SMS_SEND_FAILED);
        }

        return new SmsResponse.SmsSentResponse("인증번호가 전송되었습니다.", phoneNumber, 180L);
    }

    // 랜덤 인증 번호 생성
    public String generateCode() {
        Random random = new Random();
        int code = 100000 + random.nextInt(900000); // 6자리 인증 번호
        return Integer.toString(code);
    }

    // 인증번호 확인
    public boolean verifyCode(String phoneNumber, String insertedNum) {
        String savedCode = redisUtil.getData(phoneNumber);

        if (savedCode != null && savedCode.equals(insertedNum)) {
            redisUtil.deleteData(phoneNumber);
            // 인증 완료
            return true;
        }
        return false;
    }

    // 인증번호 확인 후 유저 이메일 반환
    public String isPhoneVerified(String phoneNumber, String insertedNum) {
        if (verifyCode(phoneNumber, insertedNum)) {
            User user = userRepository.findUserByPhoneNumber(phoneNumber)
                    .orElseThrow(() -> new UserHandler(UserErrorCode.USER_NOT_FOUND));
            return user.getEmail();
        } else
            throw new UserHandler(UserErrorCode.USER_SMS_NOT_VERIFIED);
    }
}
