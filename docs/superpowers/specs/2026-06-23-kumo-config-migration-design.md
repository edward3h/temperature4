# Migrate kumojs `config` tool to Java

## Context

The `kumo` module already migrated kumojs's device-control functions (`kumoCmd`/`kumoServer`) into Java (commit `794f709`, "internalize kumo device control"). The one remaining piece still implemented only in the external `kumojs` npm package is the `config` tool (`kumoconfig` / `npm run config`): an interactive CLI that logs into Kumo Cloud and downloads the per-device credentials (`serial`, `cryptoSerial`, `password`, `address`, etc.) needed to talk to each heat pump unit locally, writing them to `kumo.cfg`.

This was prompted by a live incident where a router reboot reshuffled DHCP leases among the heat pump units, desyncing `kumo.cfg`'s hardcoded addresses from reality. Fixing that required manually re-running the legacy Node tool and diffing/copying its output into the project by hand. Migrating `config` into Java removes the last dependency on the `kumojs` Node project and its toolchain for ongoing maintenance of this app.

## Goals

- Re-implement the Kumo Cloud login + config-download flow in Java, matching the legacy tool's behaviour (interactive masked credential prompt, writes `./kumo.cfg`).
- Drop the `module.exports = ...;` JavaScript-module wrapper the legacy tool emits — now that nothing reads this file as a JS module, it can be pure JSON. `KumoConfigParser` is simplified accordingly (no more wrapper-stripping logic).
- Keep the new tool decoupled from the existing `kumo` library module — it shares no code with it, only the file format.

## Non-goals

- No integration test against the real Kumo Cloud login endpoint (storing real account credentials for an automated test isn't worth the risk for a tool run by hand occasionally).
- No automated deployment of the regenerated `kumo.cfg` to the production host — that remains a manual step, same as today.
- No change to `kumo.cfg`'s per-device field set (`S`/`W` constants stay hardcoded, matching the legacy tool exactly).

## Architecture

A new Gradle module, `kumo-config`, sibling to `app` and `kumo` (added to `settings.gradle`). It uses the `application` plugin since it needs a `main()` entry point — unlike `kumo`, which stays a pure library. It depends on neither `app` nor `kumo`; it only needs `avaje-jsonb` (already used elsewhere in the project) and Java's built-in `HttpClient`. The only thing it shares with `kumo` is the JSON file format, not code.

## Components

- **`KumoCloudClient`** — POSTs `{"username":..,"password":..,"appVersion":"2.2.0"}` to `https://geo-c.kumocloud.com/login` using `java.net.http.HttpClient`. Returns the raw JSON response body on success. Throws `KumoCloudException` distinguishing transport failure (couldn't reach the cloud) from a rejected login (non-2xx status).

- **`KumoCloudConfigParser`** — Parses the raw response body with avaje-jsonb's streaming `JsonReader` (matching the existing house style in `KumoConfigParser.java`, not a generic Map/Object deserializer). Mirrors the legacy `kumoConfig.ts`'s `processcfg`/`parsechildren`/`parsezone` recursion: response is `[ {username:...}, ..., {children: {...}} ]`; each entry in `children` may have a `zoneTable` (map of zone-id → device fields) and its own nested `children`, recursed indefinitely. Returns the account name plus a `List<KumoCloudDevice>`. Throws a clear "unexpected response from Kumo Cloud" exception if the expected shape isn't found, rather than a raw `NullPointerException`.

- **`KumoCloudDevice`** (record) — `serial, label, cryptoSerial, cryptoKeySet, password, address`, all as the raw strings returned by the cloud (not decoded bytes — `KumoConfigParser` does that decoding when the running app later reads the file).

- **`KumoConfigWriter`** — Given the account name and `List<KumoCloudDevice>`, writes pure JSON to a file: `{"account@email":{"serial1":{...},"serial2":{...}}}`, injecting the constant `S=0` and the fixed `W` hex value into each device entry. Overwrites the target file unconditionally, matching legacy behaviour (no confirmation prompt).

- **`KumoConfigMain`** — CLI glue. Prompts `Enter username:` (plain read), then `Enter password:` (masked, via `System.console().readPassword()`). Calls the three components above in sequence. Writes to `./kumo.cfg`. Prints `Downloaded config for <n> devices. Written to ./kumo.cfg` on success.

## Data flow & error handling

1. `KumoConfigMain` reads username, then masked password from the console.
2. `KumoCloudClient.login(...)` returns the raw response body, or throws — caught in `KumoConfigMain`, reported as a one-line message to stderr (no stack trace for these expected failure modes), then `System.exit(1)`.
3. `KumoCloudConfigParser.parse(body)` returns account name + device list, or throws on unexpected shape (same handling as step 2).
4. `KumoConfigWriter.write(Path.of("kumo.cfg"), accountName, devices)` serializes and overwrites the file.
5. Success message printed; exit 0.

## Existing-code changes

- **`KumoConfigParser.java`**: remove the `module.exports =` / trailing `;` stripping logic (lines ~21-26 today) — read the file as pure JSON directly.
- **`KumoConfigParserTest.java`**: update fixtures to pure JSON (drop the wrapper from test input strings).
- **One-time data migration**: existing `kumo.cfg` files (this repo's `app/src/main/resources/kumo.cfg`, and the copy deployed on `eternal.local`) must be converted to pure JSON (drop the first `module.exports = ` line — there's no trailing `;` in the current file to worry about) before the simplified parser can read them. The repo copy will be converted as part of this implementation; the `eternal.local` copy remains the user's manual responsibility at next deploy, same as the address fix from the earlier incident.

## Testing

- **`KumoCloudConfigParserTest`** — fixture JSON shaped like a real cloud response (nested `children`/`zoneTable`), asserts extracted account name + device list. No network.
- **`KumoConfigWriterTest`** — round-trip: write a known device list, read it back with the simplified `KumoConfigParser`, assert equality. Mirrors the existing `KumoConfigParserTest`/`KumoCryptoTest` pairing style.
- `KumoCloudClient` request-building may get a unit test against a mocked/local HTTP endpoint if useful; no live-cloud integration test (see Non-goals).

## Verification

- `./gradlew :kumo-config:test :kumo:test` passes.
- Manual run: `./gradlew :kumo-config:installDist`, run the generated script from `kumo-config/build/install/kumo-config/bin/kumo-config` against real Kumo Cloud credentials, confirm the output `kumo.cfg` matches the shape currently produced by the legacy `kumojs` tool (modulo the dropped wrapper), and that the existing `KumoIntegrationTest` (gated, requires real `kumo.cfg`) still passes against it.
- Note: if invoking via `./gradlew :kumo-config:run`, the module's `build.gradle.kts` must set `standardInput = System.in` on the `run` task, or the password prompt will hang/fail — Gradle doesn't forward stdin by default.
