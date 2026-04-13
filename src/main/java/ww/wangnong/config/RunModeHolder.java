package ww.wangnong.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Purpose: 在运行时持有解析后的当前运行模式。
 * Responsibilities: 从配置中解析 hmht.run.mode，并为业务代码和条件装配提供统一能力判断入口。
 * Flow: 容器启动读取配置 -> RunMode.fromString 解析 -> 其他组件通过本类查询模式能力。
 * Dependencies: RunMode、Spring 配置注入。
 * Input: hmht.run.mode 配置值。
 * Output: 当前 RunMode 及其快捷布尔判断。
 */
@Component
public class RunModeHolder {

    private final RunMode mode;

    public RunModeHolder(@Value("${hmht.run.mode:mock}") String modeValue) {
        this.mode = RunMode.fromString(modeValue);
    }

    public RunMode getMode() {
        return mode;
    }

    public boolean isPythonRouteEnabled() {
        return mode.isPythonRouteEnabled();
    }

    public boolean isHardwareRouteEnabled() {
        return mode.isHardwareRouteEnabled();
    }

    public boolean isMockDataEnabled() {
        return mode.isMockDataEnabled();
    }

    public boolean isBridgeModeEnabled() {
        return mode.isBridgeModeEnabled();
    }
}
