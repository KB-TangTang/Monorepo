package com.kb.tangtang.common.dev;

import com.kb.tangtang.challenge.service.ChallengeGroupStatusBatchService;
import com.kb.tangtang.challenge.service.GroupChallengeEvaluationBatchService;
import com.kb.tangtang.challenge.service.GroupTrialDeadlineBatchService;
import com.kb.tangtang.fixedexpense.service.FixedExpensePaymentReminderBatchService;
import com.kb.tangtang.fixedexpense.service.FixedExpensePaymentReminderDevService;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class DevBatchTriggerControllerTest {

    private static final long USER_ID = 7L;

    private final ChallengeGroupStatusBatchService groupBatchService = mock(ChallengeGroupStatusBatchService.class);
    private final GroupChallengeEvaluationBatchService evaluationBatchService =
            mock(GroupChallengeEvaluationBatchService.class);
    private final GroupTrialDeadlineBatchService trialDeadlineBatchService =
            mock(GroupTrialDeadlineBatchService.class);
    private final FixedExpensePaymentReminderBatchService paymentReminderBatchService =
            mock(FixedExpensePaymentReminderBatchService.class);
    private final FixedExpensePaymentReminderDevService paymentReminderDevService =
            mock(FixedExpensePaymentReminderDevService.class);

    private MockMvc mockMvc() {
        DevBatchTriggerController controller = new DevBatchTriggerController(
                new DevEnvironmentGuard("local"),
                groupBatchService,
                evaluationBatchService,
                trialDeadlineBatchService,
                paymentReminderBatchService,
                paymentReminderDevService);
        return MockMvcBuilders.standaloneSetup(controller)
                .setCustomArgumentResolvers(loginUserResolver())
                .build();
    }

    @Test
    void runsPaymentReminderBatchOnlyThroughLocalDevEndpoint() throws Exception {
        when(paymentReminderBatchService.sendDuePaymentReminders()).thenReturn(2);

        mockMvc().perform(post("/api/dev/batches/fixed-expense-payment-reminders"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.batch").value("fixed-expense-payment-reminders"))
                .andExpect(jsonPath("$.data.affected").value(2));

        verify(paymentReminderBatchService).sendDuePaymentReminders();
    }

    @Test
    void runsGroupChallengeEvaluationBatchWithGivenBaseDate() throws Exception {
        when(evaluationBatchService.evaluateActiveGroups(java.time.LocalDate.of(2026, 8, 17)))
                .thenReturn(2);

        mockMvc().perform(post("/api/dev/batches/group-challenge-evaluation").param("date", "2026-08-17"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.batch").value("group-challenge-evaluation"))
                .andExpect(jsonPath("$.data.baseDate").value("2026-08-17"))
                .andExpect(jsonPath("$.data.affected").value(2));

        /* 기준일을 종료 다음 날로 넣어야 기간평가(PERIOD) 기소를 시연할 수 있다 */
        verify(evaluationBatchService).evaluateActiveGroups(java.time.LocalDate.of(2026, 8, 17));
    }

    /**
     * 변론 마감 배치는 기준일을 받지 않는다 — 마감 판정을 SQL 이 {@code NOW()} 로 직접 하기 때문이다.
     * {@code date} 를 넘겨도 배치에는 전달되지 않으며, 응답의 {@code baseDate} 는 표시용일 뿐이다.
     */
    @Test
    void runsTrialDeadlineBatchWithoutPassingBaseDate() throws Exception {
        when(trialDeadlineBatchService.closeExpiredDefenses()).thenReturn(3);

        mockMvc().perform(post("/api/dev/batches/group-trial-deadline"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.batch").value("group-trial-deadline"))
                .andExpect(jsonPath("$.data.affected").value(3));

        verify(trialDeadlineBatchService).closeExpiredDefenses();
    }

    @Test
    void resetsOnlyCurrentUsersPaymentReminderTestRecords() throws Exception {
        when(paymentReminderDevService.resetPaymentDueReminders(USER_ID))
                .thenReturn(new FixedExpensePaymentReminderDevService.ResetResult(3, 4));

        mockMvc().perform(delete("/api/dev/batches/fixed-expense-payment-reminders"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.deletedNotifications").value(3))
                .andExpect(jsonPath("$.data.deletedReminderHistory").value(4));

        verify(paymentReminderDevService).resetPaymentDueReminders(USER_ID);
    }

    private HandlerMethodArgumentResolver loginUserResolver() {
        return new HandlerMethodArgumentResolver() {
            @Override
            public boolean supportsParameter(MethodParameter parameter) {
                return parameter.hasParameterAnnotation(com.kb.tangtang.common.auth.LoginUser.class);
            }

            @Override
            public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                          NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
                return USER_ID;
            }
        };
    }
}
