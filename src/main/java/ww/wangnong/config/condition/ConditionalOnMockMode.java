package ww.wangnong.config.condition;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.context.annotation.Conditional;

/**
 * Purpose: 标记仅在 mock 模式装配的 Bean。
 * Responsibilities: 把默认 mock 模式条件封装为统一注解。
 * Flow: 组件标注该注解 -> Spring 调用 OnMockModeCondition.matches -> 条件满足时注册 Bean。
 * Dependencies: OnMockModeCondition。
 * Output: 条件成立时的 Bean 装配结果。
 */
@Target({ ElementType.TYPE, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Conditional(OnMockModeCondition.class)
public @interface ConditionalOnMockMode {
}
