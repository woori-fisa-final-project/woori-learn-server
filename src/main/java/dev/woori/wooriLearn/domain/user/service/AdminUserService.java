package dev.woori.wooriLearn.domain.user.service;

import dev.woori.wooriLearn.config.response.PageResponse;
import dev.woori.wooriLearn.domain.user.dto.AdminUserListResDto;
import dev.woori.wooriLearn.domain.user.entity.Users;
import dev.woori.wooriLearn.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class AdminUserService {

    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public PageResponse<AdminUserListResDto> getAdminUserList(int page, int size) {
        Pageable pageable = PageRequest.of(page - 1, size, Sort.by("createdAt").descending());
        Page<Users> usersPage = userRepository.findAll(pageable);

        List<AdminUserListResDto> content = usersPage.stream()
                .map(user -> AdminUserListResDto.builder()
                        .id(user.getId())
                        .userId(user.getUserId())
                        .nickname(user.getNickname())
                        .points(user.getPoints())
                        .role(user.getAuthUser().getRole())
                        .createdAt(user.getCreatedAt())
                        .build())
                .toList();

        return PageResponse.of(usersPage, content);
    }
}
