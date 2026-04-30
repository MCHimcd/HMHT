import type { TaskRequest } from "../api/types";

export function validateTaskRequest(task: TaskRequest) {
  if (task.taskName.trim().length < 2 || task.taskName.trim().length > 30) {
    return "任务名称需要 2 到 30 个字符";
  }
  if (task.hour < 0 || task.hour > 23) {
    return "小时必须在 0 到 23";
  }
  if (task.minute < 0 || task.minute > 59) {
    return "分钟必须在 0 到 59";
  }
  if (!task.repeatDays) {
    return "至少选择一个重复日期";
  }
  return "";
}
