package com.finediningtheater.program;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.finediningtheater.global.security.JwtProvider;
import com.finediningtheater.global.support.SiteLocale;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ProgramController.class)
@AutoConfigureMockMvc(addFilters = false)
class ProgramControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockitoBean private ProgramService programService;
    @MockitoBean private JwtProvider jwtProvider;

    @Test
    void 발행된_프로그램_목록을_반환한다() throws Exception {
        Program program = new Program();
        program.addTranslation(SiteLocale.KO, "여름 시식회", "참가는 구글폼으로");
        program.changeApplyUrl("https://forms.gle/abcd");
        program.changeLocationUrl("https://map.naver.com/p/somewhere");
        when(programService.listPublished()).thenReturn(List.of(program));

        mockMvc.perform(get("/api/programs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].title").value("여름 시식회"))
                .andExpect(jsonPath("$.data[0].applyUrl").value("https://forms.gle/abcd"))
                .andExpect(jsonPath("$.data[0].locationUrl").value("https://map.naver.com/p/somewhere"));
    }
}
