package ww.wangnong.config.condition;

import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;

import ww.wangnong.config.RunMode;

/**
 * 非 Python 路由条件：当 hmht.run.mode 为 mock 或 hardware 时匹配。
 * 用于需要在没有 Python 后端时提供 Mock 实现的场景（如 ChatService）。
 */
public class OnNoPythonRouteCondition implements Condition {

    @Override
    public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
        String modeValue = context.getEnvironment().getProperty("hmht.run.mode", "mock");
        RunMode mode = RunMode.fromString(modeValue);
        return !mode.isPythonRouteEnabled();
    }
}
