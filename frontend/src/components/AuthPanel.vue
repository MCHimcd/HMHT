<script setup lang="ts">
import { Sprout } from "lucide-vue-next";
</script>

<template>
  <main class="auth-page">
    <section class="auth-visual">
      <div class="brand-row">
        <div class="brand-mark"><Sprout :size="24" /></div>
        <div>
          <strong>HMHT</strong>
          <span>智慧农业课程版</span>
        </div>
      </div>
      <div class="visual-copy">
        <p class="eyebrow">Greenhouse Course Console</p>
        <h1>围绕 SIM_Node_01 的农业数据演示后台</h1>
        <p>
          登录后可联调节点、传感器曲线、设备统计、告警处理、定时任务与管理流程。
        </p>
      </div>
      <div class="visual-grid">
        <div><span>空气温度</span><strong>26.4 C</strong></div>
        <div><span>土壤湿度</span><strong>61%</strong></div>
        <div><span>未处理告警</span><strong>3</strong></div>
      </div>
    </section>
    <section class="auth-card">
      <slot />
    </section>
  </main>
</template>

<style scoped>
.auth-page {
  min-height: 100vh;
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(380px, 480px);
  gap: 24px;
  padding: 24px;
}

.auth-visual {
  display: grid;
  align-content: space-between;
  min-height: calc(100vh - 48px);
  border: 1px solid var(--border);
  border-radius: 8px;
  padding: 34px;
  background:
    linear-gradient(135deg, rgba(255, 255, 255, 0.92), rgba(222, 245, 229, 0.82)),
    radial-gradient(circle at 22% 20%, rgba(22, 128, 67, 0.12), transparent 34%);
  box-shadow: var(--shadow);
}

.brand-row {
  display: flex;
  align-items: center;
  gap: 12px;
}

.brand-mark {
  display: grid;
  place-items: center;
  width: 46px;
  height: 46px;
  border-radius: 8px;
  background: var(--green);
  color: #fff;
}

.brand-row strong,
.brand-row span {
  display: block;
}

.brand-row span,
.visual-copy p {
  color: var(--muted);
}

.visual-copy {
  max-width: 650px;
}

.visual-copy h1 {
  max-width: 620px;
  font-size: 42px;
}

.visual-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 14px;
}

.visual-grid div {
  display: grid;
  gap: 6px;
  border: 1px solid var(--border);
  border-radius: 8px;
  padding: 16px;
  background: rgba(255, 255, 255, 0.72);
}

.visual-grid span {
  color: var(--muted);
  font-size: 13px;
}

.visual-grid strong {
  font-size: 24px;
}

.auth-card {
  align-self: center;
  border: 1px solid var(--border);
  border-radius: 8px;
  padding: 26px;
  background: #fff;
  box-shadow: var(--shadow);
}

@media (max-width: 920px) {
  .auth-page {
    grid-template-columns: 1fr;
  }

  .auth-visual {
    min-height: auto;
  }
}

@media (max-width: 640px) {
  .auth-page {
    padding: 14px;
  }

  .visual-copy h1 {
    font-size: 30px;
  }

  .visual-grid {
    grid-template-columns: 1fr;
  }
}
</style>
