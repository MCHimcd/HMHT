package ww.wangnong.config;

/**
 * HMHT 运行模式枚举。
 *
 * 四种模式定义了不同的数据流向：
 * - MOCK: 仅 Java 服务，使用模拟数据
 * - BRIDGE: App &lt;-&gt; Java &lt;-&gt; Python（不走硬件）
 * - HARDWARE: App &lt;-&gt; Java &lt;-&gt; 硬件（不走 Python）
 * - FULL: App &lt;-&gt; Java &lt;-&gt; Python &lt;-&gt; 硬件（完整链路）
 */
public enum RunMode {

    MOCK,
    BRIDGE,
    HARDWARE,
    FULL;

    /**
     * 是否启用 Python 路由（bridge 或 full 模式）。
     */
    public boolean isPythonRouteEnabled() {
        return this == BRIDGE || this == FULL;
    }

    /**
     * 是否启用硬件路由（hardware 或 full 模式）。
     */
    public boolean isHardwareRouteEnabled() {
        return this == HARDWARE || this == FULL;
    }

    /**
     * 是否启用 Mock 数据（仅 mock 模式）。
     */
    public boolean isMockDataEnabled() {
        return this == MOCK;
    }

    /**
     * 是否启用桥接模式（非 mock 模式）。
     */
    public boolean isBridgeModeEnabled() {
        return this != MOCK;
    }

    /**
     * 从配置字符串解析运行模式，默认为 MOCK。
     */
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
