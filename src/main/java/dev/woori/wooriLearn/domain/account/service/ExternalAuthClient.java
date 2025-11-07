package dev.woori.wooriLearn.domain.account.service;

public interface ExternalAuthClient {
    String requestOtp(String name, String birthdate, String phoneNum);
}
