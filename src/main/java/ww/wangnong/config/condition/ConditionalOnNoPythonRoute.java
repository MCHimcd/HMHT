package ww.wangnong.config.condition;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.context.annotation.Conditional;

/**
 * Purpose: 标记在没有 Python 后端路由时启用的 Bean。
 * Responsibilities: 把 mock/hardware 模式共享条件封装为注解，常用于 mock 替代实现。
 * Flow: 组件标注该注解 -> Spring 调用 OnNoPythonRouteCondition.matches -> 条件满足时注册 Bean。
 * Dependencies: OnNoPythonRouteCondition。
 * Output: 条件成立时的 Bean 装配结果。
 */
@Target({ ElementType.TYPE, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Conditional(OnNoPythonRouteCondition.class)
public @interface ConditionalOnNoPythonRoute {
}
