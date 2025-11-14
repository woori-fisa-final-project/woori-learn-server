package dev.woori.wooriLearn.domain.user;

import dev.woori.wooriLearn.config.response.PageResponse;
import dev.woori.wooriLearn.domain.auth.entity.AuthUsers;
import dev.woori.wooriLearn.domain.auth.entity.Role;
import dev.woori.wooriLearn.domain.user.dto.AdminUserListResDto;
import dev.woori.wooriLearn.domain.user.entity.Users;
import dev.woori.wooriLearn.domain.user.repository.UserRepository;
import dev.woori.wooriLearn.domain.user.service.AdminUserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminUserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private AdminUserService adminUserService;

    @Test
    void testGetAdminUserList() {
        AuthUsers auth = AuthUsers.builder()
                .userId("user1")
                .role(Role.ROLE_USER)
                .build();

        Users user = Users.builder()
                .id(1L)
                .userId("user1")
                .nickname("nick1")
                .points(100)
                .authUser(auth)
                .build();

        Page<Users> usersPage = new PageImpl<>(List.of(user));

        when(userRepository.findAll(any(Pageable.class)))
                .thenReturn(usersPage);

        PageResponse<AdminUserListResDto> result =
                adminUserService.getAdminUserList(1, 10);

        assertEquals(1, result.size());
        assertEquals("user1", result.items().get(0).userId());
    }
}