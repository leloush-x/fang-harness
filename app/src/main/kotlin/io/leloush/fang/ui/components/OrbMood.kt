package io.leloush.fang.ui.components

/**
 * The five working-glyph moods kept for existing call sites. The recovered orb has a different five-value mood set
 * (connecting/live/thinking/needsInput/ended) and a seven-value palette; each value here maps onto that palette in
 * FangOrb: IDLE→sleeping, LISTENING→listening, SPEAKING→speaking, DONE→green finale then ended, FAILED→confirming.
 */
internal enum class OrbMood { IDLE, LISTENING, SPEAKING, DONE, FAILED }
