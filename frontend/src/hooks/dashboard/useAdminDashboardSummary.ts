"use client";

import { apiGet } from "@/lib/request";
import type { AdminDashboardSummary } from "@/types/admin-dashboard";
import { useQuery } from "@tanstack/react-query";

export function useAdminDashboardSummary() {
  return useQuery({
    queryKey: ["admin-dashboard", "summary"],
    queryFn: () => apiGet<AdminDashboardSummary>("/admin-dashboard/summary"),
    staleTime: 30_000,
  });
}
