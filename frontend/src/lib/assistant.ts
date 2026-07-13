"use client";

import { request } from "./request";
import type { ApiResult } from "@/types/api";

export type AssistantChatRequest = {
  message: string;
  sessionId: string;
  source?: "web";
  useRag?: boolean;
  docType?: string;
  topK?: number;
};

export type AssistantChatResponse = {
  sessionId?: string;
  answer?: string;
  content?: string;
  message?: string;
  references?: unknown[];
};

export async function chatWithAssistant(data: AssistantChatRequest) {
  const response = await request.post<ApiResult<AssistantChatResponse>>(
    "/herb/assistant/chat",
    {
      source: "web",
      useRag: true,
      topK: 5,
      ...data,
    },
    { timeout: 120000 },
  );
  return response.data.data;
}
