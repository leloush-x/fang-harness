# Upstream flow notes — from 5 parallel dissections of github.com/anomalyco/opencode-android

Source: /tmp/opencode-android-src. NO voice/call features to port. All file:line refs are to that repo.

## TWO SEPARATE ORB SYSTEMS (critical)
1. **Wisp/Living orb** (feature/voice, visual engine only): the big glass-lens orb. 7 states. CPU-computed 2*cols x cols RGBA_F16 cell bitmap via setInputBuffer, cols=40 default. AGSL is ONE inline Kotlin string (WispOrbShaders.kt:29-213). 240Hz fixed sim, withFrameNanos, dt capped 50ms. Breath 5.5s (WispOrbCells.kt:627), empty-state breath 1.7s.
2. **AgentActivity glyph** (feature/session/agentactivity): the small "orbs everywhere". A 7x7 dot-grid disc in one GPU pass (AgentActivityOrb/AgentActivityShaders). 10-state AgentActivity + ActivityGyro designs. THIS is the one to replicate across Fang's rows.

## AGENT ACTIVITY GLYPH — the 10 states & designs (AgentActivitySource.kt:31-41, ActivityGyro.kt:654-787)
idle=shimmer, startup=condense, thinking=comet, longWork=helix, tool=kick, streaming=spiral, waiting=chase, handoff=slide, done=ring(green), failed=cross(red).
- Accents: waiting=orange, failed=red, done=green, handoff=blue.
- thinking = ONE meridian band + phosphor afterglow (steady, no bounce); streaming = THREE rising stripes + per-output kick; tool = band spin + flash per tool event; longWork = 3 tilted spirals; startup = band ramps in over 1.1s; waiting = 3 rim-chasing clusters; handoff = 2 bands slide right; idle = sparse shimmer that never rests; done = green ring sweep; failed = red X + ember flash.
- WorkingActivity phases: running/stopping/done/failed. running+needsInput→waiting; <1.4s→startup; else thinking.
- SubagentActivity phases: spawning/running/done/failed/idle → derived with recency (spawn<2.6s→handoff, start<1.4s→startup, text<0.8s→streaming, tool<2s→tool, idle>8s→longWork, else thinking).
- 13 placements: working line/turn footer (#1), finale-copy overlay (#2), subagent tool card (#3), run-error row (#4), background capsule (#5), panel subagent/shell rows (#6/#7), session sheet status (#8), shell output status (#10), browser capsule (#11), fallback glyph (#12), copy buttons (#13). NO separate thinking row — thinking is a state of the orb.
- Perf: 60Hz frame gate, tick counter read only in Canvas (no recomposition), pause when covered/backgrounded, rest fast-path.

## COMPLETION MORPH (WorkingHandoffPose, AgentActivitySource.kt:206-239)
finale 0.55s + handoff 0.34s (0.89s total), pending settles 1.5s. BLUR=6dp. Orb blur/shrink toward copy button centre while copy fades/scales in (0.6→1.0). Reduce motion = crossfade only. Same ReplyCopyGlyph drawn in same slot. 1.5s copy check reset.

## SUBAGENT CAPSULE + PANEL + FULL CHAT
- One morphing surface capsule↔panel (BackgroundWorkPanel.kt). Stage closed/capsule/panel, one Animatable progress 0=capsule 1=panel.
- Capsule: 34dp tall, glyph 18dp (or 40dp pair with shell), counts "1 subagent · 2 shells", rounded ends via clip to BackgroundWorkShape(12dp).
- Dismiss-to-capsule: drag>56dp OR flick>550dp/s → capsule (BackgroundWorkMotion.kt:31-60). Rows kept composed offscreen to avoid jank.
- Full chat = SAME SessionView pushed on a stack (SessionPageStack), full composer with steering while running, back via PredictiveBackHandler → SubagentsPopped, nested subagents push one at a time, drafts preserved on pop. View receipt via client.viewSession.
- "every-running-subagent" count includes nested + >100.

## THEMES (28-slot ThemeVariant)
- AppTheme: system/oc-2/tokyonight/catppuccin/dracula/rosepine/github. Exact RGB values recovered (ThemePaletteGenerated.kt). 28 slots.
- CRITICAL: curated theme palettes do NOT push into AGSL shaders — shaders use hard-coded palettes keyed on isDark/contrast. So no theme→shader plumbing needed.

## ASK FLOW
- Question tool → form requests, answers via replyForm / replyPermission, NEVER send_message (VoiceDelegator.kt:54-56). Failed answer → Send becomes Retry.
- Inline in composer; paging (page 1 of N), single choice auto-advance 280ms; fold via RequestFold (threshold .3, flick 700dp/s). Answered summary "N questions answered" with ✓ picks.
- Paste: long paste becomes card only for messages, NOT while answering a request (takesPasteAttachments=false) — PastedText limit >1000 chars or >12 newlines.

## KEY MAPPING TO FANG (pending Fang-map agent)
- FangOrb currently 5 moods; needs the 10-state ActivityGyro-style visual behaviors across all glyph sites.
- Need: thinking=comet, streaming=spiral, tool=kick, waiting=chase, handoff=slide, done=ring, failed=cross, idle=shimmer, startup=condense, longWork=helix.
- Apply completion morph blur/shrink to Fang's MorphGlyph.
- Wisp big orb visual already close; adjust breath/personality if needed.
- Subagent capsule/panel + full chat mostly exists; enhance to match.
