package ww.wangnong.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 持有当前运行模式的组件，供运行时和条件装配使用。
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
