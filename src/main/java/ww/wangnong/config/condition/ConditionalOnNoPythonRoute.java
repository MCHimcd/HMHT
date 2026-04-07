package ww.wangnong.config.condition;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.context.annotation.Conditional;

/**
 * 条件注解：在 Python 路由未启用时（mock/hardware 模式）激活组件。
 * 用于需要在没有 Python 后端时提供 Mock 实现的场景（如 ChatService）。
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Conditional(OnNoPythonRouteCondition.class)
public @interface ConditionalOnNoPythonRoute {
}
