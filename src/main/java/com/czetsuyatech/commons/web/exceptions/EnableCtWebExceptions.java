package com.czetsuyatech.commons.web.exceptions;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.context.annotation.Import;

@Target({java.lang.annotation.ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Import({WebExceptionHandlerConfig.class})
public @interface EnableCtWebExceptions {

}
