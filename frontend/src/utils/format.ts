export function formatDateTime(value: number | string | null | undefined) {
  if (!value) return "-";
  const date = typeof value === "number" ? new Date(value) : new Date(value);
  if (Number.isNaN(date.getTime())) return String(value);
  const pad = (part: number) => String(part).padStart(2, "0");
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}`;
}

export function formatRepeatDays(value: string) {
  const labels = ["一", "二", "三", "四", "五", "六", "日"];
  return value
    .split(",")
    .filter(Boolean)
    .map((day) => labels[Number(day) - 1] || day)
    .join("、");
}

export function formatTime(hour: number, minute: number) {
  return `${String(hour).padStart(2, "0")}:${String(minute).padStart(2, "0")}`;
}

export function errorMessage(error: unknown) {
  return error instanceof Error ? error.message : "操作失败";
}
