"use client";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { apiDelete, apiGet, apiPut } from "@/lib/request";
import type { SettingNamespace } from "@/types/settings";

export function useSettingNamespace<T extends object>(namespace: string) {
  const queryClient = useQueryClient();
  const queryKey = ["settings", namespace] as const;
  const query = useQuery({
    queryKey,
    queryFn: () => apiGet<SettingNamespace<T>>(`/me/settings/${namespace}`),
  });
  const save = useMutation({
    mutationFn: (values: T) =>
      apiPut<SettingNamespace<T>>(`/me/settings/${namespace}`, {
        version: query.data?.version ?? 0,
        values,
      }),
    onSuccess: (data) => queryClient.setQueryData(queryKey, data),
  });
  const reset = useMutation({
    mutationFn: () => apiDelete<SettingNamespace<T>>(`/me/settings/${namespace}`),
    onSuccess: (data) => queryClient.setQueryData(queryKey, data),
  });
  return { ...query, save, reset };
}
