# -*- coding: utf-8 -*-
"""
匹配 SLA 明细报表（新/老版）地区 与 Zabbix 企微群告警统计，
导出 SLA 相关地区的告警数量汇总。

数据源:
  - SLA 老版: afj5c5o5v3rb4f / aftk5ryifcfeof  panel 4  → zabbix6 看板 cftqea6n7x98ge
  - SLA 新版: 同上 panel 2                         → zabbix7 看板 dftqths109wcgc
  - 告警面板: 「各企微群总体统计」(id=4)，表 wecom_alert_202607（7 月）

用法:
  python export_sla_region_alarms.py
输出:
  doc/SLA地区告警统计-2026-07.xlsx
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
OUT_PATH = os.path.join(SCRIPT_DIR, "SLA地区告警统计-2026-07.xlsx")

# 企微群名 → 用于在 SLA 系统名中匹配的关键词（可多条，命中任一即可）
REGION_ALIASES = {
    "金华全地区": ["金华"],
    "河北雄安": ["雄安"],
    "海南省": ["海南"],
    "山东省": ["山东"],
    "山西省": ["山西"],  # 含大同/忻州/朔州/晋中等省内名时另靠别名不够，见 EXTRA
    "山西省本级": ["山西省本级", "山西省级"],
    "陕西省本级": ["陕西省本级", "陕西"],
    "广州开评标": ["广州"],
    "广东江门": ["江门"],
    "平潭二期国产化": ["平潭"],
    "贵州国产化": ["贵州"],
    "贵州互联网": ["贵州"],
    "天津国产化": ["天津"],
    "乌鲁木齐开评标-信创": ["乌鲁木齐"],
    "上海一期": ["上海"],
    "西藏机器管": ["西藏机器管", "西藏住建机器管"],
    "西藏一张网": ["西藏一张网"],
    "北京亦庄": ["亦庄"],
    "北京通州": ["通州"],
    "北京门头沟": ["门头沟"],
    "北京海淀": ["海淀"],
    "北京石景山": ["石景山"],
    "北京大兴": ["大兴"],
    "北京昌平": ["昌平"],
    "北京顺义": ["顺义"],
    "石家庄": ["石家庄"],
    "成都": ["成都"],
    "杭州": ["杭州", "杭州标办", "西湖景区"],
    "宁波": ["宁波"],
    "沈阳": ["沈阳"],
    "福建": ["福建", "平潭"],  # 福建群可能覆盖平潭；若不想合并可删平潭
}

# 山西省告警群：除「山西省本级」外，省内地市也算 SLA 命中
SHANXI_CITY_KEYWORDS = ["大同", "忻州", "朔州", "晋中"]

PAIRS = [
    {
        "version": "老版监控",
        "zabbix": "zabbix6",
        "alarm_uid": "cftqea6n7x98ge",
        "sla_uids": [
            ("子系统", "afj5c5o5v3rb4f", "子系统名称"),
            ("父系统", "aftk5ryifcfeof", "父系统名称"),
        ],
        "sla_panel_id": 4,
    },
    {
        "version": "新版监控",
        "zabbix": "zabbix7",
        "alarm_uid": "dftqths109wcgc",
        "sla_uids": [
            ("子系统", "afj5c5o5v3rb4f", "子系统名称"),
            ("父系统", "aftk5ryifcfeof", "父系统名称"),
        ],
        "sla_panel_id": 2,
    },
]


def load_auth(yml_path):
    text = open(yml_path, encoding="utf-8").read()
    base = re.search(r"base-url:\s*(\S+)", text).group(1).rstrip("/")
    user = re.search(r"username:\s*(\S+)", text).group(1)
    password = re.search(r"password:\s*'([^']+)'", text).group(1)
    return base, user, password


def opener_login(base, user, password):
    cj = http.cookiejar.CookieJar()
    op = urllib.request.build_opener(urllib.request.HTTPCookieProcessor(cj))
    op.open(
        urllib.request.Request(
            base + "/login",
            data=json.dumps({"user": user, "password": password}).encode("utf-8"),
            headers={"Content-Type": "application/json"},
        ),
        timeout=60,
    )
    return op


def fetch_dashboard(op, base, uid):
    return json.loads(op.open(base + "/api/dashboards/uid/" + uid, timeout=60).read().decode("utf-8"))


def find_panel(dash, panel_id):
    for p in dash.get("dashboard", {}).get("panels") or []:
        if p.get("id") == panel_id:
            return p
        for n in p.get("panels") or []:
            if n.get("id") == panel_id:
                return n
    raise KeyError("panel %s not found" % panel_id)


def query_table(op, base, panel):
    t = panel["targets"][0]
    ds = t["datasource"]
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
        "from": "now-1h",
        "to": "now",
    }
    resp = json.loads(
        op.open(
            urllib.request.Request(
                base + "/api/ds/query",
                data=json.dumps(payload).encode("utf-8"),
                headers={"Content-Type": "application/json"},
            ),
            timeout=300,
        )
        .read()
        .decode("utf-8")
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
        row = OrderedDict()
        for c, name in enumerate(cols):
            v = values[c][i]
            row[name] = "" if v is None else v
        rows.append(row)
    return rows


def region_keywords(region):
    region = (region or "").strip()
    if not region:
        return []
    if region in REGION_ALIASES:
        keys = list(REGION_ALIASES[region])
    else:
        keys = [region]
        stripped = region
        for suf in ("全地区", "国产化", "一期", "二期国产化", "开评标-信创", "开评标", "互联网", "省"):
            if stripped.endswith(suf) and len(stripped) > len(suf):
                stripped = stripped[: -len(suf)]
                break
        stripped = stripped.strip("-")
        if stripped and stripped not in keys:
            keys.append(stripped)
    if region in ("山西省", "山西省本级"):
        keys = list(keys) + SHANXI_CITY_KEYWORDS
    # 长词优先
    keys = sorted(set(k for k in keys if k), key=len, reverse=True)
    return keys


def match_sla_names(region, sla_names):
    keys = region_keywords(region)
    hits = []
    for name in sla_names:
        for k in keys:
            if k and k in name:
                hits.append(name)
                break
    return hits


def collect_sla_names(op, base, pair):
    names = []
    for level, uid, name_col in pair["sla_uids"]:
        dash = fetch_dashboard(op, base, uid)
        panel = find_panel(dash, pair["sla_panel_id"])
        rows = query_table(op, base, panel)
        print("  SLA %s/%s panel=%s rows=%d" % (pair["version"], level, pair["sla_panel_id"], len(rows)))
        for r in rows:
            n = r.get(name_col)
            if n is None:
                continue
            n = str(n).strip()
            if n:
                names.append(n)
    # unique keep order
    seen = set()
    uniq = []
    for n in names:
        if n not in seen:
            seen.add(n)
            uniq.append(n)
    return uniq


def collect_alarms(op, base, alarm_uid):
    dash = fetch_dashboard(op, base, alarm_uid)
    panel = find_panel(dash, 4)
    rows = query_table(op, base, panel)
    print("  告警看板 %s 企微群=%d" % (alarm_uid, len(rows)))
    out = []
    for r in rows:
        region = str(r.get("企微群或地区") or "").strip()
        if not region:
            continue
        out.append(
            {
                "企微群或地区": region,
                "告警总数": int(r.get("告警总数") or 0),
                "已处理": int(r.get("已处理") or 0),
                "未处理": int(r.get("未处理") or 0),
                "已处理占比": str(r.get("已处理占比") or ""),
                "动作对应主机组": str(r.get("动作对应主机组") or ""),
            }
        )
    return out


def main():
    base, user, password = load_auth(YML_PATH)
    print("登录 Grafana %s ..." % base)
    op = opener_login(base, user, password)
    print("登录成功")

    matched_rows = []
    unmatched_rows = []

    for pair in PAIRS:
        print("处理 %s (%s)" % (pair["version"], pair["zabbix"]))
        sla_names = collect_sla_names(op, base, pair)
        print("  SLA 系统数=%d" % len(sla_names))
        alarms = collect_alarms(op, base, pair["alarm_uid"])
        for a in alarms:
            hits = match_sla_names(a["企微群或地区"], sla_names)
            rec = {
                "监控版本": pair["version"],
                "Zabbix": pair["zabbix"],
                "企微群或地区": a["企微群或地区"],
                "告警总数": a["告警总数"],
                "已处理": a["已处理"],
                "未处理": a["未处理"],
                "已处理占比": a["已处理占比"],
                "匹配SLA系统数": len(hits),
                "匹配关键词": "、".join(region_keywords(a["企微群或地区"])),
                "SLA系统样例": "、".join(hits[:5]),
            }
            if hits:
                matched_rows.append(rec)
            else:
                unmatched_rows.append(rec)

    total_alarm = sum(r["告警总数"] for r in matched_rows)
    total_handled = sum(r["已处理"] for r in matched_rows)
    total_unhandled = sum(r["未处理"] for r in matched_rows)

    print("匹配地区数=%d 总告警=%d 总已处理=%d 总未处理=%d" % (
        len(matched_rows), total_alarm, total_handled, total_unhandled))
    print("未匹配企微群=%d" % len(unmatched_rows))

    wb = Workbook()
    ws = wb.active
    ws.title = "SLA匹配地区告警"
    headers = [
        "监控版本", "Zabbix", "企微群或地区", "告警总数", "已处理", "未处理",
        "已处理占比", "匹配SLA系统数", "匹配关键词", "SLA系统样例",
    ]
    ws.append(headers)
    for r in sorted(matched_rows, key=lambda x: (-x["告警总数"], x["监控版本"], x["企微群或地区"])):
        ws.append([r[h] for h in headers])

    ws2 = wb.create_sheet("汇总")
    ws2.append(["指标", "数值"])
    ws2.append(["统计月份", "2026-07"])
    ws2.append(["SLA相关地区数（匹配企微群数）", len(matched_rows)])
    ws2.append(["SLA相关地区总告警数量", total_alarm])
    ws2.append(["SLA相关地区总已处理数量", total_handled])
    ws2.append(["SLA相关地区总未处理数量", total_unhandled])
    if total_alarm:
        ws2.append(["总已处理占比", "%.2f%%" % (100.0 * total_handled / total_alarm)])
    else:
        ws2.append(["总已处理占比", "-"])
    ws2.append([])
    ws2.append(["按版本小计", ""])
    ws2.append(["监控版本", "匹配地区数", "告警总数", "已处理", "未处理"])
    for ver in ("老版监控", "新版监控"):
        sub = [r for r in matched_rows if r["监控版本"] == ver]
        ws2.append([
            ver,
            len(sub),
            sum(r["告警总数"] for r in sub),
            sum(r["已处理"] for r in sub),
            sum(r["未处理"] for r in sub),
        ])

    ws3 = wb.create_sheet("未匹配企微群")
    ws3.append(headers)
    for r in unmatched_rows:
        ws3.append([r[h] for h in headers])

    for sheet in wb.worksheets:
        for col in sheet.columns:
            letter = col[0].column_letter
            width = 12
            for cell in col:
                if cell.value is not None:
                    width = max(width, min(len(str(cell.value)) + 2, 50))
            sheet.column_dimensions[letter].width = width

    wb.save(OUT_PATH)
    print("已写入: %s" % OUT_PATH)


if __name__ == "__main__":
    main()
