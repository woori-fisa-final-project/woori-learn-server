package dev.woori.wooriLearn.domain.edubankapiAccount.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.woori.wooriLearn.domain.auth.entity.AuthUsers;
import dev.woori.wooriLearn.domain.auth.entity.Role;
import dev.woori.wooriLearn.domain.auth.port.AuthUserPort;
import dev.woori.wooriLearn.domain.edubankapi.eduaccount.dto.EdubankapiTransferRequestDto;
import dev.woori.wooriLearn.domain.edubankapi.entity.AccountType;
import dev.woori.wooriLearn.domain.edubankapi.entity.EducationalAccount;
import dev.woori.wooriLearn.domain.edubankapi.eduaccount.repository.EdubankapiAccountRepository;
import dev.woori.wooriLearn.domain.user.entity.Users;
import dev.woori.wooriLearn.domain.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@WithMockUser(username = "admin1")
public class EdubankapiAccountControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AuthUserPort authUserRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EdubankapiAccountRepository edubankapiAccountRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private Long fromAccountId;
    private Long toAccountId;

    @BeforeEach
    void setUp() {
        // Create AuthUser
        AuthUsers authUser = AuthUsers.builder()
                .userId("admin1")
                .password(passwordEncoder.encode("password"))
                .role(Role.ROLE_USER)
                .build();
        authUserRepository.save(authUser);

        // Create User
        Users user = Users.builder()
                .authUser(authUser)
                .userId("admin1")
                .nickname("관리자")
                .points(10000)
                .build();
        userRepository.save(user);

        // Create EducationalAccounts
        EducationalAccount fromAccount = EducationalAccount.builder()
                .accountNumber("1122334455")
                .accountPassword(passwordEncoder.encode("1111"))
                .accountName("테스트계좌1")
                .accountType(AccountType.CHECKING)
                .balance(1000000)
                .user(user)
                .build();
        fromAccount = edubankapiAccountRepository.save(fromAccount);
        fromAccountId = fromAccount.getId();

        EducationalAccount toAccount = EducationalAccount.builder()
                .accountNumber("5544332211")
                .accountPassword(passwordEncoder.encode("2222"))
                .accountName("테스트계좌2")
                .accountType(AccountType.SAVINGS)
                .balance(500000)
                .user(user)
                .build();
        toAccount = edubankapiAccountRepository.save(toAccount);
        toAccountId = toAccount.getId();
    }

    @Test
    @DisplayName("계좌 목록 조회 테스트")
    void testGetAccountList() throws Exception {
        mockMvc.perform(get("/education/accounts/list")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isArray())
                .andDo(print());
    }

    @Test
    @DisplayName("거래내역 조회 테스트")
    void testGetTransactionList() throws Exception {
        mockMvc.perform(get("/education/accounts/transactions")
                        .param("accountId", String.valueOf(fromAccountId))
                        .param("period", "1M")
                        .param("type", "ALL")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isArray())
                .andDo(print());
    }

    @Test
    @DisplayName("계좌이체 성공 테스트")
    void testTransferSuccess() throws Exception {

        EdubankapiTransferRequestDto request = EdubankapiTransferRequestDto.builder()
                .fromAccountNumber("1122334455")
                .toAccountNumber("5544332211")
                .amount(1000)
                .accountPassword("1111")
                .displayName("생활비")
                .build();

        mockMvc.perform(post("/education/accounts/transfer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.message").value("이체가 완료되었습니다."))
                .andDo(print());
    }

    @Test
    @DisplayName("계좌이체 실패 테스트 (비밀번호 불일치)")
    void testTransferWrongPassword() throws Exception {

        EdubankapiTransferRequestDto request = EdubankapiTransferRequestDto.builder()
                .fromAccountNumber("1122334455")
                .toAccountNumber("5544332211")
                .amount(1000)
                .accountPassword("9999")  // 틀린 비번
                .displayName("생활비")
                .build();

        mockMvc.perform(post("/education/accounts/transfer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("계좌 비밀번호가 일치하지 않습니다."))
                .andDo(print());
    }
}
