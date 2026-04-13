package ww.wangnong.config.condition;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.context.annotation.Conditional;

/**
 * Purpose: 标记仅在硬件路由可用时装配的 Bean。
 * Responsibilities: 把 hardware/full 模式共享的装配条件封装为注解。
 * Flow: 组件标注该注解 -> Spring 调用 OnHardwareRouteCondition.matches -> 条件满足时注册 Bean。
 * Dependencies: OnHardwareRouteCondition。
 * Output: 条件成立时的 Bean 装配结果。
 */
@Target({ ElementType.TYPE, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Conditional(OnHardwareRouteCondition.class)
public @interface ConditionalOnHardwareRoute {
}
