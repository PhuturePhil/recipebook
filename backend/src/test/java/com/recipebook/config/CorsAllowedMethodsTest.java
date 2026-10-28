package com.recipebook.config;

import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.http.HttpMethod;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.util.ClassUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.cors.CorsConfiguration;

import java.lang.reflect.Method;
import java.util.EnumSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/** Jede im Code gemappte HTTP-Methode muss im CORS-Filter erlaubt sein, sonst lehnt er Browser-Anfragen mit Origin als 403 ab. */
class CorsAllowedMethodsTest {

    private static Set<RequestMethod> mappedMethods() throws Exception {
        ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AnnotationTypeFilter(RestController.class));
        Set<RequestMethod> methods = EnumSet.noneOf(RequestMethod.class);
        for (var candidate : scanner.findCandidateComponents("com.recipebook")) {
            Class<?> controller = ClassUtils.forName(candidate.getBeanClassName(), CorsAllowedMethodsTest.class.getClassLoader());
            RequestMapping onClass = AnnotatedElementUtils.findMergedAnnotation(controller, RequestMapping.class);
            if (onClass != null) methods.addAll(Set.of(onClass.method()));
            for (Method m : controller.getDeclaredMethods()) {
                RequestMapping mapping = AnnotatedElementUtils.findMergedAnnotation(m, RequestMapping.class);
                if (mapping != null) methods.addAll(Set.of(mapping.method()));
            }
        }
        return methods;
    }

    private static CorsConfiguration corsConfiguration() {
        SecurityConfig config = new SecurityConfig(null, null, null, null);
        ReflectionTestUtils.setField(config, "allowedOrigin", "https://pastoors.cloud");
        return config.corsConfigurationSource().getCorsConfiguration(new MockHttpServletRequest("GET", "/api/meal-plan/1"));
    }

    @Test
    void everyMappedHttpMethodIsAllowedByCors() throws Exception {
        Set<RequestMethod> mapped = mappedMethods();
        assertTrue(mapped.containsAll(EnumSet.of(RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT,
                RequestMethod.PATCH, RequestMethod.DELETE)), "Scan hat nicht alle Controller gefunden: " + mapped);

        CorsConfiguration cors = corsConfiguration();
        for (RequestMethod method : mapped) {
            assertNotNull(cors.checkHttpMethod(HttpMethod.valueOf(method.name())),
                    method + " wird im Code verwendet, fehlt aber in den erlaubten CORS-Methoden");
        }
    }
}
