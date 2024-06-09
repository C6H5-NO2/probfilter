package probfilter.util;

import java.lang.annotation.*;


/**
 * Indicates that this Scala element is (more) suitable for being used from Java than its equivalent.
 */
@Documented
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.SOURCE)
public @interface JavaFriendly {
    String scalaDelegate() default "";
}
