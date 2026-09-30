import { Capability, Readiness, describe, test } from "@teakit/test";
import type { ClientScreen, ScreenListEntrySnapshot, TeaKitTestContext } from "@teakit/test";
import { CONFIG_SCREEN, clickEntryControl, openKonfig, returnToTitle, scrollToVisibleEntry } from "./konfig-screens";

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

const CATALOG_SCREEN = "com.iamkaf.konfig.impl.v1.client.fieldset.KonfigFieldsetCatalogScreen";
const BUILT_IN_PROFILE = "Konfig Samples";
const USER_PROFILE = "User Rules";
const BUILT_IN_RULES = ["minecraft:iron_sword", "minecraft:shears"];
// KonfigDebugConfig gives new sample catalog rules this item and the "tool" role.
const NEW_RULE = "minecraft:iron_pickaxe";

describe("Konfig Fieldset catalog", () => {
  test("saves an added and edited user rule across reopens without touching built-in rules", async (ctx) => {
    const health = await ctx.runtime.health();
    const version = health.minecraftVersion ?? "";
    const loader = health.loader ?? "";

    await ctx.client.waitForScreen("Title", { timeoutMs: 30_000 });
    await ctx.runtime.wait(3500);
    try {
      await openKonfig(ctx, loader, version);
      let screen = await openCatalog(ctx);
      screen = await deleteUserRules(ctx, screen);
      assertProfiles(screen);
      await ctx.client.waitForFrames(3);
      await ctx.client.screenshot("konfig-catalog-profiles");

      await screen.widgets().activate({ label: "New Rule" });
      screen = await waitForActiveWidget(ctx, "Delete");
      await ctx.client.waitForFrames(3);
      await ctx.client.screenshot("konfig-catalog-new-entry");

      // The role dropdown cycles tool -> weapon -> utility.
      await cycleRole(ctx, screen, "weapon");
      await ctx.client.waitForFrames(3);
      await ctx.client.screenshot("konfig-catalog-edited");

      // Reopening reads the rule back from the config value, so the added rule must still be listed.
      screen = await reopenCatalog(ctx);
      screen = await openProfile(ctx, USER_PROFILE);
      const saved = assertRuleLabels(screen, [NEW_RULE]);
      await clickRow(ctx, saved[0]!);
      screen = await waitForActiveWidget(ctx, "Delete");
      await ctx.client.waitForFrames(3);
      await ctx.client.screenshot("konfig-catalog-reopened");
      // weapon -> utility only happens if the reopened rule kept the edited role.
      await cycleRole(ctx, screen, "utility");

      screen = await ctx.client.screen();
      await screen.widgets().activate({ label: "Delete" });
      await ctx.runtime.wait(300);
      screen = await ctx.client.screen();
      assertRuleLabels(screen, []);

      screen = await reopenCatalog(ctx);
      screen = await openProfile(ctx, BUILT_IN_PROFILE);
      assertRuleLabels(screen, BUILT_IN_RULES);
      // The role filter cycles All -> weapon -> utility -> All over the built-in rules' roles.
      assertRuleLabels(await cycleFilter(ctx, "Role: weapon"), [BUILT_IN_RULES[0]!]);
      await ctx.client.waitForFrames(3);
      await ctx.client.screenshot("konfig-catalog-filtered");
      assertRuleLabels(await cycleFilter(ctx, "Role: utility"), [BUILT_IN_RULES[1]!]);
      assertRuleLabels(await cycleFilter(ctx, "Role: All"), BUILT_IN_RULES);
      screen = await backToProfiles(ctx);
      screen = await openProfile(ctx, USER_PROFILE);
      assertRuleLabels(screen, []);
    } finally {
      await returnToTitle(ctx);
    }
  });
});

/**
 * Clicks the selected rule's role dropdown once and asserts it saved `expected`. Catalog detail controls are not
 * screen widgets, so the saved value is read from the field row that applied it.
 */
async function cycleRole(ctx: TeaKitTestContext, screen: ClientScreen, expected: string): Promise<void> {
  const role = rowsOf(screen, "DropdownFieldRow")[0];
  if (!role) throw new Error("Missing the role control in the rule's detail");
  const applied = await ctx.spy.method(
    "konfig.catalog.apply",
    "com.iamkaf.konfig.impl.v1.client.fieldset.KonfigFieldsetCatalogScreen$FieldRow#apply",
  );
  try {
    await ctx.client.click({ x: role.x + role.width * 0.75, y: role.y + 14, button: 0 });
    await ctx.runtime.wait(300);
    const calls = await applied.$calls();
    if (calls.length !== 1 || calls[0]?.args?.[0] !== expected || calls[0]?.returned !== true) {
      throw new Error(`Expected the role control to save ${expected} once, found ${JSON.stringify(calls)}`);
    }
  } finally {
    await ctx.spy.detach(applied);
  }
}

/** Clicks the rule filter button once and returns the screen after it shows `expected`. */
async function cycleFilter(ctx: TeaKitTestContext, expected: string): Promise<ClientScreen> {
  let screen = await ctx.client.screen();
  const filter = screen.widgets().all().find((widget) => widget.label.startsWith("Role: "));
  if (!filter) throw new Error("Missing the catalog role filter button");
  await screen.widgets().activate({ label: filter.label });
  await ctx.runtime.wait(200);
  screen = await ctx.client.screen();
  const labels = screen.widgets().all().map((widget) => widget.label);
  if (!labels.includes(expected)) {
    throw new Error(`Expected the filter button to read ${expected}, found ${JSON.stringify(labels)}`);
  }
  return screen;
}

/** Leaves the catalog with Done and opens it again from the config screen. */
async function reopenCatalog(ctx: TeaKitTestContext): Promise<ClientScreen> {
  const screen = await ctx.client.screen();
  await screen.widgets().activate({ label: "Done" });
  await ctx.client.waitForScreen(CONFIG_SCREEN, { timeoutMs: 10_000 });
  const catalog = await openCatalog(ctx);
  assertProfiles(catalog);
  return catalog;
}

async function openCatalog(ctx: TeaKitTestContext): Promise<ClientScreen> {
  const screen = await scrollToVisibleEntry(ctx, "Sample Catalog");
  await clickEntryControl(ctx, screen, "Sample Catalog");
  const catalog = await ctx.client.waitForScreen(CATALOG_SCREEN, { timeoutMs: 10_000 });
  if (catalog.screenClass !== CATALOG_SCREEN) {
    throw new Error(`Expected the Fieldset catalog screen, found ${catalog.screenClass}`);
  }
  await ctx.runtime.wait(200);
  return ctx.client.screen();
}

/** Removes user rules left by an earlier run, then returns to the profile overview. */
async function deleteUserRules(ctx: TeaKitTestContext, overview: ClientScreen): Promise<ClientScreen> {
  let screen = overview;
  if (!rowsOf(screen, "ProfileRow").some((row) => row.label === USER_PROFILE)) return screen;
  screen = await openProfile(ctx, USER_PROFILE);
  for (let attempt = 0; attempt < 10; attempt += 1) {
    const rule = rowsOf(screen, "RuleRow")[0];
    if (!rule) return backToProfiles(ctx);
    await clickRow(ctx, rule);
    screen = await waitForActiveWidget(ctx, "Delete");
    await screen.widgets().activate({ label: "Delete" });
    await ctx.runtime.wait(300);
    screen = await ctx.client.screen();
  }
  throw new Error("Timed out deleting saved sample catalog user rules");
}

async function openProfile(ctx: TeaKitTestContext, label: string): Promise<ClientScreen> {
  const screen = await ctx.client.screen();
  const profile = rowsOf(screen, "ProfileRow").find((row) => row.label === label);
  if (!profile) throw new Error(`Missing catalog profile ${label}`);
  await clickRow(ctx, profile);
  await ctx.runtime.wait(300);
  return ctx.client.screen();
}

async function backToProfiles(ctx: TeaKitTestContext): Promise<ClientScreen> {
  const screen = await ctx.client.screen();
  await screen.widgets().activate({ label: "Back" });
  await ctx.runtime.wait(300);
  return ctx.client.screen();
}

function assertProfiles(screen: ClientScreen): void {
  if (screen.screenClass !== CATALOG_SCREEN) {
    throw new Error(`Expected the Fieldset catalog screen, found ${screen.screenClass}`);
  }
  const profiles = rowsOf(screen, "ProfileRow").map((row) => row.label);
  if (profiles.join("|") !== [BUILT_IN_PROFILE, USER_PROFILE].join("|")) {
    throw new Error(`Expected the ${BUILT_IN_PROFILE} and ${USER_PROFILE} profiles, found ${JSON.stringify(profiles)}`);
  }
}

function assertRuleLabels(screen: ClientScreen, expected: readonly string[]): ScreenListEntrySnapshot[] {
  const rules = rowsOf(screen, "RuleRow");
  const labels = rules.map((row) => row.label);
  if (labels.join("|") !== expected.join("|")) {
    throw new Error(`Expected catalog rules ${JSON.stringify(expected)}, found ${JSON.stringify(labels)}`);
  }
  return rules;
}

function rowsOf(screen: ClientScreen, rowClass: string): ScreenListEntrySnapshot[] {
  return screen.lists().entries().filter((row) => row.entryClass?.endsWith(`$${rowClass}`));
}

async function clickRow(ctx: TeaKitTestContext, row: ScreenListEntrySnapshot): Promise<void> {
  await ctx.client.click({ x: row.x + row.width / 2, y: row.y + row.height / 2, button: 0 });
}

async function waitForActiveWidget(ctx: TeaKitTestContext, label: string): Promise<ClientScreen> {
  const deadline = Date.now() + 10_000;
  let screen = await ctx.client.screen();
  while (Date.now() < deadline) {
    if (screen.widgets().all().some((widget) => widget.label === label && widget.active)) return screen;
    await ctx.runtime.wait(100);
    screen = await ctx.client.screen();
  }
  throw new Error(`Expected ${label} to become active on ${screen.screenClass}`);
}
