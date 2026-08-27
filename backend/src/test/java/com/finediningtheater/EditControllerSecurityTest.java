package com.finediningtheater;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.RegexPatternTypeFilter;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * *EditController 클래스나 그 public 메서드에 @PreAuthorize가 빠지면 여기서 잡는다
 * (CLAUDE.md §11). 새 EditController를 추가했는데 이 테스트가 통과한다면 권한 검사를
 * 어딘가엔 넣었다는 뜻이다 — 어디에 넣었는지는 각 컨트롤러의 403 테스트가 검증한다.
 */
class EditControllerSecurityTest {

    @Test
    void 모든_EditController는_클래스나_메서드에_PreAuthorize가_있다() throws ClassNotFoundException {
        ClassPathScanningCandidateComponentProvider scanner =
                new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new RegexPatternTypeFilter(java.util.regex.Pattern.compile(".*EditController$")));

        Set<org.springframework.beans.factory.config.BeanDefinition> candidates =
                scanner.findCandidateComponents("com.finediningtheater");

        assertThat(candidates).as("스캔된 EditController가 하나도 없다 — 스캔 경로를 확인해라").isNotEmpty();

        for (var beanDefinition : candidates) {
            String className = ((AnnotatedBeanDefinition) beanDefinition).getMetadata().getClassName();
            Class<?> clazz = Class.forName(className);
            boolean classAnnotated = clazz.isAnnotationPresent(PreAuthorize.class);

            for (Method method : clazz.getDeclaredMethods()) {
                if (!Modifier.isPublic(method.getModifiers())) continue;
                boolean methodAnnotated = method.isAnnotationPresent(PreAuthorize.class);
                assertThat(classAnnotated || methodAnnotated)
                        .as("%s.%s()에 @PreAuthorize가 없다", clazz.getSimpleName(), method.getName())
                        .isTrue();
            }
        }
    }
}
