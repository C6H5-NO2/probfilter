package probfilter.util;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;


/**
 * Indicates that this Scala function is (more) suitable for being used from Java than its equivalent.
 */
@Target({ElementType.METHOD})
public @interface JavaFriendly {
    String scalaEquivalent() default "";
}
