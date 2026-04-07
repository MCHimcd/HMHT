package ww.wangnong.config.condition;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.context.annotation.Conditional;

/**
 * 条件注解：在桥接模式（bridge/hardware/full）时激活组件。
 * 即非 mock 模式时激活。
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Conditional(OnBridgeModeCondition.class)
public @interface ConditionalOnBridgeMode {
}
