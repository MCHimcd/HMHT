export type ApiResponse<T> = {
  code: number;
  message: string;
  data: T;
};

export type LoginResult = {
  token: string;
  expires_in: number;
};

export type CodeResult = {
  expires_in: number;
  test_code: string;
};

export type Profile = {
  userId: string;
  userName: string;
  phone: string;
  gender: string | null;
  birthDate: string | null;
  avatarSrc: string | null;
  role: string | null;
};

export type HealthData = {
  available: boolean;
  application: string;
  edition: string;
  nodeId: string;
  dataSimulated: boolean;
  time: string;
};

export type NodeInfo = {
  id: string;
  name: string;
  status: string;
};

export type NodeDetail = NodeInfo & {
  nodeState: Record<string, unknown>;
};

export type SensorMeta = {
  key: SensorMetric;
  name: string;
  unit: string;
  icon: string;
  badgeColor: string;
  bgColor: string;
  warningMin: number | null;
  warningMax: number | null;
};

export type EquipmentMeta = {
  key: EquipmentKey;
  name: string;
  icon: string;
  controllable: boolean;
  controlType: string;
};

export type ThresholdUpdate = {
  sensorKey: string;
  warningMin: number | null;
  warningMax: number | null;
};

export type SensorMetric = "temperature" | "humidity" | "light" | "soilHumidity";
export type SensorRange = "24h" | "7d" | "30d";
export type EquipmentKey = "water_pump" | "led_light" | "fan";

export type HistoryPoint = {
  timeLabel: string;
  value: number;
};

export type HistorySummary = {
  max: number;
  min: number;
  avg: number;
};

export type SensorHistory = {
  metric: SensorMetric;
  summary: HistorySummary;
  points: HistoryPoint[];
};

export type EquipmentStats = {
  summary: Record<string, unknown>;
  points: HistoryPoint[];
};

export type AlertInfo = {
  alert_id: string;
  node_id: string;
  type: string;
  type_name: string;
  level: string;
  message: string;
  source: string;
  source_name: string;
  is_handled: boolean;
  handle_time: number | null;
  timestamp: number;
};

export type AlertList = {
  total: number;
  unhandled_count: number;
  handled_recent_count: number;
  items: AlertInfo[];
};

export type TaskInfo = {
  id: number;
  nodeId: string;
  equipmentKey: EquipmentKey;
  action: string;
  taskName: string;
  hour: number;
  minute: number;
  repeatDays: string;
  enabled: boolean;
  createdAt: number;
};

export type TaskRequest = Omit<TaskInfo, "id" | "enabled" | "createdAt">;

export type AdminUser = {
  id: number;
  userName: string;
  phone: string;
  role: string;
};

export type LogItem = {
  id: number;
  userId: number | null;
  userName: string | null;
  operationType: string;
  targetType: string | null;
  targetId: string | null;
  detail: string | null;
  createdAt: string | null;
};

export type LogPage = {
  total: number;
  items: LogItem[];
};
