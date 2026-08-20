import { Capability, Readiness, describe, test } from "@teakit/test";
import type { ClientScreen, LoaderId, TeaKitTestContext } from "@teakit/test";

describe.configure({
  timeout: "4m",
  readiness: [Readiness.Title],
  capabilities: [
    Capability.ClientInput,
    Capability.ClientScreen,
    Capability.ClientScreens,
    Capability.ClientScreenshot,
    Capability.RuntimeTiming,
  ],
});

describe("Konfig config screen", () => {
  test("opens the debug config screen", async (ctx) => {
    const health = await ctx.runtime.health();
    const version = health.minecraftVersion ?? "";
    const loader = health.loader ?? "";

    await ctx.client.waitForScreen("Title", { timeoutMs: 30_000 });
    await ctx.runtime.wait(3500);
    let screen = await openKonfig(ctx, loader, version);

    for (const label of [
      "Konfig Debug Settings",
      "These entries exist to test Konfig's own screen",
      "Konfig Documentation",
      "Debug Mode",
    ]) {
      await waitForEntry(ctx, label);
    }

    await screen.lists().entry({ label: "Debug Mode" }).activate();
    await ctx.runtime.wait(300);
    await ctx.client.screenshot("konfig-debug-dropdown-open");
    screen = await ctx.client.screen();
    const debugModeEntry = screen.lists().entries()
      .find((entry) => entry.label.includes("Debug Mode"));
    if (!debugModeEntry) throw new Error("Missing debug mode entry");
    await ctx.client.click({
      x: debugModeEntry.x + debugModeEntry.width * 0.75,
      y: debugModeEntry.y - 20,
      button: 0,
    });
    await ctx.runtime.wait(200);

    const tooltipScreen = await scrollToEntry(ctx, "Enable Debug Logging");
    const tooltipEntry = tooltipScreen.lists().entries()
      .find((entry) => entry.label.includes("Enable Debug Logging"));
    if (!tooltipEntry) throw new Error("Missing translated tooltip test entry");
    await ctx.client.scroll({
      x: tooltipEntry.x + tooltipEntry.width / 2,
      y: tooltipEntry.y + tooltipEntry.height / 2,
      horizontalAmount: 0,
      verticalAmount: 0,
    });
    await ctx.runtime.wait(300);
    await ctx.client.screenshot("konfig-translated-value-tooltip");
  });
});

async function openKonfig(ctx: TeaKitTestContext, loader: LoaderId | string, version: string): Promise<ClientScreen> {
  let screen = await ctx.client.screen();
  await screen.widgets().activate({ label: "Mods", contains: true });
  await ctx.runtime.wait(800);
  screen = await ctx.client.screen();

  if (loader === "fabric" && atMost(version, "1.16.5")) {
    if (screen.screenClass === "com.iamkaf.konfig.fabric.KonfigLegacyModsScreen") {
      await screen.widgets().activate({ label: "Configure...", nth: 0 });
      return ctx.client.waitForScreen("com.iamkaf.konfig.fabric.KonfigConfigScreen", { timeoutMs: 10_000 });
    }
    screen = await selectKonfig(ctx);
    await screen.widgets().activate({ label: "Configure...", nth: 0 });
    return ctx.client.waitForScreen("com.iamkaf.konfig.fabric.KonfigConfigScreen", { timeoutMs: 10_000 });
  }

  screen = await ctx.client.waitForScreen("Mods", { timeoutMs: 5_000 });
  if (loader === "neoforge") {
    await screen.widgets().activate({ label: "Z-A", nth: 0 });
    await ctx.runtime.wait(300);
    screen = await ctx.client.screen();
  }
  if (loader === "forge" && atMost(version, "1.18.2")) {
    await screen.lists("mod_list").entry({ label: "Konfig", nth: 0 }).activate();
    await ctx.runtime.wait(200);
    screen = await ctx.client.screen();
  } else {
    screen = await selectKonfig(ctx);
  }

  if (loader === "fabric") {
    await activateFabricConfigure(screen, version);
  } else if (loader === "forge" || loader === "neoforge") {
    await screen.widgets().activate({ label: "Config", nth: 0 });
  } else {
    throw new Error(`Unsupported Konfig test runtime: ${version}-${loader}`);
  }
  await ctx.runtime.wait(800);
  return ctx.client.screen();
}

async function selectKonfig(ctx: TeaKitTestContext): Promise<ClientScreen> {
  const startedAt = Date.now();
  while (Date.now() - startedAt < 5_000) {
    const screen = await ctx.client.screen();
    const entries = screen.lists("mod_list").entries();
    const konfig = entries.find((entry) => entry.label.includes("Konfig"));
    if (konfig?.selected) return screen;
    if (!konfig) throw new Error("Missing Konfig in the mod list");
    await ctx.client.click({
      x: konfig.x + konfig.width / 2,
      y: konfig.y + konfig.height / 2,
      button: 0,
    });
    await ctx.runtime.wait(200);
  }
  throw new Error("Timed out selecting Konfig in the mod list");
}

async function activateFabricConfigure(screen: ClientScreen, version: string) {
  if (atLeast(version, "1.17") && atMost(version, "1.19.2")) {
    await screen.widgets().activate({ label: "Configure...", nth: 0 });
    return;
  }
  if (atLeast(version, "1.19.3") && atMost(version, "1.20.2")) {
    await screen.widgets().activate({ widgetClass: "com.terraformersmc.modmenu.gui.ModsScreen$1", nth: 0 });
    return;
  }
  await screen.widgets().activate({
    widgetClass: "com.terraformersmc.modmenu.gui.widget.LegacyTexturedButtonWidget",
    nth: 1,
  });
}

async function waitForEntry(ctx: TeaKitTestContext, label: string): Promise<ClientScreen> {
  const startedAt = Date.now();
  while (Date.now() - startedAt < 10_000) {
    const screen = await ctx.client.screen();
    if (screen.lists().entries().some((entry) => entry.label.includes(label))) return screen;
    await ctx.runtime.wait(100);
  }
  throw new Error(`Timed out waiting for Konfig entry: ${label}`);
}

async function scrollToEntry(ctx: TeaKitTestContext, label: string): Promise<ClientScreen> {
  const startedAt = Date.now();
  while (Date.now() - startedAt < 10_000) {
    const screen = await ctx.client.screen();
    if (screen.lists().entries().some((entry) => entry.label.includes(label))) return screen;
    await screen.scroll({ vertical: -2 });
    await ctx.runtime.wait(100);
  }
  throw new Error(`Timed out scrolling to Konfig entry: ${label}`);
}

function atLeast(actual: string, expected: string): boolean {
  return compareVersions(actual, expected) >= 0;
}

function atMost(actual: string, expected: string): boolean {
  return compareVersions(actual, expected) <= 0;
}

function compareVersions(left: string, right: string): number {
  const a = left.split(/[.-]/).map((part) => Number.parseInt(part, 10) || 0);
  const b = right.split(/[.-]/).map((part) => Number.parseInt(part, 10) || 0);
  for (let index = 0; index < Math.max(a.length, b.length); index += 1) {
    const difference = (a[index] ?? 0) - (b[index] ?? 0);
    if (difference !== 0) return difference;
  }
  return 0;
}
