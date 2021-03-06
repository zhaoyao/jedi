package jedi.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * User: zhaoyao
 * Date: 11-12-30
 * Time: 下午10:03
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface Index {

    String[] on() default "";
    
    String range() default "";

	double rangeNullValue() default -1;
    
    
}
