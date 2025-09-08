#!/usr/bin/env python3
"""
Generate app/src/main/assets/pages.json with 604 Madani pages using AlQuran Cloud API.

- Edition defaults to "quran-uthmani" but can be overridden with --edition en.asad
  Only surah.number and numberInSurah are used, so both editions work.

Usage:
  pip install requests
  python3 scripts/generate_pages_json.py --edition quran-uthmani

Output:
  app/src/main/assets/pages.json
"""
import argparse
import json
import time
from pathlib import Path

import requests


def build_segments(ayahs):
    segments = []
    if not ayahs:
        return segments
    curr_surah = ayahs[0]["surah"]["number"]
    start = ayahs[0]["numberInSurah"]
    prev = start
    for a in ayahs[1:]:
        s = a["surah"]["number"]
        n = a["numberInSurah"]
        if s == curr_surah and n == prev + 1:
            prev = n
        else:
            segments.append({"surah": curr_surah, "startAyah": start, "endAyah": prev})
            curr_surah, start, prev = s, n, n
    segments.append({"surah": curr_surah, "startAyah": start, "endAyah": prev})
    return segments


def fetch_page(page: int, edition: str, timeout: int = 20):
    url = f"https://api.alquran.cloud/v1/page/{page}/{edition}"
    r = requests.get(url, timeout=timeout)
    r.raise_for_status()
    data = r.json()["data"]
    return data["ayahs"]


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--edition", default="quran-uthmani", help="API edition (e.g., quran-uthmani or en.asad)")
    ap.add_argument("--start", type=int, default=1)
    ap.add_argument("--end", type=int, default=604)
    ap.add_argument("--sleep", type=float, default=0.12, help="seconds to sleep between requests")
    args = ap.parse_args()

    pages = []
    for p in range(args.start, args.end + 1):
        ayahs = fetch_page(p, args.edition)
        segs = build_segments(ayahs)
        pages.append({"page": p, "segments": segs})
        if p % 25 == 0 or p == args.end:
            print(f"Processed page {p}")
        time.sleep(args.sleep)

    out = {"pages": pages}
    out_path = Path("app/src/main/assets/pages.json")
    out_path.parent.mkdir(parents=True, exist_ok=True)
    out_path.write_text(json.dumps(out, ensure_ascii=False), encoding="utf-8")
    print(f"Wrote {out_path} with {len(pages)} pages")


if __name__ == "__main__":
    main()

