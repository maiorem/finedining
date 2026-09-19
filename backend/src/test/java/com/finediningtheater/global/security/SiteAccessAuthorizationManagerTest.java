package com.finediningtheater.global.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.finediningtheater.account.AdminRole;
import com.finediningtheater.site.SiteVisibilityService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.AuthorityUtils;

@ExtendWith(MockitoExtension.class)
class SiteAccessAuthorizationManagerTest {

    @Mock private SiteVisibilityService siteVisibilityService;

    private static final Authentication ANONYMOUS =
            new AnonymousAuthenticationToken("key", "anonymousUser", AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS"));
    private static final Authentication ADMIN =
            new UsernamePasswordAuthenticationToken(
                    new AdminPrincipal(1L, "admin", AdminRole.SUPER_ADMIN), null, List.of());
    private static final Authentication MEMBER =
            new UsernamePasswordAuthenticationToken(new MemberPrincipal(2L, "손님"), null, List.of());

    private boolean granted(boolean requireAuthentication, Authentication authentication) {
        return new SiteAccessAuthorizationManager(siteVisibilityService, requireAuthentication)
                .check(() -> authentication, null)
                .isGranted();
    }

    @Test
    void 공개_상태에서_공개_GET은_익명도_통과한다() {
        when(siteVisibilityService.isPublic()).thenReturn(true);

        assertThat(granted(false, ANONYMOUS)).isTrue();
    }

    @Test
    void 공개_상태에서_로그인_필수_경로는_익명을_막고_회원과_관리자는_통과시킨다() {
        when(siteVisibilityService.isPublic()).thenReturn(true);

        assertThat(granted(true, ANONYMOUS)).isFalse();
        assertThat(granted(true, MEMBER)).isTrue();
        assertThat(granted(true, ADMIN)).isTrue();
    }

    @Test
    void 비공개_상태에서는_공개_GET도_익명과_회원을_막는다() {
        when(siteVisibilityService.isPublic()).thenReturn(false);

        assertThat(granted(false, ANONYMOUS)).isFalse();
        assertThat(granted(false, MEMBER)).isFalse();
    }

    @Test
    void 비공개_상태에서도_관리자는_전부_통과한다() {
        when(siteVisibilityService.isPublic()).thenReturn(false);

        assertThat(granted(false, ADMIN)).isTrue();
        assertThat(granted(true, ADMIN)).isTrue();
    }
}
