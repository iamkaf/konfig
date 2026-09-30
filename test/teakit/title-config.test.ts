import { Capability, Readiness, describe, test } from "@teakit/test";
import type { ClientScreen, ScreenListEntrySnapshot, TeaKitTestContext } from "@teakit/test";
import {
  CONFIG_SCREEN,
  atLeast,
  clickEntryControl,
  openKonfig,
  returnToTitle,
  scrollToEntry,
  scrollToVisibleEntry,
  waitForEntry,
} from "./konfig-screens";

describe.configure({
  timeout: "4m",
  readiness: [Readiness.Title],
  capabilities: [
    Capability.ClientInput,
    Capability.ClientScreen,
    Capability.ClientScreens,
    Capability.ClientScreenshot,
    Capability.RuntimeTiming,
    Capability.SpyInstrumentation,
  ],
});

describe("Konfig config screen", () => {
  test("opens the debug config screen", async (ctx) => {
    const health = await ctx.runtime.health();
    const version = health.minecraftVersion ?? "";
    const loader = health.loader ?? "";

    await ctx.client.waitForScreen("Title", { timeoutMs: 30_000 });
    await ctx.runtime.wait(3500);
    try {
      await exerciseConfigScreen(ctx, loader, version);
    } finally {
      await returnToTitle(ctx);
    }
  });
});

async function exerciseConfigScreen(ctx: TeaKitTestContext, loader: string, version: string): Promise<void> {
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

  await stepSampleLevel(ctx, version);
  await exerciseFieldset(ctx, version);
}

// Sample Level is the debug config's integer slider (0 to 10). Arrow keys step it by exactly one value.
async function stepSampleLevel(ctx: TeaKitTestContext, version: string): Promise<void> {
  const steps = await ctx.spy.method(
    "konfig.slider.steps",
    "com.iamkaf.konfig.impl.v1.client.field.KonfigFieldValues#stepInt",
  );
  try {
    const screen = await scrollToVisibleEntry(ctx, "Sample Level");
    const row = screen.lists().entries().find((entry) => entry.label.includes("Sample Level"));
    if (!row) throw new Error("Missing the Sample Level slider");
    // KonfigConfigRow right-aligns a control min(200, max(132, width / 2)) wide; clicking its middle picks 5.
    const controlWidth = Math.min(200, Math.max(132, Math.floor(row.width / 2)));
    await ctx.client.click({ x: row.x + row.width - controlWidth / 2, y: row.y + row.height / 2, button: 0 });
    await ctx.runtime.wait(200);
    if (atLeast(version, "1.19.4")) {
      // From 1.19.4 a focused slider only takes arrows in keyboard-edit mode, which Enter or Space toggles. Vanilla
      // turns it on for mouse focus but not for arrow-navigation focus. Konfig mirrors the toggle below 1.21.11, so
      // two Enters must leave the clicked slider editable. Before 1.19.4 a focused slider always takes arrows.
      await ctx.client.key(257, { release: true });
      await ctx.client.key(257, { release: true });
    }
    await ctx.client.key(262, { release: true });
    // Park the pointer in a corner so the slider tooltip does not cover the stepped value.
    await ctx.client.scroll({ x: 2, y: 2, horizontalAmount: 0, verticalAmount: 0 });
    await ctx.runtime.wait(200);
    await ctx.client.screenshot("konfig-slider-stepped");
    await ctx.client.key(263, { release: true });
    await ctx.runtime.wait(200);

    // stepInt(current, direction, min, max) returns the value the slider shows next.
    const calls = await steps.$calls();
    const [right, left] = calls;
    if (calls.length !== 2 || !right || !left) {
      throw new Error(`Expected one Right and one Left slider step, found ${JSON.stringify(calls)}`);
    }
    const start = Number(right.args?.[0]);
    if (right.args?.[1] !== 1 || right.returned !== start + 1) {
      throw new Error(`Right did not step Sample Level from ${start} to ${start + 1}: ${JSON.stringify(right)}`);
    }
    if (left.args?.[0] !== start + 1 || left.args?.[1] !== -1 || left.returned !== start) {
      throw new Error(`Left did not step Sample Level back to ${start}: ${JSON.stringify(left)}`);
    }
  } finally {
    await ctx.spy.detach(steps);
  }
}

async function exerciseFieldset(ctx: TeaKitTestContext, version: string): Promise<void> {
  let screen = await scrollToVisibleEntry(ctx, "Sample Rules");
  await clickEntryControl(ctx, screen, "Sample Rules");
  await ctx.runtime.wait(300);
  screen = await ctx.client.screen();
  assertFieldsetListScreen(screen);

  const search = screen.widgets().all().find((widget) => widget.label.includes("Search"));
  if (!search) throw new Error("Missing fieldset search input");
  await assertBuiltInFieldsDoNotTrapFocus(ctx, screen);
  screen = await ctx.client.screen();
  screen = await removeUserFieldsetRows(ctx, screen);
  assertFieldsetRows(screen, 1, 0);
  await ctx.client.screenshot("konfig-fieldset-collapsed");

  await screen.widgets().activate({ label: "Copy" });
  await ctx.runtime.wait(200);
  screen = await ctx.client.screen();
  assertExpandedFieldsetRow(screen, 1);

  await screen.widgets().activate({ label: "Add" });
  await ctx.runtime.wait(200);
  screen = await ctx.client.screen();
  assertExpandedFieldsetRow(screen, 2);

  await screen.widgets().activate({ label: "Up" });
  await ctx.runtime.wait(200);
  screen = await ctx.client.screen();
  assertExpandedFieldsetRow(screen, 1);

  await screen.widgets().activate({ label: "Delete" });
  await ctx.runtime.wait(200);
  screen = await ctx.client.screen();
  const remainingRows = assertFieldsetRows(screen, 2, 0);
  const copiedRow = remainingRows.find((row) => row.entryIndex === 1);
  if (!copiedRow) throw new Error("Missing copied sample rule after deleting the added rule");
  await clickFieldsetCardHeader(ctx, copiedRow);

  await ctx.runtime.wait(200);
  screen = await ctx.client.screen();
  const expandedCopy = assertExpandedFieldsetRow(screen, 1);
  if (screen.widgets().all().some((widget) => widget.label === "Suggest")) {
    throw new Error("Fieldset registry input still exposes the legacy Suggest button");
  }
  assertCardSummary(expandedCopy, "weapon");
  await clickInlineField(ctx, expandedCopy, 0);
  await ctx.runtime.wait(200);
  screen = await ctx.client.screen();
  // Selecting a field in a card taller than the list must not scroll the clicked field away.
  const afterItemClick = assertExpandedFieldsetRow(screen, 1);
  if (afterItemClick.y !== expandedCopy.y) {
    throw new Error(`Clicking the Item field scrolled its card from y=${expandedCopy.y} to y=${afterItemClick.y}`);
  }
  await ctx.client.screenshot("konfig-fieldset-registry-suggestions");
  await ctx.client.key(256, { release: true });
  await clickInlineField(ctx, expandedCopy, 1);
  await ctx.runtime.wait(300);
  screen = await ctx.client.screen();
  assertCardSummary(assertExpandedFieldsetRow(screen, 1), "utility");
  await ctx.client.screenshot("konfig-fieldset-edited");
  if (screen.widgets().all().some((widget) => widget.label === "Save" || widget.label === "Cancel")) {
    throw new Error("Auto-saving Fieldset screen still exposes Save or Cancel");
  }
  if (atLeast(version, "1.21.9")) await assertTabRevealsLastField(ctx);
  await screen.widgets().activate({ label: "Done" });

  await ctx.client.waitForScreen(CONFIG_SCREEN, { timeoutMs: 10_000 });
  screen = await scrollToVisibleEntry(ctx, "Sample Rules");
  await clickEntryControl(ctx, screen, "Sample Rules");
  screen = await ctx.client.waitForScreen(
    "com.iamkaf.konfig.impl.v1.client.fieldset.KonfigFieldsetListScreen",
    { timeoutMs: 10_000 },
  );

  const reopenedRows = assertFieldsetRows(screen, 2, 0);
  const reopenedCopy = reopenedRows.find((row) => row.entryIndex === 1);
  if (!reopenedCopy) throw new Error("Missing copied sample rule after reopening");
  assertCardSummary(reopenedCopy, "utility");
  await ctx.runtime.wait(300);
  await ctx.client.screenshot("konfig-fieldset-reopened");
}

// From 1.21.9 Konfig keeps a partly visible card taller than the list from scrolling when it is selected. Keyboard
// focus must still scroll the focused control into view: Tab from the clicked Role dropdown to Priority, then to
// Active, the card's last field, which starts below the list on a short window.
async function assertTabRevealsLastField(ctx: TeaKitTestContext): Promise<void> {
  await ctx.client.key(258, { release: true });
  await ctx.runtime.wait(100);
  await ctx.client.key(258, { release: true });
  await ctx.runtime.wait(300);
  const screen = await ctx.client.screen();
  const card = assertExpandedFieldsetRow(screen, 1);
  const list = screen.widgets().all().find((widget) => widget.widgetClass.endsWith("$EntryList"));
  if (!list) throw new Error("Missing the Fieldset card list widget");
  const activeTop = card.y + FIELD_CONTROL_TOP + 3 * FIELD_HEIGHT;
  const activeBottom = activeTop + CONTROL_HEIGHT;
  if (activeTop < list.y || activeBottom > list.y + list.height) {
    throw new Error(
      `Tab left the Active field at y=${activeTop}..${activeBottom} outside the list band ${list.y}..${list.y + list.height}`,
    );
  }
  await ctx.client.screenshot("konfig-fieldset-tab-revealed");
}

// KonfigFieldsetListScreen: an expanded card's fields start COLLAPSED_HEIGHT + 2 below the card's content top, each
// FIELD_HEIGHT tall, with the control 4px into its field.
const FIELD_CONTROL_TOP = 46;
const FIELD_HEIGHT = 38;
const CONTROL_HEIGHT = 20;

// A Fieldset card's row label is its title and summary ("minecraft:iron_sword, weapon  ·  4"); the summary leads with the role.
function assertCardSummary(row: ScreenListEntrySnapshot, role: string): void {
  if (!row.label.includes(`, ${role}`)) {
    throw new Error(`Expected Fieldset card ${row.entryIndex} to have role ${role}, found label ${JSON.stringify(row.label)}`);
  }
}

async function assertBuiltInFieldsDoNotTrapFocus(
  ctx: TeaKitTestContext,
  initialScreen: ClientScreen,
): Promise<void> {
  const builtIn = initialScreen.lists().entries().find((row) => row.entryIndex === 0);
  if (!builtIn) throw new Error("Missing built-in Fieldset card");
  await clickFieldsetCardHeader(ctx, builtIn);
  await ctx.runtime.wait(150);

  let screen = await ctx.client.screen();
  const expanded = assertExpandedFieldsetRow(screen, 0);
  await clickInlineField(ctx, expanded, 0);
  await clickFieldsetCardHeader(ctx, expanded);
  await ctx.runtime.wait(150);
  screen = await ctx.client.screen();
  assertFieldsetRows(screen, screen.lists().entries().length, 0);
}

async function removeUserFieldsetRows(ctx: TeaKitTestContext, initialScreen: ClientScreen): Promise<ClientScreen> {
  let screen = initialScreen;
  const startedAt = Date.now();
  while (Date.now() - startedAt < 10_000) {
    const userRow = screen.lists().entries().find((row) => row.entryIndex === 1);
    if (!userRow) return screen;

    await clickFieldsetCardHeader(ctx, userRow);
    await ctx.runtime.wait(100);
    screen = await ctx.client.screen();
    const deleteButton = screen.widgets().all().find((widget) => widget.label === "Delete");
    if (!deleteButton?.active) throw new Error("Existing user Fieldset card could not be selected for deletion");
    await screen.widgets().activate({ label: "Delete" });
    await ctx.runtime.wait(100);
    screen = await ctx.client.screen();
  }
  throw new Error("Timed out resetting saved user Fieldset cards");
}

function assertFieldsetListScreen(screen: ClientScreen): void {
  if (screen.screenClass !== "com.iamkaf.konfig.impl.v1.client.fieldset.KonfigFieldsetListScreen") {
    throw new Error(`Expected the Fieldset list screen, found ${JSON.stringify(screen)}`);
  }
}

function assertFieldsetRows(
  screen: ClientScreen,
  expectedRows: number,
  expectedExpanded: number,
): ScreenListEntrySnapshot[] {
  assertFieldsetListScreen(screen);
  const rows = screen.lists().entries();
  const expanded = rows.filter((row) => row.height > 80);
  if (rows.length !== expectedRows || expanded.length !== expectedExpanded) {
    throw new Error(
      `Expected ${expectedRows} Fieldset cards with ${expectedExpanded} expanded, found ${rows.length} cards with ${expanded.length} expanded`,
    );
  }
  return rows;
}

function assertExpandedFieldsetRow(
  screen: ClientScreen,
  expectedEntryIndex: number,
): ScreenListEntrySnapshot {
  assertFieldsetListScreen(screen);
  const rows = screen.lists().entries();
  const expandedRows = rows.filter((row) => row.height > 80);
  if (expandedRows.length !== 1) {
    throw new Error(`Expected one visible expanded Fieldset card, found ${expandedRows.length}`);
  }
  const expanded = expandedRows[0];
  if (!expanded || expanded.entryIndex !== expectedEntryIndex) {
    throw new Error(
      `Expected Fieldset card ${expectedEntryIndex} to be the sole expanded card, found ${expanded?.entryIndex ?? "none"}`,
    );
  }
  return expanded;
}

async function clickFieldsetCardHeader(ctx: TeaKitTestContext, row: ScreenListEntrySnapshot): Promise<void> {
  await ctx.client.click({
    x: row.x + row.width / 2,
    y: row.y + 20,
    button: 0,
  });
}

async function clickInlineField(
  ctx: TeaKitTestContext,
  row: ScreenListEntrySnapshot,
  fieldIndex: number,
): Promise<void> {
  await ctx.client.click({
    x: row.x + row.width * 0.75,
    y: row.y + 60 + fieldIndex * 38,
    button: 0,
  });
}
