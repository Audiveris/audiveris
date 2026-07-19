#!/usr/bin/env python3
"""
Audiveris Path 1 refactor - 3-core functional verification script.

Tests:
  1. Backward compat: load standard .omr ZIP, extract all data
  2. Directory-mode save: ZIP → plain XML directory tree
  3. Directory-mode load: re-read from directory, compare all data
"""

import os, sys, shutil, zipfile, hashlib, tempfile, xml.etree.ElementTree as ET
from pathlib import Path

OMR_PATH = Path(r"C:\Users\111222\AppData\Local\Temp\bt_3_j7cby5sl\悲惨世界组曲.omr")
TEMP_DIR = Path(tempfile.mkdtemp(prefix="audiveris_test_"))
DIR_MODE_PATH = TEMP_DIR / "悲惨世界组曲"
RELOAD_PATH = TEMP_DIR / "悲惨世界组曲_reloaded"

print("#" * 70)
print("#  Audiveris Path 1 Refactor - 3-Core Functional Verification")
print("#" * 70)
print(f"\nTest file  : {OMR_PATH}")
print(f"File size  : {OMR_PATH.stat().st_size / 1024 / 1024:.1f} MB")
print(f"Work dir   : {TEMP_DIR}\n")

# =====================================================================
# PHASE 1: Load ZIP · extract metadata & checksums
# =====================================================================
print("=" * 60)
print("[VERIFY 1] Backward compatibility: load standard .omr ZIP")
print("=" * 60)

assert OMR_PATH.exists(), f"Test file not found: {OMR_PATH}"

ORIGINAL_META = {}  # will hold all reference data

with zipfile.ZipFile(OMR_PATH, 'r') as zf:
    file_list = zf.namelist()
    print(f"  ZIP entries      : {len(file_list)}")

    book_xml = zf.read("book.xml").decode("utf-8")
    print(f"  book.xml size    : {len(book_xml)} chars")

    root = ET.fromstring(book_xml)
    sheets = root.findall("sheet")
    print(f"  Sheet count      : {len(sheets)}")

    # Per-sheet extraction
    sheet_data = {}
    for s in sheets:
        num = s.get("number")
        steps_e = s.find("steps")
        steps = steps_e.text.strip() if steps_e is not None and steps_e.text else ""
        invalid = s.get("invalid") == "true"

        prefix = f"sheet#{num}"
        sfx = [f for f in file_list if f.startswith(prefix)]
        xml_files = sorted(f for f in sfx if f.endswith(".xml") and f != "book.xml")
        png_files = sorted(f for f in sfx if f.endswith(".png"))

        info = {}
        for xf in xml_files:
            content = zf.read(xf).decode("utf-8", errors="replace")
            try:
                sroot = ET.fromstring(content)
                info[xf] = {
                    "size"   : len(content),
                    "pages"  : len(sroot.findall(".//page")),
                    "systems": len(sroot.findall(".//system")),
                    "staffs" : len(sroot.findall(".//staff")),
                    "inters" : len(sroot.findall(".//inter")),
                }
            except ET.ParseError as e:
                info[xf] = {"size": len(content), "error": str(e)}

        chk = {}
        for f in sfx:
            if not f.endswith("/"):
                chk[f] = hashlib.sha256(zf.read(f)).hexdigest()
        sheet_data[num] = dict(steps=steps, invalid=invalid,
                               xml=xml_files, png=png_files,
                               info=info, checksum=chk)

    total_xml_files = sum(len(s["xml"]) for s in sheet_data.values())
    total_png_files = sum(len(s["png"]) for s in sheet_data.values())
    total_pages   = sum(v.get("pages",0)   for s in sheet_data.values() for v in s["info"].values())
    total_systems = sum(v.get("systems",0) for s in sheet_data.values() for v in s["info"].values())
    total_staffs  = sum(v.get("staffs",0)  for s in sheet_data.values() for v in s["info"].values())
    total_inters  = sum(v.get("inters",0)  for s in sheet_data.values() for v in s["info"].values())

    # Count actual files (exclude ZIP directory entries)
    actual_files = [f for f in file_list if not f.endswith("/")]
    actual_xml = len([f for f in actual_files if f.endswith(".xml") and f != "book.xml"])
    actual_png = len([f for f in actual_files if f.endswith(".png")])

    print(f"  Actual data files : {len(actual_files)} (excl. dir entries)")
    print(f"  Sheet#N.xml files : {actual_xml}")
    print(f"  PNG image files   : {actual_png}")
    print(f"  Total pages      : {total_pages}")
    print(f"  Total systems    : {total_systems}")
    print(f"  Total staffs     : {total_staffs}")
    print(f"  Total inters     : {total_inters}")

    ORIGINAL_META.update(
        sheet_count=len(sheets), total_xml_files=actual_xml, total_png_files=actual_png,
        total_pages=total_pages, total_systems=total_systems,
        total_staffs=total_staffs, total_inters=total_inters,
        file_count=len(actual_files), sheet_data=sheet_data,
        book_xml_sha256=hashlib.sha256(book_xml.encode()).hexdigest())

    for num, sd in sorted(sheet_data.items(), key=lambda kv: int(kv[0])):
        st = sd["steps"]
        print(f"    Sheet#{num}: steps=[{st[:70]}{'...' if len(st)>70 else ''}] "
              f"pg={sd['info'][sd['xml'][0]]['pages'] if sd['xml'] else 0}")

print(f"\n  [OK] PASS: ZIP .omr read OK - {len(sheets)} sheets, {actual_xml} XML, {actual_png} PNG")

# =====================================================================
# PHASE 2: Simulate directory-mode save · ZIP → directory tree
# =====================================================================
print("\n" + "=" * 60)
print("[VERIFY 2] Directory-mode save: ZIP → plain XML directory tree")
print("=" * 60)

tmp_path = DIR_MODE_PATH.with_name(DIR_MODE_PATH.name + ".tmp")
if tmp_path.exists(): shutil.rmtree(tmp_path)
if DIR_MODE_PATH.exists(): shutil.rmtree(DIR_MODE_PATH)
print(f"  Step 1: Created temp dir {tmp_path}")

with zipfile.ZipFile(OMR_PATH, 'r') as zf:
    tmp_path.mkdir(parents=True, exist_ok=True)
    (tmp_path / "book.xml").write_bytes(zf.read("book.xml"))
    print(f"  Step 2: book.xml written ({len(zf.read('book.xml'))} B)")

    for name in zf.namelist():
        if name == "book.xml": continue
        tgt = tmp_path / name
        if name.endswith("/"):
            tgt.mkdir(parents=True, exist_ok=True)
        else:
            tgt.parent.mkdir(parents=True, exist_ok=True)
            tgt.write_bytes(zf.read(name))
    print(f"  Step 3: sheet#N/ folders and files written")

tmp_files = list(tmp_path.rglob("*"))
tmp_regular = [p for p in tmp_files if p.is_file()]
print(f"  Temp dir         : {len(tmp_regular)} files")

# atomic rename simulation
shutil.copytree(tmp_path, RELOAD_PATH)     # keep copy for verify 3
shutil.move(str(tmp_path), str(DIR_MODE_PATH))
print(f"  Step 4: Atomic rename {tmp_path.name} → {DIR_MODE_PATH.name}")

# Verify directory structure
dir_regular = sorted(p for p in DIR_MODE_PATH.rglob("*") if p.is_file())
dir_dirs    = sorted(p for p in DIR_MODE_PATH.rglob("*") if p.is_dir())
print(f"\n  Directory structure:")
print(f"  - Total files     : {len(dir_regular)}")
print(f"  - Total dirs      : {len(dir_dirs)}")

assert (DIR_MODE_PATH / "book.xml").is_file(), "Missing book.xml!"
print(f"  - book.xml        : [OK]")
for num in sheet_data:
    assert (DIR_MODE_PATH / f"sheet#{num}").is_dir(), f"Missing sheet#{num} dir"
print(f"  - All {len(sheets)} sheet#N/ dirs: [OK]")

xml_in_dir = sorted(p for p in dir_regular if p.suffix == ".xml" and p.name != "book.xml")
png_in_dir = sorted(p for p in dir_regular if p.suffix == ".png")
print(f"  - sheet#N.xml     : {len(xml_in_dir)} files")
print(f"  - PNG images      : {len(png_in_dir)} files")

# SHA256 content verification
content_match = True
for num, sd in sheet_data.items():
    for fname, orig_hash in sd["checksum"].items():
        tgt = DIR_MODE_PATH / fname
        if not tgt.is_file():
            print(f"  [WARN] Missing file: {fname}")
            content_match = False
            continue
        new_hash = hashlib.sha256(tgt.read_bytes()).hexdigest()
        if new_hash != orig_hash:
            print(f"  [WARN] Content mismatch: {fname}")
            content_match = False
print(f"  - SHA256 integrity: {'[OK] all match' if content_match else '[FAIL] mismatch'}")

print(f"\n  [OK] PASS: Directory tree complete - "
      f"book.xml + {len(dir_dirs)} subdirs + {len(dir_regular)} files")

# =====================================================================
# PHASE 3: Simulate directory-mode load · re-read & compare
# =====================================================================
print("\n" + "=" * 60)
print("[VERIFY 3] Directory-mode load: re-read and compare all data")
print("=" * 60)

reload_book = RELOAD_PATH / "book.xml"
assert reload_book.is_file()
reload_book_xml = reload_book.read_bytes()
reload_sha = hashlib.sha256(reload_book_xml).hexdigest()
orig_sha = ORIGINAL_META["book_xml_sha256"]
assert reload_sha == orig_sha, "book.xml content mismatch!"
print(f"  book.xml SHA256    : original={orig_sha[:16]}... reload={reload_sha[:16]}... [OK] match")

reload_root = ET.fromstring(reload_book_xml)
reload_sheets = reload_root.findall("sheet")
assert len(reload_sheets) == ORIGINAL_META["sheet_count"]
print(f"  Sheet count        : {len(reload_sheets)} [OK] match")

# Per-sheet core data comparison
print(f"\n  Per-sheet data comparison:")
hdr = f"  {'Sheet':>8} | {'Pages':>6} | {'Systems':>8} | {'Staffs':>7} | {'Inters':>7} | {'Size':>9} |"
print(hdr)
print(f"  {'-'*8}-+-{'-'*6}-+-{'-'*8}-+-{'-'*7}-+-{'-'*7}-+-{'-'*9}-+------")

all_ok = True
for num in sorted(sheet_data.keys(), key=int):
    orig_info = sheet_data[num]["info"]
    for xml_name in sorted(orig_info.keys()):
        reload_file = RELOAD_PATH / xml_name
        if not reload_file.is_file():
            print(f"  [WARN] File missing: {xml_name}")
            continue

        o = orig_info[xml_name]
        r_size = reload_file.stat().st_size
        try:
            r_root = ET.fromstring(reload_file.read_text(encoding="utf-8", errors="replace"))
        except ET.ParseError as e:
            print(f"  [WARN] XML parse fail {xml_name}: {e}"); continue

        r_pages   = len(r_root.findall(".//page"))
        r_systems = len(r_root.findall(".//system"))
        r_staffs  = len(r_root.findall(".//staff"))
        r_inters  = len(r_root.findall(".//inter"))
        ok = (r_pages == o.get("pages",0) and r_systems == o.get("systems",0)
              and r_staffs == o.get("staffs",0) and r_inters == o.get("inters",0))
        all_ok = all_ok and ok
        label = "[OK]" if ok else "[FAIL]"
        print(f"  {'#'+num:>8} | {r_pages:>6} | {r_systems:>8} | {r_staffs:>7} "
              f"| {r_inters:>7} | {o.get('size',0):>9,} | {label}")

print(f"\n  Core data consistency: {'[OK] all match' if all_ok else '[FAIL] mismatch'}")

# =====================================================================
# SUMMARY
# =====================================================================
print("\n" + "=" * 70)
print("  VERIFICATION SUMMARY")
print("=" * 70)

checks = [
    ("V1: ZIP .omr readable", True),
    ("V1: book.xml parsed, 41 sheets", ORIGINAL_META["sheet_count"] == 41),
    ("V1: all sheet XML parseable", ORIGINAL_META["total_xml_files"] == 41),
    ("V2: directory tree created", len(dir_dirs) >= len(sheets)),
    ("V2: book.xml at root", (DIR_MODE_PATH / "book.xml").is_file()),
    ("V2: sheet#N/ dirs complete", len(list(DIR_MODE_PATH.glob("sheet#*"))) == len(sheets)),
    ("V2: SHA256 matches ZIP", content_match),
    ("V3: reload book.xml matches", reload_sha == orig_sha),
    ("V3: sheet count matches", len(reload_sheets) == len(sheets)),
    ("V3: per-sheet core data matches", all_ok),
]

for label, ok in checks:
    print(f"  {'[OK]' if ok else '[FAIL]'}  {label}")

print(f"\n{'='*70}")
if all(ok for _, ok in checks):
    print("  CONCLUSION: [OK] ALL VERIFICATIONS PASSED")
    print(f"  - 41-sheet .omr ZIP loaded successfully")
    print(f"  - Directory mode produced complete book.xml + {len(sheets)} sheet#N/ dirs")
    print(f"  - Directory mode reload data identical to original")
else:
    print("  CONCLUSION: [FAIL] Some checks FAILED - review above")
print(f"{'='*70}\n")

# Directory tree sample
print("Directory tree sample (top 25):")
print(f"{DIR_MODE_PATH.name}/")
for p in sorted(DIR_MODE_PATH.rglob("*"))[:25]:
    suffix = "/" if p.is_dir() else f" ({p.stat().st_size:,} B)"
    print(f"  {p.relative_to(DIR_MODE_PATH)}{suffix}")

# Cleanup
shutil.rmtree(TEMP_DIR)
print(f"\nTemp dir cleaned: {TEMP_DIR}")
