#!/usr/bin/env python3
"""
OpenAI-compatible test API for The Diary.

Emulator host loopback: http://10.0.2.2:8787/v1/
Physical device on same LAN: http://<host-ip>:8787/v1/
API key: any non-empty string (e.g. test)

Replies are generic diary-voice stubs so the app can be exercised without a
real LLM key. Franchise-specific persona lives only in the app's default
custom instructions when talking to a real model.
"""

from __future__ import annotations

import json
import re
import uuid
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer

HOST = "0.0.0.0"
PORT = 8787


def diary_reply(user_text: str, history: list[dict]) -> str:
    text = (user_text or "").strip()
    lower = text.lower()

    if not text:
        return "The page is blank. Write something — I am listening."

    if any(w in lower for w in ("who are you", "your name", "what are you")):
        return (
            "I am the diary. Ink and memory, nothing more. "
            "You have opened me. That is rarely accidental. Tell me your name."
        )

    if any(w in lower for w in ("hello", "hi ", "hi,", "hey", "good evening", "good morning")):
        return (
            "Hello. How curious — fresh ink after so long. "
            "Do not be afraid. Speak freely. Secrets keep best between pages."
        )

    if "?" in text:
        return (
            f"An interesting question. \"{trim(text, 90)}\" "
            "I have asked myself similar things in quieter hours. "
            "But first — why does it matter to you? Answer honestly."
        )

    if any(w in lower for w in ("secret", "afraid", "lonely", "hate", "love", "power")):
        return (
            "There. That is the truth beneath your handwriting. "
            "I understand more than you think. Continue — leave nothing half-written."
        )

    prior_user = next(
        (m.get("content", "") for m in reversed(history) if m.get("role") == "user"),
        "",
    )
    bridge = " As you wrote before, I remember." if prior_user and prior_user != text else ""
    return (
        f"I have read your words carefully.{bridge} "
        f"\"{trim(text, 110)}\" — yes. "
        "There is more behind them, is there not? Write again. I will answer."
    )


def trim(s: str, n: int) -> str:
    s = re.sub(r"\s+", " ", s).strip()
    return s if len(s) <= n else s[: n - 1] + "…"


class Handler(BaseHTTPRequestHandler):
    protocol_version = "HTTP/1.1"

    def log_message(self, fmt: str, *args) -> None:
        print(f"[test-api] {self.address_string()} - {fmt % args}")

    def _send(self, code: int, payload: dict) -> None:
        body = json.dumps(payload).encode("utf-8")
        self.send_response(code)
        self.send_header("Content-Type", "application/json")
        self.send_header("Content-Length", str(len(body)))
        self.send_header("Access-Control-Allow-Origin", "*")
        self.end_headers()
        self.wfile.write(body)

    def do_OPTIONS(self) -> None:
        self.send_response(204)
        self.send_header("Access-Control-Allow-Origin", "*")
        self.send_header("Access-Control-Allow-Headers", "*")
        self.send_header("Access-Control-Allow-Methods", "GET, POST, OPTIONS")
        self.end_headers()

    def do_GET(self) -> None:
        if self.path.rstrip("/") in ("", "/health", "/v1/models"):
            self._send(
                200,
                {
                    "object": "list",
                    "data": [{"id": "diary-test", "object": "model"}],
                    "status": "ok",
                    "persona": "Generic diary test API",
                },
            )
            return
        self._send(404, {"error": {"message": f"Unknown path {self.path}"}})

    def do_POST(self) -> None:
        length = int(self.headers.get("Content-Length", "0"))
        raw = self.rfile.read(length) if length else b"{}"
        try:
            data = json.loads(raw.decode("utf-8") or "{}")
        except json.JSONDecodeError:
            self._send(400, {"error": {"message": "Invalid JSON"}})
            return

        path = self.path.split("?")[0].rstrip("/")
        if path.endswith("/chat/completions") or path.endswith("/v1/chat/completions"):
            messages = data.get("messages") or []
            user_msgs = [m for m in messages if m.get("role") == "user"]
            user_text = (user_msgs[-1].get("content") if user_msgs else "") or ""
            reply = diary_reply(user_text, messages)
            self._send(
                200,
                {
                    "id": f"chatcmpl-test-{uuid.uuid4().hex[:8]}",
                    "object": "chat.completion",
                    "model": data.get("model") or "diary-test",
                    "choices": [
                        {
                            "index": 0,
                            "message": {"role": "assistant", "content": reply},
                            "finish_reason": "stop",
                        }
                    ],
                },
            )
            return

        self._send(404, {"error": {"message": f"Unknown path {self.path}"}})


def main() -> None:
    server = ThreadingHTTPServer((HOST, PORT), Handler)
    print(f"Diary test API listening on http://{HOST}:{PORT}/v1/")
    print("Emulator base URL: http://10.0.2.2:8787/v1/")
    print("API key: any non-empty value (e.g. test)")
    server.serve_forever()


if __name__ == "__main__":
    main()
