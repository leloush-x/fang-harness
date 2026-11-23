# Subagent grouping fix (worktree /root/fang-group, branch feature/subagent-group)

Base: origin/main ac70262 (v0.1.31). Upstream: /tmp/opencode-android-src.

## Gap
Fang: FlowRow of one pill per subagent + header count.
Upstream (AgentActivitySource.kt:478-495, BackgroundWorkPanel.kt:282-341):
- ONE capsule grouping all subagents; single glyph driven by `leading()` = most-telling activity by priority:
  waiting > failed > handoff > startup > tool > streaming > longWork > thinking > done > idle
- count "N subagents"; tap capsule → morphing panel listing each subagent row; row tap → open full chat;
  drag/fling dismisses back to capsule; rounded ends on press.

## Plan
- [ ] Rewrite SubagentCapsules: grouped capsule + aggregate glyph + expandable panel of rows
- [ ] leadingActivity(facts) helper with exact priority; unit test
- [ ] Keep API SubagentCapsules(facts, onOpen, modifier); keep foldOnFling Final-pass guard
- [ ] 2 reviews + CI gate; ask before main/release
