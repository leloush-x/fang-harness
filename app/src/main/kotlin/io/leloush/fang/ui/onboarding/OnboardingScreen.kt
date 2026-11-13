package io.leloush.fang.ui.onboarding

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.leloush.fang.R
import io.leloush.fang.appGraph
import io.leloush.fang.ui.auth.LoginSheet
import io.leloush.fang.ui.components.SimmeringPot
import io.leloush.fang.ui.theme.FangTokens
import io.leloush.fang.ui.theme.fangColors

/**
 * First-run setup, one step per page: welcome, then every [SetupStep] that still needs the user, then a closing page.
 * Steps that are already done are passed over, and finishing one moves on by itself, so the wizard reacts to what the
 * user did elsewhere (granted a permission, came back from settings). Skipped optional steps and the welcome are
 * remembered, so reopening Fang Harness resumes at the first step still open. Opened again after setup (a "needs
 * attention" tap), it starts at the broken step and [onClose] leaves it.
 */
@Composable
internal fun OnboardingScreen(onFinished: () -> Unit, onAddProvider: () -> Unit, onClose: (() -> Unit)?) {
  val context = LocalContext.current
  val graph = remember(context) { context.appGraph }
  var loginProvider by rememberSaveable { mutableStateOf<String?>(null) }
  val checks = rememberDeviceChecks()
  val state = rememberOnboardingState(graph, checks.value)
  val onAction = rememberSetupActions(
    state = state,
    onPermissionResult = checks::refresh,
    onLogin = { loginProvider = it },
    onAddProvider = onAddProvider,
  )
  AutoInstall(graph, state, checks::refresh)
  val history = rememberSaveable(saver = HistorySaver) { mutableStateListOf(firstPage(state)) }
  val page = history.last()
  val step = stepOf(page)

  fun goTo(next: String) {
    if (next != history.last()) history.add(next)
  }

  fun advance(from: OnboardingState = state) = goTo(from.nextOpenStep()?.name ?: DONE)

  fun pass(passed: String) {
    graph.preferences.passSetupPage(passed)
    advance(state.copy(passedPages = state.passedPages + passed))
  }

  fun finish() {
    graph.preferences.setSetupComplete(true)
    onFinished()
  }

  fun back() {
    if (history.size > 1) history.removeAt(history.lastIndex) else onClose?.invoke()
  }

  AdvanceWhenDone(page, step, state) { advance(it) }
  // A failure pinned to another step, such as an install that finds the runtime broken, shows on that step.
  val failedStep = state.currentError?.step?.takeIf { it in state.visibleSteps }
  LaunchedEffect(failedStep) { failedStep?.let { goTo(it.name) } }
  BackHandler(enabled = history.size > 1 || onClose != null) { back() }

  Column(
    modifier = Modifier.fillMaxSize().background(fangColors.surface0).windowInsetsPadding(WindowInsets.safeDrawing),
  ) {
    WizardTopBar(
      state = state,
      step = step,
      onBack = if (history.size > 1) ::back else null,
      onClose = onClose,
    )
    // Pages change instantly: no slide or fade animation.
    Box(modifier = Modifier.weight(1f)) {
      val shownStep = stepOf(page)
      when {
        page == WELCOME -> WelcomePage(onStart = { pass(WELCOME) })

        shownStep == null -> DonePage(signedIn = state.authStatus?.anyConfigured == true, onFinish = ::finish)

        else -> StepPage(
          spec = stepSpec(shownStep, state, onAction),
          onContinue = { advance() },
          onSkip = { pass(shownStep.name) },
        )
      }
    }
  }

  loginProvider?.let { LoginSheet(providerId = it, onDismiss = { loginProvider = null }) }
}

/**
 * Moves on once the shown [step] gets done while it's on screen, instantly; a step that
 * was already done when the user came back to it waits for Continue. A step this phone or build no longer shows
 * (a permission the phone does not need) moves on right away.
 */
@Composable
private fun AdvanceWhenDone(
  page: String,
  step: SetupStep?,
  state: OnboardingState,
  advance: (OnboardingState) -> Unit,
) {
  if (step == null) return
  val done = state.status(step) == StepStatus.Done
  val doneOnArrival = remember(page) { done }
  val currentState by rememberUpdatedState(state)
  val currentAdvance by rememberUpdatedState(advance)
  val hidden = step !in state.visibleSteps
  LaunchedEffect(page, done, hidden) {
    if (hidden) {
      currentAdvance(currentState)
    } else if (done && !doneOnArrival) {
      currentAdvance(currentState)
    }
  }
}

@Composable
private fun WelcomePage(onStart: () -> Unit) {
  WizardPage(
    illustration = {
      SimmeringPot(size = POT_SIZE, tint = fangColors.foreground)
    },
    title = stringResource(R.string.onboarding_welcome_title),
    body = stringResource(R.string.onboarding_welcome_body),
    buttons = { WizardPrimary(stringResource(R.string.onboarding_get_started), onStart) },
  )
}

@Composable
private fun DonePage(signedIn: Boolean, onFinish: () -> Unit) {
  WizardPage(
    illustration = { SimmeringPot(size = POT_SIZE, tint = fangColors.foreground) },
    title = stringResource(R.string.onboarding_done_title),
    body = stringResource(if (signedIn) R.string.onboarding_done_body else R.string.onboarding_done_body_no_model),
    buttons = { WizardPrimary(stringResource(R.string.onboarding_start), onFinish) },
  )
}

/** One setup step as a page: its illustration, text and live status, with its actions at the bottom. */
@Composable
private fun StepPage(spec: StepSpec, onContinue: () -> Unit, onSkip: () -> Unit) {
  WizardPage(
    illustration = { StepIllustration(spec) },
    tag = requirementLabel(spec.step.requirement),
    title = spec.title,
    body = spec.body,
    content = {
      spec.error?.let {
        Text(
          text = it,
          color = fangColors.destructive,
          fontSize = FangTokens.FontSize.base,
          textAlign = TextAlign.Center,
        )
      }
      spec.waiting?.let { WaitingRow(it) }
      spec.extra?.invoke()
      spec.note?.let {
        Text(
          text = it,
          color = fangColors.foregroundMuted,
          fontSize = FangTokens.FontSize.meta,
          lineHeight = NOTE_LINE_HEIGHT,
          textAlign = TextAlign.Center,
        )
      }
    },
    buttons = { StepButtons(spec, onContinue, onSkip) },
  )
}

/**
 * The runtime install simmers while it runs; the rest show
 * their icon.
 */
@Composable
private fun StepIllustration(spec: StepSpec) {
  val done = spec.status == StepStatus.Done
  when {
    spec.step == SetupStep.Runtime && spec.status == StepStatus.Running ->
      SimmeringPot(size = POT_SIZE, tint = fangColors.foreground)

    else -> IconTile(icon = spec.icon, done = done)
  }
}

private fun firstPage(state: OnboardingState): String = when {
  !state.setupComplete && OnboardingState.WELCOME !in state.passedPages -> WELCOME
  else -> state.nextOpenStep()?.name ?: DONE
}

private fun stepOf(page: String): SetupStep? = SetupStep.entries.firstOrNull { it.name == page }

private val HistorySaver = listSaver<SnapshotStateList<String>, String>(
  save = { it.toList() },
  restore = { it.toMutableStateList() },
)

private const val WELCOME = OnboardingState.WELCOME
private const val DONE = "done"
private val POT_SIZE = 112.dp
private val NOTE_LINE_HEIGHT = 19.sp
