"""Throwaway probe for the Gemini API: check a key works, then see what it returns for a schedule photo.

The key is read from the GEMINI_API_KEY environment variable and is never printed or stored.

  python tools/gemini_probe.py --ping                      # text-only: does the key work?
  python tools/gemini_probe.py samples/cor.jpg             # image -> structured JSON
  python tools/gemini_probe.py samples/cor.jpg --model gemini-3.1-flash-lite
"""
import argparse
import base64
import json
import mimetypes
import os
import sys
import time
import urllib.error
import urllib.request

DEFAULT_MODEL = "gemini-3.5-flash-lite"
ENDPOINT = "https://generativelanguage.googleapis.com/v1beta/models/{model}:generateContent"

PROMPT = (
    "This is a photo of a student's class schedule (certificate of registration). "
    "Extract ONLY the table of classes. Ignore names, student numbers, and any other personal details. "
    "Return one entry per class per meeting day: if a class meets on several days, repeat it for each day. "
    "Copy codes and titles exactly as printed. Use 24-hour HH:mm for start and end. "
    "Use null for anything that is missing or unreadable. Do not invent classes."
)

SCHEMA = {
    "type": "object",
    "properties": {
        "classes": {
            "type": "array",
            "items": {
                "type": "object",
                "properties": {
                    "title": {"type": "string", "description": "Subject code and name as printed"},
                    "day": {
                        "type": "string",
                        "enum": ["Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"],
                    },
                    "start": {"type": "string", "description": "24-hour HH:mm"},
                    "end": {"type": "string", "description": "24-hour HH:mm"},
                    "location": {"type": ["string", "null"], "description": "Room or venue"},
                    "description": {"type": ["string", "null"]},
                },
                "required": ["title", "day", "start", "end"],
            },
        }
    },
    "required": ["classes"],
}


PROMPT_V2 = (
    "This is a photo of a student's certificate of registration. Transcribe ONLY the rows of the class table "
    "(columns like SubjectCode, Description, From, To, Day, Room), exactly as printed. "
    "One entry per printed line of times: a subject can have several lines, each with its own times, day and room. "
    "Repeat the subject code and description on every line of that subject. "
    "Do NOT convert anything: copy the day text (e.g. 'TueThu', 'MonWed', 'Fri') and the times (e.g. '01:00 PM') verbatim. "
    "Ignore names, student numbers, fees, signatures and every other part of the page. Do not invent rows; use null for unreadable values."
)

SCHEMA_V2 = {
    "type": "object",
    "properties": {
        "rows": {
            "type": "array",
            "items": {
                "type": "object",
                "properties": {
                    "subject_code": {"type": "string"},
                    "description": {"type": ["string", "null"]},
                    "from": {"type": "string", "description": "Start time exactly as printed"},
                    "to": {"type": "string", "description": "End time exactly as printed"},
                    "day": {"type": "string", "description": "Day text exactly as printed"},
                    "room": {"type": ["string", "null"]},
                },
                "required": ["subject_code", "from", "to", "day"],
            },
        }
    },
    "required": ["rows"],
}


def call(model, key, parts, schema=None):
    body = {"contents": [{"role": "user", "parts": parts}]}
    if schema is not None:
        body["generationConfig"] = {
            "responseMimeType": "application/json",
            "responseJsonSchema": schema,
            "mediaResolution": "MEDIA_RESOLUTION_HIGH",
        }
    request = urllib.request.Request(
        ENDPOINT.format(model=model),
        data=json.dumps(body).encode(),
        headers={"Content-Type": "application/json", "x-goog-api-key": key},
        method="POST",
    )
    started = time.time()
    try:
        with urllib.request.urlopen(request, timeout=90) as response:
            return response.status, json.loads(response.read()), time.time() - started
    except urllib.error.HTTPError as e:
        raw = e.read().decode(errors="replace")
        try:
            return e.code, json.loads(raw), time.time() - started
        except ValueError:
            return e.code, {"raw": raw}, time.time() - started


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("image", nargs="?")
    ap.add_argument("--ping", action="store_true")
    ap.add_argument("--model", default=DEFAULT_MODEL)
    ap.add_argument("--v2", action="store_true", help="transcribe-only schema (code does the parsing)")
    ap.add_argument("--text", action="store_true", help="print only the model's text + usage")
    args = ap.parse_args()

    key = os.environ.get("GEMINI_API_KEY", "").strip()
    if not key:
        sys.exit("Set GEMINI_API_KEY first (see the setup steps).")

    if args.ping:
        status, data, secs = call(args.model, key, [{"text": "Reply with the single word: ready"}])
    else:
        if not args.image:
            sys.exit("Give an image path, or use --ping.")
        ext = os.path.splitext(args.image)[1].lower()
        mime = {".heic": "image/heic", ".heif": "image/heif"}.get(ext) or mimetypes.guess_type(args.image)[0] or "image/jpeg"
        with open(args.image, "rb") as f:
            raw = f.read()
        print(f"image: {len(raw) / 1024:.0f} KB, {mime}", file=sys.stderr)
        parts = [
            {"text": PROMPT_V2 if args.v2 else PROMPT},  # text first, then the image
            {"inlineData": {"mimeType": mime, "data": base64.b64encode(raw).decode()}},
        ]
        status, data, secs = call(args.model, key, parts, SCHEMA_V2 if args.v2 else SCHEMA)

    print(f"HTTP {status} in {secs:.1f}s  (model {args.model})", file=sys.stderr)
    if args.text and status == 200:
        parts_out = data["candidates"][0]["content"]["parts"]
        print("".join(p.get("text", "") for p in parts_out))
        print(json.dumps(data.get("usageMetadata", {}).get("totalTokenCount")), "total tokens", file=sys.stderr)
    else:
        print(json.dumps(data, indent=2, ensure_ascii=False))


if __name__ == "__main__":
    main()
