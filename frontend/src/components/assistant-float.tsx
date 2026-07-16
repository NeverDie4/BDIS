"use client";

import { chatWithAssistant } from "@/lib/assistant";
import { getStoredToken } from "@/lib/auth-token";
import { isAuthRedirectError } from "@/lib/request";
import axios from "axios";
import { Loader2, SendHorizontal, X } from "lucide-react";
import Image from "next/image";
import { usePathname } from "next/navigation";
import {
  Fragment,
  type KeyboardEvent,
  type PointerEvent as ReactPointerEvent,
  type ReactNode,
  useEffect,
  useMemo,
  useRef,
  useState,
} from "react";
import styles from "./assistant-float.module.css";

type ChatMessage = {
  role: "assistant" | "user";
  content: string;
};

type PageContext = {
  title: string;
  description: string;
  quickQuestions: string[];
};

type FloatPosition = {
  x: number;
  y: number;
};

type FloatAnchor = {
  side: "left" | "right";
  top: number;
};

type FloatDragState = {
  pointerId: number;
  startX: number;
  startY: number;
  originX: number;
  originY: number;
  width: number;
  height: number;
  moved: boolean;
  lastPosition: FloatPosition;
};

const SESSION_KEY = "web_assistant_session_id";
const FLOAT_POSITION_KEY = "web_assistant_float_position";
const LEGACY_FLOAT_POSITION_KEY = "web_assistant_float_position_v1";
const FLOAT_POSITION_EVENT = "web-assistant-float-position";
const FLOAT_HORIZONTAL_GAP = 24;
const FLOAT_GROWTH_HORIZONTAL_GAP = 32;
const FLOAT_BOTTOM_GAP = 24;
const FLOAT_NAV_HEIGHT = 72;
const FLOAT_NAV_GAP = 16;
const FLOAT_DEFAULT_SIZE = 68;
const FLOAT_MOBILE_SIZE = 62;
const FLOAT_DRAG_THRESHOLD = 5;
const ICON_SRC = "/images/assistant-float.png";

const DEFAULT_CONTEXT: PageContext = {
  title: "当前页面：本草研究院标本馆",
  description: "可询问：采集任务、图谱识别、审核流程、溯源管理和系统使用等问题。",
  quickQuestions: [
    "生长记录和采集批次是什么关系？",
    "生长记录怎么提交审核？",
    "审核通过和驳回有什么区别？",
    "溯源时间线怎么看？",
    "溯源二维码怎么生成？",
  ],
};

const GROWTH_CONTEXT: PageContext = {
  title: "当前页面：生长数据管理",
  description: "可询问：生长记录、审核流程、溯源时间线、二维码生成和现场图片证据等问题。",
  quickQuestions: [
    "什么是观测点？",
    "生长记录和采集批次是什么关系？",
    "审核员如何处理待审核记录？",
    "溯源时间线怎么看？",
    "现场图片证据从哪里来？",
  ],
};

const WELCOME_MESSAGE =
  "你好，我是本草 AI 小助手。可以帮助你查询采集任务、解释图谱识别结果，并了解生长记录、审核与溯源流程。";

export function AssistantFloat() {
  const pathname = usePathname();
  const [mounted, setMounted] = useState(false);
  const [open, setOpen] = useState(false);
  const [input, setInput] = useState("");
  const [loading, setLoading] = useState(false);
  const [sessionId, setSessionId] = useState("");
  const [floatPosition, setFloatPosition] = useState<FloatPosition | null>(null);
  const [dragging, setDragging] = useState(false);
  const [quickExpanded, setQuickExpanded] = useState(true);
  const [messages, setMessages] = useState<ChatMessage[]>([
    { role: "assistant", content: WELCOME_MESSAGE },
  ]);
  const messagesRef = useRef<HTMLDivElement>(null);
  const inputRef = useRef<HTMLTextAreaElement>(null);
  const floatDragRef = useRef<FloatDragState | null>(null);
  const floatAnchorRef = useRef<FloatAnchor>({ side: "right", top: 0 });
  const suppressFloatClickRef = useRef(false);

  useEffect(() => {
    setMounted(true);
    setSessionId(resolveSessionId());

    const applyPosition = (anchor: FloatAnchor) => {
      const size = getFloatSize();
      const normalizedAnchor = normalizeFloatAnchor(anchor, size);
      floatAnchorRef.current = normalizedAnchor;
      setFloatPosition(resolveFloatAnchor(normalizedAnchor, size));
    };
    applyPosition(readFloatPosition() ?? getDefaultFloatPosition());

    const handleStorage = (event: StorageEvent) => {
      if (event.key !== FLOAT_POSITION_KEY) {
        return;
      }
      const nextAnchor = parseFloatPosition(event.newValue);
      if (nextAnchor) {
        applyPosition(nextAnchor);
      }
    };
    const handlePositionEvent = (event: Event) => {
      const nextAnchor = (event as CustomEvent<FloatAnchor>).detail;
      if (nextAnchor) {
        applyPosition(nextAnchor);
      }
    };
    const handleResize = () => {
      applyPosition(floatAnchorRef.current);
    };

    window.addEventListener("storage", handleStorage);
    window.addEventListener(FLOAT_POSITION_EVENT, handlePositionEvent);
    window.addEventListener("resize", handleResize);
    return () => {
      window.removeEventListener("storage", handleStorage);
      window.removeEventListener(FLOAT_POSITION_EVENT, handlePositionEvent);
      window.removeEventListener("resize", handleResize);
    };
  }, []);

  useEffect(() => {
    if (!mounted) {
      return;
    }
    const size = getFloatSize();
    setFloatPosition(resolveFloatAnchor(floatAnchorRef.current, size));
  }, [mounted, pathname]);

  useEffect(() => {
    if (!open) {
      return;
    }
    messagesRef.current?.scrollTo({
      top: messagesRef.current.scrollHeight,
      behavior: "smooth",
    });
  }, [messages, loading, open]);

  useEffect(() => {
    if (open && !isCoarsePointer()) {
      window.setTimeout(() => inputRef.current?.focus(), 120);
    }
  }, [open]);

  const hidden = useMemo(() => {
    if (!mounted) {
      return true;
    }
    if (!getStoredToken()) {
      return true;
    }
    return pathname === "/login" || pathname?.startsWith("/trace/");
  }, [mounted, pathname]);

  const pageContext = pathname === "/growth" ? GROWTH_CONTEXT : DEFAULT_CONTEXT;

  if (hidden) {
    return null;
  }

  async function sendMessage(rawMessage?: string) {
    const message = (rawMessage ?? input).trim();
    if (!message || loading || !sessionId) {
      return;
    }
    setInput("");
    setQuickExpanded(false);
    setMessages((current) => [...current, { role: "user", content: message }]);
    setLoading(true);

    try {
      const response = await chatWithAssistant({
        message,
        sessionId,
        source: "web",
        useRag: true,
        topK: 5,
      });
      const nextSessionId = response?.sessionId;
      if (nextSessionId && nextSessionId !== sessionId) {
        window.localStorage.setItem(SESSION_KEY, nextSessionId);
        setSessionId(nextSessionId);
      }
      setMessages((current) => [
        ...current,
        { role: "assistant", content: resolveAnswer(response) },
      ]);
    } catch (error) {
      setMessages((current) => [...current, { role: "assistant", content: resolveError(error) }]);
    } finally {
      setLoading(false);
    }
  }

  function handleKeyDown(event: KeyboardEvent<HTMLTextAreaElement>) {
    if (event.key !== "Enter" || event.shiftKey) {
      return;
    }
    event.preventDefault();
    void sendMessage();
  }

  function handleFloatPointerDown(event: ReactPointerEvent<HTMLButtonElement>) {
    if (event.button !== 0) {
      return;
    }
    const rect = event.currentTarget.getBoundingClientRect();
    const origin = clampFloatPosition(
      floatPosition ?? { x: rect.left, y: rect.top },
      rect.width,
      rect.height,
    );
    floatDragRef.current = {
      pointerId: event.pointerId,
      startX: event.clientX,
      startY: event.clientY,
      originX: origin.x,
      originY: origin.y,
      width: rect.width,
      height: rect.height,
      moved: false,
      lastPosition: origin,
    };
    suppressFloatClickRef.current = false;
    event.currentTarget.setPointerCapture(event.pointerId);
  }

  function handleFloatPointerMove(event: ReactPointerEvent<HTMLButtonElement>) {
    const drag = floatDragRef.current;
    if (!drag || drag.pointerId !== event.pointerId) {
      return;
    }
    const deltaX = event.clientX - drag.startX;
    const deltaY = event.clientY - drag.startY;
    if (!drag.moved && Math.hypot(deltaX, deltaY) <= FLOAT_DRAG_THRESHOLD) {
      return;
    }
    drag.moved = true;
    setDragging(true);
    const nextPosition = clampFloatPosition(
      { x: drag.originX + deltaX, y: drag.originY + deltaY },
      drag.width,
      drag.height,
    );
    drag.lastPosition = nextPosition;
    setFloatPosition(nextPosition);
  }

  function finishFloatDrag(event: ReactPointerEvent<HTMLButtonElement>) {
    const drag = floatDragRef.current;
    if (!drag || drag.pointerId !== event.pointerId) {
      return;
    }
    if (event.currentTarget.hasPointerCapture(event.pointerId)) {
      event.currentTarget.releasePointerCapture(event.pointerId);
    }
    if (drag.moved) {
      suppressFloatClickRef.current = true;
      const snappedPosition = snapFloatToSide(
        drag.lastPosition,
        drag.width,
        drag.height,
      );
      floatAnchorRef.current = snappedPosition.anchor;
      setFloatPosition(snappedPosition.position);
      persistFloatPosition(snappedPosition.anchor);
    }
    floatDragRef.current = null;
    setDragging(false);
  }

  function handleFloatPointerUp(event: ReactPointerEvent<HTMLButtonElement>) {
    finishFloatDrag(event);
  }

  function handleFloatPointerCancel(event: ReactPointerEvent<HTMLButtonElement>) {
    finishFloatDrag(event);
  }

  function handleFloatClick() {
    if (suppressFloatClickRef.current) {
      suppressFloatClickRef.current = false;
      return;
    }
    setOpen(true);
  }

  return (
    <>
      {!open ? (
        <button
          className={`${styles.floatButton} ${dragging ? styles.floatButtonDragging : ""}`}
          type="button"
          aria-label="打开本草 AI 小助手"
          style={
            floatPosition
              ? { left: floatPosition.x, top: floatPosition.y, right: "auto", bottom: "auto" }
              : undefined
          }
          onClick={handleFloatClick}
          onPointerCancel={handleFloatPointerCancel}
          onPointerDown={handleFloatPointerDown}
          onPointerMove={handleFloatPointerMove}
          onPointerUp={handleFloatPointerUp}
        >
          <Image
            className={styles.floatIcon}
            src={ICON_SRC}
            alt=""
            width={68}
            height={68}
            draggable={false}
          />
        </button>
      ) : null}

      <div
        className={`${styles.backdrop} ${open ? styles.backdropOpen : ""}`}
        aria-hidden={!open}
        onClick={() => setOpen(false)}
      />

      <aside
        className={`${styles.sidebar} ${open ? styles.sidebarOpen : ""}`}
        aria-label="本草 AI 小助手"
        aria-hidden={!open}
      >
        <header className={styles.header}>
          <div className={styles.headerLeft}>
            <Image
              className={styles.headerIcon}
              src={ICON_SRC}
              alt=""
              width={44}
              height={44}
              priority={false}
            />
            <div className={styles.titleGroup}>
              <h2 className={styles.title}>本草 AI 小助手</h2>
              <p className={styles.subtitle}>采集、识别、审核与溯源问答</p>
            </div>
          </div>
          <button
            className={styles.closeButton}
            type="button"
            aria-label="关闭本草 AI 小助手"
            onClick={() => setOpen(false)}
          >
            <X size={18} />
          </button>
        </header>

        <section className={styles.contextBox} aria-label="当前页面上下文">
          <p className={styles.contextTitle}>
            <span>当前页面</span>
            <strong>{pageContext.title.replace(/^当前页面：/, "")}</strong>
          </p>
          <p className={styles.contextText}>可问：采集任务、图谱识别、审核流程、溯源管理</p>
        </section>

        <section className={styles.quickArea} aria-label="快捷问题">
          <div className={styles.quickHeader}>
            <button
              className={styles.quickTitleButton}
              type="button"
              onClick={() => setQuickExpanded((current) => !current)}
            >
              推荐问题
            </button>
            <button
              className={styles.quickToggle}
              type="button"
              onClick={() => setQuickExpanded((current) => !current)}
            >
              {quickExpanded ? "收起" : "展开"}
            </button>
          </div>
          {quickExpanded ? (
            <div className={styles.quickScroller}>
              {pageContext.quickQuestions.map((question) => (
                <button
                  className={styles.quickButton}
                  disabled={loading}
                  key={question}
                  type="button"
                  onClick={() => void sendMessage(question)}
                >
                  {question}
                </button>
              ))}
            </div>
          ) : null}
        </section>

        <div className={styles.messages} ref={messagesRef}>
          {messages.map((message, index) => (
            <div
              className={`${styles.messageRow} ${
                message.role === "user" ? styles.userRow : styles.assistantRow
              }`}
              key={`${message.role}-${index}`}
            >
              {message.role === "assistant" ? (
                <Image
                  className={styles.messageAvatar}
                  src={ICON_SRC}
                  alt=""
                  width={32}
                  height={32}
                />
              ) : null}
              <div
                className={`${styles.bubble} ${
                  message.role === "user" ? styles.userBubble : styles.assistantBubble
                }`}
              >
                {message.role === "assistant"
                  ? renderAssistantText(message.content)
                  : message.content}
              </div>
            </div>
          ))}
          {loading ? (
            <div className={`${styles.messageRow} ${styles.assistantRow}`}>
              <Image
                className={styles.messageAvatar}
                src={ICON_SRC}
                alt=""
                width={32}
                height={32}
              />
              <div className={`${styles.bubble} ${styles.assistantBubble} ${styles.loadingBubble}`}>
                <span className={styles.typingDots} aria-hidden="true"><i /><i /><i /></span>
                正在整理回答
              </div>
            </div>
          ) : null}
        </div>

        <div className={styles.inputArea}>
          <div className={styles.composer}>
            <textarea
              ref={inputRef}
              className={styles.input}
              disabled={loading}
              onChange={(event) => setInput(event.target.value)}
              onKeyDown={handleKeyDown}
              placeholder="输入你的问题"
              rows={1}
              value={input}
            />
            <button
              className={styles.sendButton}
              disabled={loading || !input.trim()}
              type="button"
              aria-label="发送"
              onClick={() => void sendMessage()}
            >
              {loading ? <Loader2 className={styles.spin} size={18} /> : <SendHorizontal size={18} />}
            </button>
          </div>
        </div>
      </aside>
    </>
  );
}

function getDefaultFloatPosition(): FloatAnchor {
  const size = getFloatSize();
  return normalizeFloatAnchor(
    { side: "right", top: window.innerHeight - size - 32 },
    size,
  );
}

function clampFloatPosition(
  position: FloatPosition,
  width = FLOAT_DEFAULT_SIZE,
  height = FLOAT_DEFAULT_SIZE,
): FloatPosition {
  const horizontalGap = getFloatHorizontalGap();
  const minTop = FLOAT_NAV_HEIGHT + FLOAT_NAV_GAP;
  const maxX = Math.max(horizontalGap, window.innerWidth - width - horizontalGap);
  const maxY = Math.max(minTop, window.innerHeight - height - FLOAT_BOTTOM_GAP);
  return {
    x: Math.min(Math.max(position.x, horizontalGap), maxX),
    y: Math.min(Math.max(position.y, minTop), maxY),
  };
}

function snapFloatToSide(
  position: FloatPosition,
  width = FLOAT_DEFAULT_SIZE,
  height = FLOAT_DEFAULT_SIZE,
): { anchor: FloatAnchor; position: FloatPosition } {
  const side = position.x + width / 2 < window.innerWidth / 2 ? "left" : "right";
  const anchor = normalizeFloatAnchor({ side, top: position.y }, height);
  return {
    anchor,
    position: resolveFloatAnchor(anchor, width, height),
  };
}

function resolveFloatAnchor(
  anchor: FloatAnchor,
  width = FLOAT_DEFAULT_SIZE,
  height = FLOAT_DEFAULT_SIZE,
): FloatPosition {
  const horizontalGap = getFloatHorizontalGap();
  return {
    x:
      anchor.side === "left"
        ? horizontalGap
        : Math.max(horizontalGap, window.innerWidth - width - horizontalGap),
    y: normalizeFloatAnchor(anchor, height).top,
  };
}

function normalizeFloatAnchor(anchor: FloatAnchor, height = FLOAT_DEFAULT_SIZE): FloatAnchor {
  const minTop = FLOAT_NAV_HEIGHT + FLOAT_NAV_GAP;
  const maxTop = Math.max(minTop, window.innerHeight - height - FLOAT_BOTTOM_GAP);
  return {
    side: anchor.side,
    top: Math.min(Math.max(anchor.top, minTop), maxTop),
  };
}

function getFloatSize() {
  return window.innerWidth <= 720 ? FLOAT_MOBILE_SIZE : FLOAT_DEFAULT_SIZE;
}

function getFloatHorizontalGap() {
  return window.location.pathname === "/growth"
    ? FLOAT_GROWTH_HORIZONTAL_GAP
    : FLOAT_HORIZONTAL_GAP;
}

function parseFloatPosition(value: string | null): FloatAnchor | null {
  if (!value) {
    return null;
  }
  try {
    const parsed = JSON.parse(value) as Partial<FloatAnchor>;
    if ((parsed.side === "left" || parsed.side === "right") && Number.isFinite(parsed.top)) {
      return { side: parsed.side, top: Number(parsed.top) };
    }
  } catch {
    return null;
  }
  return null;
}

function readFloatPosition(): FloatAnchor | null {
  try {
    const currentPosition = parseFloatPosition(window.localStorage.getItem(FLOAT_POSITION_KEY));
    if (currentPosition) {
      return currentPosition;
    }
    const legacyValue = window.localStorage.getItem(LEGACY_FLOAT_POSITION_KEY);
    if (!legacyValue) {
      return null;
    }
    const legacyPosition = JSON.parse(legacyValue) as Partial<FloatPosition>;
    if (!Number.isFinite(legacyPosition.x) || !Number.isFinite(legacyPosition.y)) {
      return null;
    }
    const migrated = snapFloatToSide({
      x: Number(legacyPosition.x),
      y: Number(legacyPosition.y),
    }).anchor;
    window.localStorage.setItem(FLOAT_POSITION_KEY, JSON.stringify(migrated));
    window.localStorage.removeItem(LEGACY_FLOAT_POSITION_KEY);
    return migrated;
  } catch {
    return null;
  }
}

function persistFloatPosition(position: FloatAnchor) {
  try {
    window.localStorage.setItem(FLOAT_POSITION_KEY, JSON.stringify(position));
  } catch {
    // 浏览器禁用本地存储时，当前页面内仍保持拖动后的位置。
  }
  window.dispatchEvent(new CustomEvent<FloatAnchor>(FLOAT_POSITION_EVENT, { detail: position }));
}

function resolveSessionId() {
  const existing = window.localStorage.getItem(SESSION_KEY);
  if (existing) {
    return existing;
  }
  const random = Math.random().toString(36).slice(2, 10);
  const next = `web_assistant_${Date.now()}_${random}`;
  window.localStorage.setItem(SESSION_KEY, next);
  return next;
}

function isCoarsePointer() {
  return typeof window !== "undefined" && window.matchMedia("(pointer: coarse)").matches;
}

function resolveAnswer(response: unknown) {
  if (!response || typeof response !== "object") {
    return "AI 小助手暂时没有返回内容。";
  }
  const data = response as Record<string, unknown>;
  const answer = data.answer || data.content || data.message;
  return typeof answer === "string" && answer.trim() ? answer : "AI 小助手暂时没有返回内容。";
}

function resolveError(error: unknown) {
  if (isAuthRedirectError(error) || (axios.isAxiosError(error) && error.response?.status === 401)) {
    return "登录状态已失效，请重新登录后再使用 AI 小助手。";
  }
  if (axios.isAxiosError(error) && error.response?.status && error.response.status >= 500) {
    return "AI 小助手暂时不可用，请稍后再试。";
  }
  if (axios.isAxiosError(error) && !error.response) {
    return "网络异常，请检查连接后再试。";
  }
  return "AI 小助手暂时不可用，请稍后再试。";
}

function renderAssistantText(content: string) {
  const lines = content
    .replace(/\r\n?/g, "\n")
    .split("\n")
    .map((line) => line.trimEnd());
  const nodes: ReactNode[] = [];
  let listItems: string[] = [];

  function flushList() {
    if (listItems.length === 0) {
      return;
    }
    nodes.push(
      <ul className={styles.list} key={`list-${nodes.length}`}>
        {listItems.map((item, index) => (
          <li key={`${item}-${index}`}>{renderInline(item)}</li>
        ))}
      </ul>,
    );
    listItems = [];
  }

  lines.forEach((line) => {
    const heading = line.match(/^#{1,6}\s+(.+)$/);
    if (heading) {
      flushList();
      nodes.push(
        <p className={styles.heading} key={`heading-${nodes.length}`}>
          {renderInline(heading[1])}
        </p>,
      );
      return;
    }

    const bullet = line.match(/^[-*+]\s+(.+)$/) || line.match(/^\d+[.)]\s+(.+)$/);
    if (bullet) {
      listItems.push(bullet[1]);
      return;
    }

    flushList();
    if (!line.trim()) {
      nodes.push(<Fragment key={`space-${nodes.length}`}>{"\n"}</Fragment>);
      return;
    }
    nodes.push(
      <p className={styles.paragraph} key={`paragraph-${nodes.length}`}>
        {renderInline(line)}
      </p>,
    );
  });

  flushList();
  return nodes;
}

function renderInline(text: string) {
  const parts = text
    .replace(/\*\*(.*?)\*\*/g, "$1")
    .replace(/__(.*?)__/g, "$1")
    .split(/(`[^`]+`)/g);
  return parts.map((part, index) => {
    if (part.startsWith("`") && part.endsWith("`")) {
      return (
        <code className={styles.inlineCode} key={`${part}-${index}`}>
          {part.slice(1, -1)}
        </code>
      );
    }
    return <Fragment key={`${part}-${index}`}>{part}</Fragment>;
  });
}
