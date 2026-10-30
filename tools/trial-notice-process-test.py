"""T08: run against an already installed activepolicy APK on a disposable emulator.

The instrumentation invocation only seeds the fixture. Both restoration cycles run
without target instrumentation, with the real MainActivity and Android's saved task.
"""

import argparse
import json
import re
import subprocess
import time
from pathlib import Path


PACKAGE = "com.finnvek.knittools.activepolicytest"
ACTIVITY = f"{PACKAGE}/com.finnvek.knittools.MainActivity"
TITLE = "Your Pro Trial Has Ended"


def require(condition, message):
    if not condition:
        raise AssertionError(message)


def proto_fields(data):
    """Read the varint/length fields used by AndroidX Preferences' bool/long map.

    Schema: datastore-preferences-proto/src/main/proto/preferences.proto.
    Reject other wire types: this fixture contains only bool and long preferences.
    """
    offset = 0

    def varint():
        nonlocal offset
        value = 0
        for shift in range(0, 70, 7):
            byte = data[offset]
            offset += 1
            value |= (byte & 127) << shift
            if byte < 128:
                return value
        raise AssertionError("Malformed protobuf varint")

    while offset < len(data):
        tag = varint()
        if tag & 7 == 0:
            value = varint()
        else:
            require(tag & 7 == 2, "Unexpected trial preference wire type")
            length = varint()
            value = data[offset:offset + length]
            require(len(value) == length, "Truncated preference")
            offset += length
        yield tag >> 3, value


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--serial", required=True)
    parser.add_argument("--avd", required=True, help="Task-owned disposable AVD name")
    parser.add_argument("--output", required=True, type=Path)
    args = parser.parse_args()
    require(args.serial.startswith("emulator-"), "Only an emulator is allowed")
    args.output.mkdir(parents=True, exist_ok=True)

    def adb(*command, binary=False, allow_missing=False):
        with (args.output / "commands.txt").open("a", encoding="utf-8") as log:
            log.write(f"adb -s {args.serial} {' '.join(command)}\n")
        result = subprocess.run(
            ["adb", "-s", args.serial, *command], capture_output=True, timeout=90,
        )
        require(allow_missing or result.returncode == 0, result.stderr.decode(errors="replace"))
        return result.stdout if binary else result.stdout.decode("utf-8", errors="replace").strip()

    def save(name, value):
        (args.output / name).write_text(value, encoding="utf-8")

    def wait_for(description, probe, timeout=40):
        deadline = time.monotonic() + timeout
        while time.monotonic() < deadline:
            value = probe()
            if value:
                return value
            time.sleep(0.5)
        raise AssertionError(f"Timed out: {description}")

    def layout(name):
        path = args.output / f"{name}.json"
        subprocess.run(
            ["android", "layout", "--device", args.serial, "--flat", "--full", "--output", str(path)],
            check=True, capture_output=True, timeout=45,
        )
        windows = json.loads(path.read_text(encoding="utf-8"))
        return [node for window in windows for node in window["content"]]

    def visible_text(nodes, text):
        return [node for node in nodes if node.get("text") == text and not node.get("off-screen", False)]

    def wait_text(text, name):
        return wait_for(text, lambda: visible_text(layout(name), text))

    def tap(node):
        center = node["center"]
        if isinstance(center, str):
            center = [int(value) for value in re.findall(r"\d+", center)]
        adb("shell", "input", "tap", *map(str, center))

    def preferences(name, claimed):
        data = adb("exec-out", "run-as", PACKAGE, "cat", "files/datastore/trial_state.preferences_pb", binary=True)
        (args.output / f"{name}.preferences_pb").write_bytes(data)
        values = {}
        for field, entry in proto_fields(data):
            require(field == 1, "Unexpected Preferences map field")
            item = dict(proto_fields(entry))
            value_fields = list(proto_fields(item[2]))
            require(len(value_fields) == 1 and value_fields[0][0] in (1, 4), "Expected bool or long")
            values[item[1].decode()] = value_fields[0][1]
        require(bool(values.get("trial_end_notice_shown", 0)) == claimed, "Unexpected persisted notice claim")
        require(values.get("clock_tampered", 0) == 0, "Fixture must expire normally, without clock tampering")
        require(values["trial_elapsed_duration_millis"] >= 15 * 86_400_000, "Trial is not expired")
        save(f"{name}-preferences.json", json.dumps(values, indent=2))
        return values

    def activity(name):
        dump = adb("shell", "dumpsys", "activity", "activities")
        save(f"{name}-activities.txt", dump)
        blocks = re.split(r"(?=^[ \t]*\* (?:Hist\s+#\d+:|Task\{))", dump, flags=re.MULTILINE)
        for block in blocks:
            header = re.match(r"\s*\* Hist\s+#\d+: ActivityRecord\{[^\n]+", block)
            if header and ACTIVITY in header[0]:
                task = re.search(r"\bt(\d+)\b", header[0])
                require(task is not None, "Missing retained task identity")
                return int(task[1]), block
        return None

    def pid():
        return adb("shell", "pidof", PACKAGE, allow_missing=True)

    require(adb("shell", "getprop", "ro.kernel.qemu") == "1", "Not an emulator")
    require(adb("emu", "avd", "name").splitlines()[0] == args.avd, "Wrong disposable emulator")
    require(adb("shell", "getprop", "sys.boot_completed") == "1", "Emulator has not booted")
    package_dump = adb("shell", "dumpsys", "package", PACKAGE)
    requested = package_dump.split("requested permissions:")[1].split("install permissions:")[0]
    require("android.permission.INTERNET" not in requested, "Test package must be networkless")
    require("com.android.vending.BILLING" not in requested, "Test package must not use Play Billing")
    require(adb("shell", "pm", "clear", PACKAGE) == "Success", "Could not reset synthetic fixture")
    seeded = adb(
        "shell", "am", "instrument", "-w", "-r", "-e", "trialProcessSeed", "true",
        "-e", "class", "com.finnvek.knittools.pro.TrialNoticeProcessSeedTest",
        f"{PACKAGE}.test/androidx.test.runner.AndroidJUnitRunner",
    )
    save("seed.log", seeded)
    require("OK (1 test)" in seeded, "Expired trial seed failed")
    initial_preferences = preferences("seed", claimed=False)
    adb("shell", "am", "start", "-W", "-a", "android.intent.action.MAIN", "-c", "android.intent.category.LAUNCHER", "-n", ACTIVITY)
    wait_text(TITLE, "initial-dialog")
    initial_pid = pid()
    require(initial_pid.isdigit(), "Expected one standalone app process")
    initial_task, _ = activity("initial")
    preferences("initial-claimed", claimed=True)
    cycles = []
    for name, pending in (("pending", True), ("dismissed", False)):
        old_pid = pid()
        adb("shell", "input", "keyevent", "KEYCODE_HOME")

        def saved_activity():
            current = activity(f"{name}-saved")
            if current and "state=STOPPED" in current[1] and "mHaveState=true" in current[1] and "mIcicle=Bundle[" in current[1]:
                require(current[0] == initial_task, "Task changed before termination")
                return current
            return None

        wait_for("Android saved the stopped Activity", saved_activity)
        before = preferences(f"{name}-before-kill", claimed=True)
        adb("shell", "am", "kill", PACKAGE)
        wait_for("old process ended", lambda: not pid())
        processes = adb("shell", "ps", "-A", "-o", "PID,NAME")
        require(not re.search(rf"^\s*{old_pid}\s", processes, re.MULTILINE), "Old PID is still alive")
        save(f"{name}-process-absent.txt", f"old_pid={old_pid}\npidof=empty\nold_pid_in_ps=false\n")
        retained_task, retained = activity(f"{name}-after-kill")
        require(retained_task == initial_task and "app=null" in retained and "mHaveState=true" in retained, "Saved task did not survive")
        require(preferences(f"{name}-after-kill", claimed=True) == before, "Persisted trial changed while the process was absent")
        adb("shell", "input", "keyevent", "KEYCODE_APP_SWITCH")
        cards = wait_for(
            "KnitTools retained Recents card",
            lambda: [node for node in layout(f"{name}-recents") if node.get("content-desc", "").startswith("KnitTools")],
        )
        tap(cards[0])
        wait_text(TITLE if pending else "No active projects", f"{name}-restored-ui")
        new_pid = pid()
        require(new_pid.isdigit() and new_pid != old_pid, "No actual process replacement")
        restored_task, restored = activity(f"{name}-restored")
        require(restored_task == initial_task and "state=RESUMED" in restored, "Did not resume the retained task")
        restored_preferences = preferences(f"{name}-restored", claimed=True)
        require(restored_preferences["trial_start_timestamp"] == initial_preferences["trial_start_timestamp"], "Trial was reseeded")
        require(restored_preferences["trial_anchor_elapsed_realtime_millis"] > before["trial_anchor_elapsed_realtime_millis"], "Real trial initialization did not advance its persisted anchor")
        cycles.append({"phase": name, "old_pid": old_pid, "new_pid": new_pid, "task": restored_task, "claim_consumed": True})
        if pending:
            tap(wait_text("Continue free", "continue-free")[0])
            wait_text("No active projects", "dismissed-ui")
        require(not visible_text(layout(f"{name}-no-dialog"), TITLE), "Notice reappeared after consumption")
    # Recheck after initialization has settled; absence alone is insufficient, so Projects was required above.
    time.sleep(3)
    require(not visible_text(layout("final-no-dialog"), TITLE), "Consumed notice returned")
    preferences("final", claimed=True)
    save("result.json", json.dumps({"result": "PASS", "serial": args.serial, "avd": args.avd, "initial_pid": initial_pid, "cycles": cycles}, indent=2))
    print(json.dumps({"result": "PASS", "cycles": cycles}))


if __name__ == "__main__":
    main()
