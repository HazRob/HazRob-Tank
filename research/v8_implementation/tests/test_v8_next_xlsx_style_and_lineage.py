#!/usr/bin/env python3
"""Inspect physical XLSX OOXML produced by Android production exporters.

Checks the user's approved Excel formatting, as emitted by the same V7
SmartXlsxPackage used for historical reports: dark header, pale blue section,
warning/critical fill, numeric formatting, RTL, column widths, frozen header
and full inline UTF-8 text.  No Excel renderer or guessed workbook cells.
This CI checks test-only archived sessions; it is not Tank G3.
"""
from pathlib import Path
from hashlib import sha256
from zipfile import ZipFile, is_zipfile
import sys
import xml.etree.ElementTree as ET

N = "{http://schemas.openxmlformats.org/spreadsheetml/2006/main}"
BASE = Path(sys.argv[1] if len(sys.argv)>1 else "/tmp/v6-n06g-approved")
TARGETS = {
    "n06g_full_approved_field.xlsx": (20, {"V6 بحثي غازات","V6 مصادر وتعارضات",
        "V8 البيئة الأصلية","V8 ADC ADS الأصلية","V8 القنوات المساعدة","V8 JSON كامل"}),
    "n06g_next_approved_field.xlsx": (20, {"المرشحون Next","مصادر Next المستقلة",
        "عقد Next","تشخيص الخبراء","خريطة الأدلة Next",
        "V8 البيئة الأصلية","V8 ADC ADS الأصلية","V8 القنوات المساعدة","V8 JSON كامل"}),
    "n06g_next_site_two_approved.xlsx": (6, {"تكرار مصادر Next",
        "أدلة Next والجودة","المنهجية والمصدر"})
}
styles = {}
for filename, (expected_count, expected_sheets) in TARGETS.items():
    path = BASE / filename
    assert path.exists() and is_zipfile(path), path
    with ZipFile(path) as z:
        assert z.testzip() is None
        entries = [name for name in z.namelist()
                   if name.startswith("xl/worksheets/sheet") and name.endswith(".xml")]
        assert len(entries) == expected_count, (filename, len(entries))
        workbook = ET.fromstring(z.read("xl/workbook.xml"))
        names = [x.get("name") for x in workbook.findall(".//"+N+"sheet")]
        assert len(names) == expected_count
        assert expected_sheets.issubset(set(names)), (filename, expected_sheets-set(names))
        theme = z.read("xl/styles.xml")
        styles[filename] = sha256(theme).hexdigest()
        t = ET.fromstring(theme)
        fonts = t.find(N+"fonts")
        fills = t.find(N+"fills")
        borders = t.find(N+"borders")
        xfs = t.find(N+"cellXfs")
        fmts = t.find(N+"numFmts")
        assert len(fonts) == 3
        assert len(fills) == 6
        assert len(borders) == 2
        assert len(xfs) == 8
        assert {e.get("numFmtId") for e in fmts} >= {"164","165"}
        assert b"FF263238" in theme and b"FFE3F2FD" in theme
        assert b"FFFFF3E0" in theme and b"FFFFEBEE" in theme
        assert b"FFCFD8DC" in theme
        total_numeric_cells, total_header_cells = 0, 0
        for name in entries:
            raw = z.read(name)
            node = ET.fromstring(raw)
            view = node.find(".//"+N+"sheetView")
            assert view is not None and view.get("rightToLeft") == "1", (filename,name)
            pane = node.find(".//"+N+"pane")
            assert pane is not None and pane.get("ySplit") == "1", (filename,name)
            cols = node.find(N+"cols")
            assert cols is not None and len(cols)>0, (filename,name)
            assert all(float(c.get("width"))>=7 for c in cols)
            for cell in node.findall(".//"+N+"c"):
                style = int(cell.get("s","0"))
                assert 0<=style<8, (filename,name,style)
                if style==1: total_header_cells += 1
                if cell.get("t") == "n": total_numeric_cells += 1
        assert total_header_cells>30 and total_numeric_cells>10, (
            filename,total_header_cells,total_numeric_cells)
        all_text=b"\n".join(z.read(n) for n in entries)
        if filename.startswith("n06g_next_approved_field"):
            assert "CI-RESEARCH-ROOT-ONE".encode() in all_text
            assert b"RESEARCH_CANDIDATES" in all_text
            assert b"NEXT-" in all_text
            assert b"screening_144" not in all_text
            assert b"Top ranking weight" not in all_text
        if filename.startswith("n06g_next_site"):
            assert b"LEGACY_ARCHIVE" in all_text
            assert b"CHEMCORE_NEXT" in all_text
            assert "CI-RESEARCH-ROOT-ONE".encode() in all_text
            assert b"RESEARCH_CANDIDATES" in all_text
        print("PASS",filename,"sheets",len(names),
              "numeric cells",total_numeric_cells,"header cells",total_header_cells,
              "style SHA256",styles[filename][:18])
assert len(set(styles.values()))==1, "Original approved XLSX styles must remain identical"
print("PASS: original approved Excel styling identical across legacy, Next field and mixed Next site")
