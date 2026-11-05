#!/usr/bin/env python3
"""The 1.3.3 pack's chain lab (packs/Veinminer/dev/lab/lab.py, same world, cameras, probe, takes in its .work/) pointed at the
Veinminer 2.0 mod, so the pack and the mod are filmed and measured with identical cameras:

  python3 mods/Veinminer/dev/lab/lab_mod.py rec mod --jar <veinminer-*.jar> --shots small:fp,cave:fp
  python3 mods/Veinminer/dev/lab/lab_mod.py compare pack mod
"""
import importlib.util
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[4]
PACK_LAB = ROOT / "packs/Veinminer/dev/lab/lab.py"
spec = importlib.util.spec_from_file_location("pack_lab", PACK_LAB)
pack = importlib.util.module_from_spec(spec)
sys.path.insert(0, str(PACK_LAB.parent))
spec.loader.exec_module(pack)

pack.LAB.exclude_mods = ["Veinminer-*.jar", "veinminer-*.jar", "EnchantedVeinminer-*.jar"]
# the mod needs no scoreboard setup; its defaults equal the pack's
pack.LAB.setup = [s for s in pack.LAB.setup if "veinminer.welcomed" not in s]

if __name__ == "__main__":
    pack.L.main(pack.LAB)
