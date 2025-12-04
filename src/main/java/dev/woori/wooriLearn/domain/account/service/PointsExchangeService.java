package dev.woori.wooriLearn.domain.account.service;

import dev.woori.wooriLearn.config.exception.CommonException;
import dev.woori.wooriLearn.config.exception.ErrorCode;
import dev.woori.wooriLearn.domain.account.dto.ExchangeProcessContext;
import dev.woori.wooriLearn.domain.account.dto.external.response.BankTransferResDto;
import dev.woori.wooriLearn.domain.account.dto.request.PointsExchangeRequestDto;
import dev.woori.wooriLearn.domain.account.dto.response.PointsExchangeResponseDto;
import dev.woori.wooriLearn.domain.account.dto.response.PointsHistoryResponseDto;
import dev.woori.wooriLearn.domain.account.entity.*;
import dev.woori.wooriLearn.domain.account.repository.AccountRepository;
import dev.woori.wooriLearn.domain.account.repository.PointsHistoryRepository;
import dev.woori.wooriLearn.domain.user.entity.Users;
import dev.woori.wooriLearn.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.cache.CacheManager;

import java.time.Clock;
import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class PointsExchangeService {

    private final Clock clock;
    private final PointsHistoryRepository pointsHistoryRepository;
    private final UserRepository userRepository;
    private final AccountRepository accountRepository;
    private final CacheManager cacheManager;

    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 200;

    /**
     * ì²˜ë¦¬ ?œì„œ
     * 1) ?¬ìš©????? ê¸ˆ ì¡°íšŒ (for update)
     * 2) ?”ì²­ ê¸ˆì•¡/?”ì•¡ ê²€ì¦?
     * 3) ì¶œê¸ˆ ê³„ì¢Œ ?Œìœ ??ê²€ì¦?
     * 4) ì¶œê¸ˆ APPLY ?´ë ¥ ?€??
     * 5) ?‘ë‹µ DTO êµ¬ì„±
     */
    @Transactional
    public PointsExchangeResponseDto requestExchange(String username, PointsExchangeRequestDto dto) {

        // 1) ?¬ìš©????? ê¸ˆ ì¡°íšŒ
        Users user = userRepository.findByUserIdForUpdate(username)
                .orElseThrow(() -> new CommonException(
                        ErrorCode.ENTITY_NOT_FOUND,
                        "?¬ìš©?ë? ì°¾ì„ ???†ìŠµ?ˆë‹¤. userId=" + username
                ));

        // 2) ?”ì²­ ê¸ˆì•¡/?”ì•¡ ê²€ì¦?
        if (dto.exchangeAmount() <= 0) {
            throw new CommonException(ErrorCode.INVALID_REQUEST, "êµí™˜ ?”ì²­ ê¸ˆì•¡??0ë³´ë‹¤ ì»¤ì•¼ ?©ë‹ˆ??);
        }
        if (user.getPoints() < dto.exchangeAmount()) {
            throw new CommonException(ErrorCode.CONFLICT, "?¬ì¸?¸ê? ë¶€ì¡±í•˜??ì¶œê¸ˆ ?”ì²­??ì²˜ë¦¬?????†ìŠµ?ˆë‹¤.");
        }

        // 3) ì¶œê¸ˆ ê³„ì¢Œ ?Œìœ ??ê²€ì¦?
        Account account = getValidateAccount(dto.accountNum(), user.getId());

        // ? ì°¨ê°?
        user.subtractPoints(dto.exchangeAmount());

        // 4) ì¶œê¸ˆ APPLY ?´ë ¥ ?€??
        PointsHistory history = pointsHistoryRepository.save(
                PointsHistory.builder()
                        .user(user)
                        .amount(dto.exchangeAmount())
                        .type(PointsHistoryType.WITHDRAW)
                        .status(PointsStatus.APPLY)
                        .accountNumber(account.getAccountNumber())
                        .build()
        );

        // Evict user info cache after points subtraction
        {
            var cache = cacheManager.getCache("userInfo_v2");
            if (cache != null) cache.evict(username);
        }

        // 5) ?‘ë‹µ DTO êµ¬ì„±
        return PointsExchangeResponseDto.builder()
                .requestId(history.getId())
                .userId(user.getId())
                .exchangeAmount(history.getAmount())
                .currentBalance(user.getPoints())
                .status(history.getStatus())
                .requestDate(history.getCreatedAt())
                .message("ì¶œê¸ˆ ?”ì²­???•ìƒ?ìœ¼ë¡??‘ìˆ˜?˜ì—ˆ?µë‹ˆ??")
                .build();
    }

    /**
     * ?¸ë? api?€ ?µì‹  ì¤€ë¹?
     * lock ?¤ì • + ê²€ì¦?+ Process ?íƒœë¡??„í™˜
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public ExchangeProcessContext prepareTransfer(Long requestId) {

        PointsHistory history = pointsHistoryRepository.findAndLockById(requestId)
                .orElseThrow(() -> new CommonException(
                        ErrorCode.ENTITY_NOT_FOUND,
                        "ì¶œê¸ˆ ?”ì²­??ì°¾ì„ ???†ìŠµ?ˆë‹¤. requestId=" + requestId
                ));

        if (history.getStatus() != PointsStatus.APPLY) {
            throw new CommonException(ErrorCode.CONFLICT, "?´ë? ì²˜ë¦¬???”ì²­?…ë‹ˆ??");
        }

        // ?¬ìš©??? íš¨??ê²€??
        Users user = userRepository.findByIdForUpdate(history.getUser().getId())
                .orElseThrow(() -> new CommonException(ErrorCode.ENTITY_NOT_FOUND, "?¬ìš©?ë? ì°¾ì„ ???†ìŠµ?ˆë‹¤. Id=" + history.getUser().getId()));

        // ê³„ì¢Œë²ˆí˜¸ ? íš¨??ê²€??
        Account account = getValidateAccount(history.getAccountNumber(), user.getId());

        // ?íƒœë¥?Processing?¼ë¡œ ë³€ê²?
        history.markProcessing();

        return ExchangeProcessContext.builder()
                .requestId(requestId)
                .userId(user.getId())
                .accountNum(account.getAccountNumber())
                .amount(history.getAmount())
                .build();
    }

    // ?´ì²´ ?‘ë‹µ???”ì„ ê²½ìš°
    @Transactional
    public PointsExchangeResponseDto processResult(Long requestId, BankTransferResDto bankRes){
        PointsHistory history = pointsHistoryRepository.findAndLockById(requestId)
                .orElseThrow(() -> new CommonException(ErrorCode.ENTITY_NOT_FOUND,
                        "ì¶œê¸ˆ ?”ì²­??ì°¾ì„ ???†ìŠµ?ˆë‹¤. requestId=" + requestId));
        Users user = userRepository.findByUserIdForUpdate(history.getUser().getUserId())
                .orElseThrow(() -> new CommonException(ErrorCode.ENTITY_NOT_FOUND, "?¬ìš©?ë? ì°¾ì„ ???†ìŠµ?ˆë‹¤."));

        LocalDateTime now = LocalDateTime.now(clock);

        if (bankRes != null && bankRes.code() == 200) {
            history.markSuccess(now);
            // Evict cache to ensure fresh balance on next read
            {
                var cache = cacheManager.getCache("userInfo_v2");
                if (cache != null) cache.evict(user.getUserId());
            }
            return buildResponse(history, user, "?•ìƒ?ìœ¼ë¡?ì²˜ë¦¬?˜ì—ˆ?µë‹ˆ??");
        }  else { // ?ëŸ¬ ë©”ì‹œì§€ return
            user.addPoints(history.getAmount());
            history.markFailed(PointsFailReason.PROCESSING_ERROR, now);
            {
                var cache = cacheManager.getCache("userInfo_v2");
                if (cache != null) cache.evict(user.getUserId());
            }
            return buildResponse(history, user, "ì²˜ë¦¬ ì¤??¤ë¥˜ê°€ ë°œìƒ?ˆìŠµ?ˆë‹¤.");
        }
    }

    // ?µì‹  ?¤íŒ¨ ??
    @Transactional
    public PointsExchangeResponseDto processFailure(Long requestId){
        PointsHistory history = pointsHistoryRepository.findAndLockById(requestId)
                .orElseThrow(() -> new CommonException(ErrorCode.ENTITY_NOT_FOUND
                        , "ì¶œê¸ˆ ?”ì²­??ì°¾ì„ ???†ìŠµ?ˆë‹¤. requestId=" + requestId));
        Users user = userRepository.findByUserIdForUpdate(history.getUser().getUserId())
                .orElseThrow(() -> new CommonException(ErrorCode.ENTITY_NOT_FOUND, "?¬ìš©?ë? ì°¾ì„ ???†ìŠµ?ˆë‹¤."));

        LocalDateTime now = LocalDateTime.now(clock);
        user.addPoints(history.getAmount());
        history.markFailed(PointsFailReason.PROCESSING_ERROR, now);
        {
            var cache = cacheManager.getCache("userInfo_v2");
            if (cache != null) cache.evict(user.getUserId());
        }
        return buildResponse(history, user, "?€???œë²„?ì„œ ?´ì²´ ?¤íŒ¨ê°€ ë°œìƒ?ˆìŠµ?ˆë‹¤.");
    }

    public PointsExchangeResponseDto buildResponse(PointsHistory history, Users user, String message) {
        return PointsExchangeResponseDto.builder()
                .requestId(history.getId())
                .userId(user.getId())
                .exchangeAmount(history.getAmount())
                .currentBalance(user.getPoints())
                .status(history.getStatus())
                .message(message)
                .processedDate(history.getProcessedAt())
                .build();
    }

    /**
     * ê´€ë¦¬ì?? ?˜ì „ ? ì²­(APPLY) ?„ì²´ ì¡°íšŒ (?˜ì´ì§€?¤ì´??
     */
    @Transactional(readOnly = true)
    public Page<PointsHistoryResponseDto> getPendingWithdrawals(Integer page, Integer size) {

        int pageNumber = (page == null || page < 1) ? 1 : page;
        int pageSize = (size == null || size < 1) ? DEFAULT_PAGE_SIZE : Math.min(size, MAX_PAGE_SIZE);

        PageRequest pageRequest = PageRequest.of(
                pageNumber - 1,
                pageSize,
                Sort.by(Sort.Direction.DESC, "createdAt")
        );

        return pointsHistoryRepository.findByTypeAndStatus(
                        PointsHistoryType.WITHDRAW,
                        PointsStatus.APPLY,
                        pageRequest
                )
                .map(PointsHistoryResponseDto::new);
    }

    private Account getValidateAccount(String accountNumber, Long userId){
        // 1. ê³„ì¢Œ ì¡°íšŒ
        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new CommonException(
                        ErrorCode.ENTITY_NOT_FOUND,
                        "ê³„ì¢Œë¥?ì°¾ì„ ???†ìŠµ?ˆë‹¤. accountNum=" + accountNumber
                ));

        // 2. ?Œìœ ì£?ê²€ì¦?
        if (!account.getUser().getId().equals(userId)) {
            throw new CommonException(ErrorCode.FORBIDDEN, "?´ë‹¹ ê³„ì¢Œ???Œìœ ?ê? ?„ë‹™?ˆë‹¤.");
        }

        return account;
    }
}
