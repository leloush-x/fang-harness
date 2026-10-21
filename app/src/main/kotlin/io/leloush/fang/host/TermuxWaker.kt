package io.leloush.fang.host

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ProcessLifecycleOwner
import io.leloush.fang.appGraph
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Starts Fang Harness's helper in Termux's Play build by opening Termux in a new terminal session: the hook setup wrote for
 * its login shell (bash, zsh or fish) starts the helper. Fang Harness switches back as soon as Termux is up and waits there,
 * showing [waking], while the helper starts.
 *
 * Android lets an app open activities from the background for a few seconds after one of its own starts or finishes,
 * unless the user leaves for the home screen meanwhile. [TermuxWakeActivity] opens Termux for that reason, and
 * Fang Harness switches back once Termux covers it, when Termux's activity, and so its new session, exist.
 */
internal object TermuxWaker {
  private const val TAG = "Fang Harness"
  private const val POLL_MILLIS = 250L

  // From a frozen Termux on the emulator: 4.5 s to the new session, 3.4 s for `am startservice`, 6 s for Node.
  private const val WAIT_MILLIS = 30_000L
  private const val RETURN_GRACE_MILLIS = 1_000L

  /** Inside Android's 10 s allowance; should Termux not cover Fang Harness by then, Fang Harness switches back anyway. */
  private const val COVER_TIMEOUT_MILLIS = 8_000L

  private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
  private val lock = Mutex()
  private var attempt: Deferred<Boolean>? = null

  /** Completed by whichever comes first of Termux covering Fang Harness and the timeout; that one switches back. */
  @Volatile
  private var covering: CompletableDeferred<Unit>? = null

  private val wakingState = MutableStateFlow(false)

  /** Fang Harness opened Termux and is waiting for the helper to answer. */
  val waking: StateFlow<Boolean> = wakingState.asStateFlow()

  /**
   * A wake that didn't bring the helper up (no key, Node missing, ports taken) would fail the same way on every
   * return to Fang Harness and send the user back to Termux each time; until the helper answers again, only the user's
   * own Open Termux tap opens it.
   */
  @Volatile
  private var failed = false

  /** Counts Fang Harness's screen starts, so a wake can tell whether Fang Harness came back over Termux since it began. */
  @Volatile
  private var shows = 0

  /**
   * Whether the helper answers, after opening Termux for it if it didn't. [asked] is the user's tap on Open Termux;
   * without it, Termux isn't opened again after a wake that failed, and with it Fang Harness stays in Termux, which shows
   * why the helper didn't start. Concurrent callers share one attempt. Termux is only opened while Fang Harness is on
   * screen: from the background, Android would block the switch back.
   */
  suspend fun wake(context: Context, asked: Boolean): Boolean {
    val app = context.applicationContext
    val shared = lock.withLock {
      attempt?.takeIf { it.isActive } ?: scope.async { wakeOnce(app, asked) }.also { attempt = it }
    }
    return shared.await()
  }

  /** The helper answered: the next time it doesn't, opening Termux may help again. */
  fun reached() {
    failed = false
  }

  fun fangShown() {
    shows++
  }

  /** Fang Harness's screen stopped; during a wake, Termux covers it, so Fang Harness switches back right away. */
  fun fangCovered(context: Context) {
    if (covering?.complete(Unit) == true) switchBack(context)
  }

  private suspend fun wakeOnce(context: Context, asked: Boolean): Boolean {
    if (HelperBridge.listening(context)) return true
    if (!fangStarted() || (failed && !asked)) return false
    val stayInTermux = failed
    wakingState.value = true
    try {
      // Kept behind Termux, Android would freeze Fang Harness within seconds, long before a cold helper is up.
      val listening = context.appGraph.keepAlive.acquire("waking Termux").use { waitForHelper(context, stayInTermux) }
      failed = !listening
      return listening
    } finally {
      wakingState.value = false
    }
  }

  private suspend fun waitForHelper(context: Context, stayInTermux: Boolean): Boolean {
    val showsBefore = shows
    val covered = CompletableDeferred<Unit>()
    if (!stayInTermux) covering = covered
    context.startActivity(Intent(context, TermuxWakeActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    if (!stayInTermux) {
      withTimeoutOrNull(COVER_TIMEOUT_MILLIS) { covered.await() }
      covering = null
      if (covered.complete(Unit)) switchBack(context)
    }
    val deadline = System.currentTimeMillis() + WAIT_MILLIS
    var listening = false
    while (!listening && System.currentTimeMillis() < deadline) {
      delay(POLL_MILLIS)
      listening = HelperBridge.listening(context)
    }
    Log.i(TAG, "Opened Termux for the helper: listening=$listening")
    if (!listening) return false
    // ProcessLifecycleOwner reports a stop 700 ms late; by now, a Fang Harness still stopped that never came back stayed
    // behind Termux, which is on screen and may open Fang Harness itself. One that came back and then stopped was left by
    // the user, who shouldn't be pulled back to it.
    delay(RETURN_GRACE_MILLIS)
    if (!fangStarted() && shows == showsBefore) HelperBridge.requestReturn(context)
    return true
  }

  /** Brings Fang Harness's existing screen back over Termux, rather than stacking a new one on it. */
  private fun switchBack(context: Context) {
    val intent = checkNotNull(context.packageManager.getLaunchIntentForPackage(context.packageName))
    Log.i(TAG, "Switching back from Termux")
    context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT))
  }

  private fun fangStarted(): Boolean =
    ProcessLifecycleOwner.get().lifecycle.currentStateFlow.value.isAtLeast(Lifecycle.State.STARTED)
}
