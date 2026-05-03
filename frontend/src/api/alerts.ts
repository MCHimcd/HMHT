import { request } from "./client";
import type { AlertInfo, AlertList } from "./types";

export type AlertQuery = {
  nodeId: string;
  handled?: boolean | null;
  page: number;
  size: number;
};

export function listAlerts(query: AlertQuery) {
  const filterStatus = query.handled == null ? "all" : query.handled ? "handled" : "unhandled";
  return request<RawAlertList>("/v1/alerts/list", {
    method: "POST",
    body: {
      query: {
        nodeId: query.nodeId,
        node_id: query.nodeId,
        filter_status: filterStatus,
        page: query.page,
        pageSize: query.size,
      },
    },
  }).then(normalizeAlertList);
}

export function handleAlert(alertId: string) {
  return request<null>("/v1/alerts/handle", {
    method: "POST",
    body: {
      data: {
        alert_id: alertId,
        alertId,
        action: "handle",
      },
    },
  });
}

type RawAlertInfo = Partial<AlertInfo> & {
  alertId?: string;
  nodeId?: string;
  typeName?: string;
  sourceName?: string;
  isHandled?: boolean;
  handleTime?: number | null;
};

type RawAlertList = Omit<Partial<AlertList>, "items"> & {
  unhandledCount?: number;
  handledRecentCount?: number;
  items?: RawAlertInfo[];
};

function normalizeAlert(item: RawAlertInfo): AlertInfo {
  return {
    alert_id: item.alert_id ?? item.alertId ?? "",
    node_id: item.node_id ?? item.nodeId ?? "",
    type: item.type ?? "",
    type_name: item.type_name ?? item.typeName ?? item.type ?? "",
    level: item.level ?? "warning",
    message: item.message ?? "",
    source: item.source ?? "",
    source_name: item.source_name ?? item.sourceName ?? item.source ?? "",
    is_handled: item.is_handled ?? item.isHandled ?? false,
    handle_time: item.handle_time ?? item.handleTime ?? null,
    timestamp: item.timestamp ?? Date.now(),
  };
}

export function normalizeAlertList(data: RawAlertList): AlertList {
  const items = (data.items ?? []).map(normalizeAlert);
  return {
    total: data.total ?? items.length,
    unhandled_count: data.unhandled_count ?? data.unhandledCount ?? items.filter((item) => !item.is_handled).length,
    handled_recent_count: data.handled_recent_count ?? data.handledRecentCount ?? items.filter((item) => item.is_handled).length,
    items,
  };
}
