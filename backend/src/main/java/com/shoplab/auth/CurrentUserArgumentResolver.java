package com.shoplab.auth;

import org.springframework.core.MethodParameter;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

/**
 * Spring asks this class to fill every controller parameter marked {@link CurrentUser}.
 * It reads the Authorization header and looks the token up in the database.
 * (A simpler alternative to the full Spring Security framework, good enough for a training app.)
 */
@Component
public class CurrentUserArgumentResolver implements HandlerMethodArgumentResolver {

    private final AuthService authService;

    public CurrentUserArgumentResolver(AuthService authService) {
        this.authService = authService;
    }

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(CurrentUser.class)
                && parameter.getParameterType().equals(User.class);
    }

    @Override
    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                  NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
        return authService.authenticate(webRequest.getHeader(HttpHeaders.AUTHORIZATION));
    }
}
