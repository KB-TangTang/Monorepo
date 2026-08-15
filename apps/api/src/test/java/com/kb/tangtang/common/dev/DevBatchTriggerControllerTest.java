package com.kb.tangtang.common.dev;

import com.kb.tangtang.challenge.service.ChallengeGroupStatusBatchService;
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
    private final FixedExpensePaymentReminderBatchService paymentReminderBatchService =
            mock(FixedExpensePaymentReminderBatchService.class);
    private final FixedExpensePaymentReminderDevService paymentReminderDevService =
            mock(FixedExpensePaymentReminderDevService.class);

    private MockMvc mockMvc() {
        DevBatchTriggerController controller = new DevBatchTriggerController(
                new DevEnvironmentGuard("local"),
                groupBatchService,
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
