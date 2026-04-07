package ww.wangnong.config.condition;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.context.annotation.Conditional;

/**
 * 条件注解：仅在 Mock 模式（hmht.run.mode=mock 或未配置）时激活组件。
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Conditional(OnMockModeCondition.class)
public @interface ConditionalOnMockMode {
}
