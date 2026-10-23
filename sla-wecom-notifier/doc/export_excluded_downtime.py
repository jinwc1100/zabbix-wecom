# -*- coding: utf-8 -*-
"""
从 Grafana 子系统/父系统宕机明细看板拉取数据，
导出排除状态为「部分排除」「全部排除」的行到 Excel。

用法:
  python export_excluded_downtime.py
输出:
  doc/排除宕机明细.xlsx
"""
from __future__ import print_function

import json
import os
import re
import sys
import http.cookiejar
import urllib.request
from collections import OrderedDict

try:
    from openpyxl import Workbook
except ImportError:
    print("需要 openpyxl: pip install openpyxl", file=sys.stderr)
    sys.exit(1)

SCRIPT_DIR = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.dirname(SCRIPT_DIR)
YML_PATH = os.path.join(ROOT, "src", "main", "resources", "application.yml")
OUT_PATH = os.path.join(SCRIPT_DIR, "排除宕机明细.xlsx")

ALLOWED_STATUS = ("部分排除", "全部排除")

SOURCES = [
    {
        "label": "子系统",
        "dashboard_uid": "cfn36wzatcpa8d",
        "panel_ids": (4, 2),
        "name_col": "子系统名称",
        "start_col": "宕机开始时间",
        "end_col": "恢复时间",
        "duration_cols": ("宕机时长", "排除后实际宕机时间", "原始宕机时间"),
        "reason_col": "排除原因",
        "status_col": "排除状态",
    },
    {
        "label": "父系统",
        "dashboard_uid": "cfsre5mthdr7kc",
        "panel_ids": (4, 2),
        "name_col": "父系统名称",
        "start_col": "宕机开始时间",
        "end_col": "宕机结束时间",
        "duration_cols": ("原始宕机时间", "宕机时长", "排除后实际宕机时间"),
        "reason_col": "排除原因",
        "status_col": "排除状态",
    },
]


def load_grafana_auth(yml_path):
    text = open(yml_path, encoding="utf-8").read()
    base = re.search(r"base-url:\s*(\S+)", text)
    user = re.search(r"username:\s*(\S+)", text)
    password = re.search(r"password:\s*'([^']+)'", text)
    if not (base and user and password):
        raise RuntimeError("无法从 application.yml 解析 grafana 登录信息")
    return base.group(1).rstrip("/"), user.group(1), password.group(1)


def grafana_opener(base, user, password):
    cj = http.cookiejar.CookieJar()
    opener = urllib.request.build_opener(urllib.request.HTTPCookieProcessor(cj))
    req = urllib.request.Request(
        base + "/login",
        data=json.dumps({"user": user, "password": password}).encode("utf-8"),
        headers={"Content-Type": "application/json"},
    )
    resp = opener.open(req, timeout=60)
    body = resp.read().decode("utf-8", errors="replace")
    if resp.getcode() not in (200, 302):
        raise RuntimeError("Grafana 登录失败 HTTP %s: %s" % (resp.getcode(), body[:200]))
    return opener


def fetch_dashboard(opener, base, uid):
    req = urllib.request.Request(base + "/api/dashboards/uid/" + uid)
    return json.loads(opener.open(req, timeout=60).read().decode("utf-8"))


def iter_panels(dashboard):
    """展平 panels（含 row 嵌套）。"""
    for p in dashboard.get("dashboard", {}).get("panels") or []:
        yield p
        for nested in p.get("panels") or []:
            yield nested


def query_panel_rows(opener, base, panel):
    targets = panel.get("targets") or []
    if not targets:
        return []
    t = targets[0]
    raw_sql = t.get("rawSql")
    if not raw_sql:
        return []
    ds = t.get("datasource") or {}
    payload = {
        "queries": [
            {
                "refId": "A",
                "format": "table",
                "rawQuery": True,
                "rawSql": raw_sql,
                "datasource": {"type": ds.get("type"), "uid": ds.get("uid")},
            }
        ],
        # 宽时间窗，尽量覆盖看板 SQL 全量结果
        "from": "now-5y",
        "to": "now",
    }
    req = urllib.request.Request(
        base + "/api/ds/query",
        data=json.dumps(payload).encode("utf-8"),
        headers={"Content-Type": "application/json"},
    )
    resp = json.loads(opener.open(req, timeout=300).read().decode("utf-8"))
    result = resp.get("results", {}).get("A") or {}
    if result.get("error"):
        raise RuntimeError("查询错误 panel=%s: %s" % (panel.get("id"), result.get("error")))
    frames = result.get("frames") or []
    if not frames:
        return []
    fields = frames[0].get("schema", {}).get("fields") or []
    values = frames[0].get("data", {}).get("values") or []
    if not fields or not values:
        return []
    cols = [f.get("name") for f in fields]
    row_count = len(values[0])
    rows = []
    for i in range(row_count):
        row = OrderedDict()
        for c, name in enumerate(cols):
            cell = values[c][i] if c < len(values) else None
            if cell is None:
                row[name] = ""
            else:
                row[name] = str(cell).strip()
        rows.append(row)
    return rows


def pick(row, keys):
    if isinstance(keys, str):
        keys = (keys,)
    for k in keys:
        v = row.get(k)
        if v is not None and str(v).strip() != "":
            return str(v).strip()
    return ""


def collect_rows(opener, base):
    out = []
    seen = set()
    for src in SOURCES:
        dash = fetch_dashboard(opener, base, src["dashboard_uid"])
        title = dash.get("dashboard", {}).get("title", src["dashboard_uid"])
        print("看板: %s (%s)" % (title, src["dashboard_uid"]))
        for panel in iter_panels(dash):
            if panel.get("id") not in src["panel_ids"]:
                continue
            if panel.get("type") != "table" and panel.get("type") is not None:
                # 仍尝试查询（有的 type 标记不同）
                pass
            rows = query_panel_rows(opener, base, panel)
            print("  panel %s %s -> %d 行" % (panel.get("id"), panel.get("title"), len(rows)))
            for row in rows:
                status = pick(row, src["status_col"])
                if status not in ALLOWED_STATUS:
                    continue
                name = pick(row, src["name_col"])
                start = pick(row, src["start_col"])
                end = pick(row, src["end_col"])
                duration = pick(row, src["duration_cols"])
                reason = pick(row, src["reason_col"])
                key = "|".join([src["label"], name, start, end, status, reason])
                if key in seen:
                    continue
                seen.add(key)
                out.append(
                    {
                        "系统名称": name,
                        "宕机开始时间": start,
                        "宕机结束时间": end,
                        "宕机时长": duration,
                        "排除原因": reason,
                        "_系统类别": src["label"],
                        "_排除状态": status,
                    }
                )
    return out


def write_xlsx(path, rows):
    wb = Workbook()
    # 汇总页
    ws = wb.active
    ws.title = "排除宕机明细"
    headers = ["系统名称", "宕机开始时间", "宕机结束时间", "宕机时长", "排除原因"]
    ws.append(headers)
    for r in rows:
        ws.append([r[h] for h in headers])
    for col in ws.columns:
        max_len = 0
        col_letter = col[0].column_letter
        for cell in col:
            val = "" if cell.value is None else str(cell.value)
            if len(val) > max_len:
                max_len = len(val)
        ws.column_dimensions[col_letter].width = min(max(12, max_len + 2), 60)

    # 可选：按状态拆分便于核对
    for status in ALLOWED_STATUS:
        subset = [r for r in rows if r.get("_排除状态") == status]
        name = status[:31]
        w2 = wb.create_sheet(name)
        w2.append(headers + ["系统类别", "排除状态"])
        for r in subset:
            w2.append([r[h] for h in headers] + [r["_系统类别"], r["_排除状态"]])

    wb.save(path)


def main():
    base, user, password = load_grafana_auth(YML_PATH)
    print("登录 Grafana: %s user=%s" % (base, user))
    opener = grafana_opener(base, user, password)
    print("登录成功")
    rows = collect_rows(opener, base)
    print("命中排除行（去重后）: %d" % len(rows))
    write_xlsx(OUT_PATH, rows)
    print("已写入: %s" % OUT_PATH)
    # 简单统计
    from collections import Counter
    c = Counter(r["_排除状态"] for r in rows)
    for k, v in c.items():
        print("  %s: %d" % (k, v))


if __name__ == "__main__":
    main()
