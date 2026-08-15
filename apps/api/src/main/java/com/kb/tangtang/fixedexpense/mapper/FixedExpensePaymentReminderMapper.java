package com.kb.tangtang.fixedexpense.mapper;

import com.kb.tangtang.fixedexpense.domain.FixedExpenseCandidate;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;

/** 고정지출 결제 예정 알림 대상과 결제 주기별 발송 이력을 조회·등록한다. */
@Mapper
public interface FixedExpensePaymentReminderMapper {

    List<FixedExpenseCandidate> findPaymentReminderCandidates(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    /** 유니크 키 충돌이면 0을 반환한다. 같은 결제 주기에는 이벤트를 다시 발행하지 않는다. */
    int registerPaymentReminder(@Param("candidateId") long candidateId,
                                @Param("expectedPaymentDate") LocalDate expectedPaymentDate,
                                @Param("notificationType") String notificationType);

    /** 개발 검증을 위해 해당 사용자의 결제 예정 알림 발송 이력만 초기화한다. */
    int deletePaymentRemindersByUser(@Param("userId") long userId);
}
