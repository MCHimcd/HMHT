package ww.wangnong.config.condition;

import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;

import ww.wangnong.config.RunMode;

/**
 * Purpose: 实现桥接模式 Bean 条件判定。
 * Responsibilities: 读取 hmht.run.mode 并判断是否属于非 mock 的桥接链路模式。
 * Flow: Spring 条件装配调用 matches -> 解析运行模式 -> 返回是否启用桥接模式。
 * Dependencies: RunMode。
 * Input: Spring Environment 中的 hmht.run.mode。
 * Output: true 表示 bridge/hardware/full 模式下应装配 Bean。
 */
public class OnBridgeModeCondition implements Condition {

    @Override
    public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
        String modeValue = context.getEnvironment().getProperty("hmht.run.mode", "mock");
        RunMode mode = RunMode.fromString(modeValue);
        return mode.isBridgeModeEnabled();
    }
}
