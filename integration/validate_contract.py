#!/usr/bin/env python3
import json
import pathlib
import re
import sys

ROOT = pathlib.Path(__file__).resolve().parents[1]
contract = json.loads((ROOT / "integration" / "contract.json").read_text(encoding="utf-8"))
errors = []

def require(condition, message):
    if not condition:
        errors.append(message)

require(contract.get("contractVersion") == 1, "contractVersion must be 1")
require(contract.get("coreVersion") == "1.5.0", "coreVersion must be 1.5.0 for Foundation v1.5")
require(contract.get("contentSchemaVersion") == 1, "contentSchemaVersion must be 1")
require(contract.get("android", {}).get("minSdk") == 23, "minSdk must be 23")
require(contract.get("android", {}).get("compileSdk") == 36, "compileSdk must be 36")
require(contract.get("android", {}).get("javaVersion") == 17, "javaVersion must be 17")

coordinate = contract.get("coordinates", {}).get("core", "")
require(coordinate == f"com.asdevelopers.academy:core:{contract.get('coreVersion')}", "Core coordinate/version mismatch")

root_build = (ROOT / "build.gradle.kts").read_text(encoding="utf-8")
require(f'version = "{contract.get("coreVersion")}"' in root_build, "Root Gradle version differs from foundation contract")

build = (ROOT / "core" / "build.gradle.kts").read_text(encoding="utf-8")
require(re.search(r"compileSdk\s*=\s*36", build) is not None, "core compileSdk differs from contract")
require(re.search(r"minSdk\s*=\s*23", build) is not None, "core minSdk differs from contract")
require("JavaVersion.VERSION_17" in build, "core Java version differs from contract")
require("compose" not in build.lower(), "Core must not own Compose/UI dependencies")

rules = contract.get("architectureRules", {})
require(rules.get("backendImplementationOwnedByCore") is True, "Backend ownership must remain in Core")
require(rules.get("singleRuntimeOwner") == "AS-Academy-Core", "Core must remain the single runtime owner")

runtime_path = ROOT / "core" / "src" / "main" / "kotlin" / "com" / "asdevelopers" / "academy" / "core" / "runtime" / "AcademyRuntime.kt"
require(runtime_path.exists(), "Core must expose AcademyRuntime as the single composition root")
if runtime_path.exists():
    runtime = runtime_path.read_text(encoding="utf-8")
    require("AcademyDatabase.create(" in runtime, "AcademyRuntime must own database composition")
    require("AcademyPreferencesRepository(" in runtime, "AcademyRuntime must own preferences composition")
    require("StudyReminderScheduler(" in runtime, "AcademyRuntime must own scheduler composition")
    require("val backend: AcademyBackend" in runtime, "AcademyRuntime must own the backend abstraction")
    require("OfflineAcademyBackend" in runtime, "AcademyRuntime must provide an offline-safe backend default")

core_source = ROOT / "core" / "src" / "main" / "kotlin" / "com" / "asdevelopers" / "academy" / "core"
ui_source = core_source / "ui"
require(not ui_source.exists() or not any(ui_source.rglob("*.kt")), "Core must not contain presentation/UI Kotlin sources")
require(not (core_source / "navigation" / "AcademyNavigation.kt").exists(), "Compose navigation belongs to MainUi, not Core")

backend_path = core_source / "backend" / "AcademyBackend.kt"
require(backend_path.exists(), "Core must expose a backend gateway for provider implementations")

if errors:
    print("Foundation contract validation failed:")
    for error in errors:
        print(f" - {error}")
    sys.exit(1)

print("Foundation contract OK: Core 1.5.0 is runtime-only and owns platform/backend composition")
