import type { ClientScreen, LoaderId, TeaKitTestContext } from "@teakit/test";

// Screen helpers shared by the Konfig UI tests, which all start from the title screen.

export const CONFIG_SCREEN = "com.iamkaf.konfig.impl.v1.client.screen.KonfigConfigScreen";

export async function openKonfig(ctx: TeaKitTestContext, loader: LoaderId | string, version: string): Promise<ClientScreen> {
  let screen = await ctx.client.screen();
  await screen.widgets().activate({ label: "Mods", contains: true });
  await ctx.runtime.wait(800);
  screen = await ctx.client.screen();

  screen = await ctx.client.waitForScreen("Mods", { timeoutMs: 5_000 });
  if (loader === "neoforge" && !atLeast(version, "26.3")) {
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

/** Presses Escape until the title screen is showing again, so the next test starts from a clean screen stack. */
export async function returnToTitle(ctx: TeaKitTestContext): Promise<void> {
  for (let attempt = 0; attempt < 8; attempt += 1) {
    const screen = await ctx.client.screen();
    if (screen.screenClass?.endsWith("TitleScreen")) return;
    await ctx.client.key(256, { release: true });
    await ctx.runtime.wait(300);
  }
  await ctx.client.waitForScreen("Title", { timeoutMs: 5_000 });
}

async function selectKonfig(ctx: TeaKitTestContext): Promise<ClientScreen> {
  const startedAt = Date.now();
  while (Date.now() - startedAt < 5_000) {
    const screen = await ctx.client.screen();
    const entries = screen.lists().entries();
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

export async function activateFabricConfigure(screen: ClientScreen, version: string) {
  if (screen.widgets().all().some((widget) => widget.label === "Configure...")) {
    await screen.widgets().activate({ label: "Configure..." });
    return;
  }
  if (atMost(version, "1.19.2")) {
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

export async function clickEntryControl(ctx: TeaKitTestContext, screen: ClientScreen, label: string): Promise<void> {
  const entry = screen.lists().entries().find((candidate) => candidate.label.includes(label));
  if (!entry) throw new Error(`Missing Konfig entry control: ${label}`);
  await ctx.client.click({
    x: entry.x + entry.width * 0.75,
    y: entry.y + entry.height / 2,
    button: 0,
  });
}

// KonfigConfigScreen lays its list out from KonfigScreenMetrics.LIST_TOP to height - LIST_BOTTOM_MARGIN and puts the
// Done button at height - 26 (rebuildScreenWidgets).
const CONFIG_LIST_TOP = 28;
const CONFIG_LIST_BOTTOM_MARGIN = 52;
const CONFIG_DONE_BUTTON_OFFSET = 26;

export async function scrollToVisibleEntry(ctx: TeaKitTestContext, label: string): Promise<ClientScreen> {
  const startedAt = Date.now();
  while (Date.now() - startedAt < 10_000) {
    const screen = await ctx.client.screen();
    const band = configListBand(screen);
    const entry = screen.lists().entries().find((candidate) => candidate.label.includes(label));
    if (entry && entry.y >= band.top && entry.y + entry.height <= band.bottom) {
      return screen;
    }
    await screen.scroll({ vertical: entry && entry.y < band.top ? 2 : -2 });
    await ctx.runtime.wait(100);
  }
  throw new Error(`Timed out scrolling to visible Konfig entry: ${label}`);
}

function configListBand(screen: ClientScreen): { top: number; bottom: number } {
  const list = screen.widgets().all().find((widget) => widget.widgetClass.includes("KonfigEntryList"));
  if (list) return { top: list.y, bottom: list.y + list.height };
  // Selection lists are not widgets before 1.20.3. The snapshot has no screen height, so derive it from Done.
  const done = screen.widgets().all().find((widget) => widget.label === "Done");
  if (!done) throw new Error("Missing the Konfig config screen Done button");
  const screenHeight = done.y + CONFIG_DONE_BUTTON_OFFSET;
  return { top: CONFIG_LIST_TOP, bottom: screenHeight - CONFIG_LIST_BOTTOM_MARGIN };
}

export async function waitForEntry(ctx: TeaKitTestContext, label: string): Promise<ClientScreen> {
  const startedAt = Date.now();
  while (Date.now() - startedAt < 10_000) {
    const screen = await ctx.client.screen();
    if (screen.lists().entries().some((entry) => entry.label.includes(label))) return screen;
    await ctx.runtime.wait(100);
  }
  throw new Error(`Timed out waiting for Konfig entry: ${label}`);
}

export async function scrollToEntry(ctx: TeaKitTestContext, label: string): Promise<ClientScreen> {
  const startedAt = Date.now();
  while (Date.now() - startedAt < 10_000) {
    const screen = await ctx.client.screen();
    if (screen.lists().entries().some((entry) => entry.label.includes(label))) return screen;
    await screen.scroll({ vertical: -2 });
    await ctx.runtime.wait(100);
  }
  throw new Error(`Timed out scrolling to Konfig entry: ${label}`);
}

export function atLeast(actual: string, expected: string): boolean {
  return compareVersions(actual, expected) >= 0;
}

export function atMost(actual: string, expected: string): boolean {
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
