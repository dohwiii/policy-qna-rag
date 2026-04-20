package com.company.policyqna.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.web.servlet.view.InternalResourceViewResolver;

@Configuration
public class WebConfig {

    /**
     * JSP view resolver를 LOWEST_PRECEDENCE로 설정.
     * 이렇게 해야 springdoc/REST 컨트롤러 요청을 JSP resolver가 가로채지 않음.
     */
    @Bean
    public InternalResourceViewResolver jspViewResolver() {
        var resolver = new InternalResourceViewResolver();
        resolver.setPrefix("/WEB-INF/views/");
        resolver.setSuffix(".jsp");
        resolver.setOrder(Ordered.LOWEST_PRECEDENCE);
        return resolver;
    }
}
