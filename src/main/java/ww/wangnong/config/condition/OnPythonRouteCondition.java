package ww.wangnong.config.condition;

import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;

import ww.wangnong.config.RunMode;

/**
 * Purpose: 实现 Python 路由 Bean 条件判定。
 * Responsibilities: 判断当前模式是否需要与 agri Python 后端交互。
 * Flow: Spring 调用 matches -> 解析运行模式 -> 返回 Python 路由是否启用。
 * Dependencies: RunMode。
 * Input: hmht.run.mode 配置。
 * Output: true 表示 bridge/full 模式下应装配 Bean。
 */
public class OnPythonRouteCondition implements Condition {

    @Override
    public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
        String modeValue = context.getEnvironment().getProperty("hmht.run.mode", "mock");
        RunMode mode = RunMode.fromString(modeValue);
        return mode.isPythonRouteEnabled();
    }
}
