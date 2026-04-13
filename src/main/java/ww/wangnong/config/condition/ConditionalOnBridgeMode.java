package ww.wangnong.config.condition;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.context.annotation.Conditional;

/**
 * Purpose: 标记仅在桥接链路启用时装配的 Bean。
 * Responsibilities: 把 bridge/hardware/full 三种模式共享的激活条件抽象为可复用注解。
 * Flow: 组件标注该注解 -> Spring 调用 OnBridgeModeCondition.matches -> 条件满足时注册 Bean。
 * Dependencies: OnBridgeModeCondition。
 * Output: 条件成立时的 Bean 装配结果。
 */
@Target({ ElementType.TYPE, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Conditional(OnBridgeModeCondition.class)
public @interface ConditionalOnBridgeMode {
}
