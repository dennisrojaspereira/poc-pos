package com.poc.merchant.interfaces.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.poc.merchant.application.service.CreateMerchantUseCase;
import com.poc.merchant.application.service.GetMerchantUseCase;
import com.poc.merchant.application.service.ListMerchantsUseCase;
import com.poc.merchant.domain.model.Merchant;
import com.poc.merchant.domain.model.MerchantStatus;
import com.poc.merchant.infrastructure.security.OpaAuthorizationClient;
import com.poc.merchant.infrastructure.security.OpaAuthorizationFilter;
import com.poc.merchant.infrastructure.security.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = MerchantController.class)
@Import({SecurityConfig.class, RestExceptionHandler.class, OpaAuthorizationFilter.class})
class MerchantControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CreateMerchantUseCase createMerchantUseCase;

    @MockBean
    private GetMerchantUseCase getMerchantUseCase;

    @MockBean
    private ListMerchantsUseCase listMerchantsUseCase;

    @MockBean
    private OpaAuthorizationClient opaAuthorizationClient;

    @Test
    void shouldRejectUnauthenticatedMerchantRequest() throws Exception {
        mockMvc.perform(get("/api/merchants"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "auditor", roles = "AUDITOR")
    void shouldAllowAuditorToListMerchantsWhenOpaAllows() throws Exception {
        when(opaAuthorizationClient.isAllowed(eq("auditor"), eq(List.of("auditor")), eq("GET"), eq("/api/merchants")))
                .thenReturn(true);
        when(listMerchantsUseCase.execute()).thenReturn(List.of(
                new Merchant("m-1", "Store 1", "123", MerchantStatus.ACTIVE)
        ));

        mockMvc.perform(get("/api/merchants"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "operator", roles = "OPERATOR")
    void shouldBlockCreateWhenOpaDenies() throws Exception {
        when(opaAuthorizationClient.isAllowed(eq("operator"), eq(List.of("operator")), eq("POST"), eq("/api/merchants")))
                .thenReturn(false);

        mockMvc.perform(post("/api/merchants")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new MerchantPayload("m-1", "Store 1", "123"))))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void shouldAllowCreateWhenAdminAndOpaAllows() throws Exception {
        when(opaAuthorizationClient.isAllowed(eq("admin"), eq(List.of("admin")), eq("POST"), eq("/api/merchants")))
                .thenReturn(true);
        when(createMerchantUseCase.execute(any())).thenReturn(
                new Merchant("m-1", "Store 1", "123", MerchantStatus.ACTIVE)
        );

        mockMvc.perform(post("/api/merchants")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new MerchantPayload("m-1", "Store 1", "123"))))
                .andExpect(status().isCreated());
    }

    private record MerchantPayload(String merchantId, String legalName, String documentNumber) {
    }
}
