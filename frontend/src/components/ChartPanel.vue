<script setup lang="ts">
import { BarChart, LineChart } from "echarts/charts";
import { GridComponent, LegendComponent, TooltipComponent } from "echarts/components";
import { init, use, type ECharts } from "echarts/core";
import { CanvasRenderer } from "echarts/renderers";
import { onBeforeUnmount, onMounted, ref, watch } from "vue";

import type { HistoryPoint } from "../api/types";

use([BarChart, LineChart, GridComponent, LegendComponent, TooltipComponent, CanvasRenderer]);

type ChartSeries = {
  name: string;
  unit?: string;
  color?: string;
  points: HistoryPoint[];
  axis?: "primary" | "secondary";
  type?: "line" | "bar";
};

const props = defineProps<{
  title: string;
  unit?: string;
  points: HistoryPoint[];
  series?: ChartSeries[];
  color?: string;
  type?: "line" | "bar";
}>();

const chartRef = ref<HTMLDivElement | null>(null);
let chart: ECharts | null = null;

function render() {
  if (!chartRef.value) return;
  chart ||= init(chartRef.value);
  const series = props.series?.length
    ? props.series
    : [{ name: props.title, unit: props.unit, color: props.color || "#168043", points: props.points }];
  const xAxisLabels = series[0]?.points.map((point) => point.timeLabel) ?? [];
  const hasSecondaryAxis = series.some((item) => item.axis === "secondary");
  chart.setOption({
    color: series.map((item) => item.color || "#168043"),
    grid: { left: 40, right: 46, top: 34, bottom: 36 },
    tooltip: { trigger: "axis" },
    legend: {
      top: 0,
      icon: "roundRect",
      itemWidth: 16,
      itemHeight: 4,
      textStyle: { color: "#43515c", fontSize: 12 },
    },
    xAxis: {
      type: "category",
      data: xAxisLabels,
      axisLine: { lineStyle: { color: "#b7d6c1" } },
      axisLabel: { color: "#617267" },
    },
    yAxis: [
      {
        type: "value",
        axisLabel: {
          color: "#617267",
          formatter: props.unit ? `{value}${props.unit}` : "{value}",
        },
        splitLine: { lineStyle: { color: "#e2eee6" } },
      },
      ...(hasSecondaryAxis
        ? [
            {
              type: "value",
              position: "right",
              axisLabel: { color: "#617267" },
              splitLine: { show: false },
            },
          ]
        : []),
    ],
    series: series.map((item) => ({
      name: item.name,
      type: item.type || props.type || "line",
      smooth: true,
      showSymbol: false,
      yAxisIndex: item.axis === "secondary" ? 1 : 0,
      areaStyle: (item.type || props.type) === "bar" || series.length > 1 ? undefined : { opacity: 0.14 },
      barWidth: "46%",
      data: item.points.map((point) => point.value),
    })),
  });
}

function resize() {
  chart?.resize();
}

onMounted(() => {
  render();
  window.addEventListener("resize", resize);
});

onBeforeUnmount(() => {
  window.removeEventListener("resize", resize);
  chart?.dispose();
  chart = null;
});

watch(() => [props.points, props.series, props.unit, props.type, props.color], render, { deep: true });
</script>

<template>
  <div ref="chartRef" class="chart" role="img" :aria-label="title" />
</template>
