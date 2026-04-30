import { request } from "./client";
import type { AlertList } from "./types";

export type AlertQuery = {
  nodeId: string;
  handled?: boolean | null;
  page: number;
  size: number;
};

export function listAlerts(query: AlertQuery) {
  const filterStatus = query.handled == null ? "all" : query.handled ? "handled" : "unhandled";
  return request<AlertList>("/v1/alerts/list", {
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
  });
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
