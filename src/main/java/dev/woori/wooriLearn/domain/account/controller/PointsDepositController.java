package dev.woori.wooriLearn.domain.account.controller;

import dev.woori.wooriLearn.config.exception.CommonException;
import dev.woori.wooriLearn.config.exception.ErrorCode;
import dev.woori.wooriLearn.config.response.ApiResponse;
import dev.woori.wooriLearn.config.response.BaseResponse;
import dev.woori.wooriLearn.config.response.SuccessCode;
import dev.woori.wooriLearn.domain.account.dto.PointsDepositRequestDto;
import dev.woori.wooriLearn.domain.account.service.PointsDepositService;
import dev.woori.wooriLearn.domain.user.entity.Users;
import dev.woori.wooriLearn.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/points/deposit")
@RequiredArgsConstructor
public class PointsDepositController {

    private final PointsDepositService pointsDepositService;
    private final UserRepository userRepository;

    @PostMapping("")
    public ResponseEntity<BaseResponse<?>> deposit(
            @AuthenticationPrincipal Object principal,
            @RequestBody PointsDepositRequestDto dto
    ) {
        Long userId = getUserIdFromPrincipal(principal);
        return ApiResponse.success(SuccessCode.CREATED, pointsDepositService.depositPoints(userId, dto));
    }

    private Long getUserIdFromPrincipal(Object principal) {
        String username = extractUsername(principal);
        Users user = userRepository.findByUserId(username)
                .orElseThrow(() -> new CommonException(ErrorCode.UNAUTHORIZED, "로그인이 필요합니다."));
        return user.getId();
    }

    private String extractUsername(Object principal) {
        if (principal instanceof org.springframework.security.core.userdetails.UserDetails ud) return ud.getUsername();
        if (principal instanceof java.security.Principal p) return p.getName();
        if (principal instanceof String s && !"anonymousUser".equals(s)) return s;
        throw new CommonException(ErrorCode.UNAUTHORIZED, "로그인이 필요합니다.");
    }
}
