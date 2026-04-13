package ww.wangnong.config;

/**
 * Purpose: 定义 HMHT 的四种运行模式及其能力开关。
 * Responsibilities: 统一表达 mock、bridge、hardware、full 四种模式，并提供 Python/硬件/桥接能力判定。
 * Flow: 配置字符串 -> fromString 解析为枚举 -> RunModeHolder 与条件装配读取辅助方法。
 * Dependencies: RunModeHolder、condition 包中的条件类。
 * Input: hmht.run.mode 配置值。
 * Output: 可用于 Bean 装配和运行时分支判断的模式枚举。
 */
public enum RunMode {

    MOCK,
    BRIDGE,
    HARDWARE,
    FULL;

    public boolean isPythonRouteEnabled() {
        return this == BRIDGE || this == FULL;
    }

    public boolean isHardwareRouteEnabled() {
        return this == HARDWARE || this == FULL;
    }

    public boolean isMockDataEnabled() {
        return this == MOCK;
    }

    public boolean isBridgeModeEnabled() {
        return this != MOCK;
    }

    public static RunMode fromString(String value) {
        if (value == null || value.isBlank()) {
            return MOCK;
        }
        try {
            return valueOf(value.toUpperCase().trim());
        } catch (IllegalArgumentException e) {
            return MOCK;
        }
    }
}
