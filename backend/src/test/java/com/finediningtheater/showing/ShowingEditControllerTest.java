package com.finediningtheater.showing;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.finediningtheater.account.AdminRole;
import com.finediningtheater.global.audit.AuditLogger;
import com.finediningtheater.global.security.AdminPrincipal;
import com.finediningtheater.global.security.JwtProvider;
import com.finediningtheater.global.security.SudoMode;
import com.finediningtheater.global.support.SiteLocale;
import com.finediningtheater.production.Production;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

// 실제 hasRole('EDITOR') 인가 검증은 EditControllerSecurityTest + 라이브 스모크 테스트에서 한다.
@WebMvcTest(ShowingEditController.class)
@AutoConfigureMockMvc(addFilters = false)
class ShowingEditControllerTest {

    @TestConfiguration
    static class RealSudoModeConfig {
        @Bean
        SudoMode sudoMode() {
            return new SudoMode();
        }
    }

    @Autowired private MockMvc mockMvc;
    @Autowired private SudoMode sudoMode;

    @MockitoBean private ShowingService showingService;
    @MockitoBean private AuditLogger auditLogger;
    @MockitoBean private JwtProvider jwtProvider;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    private void loginAs(long id) {
        AdminPrincipal principal = new AdminPrincipal(id, "admin", AdminRole.SUPER_ADMIN);
        Authentication auth =
                new UsernamePasswordAuthenticationToken(
                        principal, null, List.of(new SimpleGrantedAuthority("ROLE_SUPER_ADMIN")));
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    private Showing sampleShowing() {
        return new Showing(new Production("showcase"), Instant.parse("2026-09-21T10:00:00Z"), 120, "장소", null, SiteLocale.KO, false);
    }

    @Test
    void 관리자_목록을_반환한다() throws Exception {
        loginAs(1L);
        when(showingService.listForAdmin()).thenReturn(List.of(sampleShowing()));

        mockMvc.perform(get("/api/showings/manage"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].venueName").value("장소"));
    }

    @Test
    void 회차_생성에_성공하면_감사로그를_남긴다() throws Exception {
        loginAs(1L);
        when(showingService.create(eq(1L), any(), eq(120), eq("장소"), eq(null), eq(SiteLocale.KO), eq(false)))
                .thenReturn(sampleShowing());

        mockMvc.perform(
                        post("/api/showings")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        "{\"productionId\":1,\"startsAt\":\"2026-09-21T10:00:00Z\",\"durationMinutes\":120,"
                                                + "\"venueName\":\"장소\",\"spokenLanguage\":\"KO\",\"interpretationAvailable\":false}"))
                .andExpect(status().isOk());

        verify(auditLogger).record(eq(1L), eq("SHOWING_CREATE"), eq("Showing"), any(), any(), any(), any());
    }

    @Test
    void 판매상태_변경은_PIN_없이_성공한다() throws Exception {
        loginAs(1L);
        Showing after = sampleShowing();
        after.changeSalesStatus(SalesStatus.SOLD_OUT);
        when(showingService.getForAdmin(1L)).thenReturn(sampleShowing());
        when(showingService.changeSalesStatus(1L, SalesStatus.SOLD_OUT)).thenReturn(after);

        mockMvc.perform(
                        post("/api/showings/1/sales-status")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"salesStatus\":\"SOLD_OUT\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.salesStatus").value("SOLD_OUT"));

        verify(auditLogger)
                .record(eq(1L), eq("SHOWING_SALES_STATUS_CHANGE"), eq("Showing"), eq(1L), any(), any(), any());
    }

    @Test
    void 예약URL_변경은_sudo가_없으면_거부한다() throws Exception {
        loginAs(1L);

        mockMvc.perform(
                        post("/api/showings/1/booking-url")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"bookingUrl\":\"https://booking.naver.com/x\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("PIN_REQUIRED"));
    }

    @Test
    @DirtiesContext
    void 예약URL_변경은_sudo가_열려있으면_성공하고_감사로그를_남긴다() throws Exception {
        loginAs(1L);
        sudoMode.activate(1L);
        Showing after = sampleShowing();
        when(showingService.getForAdmin(1L)).thenReturn(sampleShowing());
        when(showingService.changeBookingUrl(1L, "https://booking.naver.com/x")).thenReturn(after);

        mockMvc.perform(
                        post("/api/showings/1/booking-url")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"bookingUrl\":\"https://booking.naver.com/x\"}"))
                .andExpect(status().isOk());

        verify(auditLogger)
                .record(eq(1L), eq("SHOWING_BOOKING_URL_CHANGE"), eq("Showing"), eq(1L), any(), any(), any());
    }

    @Test
    void 발행은_sudo가_없으면_거부한다() throws Exception {
        loginAs(1L);

        mockMvc.perform(post("/api/showings/1/publish"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("PIN_REQUIRED"));
    }

    @Test
    @DirtiesContext
    void 발행은_sudo가_열려있으면_성공한다() throws Exception {
        loginAs(1L);
        sudoMode.activate(1L);
        Showing before = sampleShowing();
        Showing after = sampleShowing();
        after.publish(1L);
        when(showingService.getForAdmin(1L)).thenReturn(before);
        when(showingService.publish(1L, 1L)).thenReturn(after);

        mockMvc.perform(post("/api/showings/1/publish"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PUBLISHED"));
    }

    @Test
    void 일정_수정에_성공한다() throws Exception {
        loginAs(1L);
        when(showingService.getForAdmin(1L)).thenReturn(sampleShowing());
        when(showingService.updateDetails(
                        eq(1L), any(), eq(90), eq("새 장소"), eq(null), eq(SiteLocale.EN), eq(true)))
                .thenReturn(sampleShowing());

        mockMvc.perform(
                        put("/api/showings/1")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        "{\"startsAt\":\"2026-09-22T10:00:00Z\",\"durationMinutes\":90,"
                                                + "\"venueName\":\"새 장소\",\"spokenLanguage\":\"EN\",\"interpretationAvailable\":true}"))
                .andExpect(status().isOk());

        verify(auditLogger).record(eq(1L), eq("SHOWING_UPDATE_DETAILS"), eq("Showing"), eq(1L), any(), any(), any());
    }
}
