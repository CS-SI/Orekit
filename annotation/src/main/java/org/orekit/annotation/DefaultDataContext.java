package org.orekit.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Indicates that the annotated method, field, or constructor uses the default data
 * context. Can be used to emit warnings similar to {@code @Deprecated}.
 *
 * @author Evan Ward
 * @since 10.1
 */
@Documented
@Target({ElementType.CONSTRUCTOR, ElementType.FIELD, ElementType.METHOD,
        ElementType.TYPE})
@Retention(RetentionPolicy.CLASS)
public @interface DefaultDataContext {
}
