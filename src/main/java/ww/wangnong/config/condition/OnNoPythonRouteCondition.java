package ww.wangnong.config.condition;

import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;

import ww.wangnong.config.RunMode;

/**
 * Purpose: 实现“无 Python 路由”场景的 Bean 条件判定。
 * Responsibilities: 判断当前模式是否不依赖 Python 后端，供 mock 替代组件启用。
 * Flow: Spring 调用 matches -> 解析运行模式 -> 返回 Python 路由是否关闭。
 * Dependencies: RunMode。
 * Input: hmht.run.mode 配置。
 * Output: true 表示 mock/hardware 模式下应装配 Bean。
 */
public class OnNoPythonRouteCondition implements Condition {

    @Override
    public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
        String modeValue = context.getEnvironment().getProperty("hmht.run.mode", "mock");
        RunMode mode = RunMode.fromString(modeValue);
        return !mode.isPythonRouteEnabled();
    }
}
