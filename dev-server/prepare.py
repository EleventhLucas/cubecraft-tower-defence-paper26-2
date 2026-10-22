"""Prepare an isolated local playtest, preserving existing settings and worlds."""
import hashlib
import importlib.util
from pathlib import Path
import shutil
import urllib.request
import zipfile

ROOT = Path(__file__).resolve().parents[1]
SERVER = ROOT / "server"
PAPER_SHA = "3cce0a4922baf79bd8e1275ee5ff9f073f24675770e09b0d4be9442d513e7a6b"
FARM_SHA = "28d24136afe80358b89556d0fbe3b08d00e518af38c7c518819fa7fc225e613e"


def download(url, target, checksum):
    if target.exists():
        if hashlib.sha256(target.read_bytes()).hexdigest() != checksum:
            raise RuntimeError(f"Unexpected checksum for {target}; inspect it before replacing it.")
        return
    print(f"Downloading {target.name}...", flush=True)
    request = urllib.request.Request(url, headers={"User-Agent": "CubeCraftTD-local-playtest/1.0"})
    target.parent.mkdir(parents=True, exist_ok=True)
    temporary = target.with_suffix(target.suffix + ".part")
    try:
        with urllib.request.urlopen(request, timeout=90) as response, temporary.open("wb") as output:
            shutil.copyfileobj(response, output)
        if hashlib.sha256(temporary.read_bytes()).hexdigest() != checksum:
            raise RuntimeError(f"Download checksum mismatch: {target.name}")
        temporary.replace(target)
    finally:
        temporary.unlink(missing_ok=True)


def main():
    SERVER.mkdir(exist_ok=True)
    # Same fixed Paper build and map bytes as the upstream network test harness.
    download(f"https://fill-data.papermc.io/v1/objects/{PAPER_SHA}/paper-26.2-134.jar",
             SERVER / "paper.jar", PAPER_SHA)
    farm = SERVER / "plugins/CubeCraftTowerDefence/maps/ImprovedFarm.schem"
    download("https://drive.google.com/uc?export=download&id=1fCAVGVhO4otbywapp_Ah3EjVRX14foNt",
             farm, FARM_SHA)
    spec = importlib.util.spec_from_file_location("pack", ROOT / "scripts/package-playtest.py")
    pack = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(pack)
    pack.audit_farm(farm.read_bytes())

    # Pre-fetch Paper's verified vanilla server payload; no server/EULA acceptance yet.
    with zipfile.ZipFile(SERVER / "paper.jar") as jar:
        sha, url, name = jar.read("META-INF/download-context").decode().strip().split("\t")
    if Path(name).name != name:
        raise RuntimeError("Unexpected Paper cache filename")
    download(url, SERVER / "cache" / name, sha)

    # Gradle stages the current shadowJar, independently of version/old artifacts.
    plugin = ROOT / "build/local-playtest/CubeCraftTowerDefence.jar"
    with zipfile.ZipFile(plugin) as jar:
        if "kotlin/Unit.class" not in jar.namelist():
            raise RuntimeError("Expected the shaded plugin with its Kotlin runtime")
    shutil.copy2(plugin, SERVER / "plugins/CubeCraftTowerDefence.jar")
    properties = SERVER / "server.properties"
    if not properties.exists():
        properties.write_text(
            "server-ip=127.0.0.1\nserver-port=25566\n"
            "level-name=ctd-playtest\nlevel-type=minecraft:flat\n"
            "generate-structures=false\ngamemode=creative\ndifficulty=normal\n"
            "online-mode=true\nmax-players=16\nspawn-protection=0\n"
            "view-distance=8\nsimulation-distance=6\n"
            "motd=CubeCraft TD local playtest - Minecraft 26.2\n"
        )
    print("Prepared Paper 26.2 build 134 and the verified Farm asset in server/.")


if __name__ == "__main__":
    main()
