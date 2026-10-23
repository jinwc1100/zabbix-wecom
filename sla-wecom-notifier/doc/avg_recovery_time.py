# -*- coding: utf-8 -*-
"""计算父系统/子系统宕机平均恢复时间（全年 & 7 月），单位：分钟。"""
from __future__ import print_function

import json
import re
import sys
import http.cookiejar
import urllib.request
from datetime import datetime

SCRIPT_YML = r"f:\zabbix-sla统计\sla-wecom-notifier\src\main\resources\application.yml"
OUT_TXT = r"f:\zabbix-sla统计\sla-wecom-notifier\doc\宕机平均恢复时间.txt"

SOURCES = [
    {
        "label": "子系统",
        "uid": "cfn36wzatcpa8d",
        "panel_ids": (4, 2),
        "name_col": "子系统名称",
        "start_col": "宕机开始时间",
        "end_col": "恢复时间",
        "duration_sec_cols": ("宕机秒数", "实际宕机秒数"),
        "duration_display_cols": ("宕机时长", "排除后实际宕机时间"),
    },
    {
        "label": "父系统",
        "uid": "cfsre5mthdr7kc",
        "panel_ids": (4, 2),
        "name_col": "父系统名称",
        "start_col": "宕机开始时间",
        "end_col": "宕机结束时间",
        "duration_sec_cols": ("原始宕机秒数", "实际宕机秒数"),
        "duration_display_cols": ("原始宕机时间", "排除后实际宕机时间"),
    },
]


def load_auth():
    text = open(SCRIPT_YML, encoding="utf-8").read()
    base = re.search(r"base-url:\s*(\S+)", text).group(1).rstrip("/")
    user = re.search(r"username:\s*(\S+)", text).group(1)
    pw = re.search(r"password:\s*'([^']+)'", text).group(1)
    return base, user, pw


def opener_login(base, user, pw):
    cj = http.cookiejar.CookieJar()
    op = urllib.request.build_opener(urllib.request.HTTPCookieProcessor(cj))
    op.open(
        urllib.request.Request(
            base + "/login",
            data=json.dumps({"user": user, "password": pw}).encode(),
            headers={"Content-Type": "application/json"},
        ),
        timeout=60,
    )
    return op


def fetch_dashboard(op, base, uid):
    return json.loads(op.open(base + "/api/dashboards/uid/" + uid, timeout=60).read().decode())


def iter_panels(dash):
    for p in dash.get("dashboard", {}).get("panels") or []:
        yield p
        for n in p.get("panels") or []:
            yield n


def query_rows(op, base, panel):
    targets = panel.get("targets") or []
    if not targets:
        return []
    t = targets[0]
    if not t.get("rawSql"):
        return []
    ds = t.get("datasource") or {}
    payload = {
        "queries": [
            {
                "refId": "A",
                "format": "table",
                "rawQuery": True,
                "rawSql": t["rawSql"],
                "datasource": {"type": ds.get("type"), "uid": ds.get("uid")},
            }
        ],
        "from": "now-5y",
        "to": "now",
    }
    resp = json.loads(
        op.open(
            urllib.request.Request(
                base + "/api/ds/query",
                data=json.dumps(payload).encode(),
                headers={"Content-Type": "application/json"},
            ),
            timeout=300,
        )
        .read()
        .decode()
    )
    result = (resp.get("results") or {}).get("A") or {}
    if result.get("error"):
        raise RuntimeError(result.get("error"))
    frames = result.get("frames") or []
    if not frames:
        return []
    cols = [f.get("name") for f in frames[0]["schema"]["fields"]]
    values = frames[0]["data"]["values"]
    n = len(values[0]) if values else 0
    rows = []
    for i in range(n):
        row = {}
        for c, name in enumerate(cols):
            v = values[c][i]
            row[name] = "" if v is None else v
        rows.append(row)
    return rows


def parse_dt(raw):
    if raw is None:
        return None
    s = str(raw).strip()
    if not s or s in ("未恢复", "-", "null", "None"):
        return None
    if re.match(r"^\d+$", s):
        n = int(s)
        if n > 10**12:
            n = n / 1000.0
        elif n > 10**10:
            n = n / 1000.0
        try:
            return datetime.fromtimestamp(n)
        except Exception:
            return None
    for fmt in (
        "%Y-%m-%d %H:%M:%S",
        "%Y-%m-%dT%H:%M:%S",
        "%Y/%m/%d %H:%M:%S",
        "%Y-%m-%d %H:%M:%S.%f",
    ):
        try:
            return datetime.strptime(s[:26].replace("T", " "), fmt if "%f" in fmt else fmt)
        except ValueError:
            continue
    # try fromtimestamp ms string with decimals
    try:
        if "." in s and s.replace(".", "", 1).isdigit():
            return datetime.fromtimestamp(float(s) / (1000.0 if float(s) > 1e11 else 1.0))
    except Exception:
        pass
    return None


def parse_duration_minutes(row, src, start_dt, end_dt):
    # prefer start/end delta when both valid
    if start_dt and end_dt and end_dt >= start_dt:
        return (end_dt - start_dt).total_seconds() / 60.0
    for col in src["duration_sec_cols"]:
        v = row.get(col)
        if v is None or str(v).strip() == "":
            continue
        try:
            sec = float(str(v).strip())
            if sec >= 0:
                return sec / 60.0
        except ValueError:
            continue
    return None


def avg(values):
    if not values:
        return None
    return sum(values) / float(len(values))


def fmt_avg(v):
    if v is None:
        return "N/A (0 条)"
    return "%.2f" % v


def main():
    base, user, pw = load_auth()
    op = opener_login(base, user, pw)
    lines = []
    lines.append("宕机平均恢复时间（单位：分钟）")
    lines.append("恢复时间 = 结束/恢复时间 - 宕机开始时间；无法解析结束时间的「未恢复」等不计入")
    lines.append("")

    summary = []

    for src in SOURCES:
        dash = fetch_dashboard(op, base, src["uid"])
        title = dash.get("dashboard", {}).get("title", src["uid"])
        seen = set()
        year_mins = []
        july_mins = []
        skipped = 0
        total_raw = 0
        for panel in iter_panels(dash):
            if panel.get("id") not in src["panel_ids"]:
                continue
            rows = query_rows(op, base, panel)
            total_raw += len(rows)
            for row in rows:
                name = str(row.get(src["name_col"]) or "").strip()
                start_raw = row.get(src["start_col"])
                end_raw = row.get(src["end_col"])
                start_dt = parse_dt(start_raw)
                end_dt = parse_dt(end_raw)
                key = "|".join([name, str(start_raw), str(end_raw)])
                if key in seen:
                    continue
                seen.add(key)
                mins = parse_duration_minutes(row, src, start_dt, end_dt)
                if mins is None:
                    skipped += 1
                    continue
                year_mins.append(mins)
                if start_dt and start_dt.year == 2026 and start_dt.month == 7:
                    july_mins.append(mins)

        ya = avg(year_mins)
        ja = avg(july_mins)
        lines.append("=== %s (%s) ===" % (src["label"], title))
        lines.append("  原始行(含面板重复前)=%d 去重可用=%d 跳过=%d" % (total_raw, len(year_mins), skipped))
        lines.append("  全年平均恢复时间: %s 分钟（样本 %d）" % (fmt_avg(ya), len(year_mins)))
        lines.append("  2026-07 平均恢复时间: %s 分钟（样本 %d）" % (fmt_avg(ja), len(july_mins)))
        lines.append("")
        summary.append((src["label"], ya, len(year_mins), ja, len(july_mins)))

    lines.append("=== 汇总表 ===")
    lines.append("%-8s %18s %10s %18s %10s" % ("类别", "全年均值(分)", "全年样本", "7月均值(分)", "7月样本"))
    for label, ya, yn, ja, jn in summary:
        lines.append(
            "%-8s %18s %10d %18s %10d"
            % (label, fmt_avg(ya), yn, fmt_avg(ja), jn)
        )

    text = "\n".join(lines)
    open(OUT_TXT, "w", encoding="utf-8").write(text)
    sys.stdout.buffer.write((text + "\n").encode("utf-8"))
    print("wrote", OUT_TXT)


if __name__ == "__main__":
    main()
