import { Capability, Readiness, describe, expect, test } from "@teakit/test";
import type { LoaderId, ScenarioDefinition, ScenarioResult, ScenarioStep } from "@teakit/test";

describe("Konfig config screen", () => {
  test(
    "opens the debug config screen",
    async ({ runtime, scenario }) => {
      const health = await runtime.health();
      const minecraftVersion = health.minecraftVersion ?? "";
      const loader = health.loader ?? "";
      const definition = scenarioFor(minecraftVersion, loader);

      const result = await scenario.run(
        {
          ...definition,
          name: `${definition.name}-${loader}-${minecraftVersion}`,
          steps: [...definition.steps],
          cleanup: definition.cleanup ?? [],
        },
        { timeoutMs: 240_000 },
      );

      expect(failedSteps(result)).toEqual([]);
    },
    { capabilities: [Capability.LegacyJsonScenarios], readiness: [Readiness.Title] },
  );
});

function scenarioFor(minecraftVersion: string, loader: LoaderId | string): ScenarioDefinition {
  if (loader === "fabric") {
    if (minecraftVersion === "26.2") return fabricModern;
    if (includes(FABRIC_LEGACY, minecraftVersion)) return fabricLegacy;
    if (includes(FABRIC_117, minecraftVersion)) return fabric117;
    if (includes(FABRIC_11934, minecraftVersion)) return fabric11934;
    if (includes(FABRIC_12034, minecraftVersion)) return fabric12034;
    return fabricModernPre262;
  }

  if (loader === "forge") {
    if (minecraftVersion === "1.16.5") return forgeLegacy;
    return forgeModern;
  }

  if (loader === "neoforge") return neoforge;

  throw new Error(`Unsupported Konfig scenario runtime: ${minecraftVersion}-${loader}`);
}

function failedSteps(result: ScenarioResult): string[] {
  return ["setup", "steps", "cleanup"].flatMap((phase) => {
    const phaseResults = result[phase];
    if (!Array.isArray(phaseResults)) {
      return [];
    }

    return phaseResults
      .filter((step) => {
        const stepResult = step.result as Record<string, unknown> | undefined;
        return stepResult?.failure != null || stepResult?.failed === true || stepResult?.success === false;
      })
      .map((step) => `${phase}[${step.index ?? "?"}] ${step.action ?? "unknown"}`);
  });
}

function includes(values: readonly string[], value: string): boolean {
  return values.includes(value);
}

const TITLE_SCREEN_READY = {
  action: "wait_for_screen",
  title: "Title",
  timeoutMs: 30000,
  pollMs: 100,
} as ScenarioStep;

const TITLE_LOCALIZATION_READY = {
  action: "wait_ms",
  durationMs: 3500,
} as ScenarioStep;

const FABRIC_LEGACY = [
  "1.14.4",
  "1.15",
  "1.15.1",
  "1.15.2",
  "1.16",
  "1.16.1",
  "1.16.2",
  "1.16.3",
  "1.16.4",
  "1.16.5",
] as const;

const FABRIC_117 = [
  "1.17",
  "1.17.1",
  "1.18",
  "1.18.1",
  "1.18.2",
  "1.19",
  "1.19.1",
  "1.19.2",
] as const;

const FABRIC_11934 = ["1.19.3", "1.19.4", "1.20", "1.20.1", "1.20.2"] as const;

const FABRIC_12034 = ["1.20.3", "1.20.4", "1.21.2"] as const;

const fabricModern = {
  name: "konfig-title-config-fabric",
  steps: [
    TITLE_SCREEN_READY,
    TITLE_LOCALIZATION_READY,
    { action: "activate_widget", label: "Mods", contains: true, waitAfterMs: 800 },
    { action: "wait_for_screen", title: "Mods", timeoutMs: 5000, pollMs: 100 },
    { action: "click_list_entry", label: "Konfig", contains: false, nth: 0, listRole: "mod_list", waitAfterMs: 300 },
    { action: "activate_widget_class", widgetClass: "com.terraformersmc.modmenu.gui.widget.LegacyTexturedButtonWidget", nth: 1, waitAfterMs: 800 },
    { action: "wait_for_list_entry", label: "Konfig Debug Settings", contains: true, timeoutMs: 5000, pollMs: 100 },
    { action: "wait_for_list_entry", label: "These entries exist to test Konfig's own screen", contains: true, timeoutMs: 5000, pollMs: 100 },
    { action: "wait_for_list_entry", label: "Konfig Documentation", contains: true, timeoutMs: 5000, pollMs: 100 },
    { action: "wait_for_list_entry", label: "Debug Mode", contains: false, timeoutMs: 5000, pollMs: 100 },
    { action: "click_mouse", x: 313, y: 185 },
    { action: "wait_ms", durationMs: 300 },
    { action: "screenshot", name: "konfig-debug-dropdown-open", hideOverlay: true, hideDecoration: true, hideWindowDecoration: true },
    { action: "click_mouse", x: 8, y: 8 },
    { action: "scroll_mouse", x: 120, y: 185, verticalAmount: -4 },
    { action: "wait_ms", durationMs: 200 },
    { action: "wait_for_list_entry", label: "Enable Debug Logging", contains: false, timeoutMs: 5000, pollMs: 100 },
    { action: "screenshot", name: "konfig-debug-settings", hideOverlay: true, hideDecoration: true, hideWindowDecoration: true },
  ],
  cleanup: [],
} as ScenarioDefinition;

const fabricModernPre262 = {
  ...fabricModern,
  name: "konfig-title-config-fabric-pre262",
  steps: replaceStep(fabricModern.steps, 10, { action: "click_mouse", x: 203, y: 185 }),
} as ScenarioDefinition;

const fabric117 = {
  ...fabricModernPre262,
  name: "konfig-title-config-fabric-117",
  steps: replaceStep(fabricModernPre262.steps, 5, {
    action: "activate_widget",
    label: "Configure...",
    contains: false,
    nth: 0,
    waitAfterMs: 800,
  }),
} as ScenarioDefinition;

const fabric11934 = {
  ...fabricModernPre262,
  name: "konfig-title-config-fabric-11934",
  steps: replaceStep(fabricModernPre262.steps, 5, {
    action: "activate_widget_class",
    widgetClass: "com.terraformersmc.modmenu.gui.ModsScreen$1",
    contains: false,
    nth: 0,
    waitAfterMs: 800,
  }),
} as ScenarioDefinition;

const fabric12034 = {
  ...fabricModernPre262,
  name: "konfig-title-config-fabric-12034",
  steps: replaceStep(fabricModernPre262.steps, 5, {
    action: "activate_widget_class",
    widgetClass: "com.terraformersmc.modmenu.gui.widget.LegacyTexturedButtonWidget",
    contains: false,
    nth: 1,
    waitAfterMs: 800,
  }),
} as ScenarioDefinition;

const fabricLegacy = {
  name: "konfig-title-config-fabric-legacy",
  steps: [
    TITLE_SCREEN_READY,
    TITLE_LOCALIZATION_READY,
    { action: "activate_widget", label: "Mods", contains: true, waitAfterMs: 800 },
    { action: "activate_widget", label: "Configure...", contains: false, nth: 0, waitAfterMs: 800 },
    { action: "wait_for_screen", screenClass: "com.iamkaf.konfig.fabric.KonfigConfigScreen", timeoutMs: 10000, pollMs: 100 },
    { action: "wait_for_list_entry", label: "Konfig Debug Settings", contains: true, timeoutMs: 10000, pollMs: 100 },
    { action: "wait_for_list_entry", label: "These entries exist to test Konfig's own screen", contains: true, timeoutMs: 10000, pollMs: 100 },
    { action: "wait_for_list_entry", label: "Konfig Documentation", contains: true, timeoutMs: 10000, pollMs: 100 },
    { action: "wait_for_list_entry", label: "Debug Mode", contains: false, timeoutMs: 10000, pollMs: 100 },
    { action: "click_mouse", x: 203, y: 185 },
    { action: "wait_ms", durationMs: 300 },
    { action: "screenshot", name: "konfig-debug-dropdown-open", hideOverlay: true, hideDecoration: true, hideWindowDecoration: true },
    { action: "click_mouse", x: 8, y: 8 },
    { action: "scroll_mouse", x: 120, y: 185, verticalAmount: -4 },
    { action: "wait_ms", durationMs: 200 },
    { action: "wait_for_list_entry", label: "Enable Debug Logging", contains: false, timeoutMs: 10000, pollMs: 100 },
    { action: "screenshot", name: "konfig-debug-settings", hideOverlay: true, hideDecoration: true, hideWindowDecoration: true },
  ],
  cleanup: [],
} as ScenarioDefinition;

const forgeModern = {
  name: "konfig-title-config-forge",
  steps: [
    TITLE_SCREEN_READY,
    TITLE_LOCALIZATION_READY,
    { action: "activate_widget", label: "Mods", contains: true, waitAfterMs: 800 },
    { action: "wait_for_screen", title: "Mods", timeoutMs: 5000, pollMs: 100 },
    { action: "click_list_entry", label: "Konfig", contains: false, nth: 0, waitAfterMs: 300 },
    { action: "activate_widget", label: "Config", contains: false, waitAfterMs: 800 },
    { action: "wait_for_list_entry", label: "Konfig Debug Settings", contains: true, timeoutMs: 5000, pollMs: 100 },
    { action: "wait_for_list_entry", label: "These entries exist to test Konfig's own screen", contains: true, timeoutMs: 5000, pollMs: 100 },
    { action: "wait_for_list_entry", label: "Konfig Documentation", contains: true, timeoutMs: 5000, pollMs: 100 },
    { action: "wait_for_list_entry", label: "Debug Mode", contains: false, timeoutMs: 5000, pollMs: 100 },
    { action: "click_mouse", x: 203, y: 185 },
    { action: "wait_ms", durationMs: 300 },
    { action: "screenshot", name: "konfig-debug-dropdown-open", hideOverlay: true, hideDecoration: true, hideWindowDecoration: true },
    { action: "click_mouse", x: 8, y: 8 },
    { action: "scroll_mouse", x: 120, y: 185, verticalAmount: -4 },
    { action: "wait_ms", durationMs: 200 },
    { action: "wait_for_list_entry", label: "Enable Debug Logging", contains: false, timeoutMs: 5000, pollMs: 100 },
    { action: "screenshot", name: "konfig-debug-settings", hideOverlay: true, hideDecoration: true, hideWindowDecoration: true },
  ],
  cleanup: [],
} as ScenarioDefinition;

const forgeLegacy = {
  ...forgeModern,
  name: "konfig-title-config-forge-legacy",
  steps: [
    ...forgeModern.steps.slice(0, 6),
    {
      action: "wait_for_screen",
      screenClass: "com.iamkaf.konfig.forge.KonfigConfigScreen",
      timeoutMs: 10000,
      pollMs: 100,
    },
    ...forgeModern.steps.slice(6).map((step) =>
      step.action === "wait_for_list_entry" ? { ...step, timeoutMs: 10000 } : step
    ),
  ],
} as ScenarioDefinition;

const neoforge = {
  ...forgeModern,
  name: "konfig-title-config-neoforge",
} as ScenarioDefinition;

function replaceStep(steps: ScenarioStep[], index: number, step: ScenarioStep): ScenarioStep[] {
  const copy = [...steps];
  copy[index] = step;
  return copy;
}
