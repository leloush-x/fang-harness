# ActivityGlyph contract (shared by all workers — do not edit unless you own the engine)

Package: `io.leloush.fang.ui.components.activityglyph` (NEW files only; engine worker owns this whole package)

## Public API (must exist exactly)

```kotlin
enum class AgentActivity { IDLE, STARTUP, THINKING, LONG_WORK, TOOL, STREAMING, WAITING, HANDOFF, DONE, FAILED }

// Visual states (compressed from upstream's 10) used to derive cells
// Each maps to a design: idle=shimmer, startup=condense, thinking=comet, longWork=helix,
// tool=kick, streaming=spiral, waiting=chase, handoff=slide, done=ring(green), failed=cross(red)

@Composable
fun ActivityGlyph(
  activity: AgentActivity,
  modifier: Modifier = Modifier,
  size: Dp = 18.dp,
  isPaused: Boolean = false,
  reduceMotion: Boolean = false,
  contentDescription: String? = null,
)

// State derivation helpers (engine worker owns; chat/subagent workers call)
data class WorkingActivity(
  val running: Boolean,
  val needsInput: Boolean = false,
  val startedAtMillis: Long? = null,
  val finished: Boolean = false,
  val failed: Boolean = false,
  val stopping: Boolean = false,
) { fun activity(now: Long = System.currentTimeMillis()): AgentActivity }

data class SubagentActivity(
  val running: Boolean,
  val needsInput: Boolean = false,
  val spawnedAtMillis: Long? = null,
  val startedAtMillis: Long? = null,
  val textAtMillis: Long? = null,
  val toolAtMillis: Long? = null,
  val activityAtMillis: Long? = null,
  val done: Boolean = false,
  val failed: Boolean = false,
) { fun activity(now: Long = System.currentTimeMillis()): AgentActivity }
```

- 60 Hz frame-gated `withFrameNanos` loop, tick read only inside Canvas draw (no recomposition per frame).
- Pause the loop when `isPaused` (covered/backgrounded), rest fast-path when settled, reduce-motion still pose.
- Faithful port of upstream `AgentActivityShaders.ACTIVITY_ORB_FIELD` (7x7 dot grid, analytical halos, disc SDF) into a new raw shader asset `res/raw/activity_orb.agls` with a header citing source/version.
- Cell field generator ported from upstream `ActivityGyro` designs with exact constants from `UPSTREAM_FLOW_NOTES.md`.
- Accents: waiting=orange, failed=red, done=green, handoff=blue.
- API 33+: RuntimeShader; below 33: lightweight Canvas fallback (static dot disc) — Fang minSdk 29.

## Ownership
- Engine worker: ONLY the new `ui/components/activityglyph/` package, `res/raw/activity_orb.agls`, new tests.
- Chat worker: `ui/chat/MessageItems.kt`, `StreamBanners.kt`, `MorphGlyph.kt` (consume the API above).
- Subagent worker: `ui/chat/SubagentCapsules.kt`, `StreamSubagents.kt`, `ui/subagents/*` (consume the API above).
- Ask worker: `ui/chat/AskUserCard.kt`, `WidgetTabs.kt`, `agent/AskState.kt`, `ComposerEdges.kt`.
- Main agent: `ChatScreen.kt`, `ChatMainBody.kt`, `ChatMainState.kt`, `Composer.kt`, `strings_*`, integration.

Source of truth: /tmp/opencode-android-src feature/session/.../agentactivity/*.kt + UPSTREAM_FLOW_NOTES.md. No voice/call code.
