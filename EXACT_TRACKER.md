# Exact-orb rebuild tracker (worktree /root/fang-orb2, branch feature/orb-exact)

Base: origin/main 1682cbf (v0.1.29). Exact source: /tmp/opencode/ORB_EXACT_REPORT.md + /tmp/opencode/orb-decomp/*.java (decompiled from classes.dex, resolved by toString literals; no R8 map exists).

## User feedback being fixed
- "2 types of orbs now" — remove orbSize/orbFloats settings, floating badge, size scaling; ONE orb.
- "orbs are like shit" / "nowhere close" — rebuild from exact decompiled values (the old mode table was RECONSTRUCTED and wrong).
- "ask bubble was not bubble" — move ask into Composer's top WidgetTabStrip like todos/background, expand/contract there.
- "subagent bubble cleaner" — simplify capsule.

## Stage A — exact orb + remove settings (worker A)
- [ ] Exact palette/uniform math/tuning/1.7s breath from the recovered Java
- [ ] Remove orb settings, floating badge, size scaling

## Stage B — ask as composer tab (worker B)
- [ ] Ask request becomes a tab in WidgetTabStrip above the typing area, expand/contract like todos

## Stage C — cleaner capsule (worker C)

## Me — ChatScreen wiring: remove floating badge + ask from dialog slot

## Verify: local ktlint + 2 reviews + CI gate per stage; ask before main/release.
