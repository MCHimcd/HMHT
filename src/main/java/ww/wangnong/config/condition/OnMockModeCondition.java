package ww.wangnong.config.condition;

import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;

import ww.wangnong.config.RunMode;

/**
 * Purpose: 实现 mock 模式 Bean 条件判定。
 * Responsibilities: 判断当前模式是否应启用 mock 数据与 mock 组件。
 * Flow: Spring 调用 matches -> 解析运行模式 -> 返回是否处于 mock 模式。
 * Dependencies: RunMode。
 * Input: hmht.run.mode 配置。
 * Output: true 表示 mock 模式下应装配 Bean。
 */
public class OnMockModeCondition implements Condition {

    @Override
    public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
        String modeValue = context.getEnvironment().getProperty("hmht.run.mode", "mock");
        RunMode mode = RunMode.fromString(modeValue);
        return mode.isMockDataEnabled();
    }
}
