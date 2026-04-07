package ww.wangnong.config.condition;

import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;

import ww.wangnong.config.RunMode;

/**
 * 桥接模式条件：当 hmht.run.mode 为 bridge/hardware/full 时匹配。
 */
public class OnBridgeModeCondition implements Condition {

    @Override
    public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
        String modeValue = context.getEnvironment().getProperty("hmht.run.mode", "mock");
        RunMode mode = RunMode.fromString(modeValue);
        return mode.isBridgeModeEnabled();
    }
}
