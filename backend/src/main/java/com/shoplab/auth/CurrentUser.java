package com.shoplab.auth;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Put this on a controller parameter of type {@link User} to say
 * "this endpoint requires login; give me the logged-in user".
 * Without a valid token the request is rejected with 401 before the method runs.
 *
 * <pre>
 * &#64;GetMapping("/api/cart")
 * public CartDto get(&#64;CurrentUser User user) { ... }
 * </pre>
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface CurrentUser {
}
