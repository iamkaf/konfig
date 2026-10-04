import { Capability, Readiness, describe, test } from "@teakit/test";
import type { RuntimeModCallOptions, SpyCall, SpyHandle, TeaKitTestContext } from "@teakit/test";
import { CONFIG_SCREEN, activateFabricConfigure } from "../teakit/konfig-screens";

// Runs only in a production client and dedicated-server pair: `just remote-edit-pair <node>`.

describe.configure({
  timeout: "5m",
  readiness: [Readiness.World, Readiness.Player],
  capabilities: [
    Capability.ClientInput,
    Capability.ClientScreen,
    Capability.ClientScreens,
    Capability.ServerCommands,
    Capability.SpyInstrumentation,
  ],
});

const ON_CLIENT: RuntimeModCallOptions = { side: "client" };
// Matches server-port in modstage.toml.
const SERVER_ADDRESS = "127.0.0.1:25581";
// TeaKit 0.14 keeps only the newest method spy per class, so each class carries at most one spy at a time.
const SYNC = "com.iamkaf.konfig.impl.v1.sync.KonfigSync";
const NETWORK = "com.iamkaf.konfig.impl.v1.sync.KonfigNetwork";
const AUTHORITY = "com.iamkaf.konfig.impl.v1.sync.ConfigSyncAuthority";
const CONFIG_ID = "konfig:konfig";

describe("Konfig remote editing", () => {
  test("negotiates edit rights on join and applies an operator's edit on the server", async (ctx) => {
    if (!(await ctx.session.info()).paired) throw new Error("This test needs a client and dedicated-server pair");
    const health = await ctx.session.client.health();
    const loader = health.loader ?? "";
    const version = health.minecraftVersion ?? "";

    await ctx.server.command("deop @a");
    try {
      const player = await rejoin(ctx);
      assertHandshake(player, false);

      await ctx.server.command("op @a");
      const operator = await rejoin(ctx);
      assertHandshake(operator, true);

      // The edit step drives Mod Menu. Forge and NeoForge have a pause-menu Mods button too, but their mod list is not
      // wired into this test yet.
      if (loader === "fabric") {
        await editSampleLevel(ctx, version, operator.sampleLevel);
      }
    } finally {
      await ctx.server.command("deop @a");
    }
  });
});

interface Handshake {
  permitted: boolean;
  canEdit: boolean;
  sampleLevel: number;
}

// Leaves and rejoins the server so the client sends a fresh Hello and the server answers with its current verdict.
async function rejoin(ctx: TeaKitTestContext): Promise<Handshake> {
  const hello = await ctx.spy.method("konfig.remote.hello", `${SYNC}#onClientHello`);
  const capabilities = await ctx.spy.method("konfig.remote.capabilities", `${SYNC}#onClientCapabilities`, {}, ON_CLIENT);
  const snapshots = await ctx.spy.method("konfig.remote.snapshots", `${NETWORK}#receiveClientAuthoritySnapshot`, {}, ON_CLIENT);
  try {
    await ctx.client.leaveWorld();
    await waitForMenu(ctx);
    await ctx.client.connect(SERVER_ADDRESS, { timeoutMs: 60_000 });

    const [helloCall] = await waitForCalls(ctx, hello, 1);
    const [capabilitiesCall] = await waitForCalls(ctx, capabilities, 1, ON_CLIENT);
    const snapshotCalls = await waitForCalls(ctx, snapshots, 1, ON_CLIENT);
    const snapshotCall = snapshotCalls.find((call) => field(call.args?.[0], "configId") === CONFIG_ID);
    if (!snapshotCall) throw new Error(`The client received no ${CONFIG_ID} snapshot: ${JSON.stringify(snapshotCalls)}`);

    if (Number(helloCall?.args?.[1]) !== 1) {
      throw new Error(`The server saw an unexpected Hello: ${JSON.stringify(helloCall)}`);
    }
    return {
      permitted: helloCall?.args?.[2] === true,
      canEdit: field(capabilitiesCall?.args?.[0], "canEdit") === "true",
      sampleLevel: sampleLevelOf(field(snapshotCall.args?.[0], "jsonPayload")),
    };
  } finally {
    await ctx.spy.detach(hello);
    await ctx.spy.detach(capabilities, ON_CLIENT);
    await ctx.spy.detach(snapshots, ON_CLIENT);
  }
}

function assertHandshake(handshake: Handshake, operator: boolean): void {
  if (handshake.permitted !== operator || handshake.canEdit !== operator) {
    throw new Error(`Expected ${operator ? "an operator" : "a player"} to get canEdit=${operator}, got ${JSON.stringify(handshake)}`);
  }
}

// Opens Konfig from the pause menu, moves Sample Level (0 to 10) one step away from the server's value, and saves.
async function editSampleLevel(ctx: TeaKitTestContext, version: string, serverLevel: number): Promise<void> {
  const target = serverLevel < 10 ? serverLevel + 1 : serverLevel - 1;
  const apply = await ctx.spy.method("konfig.remote.apply", `${AUTHORITY}#apply`);
  const results = await ctx.spy.method("konfig.remote.results", `${NETWORK}#receiveClientEditResult`, {}, ON_CLIENT);
  const steps = await ctx.spy.method(
    "konfig.slider.steps",
    "com.iamkaf.konfig.impl.v1.client.field.KonfigFieldValues#stepInt",
    {},
    ON_CLIENT,
  );
  try {
    await openKonfigFromPauseMenu(ctx, version);
    await ctx.client.waitForScreen(CONFIG_SCREEN, { timeoutMs: 10_000 });

    // TeaKit cannot read the config list in a production client, so reach the slider by keyboard: the documentation
    // link, Debug Mode, Enable Debug Logging, then Sample Level. A slider focused by Tab takes arrows right away.
    for (let i = 0; i < 4; i++) {
      await ctx.client.key(258, { release: true });
      await ctx.runtime.wait(150, ON_CLIENT);
    }
    await ctx.client.key(target > serverLevel ? 262 : 263, { release: true });
    await ctx.runtime.wait(200, ON_CLIENT);
    const [step] = await steps.$calls({}, ON_CLIENT);
    if (step?.returned !== target) {
      throw new Error(`The arrow key did not step Sample Level from ${serverLevel} to ${target}: ${JSON.stringify(step)}`);
    }
    await (await ctx.client.screen()).widgets().activate({ label: "Done" });

    const [applied] = await waitForCalls(ctx, apply, 1);
    const status = field(applied?.returned, "status");
    const appliedLevel = sampleLevelOf(field(applied?.returned, "snapshotJson"));
    if (status !== "ACCEPTED" || appliedLevel !== target) {
      throw new Error(`The server did not accept Sample Level ${target}: ${JSON.stringify(applied)}`);
    }
    const [result] = await waitForCalls(ctx, results, 1, ON_CLIENT);
    if (field(result?.args?.[0], "status") !== "ACCEPTED") {
      throw new Error(`The client did not receive the accepted edit: ${JSON.stringify(result)}`);
    }
  } finally {
    await ctx.spy.detach(apply);
    await ctx.spy.detach(results, ON_CLIENT);
    await ctx.spy.detach(steps, ON_CLIENT);
  }
}

async function openKonfigFromPauseMenu(ctx: TeaKitTestContext, version: string): Promise<void> {
  const startedAt = Date.now();
  let pause = await ctx.client.screen();
  while (!pause.widgets().all().some((widget) => widget.label.includes("Mods"))) {
    if (Date.now() - startedAt > 10_000) throw new Error(`Escape did not open a pause menu with Mods: ${pause.screenClass}`);
    if (!pause.open) await ctx.client.key(256, { release: true });
    await ctx.runtime.wait(500, ON_CLIENT);
    pause = await ctx.client.screen();
  }
  await pause.widgets().activate({ label: "Mods", contains: true });
  const mods = await ctx.client.waitForScreen("Mods", { timeoutMs: 5_000 });
  // TeaKit cannot read Mod Menu's list entries in a production client. Mod Menu selects its first entry on open, and
  // Konfig sorts first among this instance's mods; the config screen check below fails loudly if that changes.
  await activateFabricConfigure(mods, version);
}

// Leaving a server passes through a progress screen; connecting before the title screen is back does not start.
async function waitForMenu(ctx: TeaKitTestContext): Promise<void> {
  await ctx.client.waitForScreen("Title", { timeoutMs: 15_000 });
  await ctx.runtime.wait(1_000, ON_CLIENT);
}

async function waitForCalls(
  ctx: TeaKitTestContext,
  spy: SpyHandle,
  count: number,
  options: RuntimeModCallOptions = {},
): Promise<SpyCall[]> {
  const startedAt = Date.now();
  while (Date.now() - startedAt < 15_000) {
    const calls = await spy.$calls({}, options);
    if (calls.length >= count) return calls;
    await ctx.runtime.wait(200, options);
  }
  throw new Error(`Timed out waiting for ${count} ${spy.name} call(s)`);
}

// TeaKit serializes a record argument with Gson, or falls back to its toString() summary on older Gson.
function field(value: unknown, name: string): string | undefined {
  if (value && typeof value === "object") {
    const record = value as Record<string, unknown>;
    if (name in record) {
      const raw = record[name];
      return typeof raw === "string" ? raw : JSON.stringify(raw);
    }
    if (typeof record.summary === "string") {
      const match = record.summary.match(new RegExp(`[\\[,] ?${name}=(.*?)(?:, \\w+=|\\]$)`));
      return match?.[1];
    }
  }
  return undefined;
}

function sampleLevelOf(json: string | undefined): number {
  const level = (JSON.parse(json ?? "{}") as { debug?: { sample_level?: unknown } }).debug?.sample_level;
  if (typeof level !== "number") throw new Error(`No debug.sample_level in snapshot ${json}`);
  return level;
}
