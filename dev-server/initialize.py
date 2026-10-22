"""Use the plugin's managed-world installer once, then shut down cleanly."""
import importlib.util
from pathlib import Path
import re
import shutil
import signal

ROOT = Path(__file__).resolve().parents[1]
SERVER = ROOT / "server"
manifest = SERVER / "plugins/CubeCraftTowerDefence/managed-farm.yml"
command = "ctdinstall create 1"
if manifest.exists():
    match = re.search(r"^phase: ['\"]?([A-Z_]+)", manifest.read_text(), re.MULTILINE)
    if not match:
        raise SystemExit("Cannot read managed Farm phase. Inspect server/plugins/CubeCraftTowerDefence/managed-farm.yml.")
    if match[1] != "CREATED":
        raise SystemExit(0)
    command = "ctdinstall configure 1"

spec = importlib.util.spec_from_file_location("deployment", ROOT / "scripts/verify-farm-deployment.py")
deployment = importlib.util.module_from_spec(spec)
spec.loader.exec_module(deployment)
print("First-run setup: creating the Farm installation for 1v1. This can take a few minutes.", flush=True)
print(f"Setup log: {SERVER / 'deployment-boot-local-install.log'}", flush=True)
server = None


def interrupted(signum, frame):
    raise KeyboardInterrupt("Local server setup interrupted")


signal.signal(signal.SIGTERM, interrupted)
try:
    server = deployment.PaperProcess(Path(shutil.which("java")), SERVER / "paper.jar", SERVER, "local-install")
    server.command(command, "Managed Farm configured for 1v1", timeout=90)
    server.stop()
    server = None
    print("Farm configured. Restarting into the playable server; watch for Managed Farm READY.", flush=True)
finally:
    if server is not None:
        server.abort()
