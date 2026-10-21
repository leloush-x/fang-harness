package io.leloush.fang.ui.onboarding

import androidx.annotation.DrawableRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import io.leloush.fang.R

/** A step's button; the first of a [StepSpec]'s is its main action. */
internal data class StepButton(val label: String, val enabled: Boolean = true, val onClick: () -> Unit)

/**
 * One setup step's content, rendered as a full page in onboarding ([OnboardingScreen]) and as a card in Settings →
 * Permissions ([PermissionsScreen]). [extra] holds what's more than text and buttons: install output or sign-in
 * choices.
 */
@Suppress("LongParameterList") // One field per thing a step page renders.
internal class StepSpec(
  val step: SetupStep,
  @param:DrawableRes val icon: Int,
  val title: String,
  val body: String,
  val status: StepStatus,
  val buttons: List<StepButton> = emptyList(),
  val error: String? = null,
  /** Shown with a spinner while Fang Harness waits on something it can't hurry. */
  val waiting: String? = null,
  /** A quieter line under the step's state. */
  val note: String? = null,
  val extra: (@Composable () -> Unit)? = null,
)

/** The content of [step] for [state]; taps go to [onAction]. */
@Composable
internal fun stepSpec(step: SetupStep, state: OnboardingState, onAction: (SetupAction) -> Unit): StepSpec =
  when (step) {
    SetupStep.Runtime -> runtimeSpec(state, onAction)
    SetupStep.Notifications -> notificationsSpec(state, onAction)
    SetupStep.Background -> permissionSpec(
      step = step,
      state = state,
      icon = R.drawable.ic_lucide_battery_charging,
      title = R.string.onboarding_background_title,
      body = R.string.onboarding_background_body,
      done = R.string.onboarding_background_done,
      action = StepButton(stringResource(R.string.onboarding_allow)) { onAction(SetupAction.AllowBackground) },
    )
    SetupStep.Storage -> permissionSpec(
      step = step,
      state = state,
      icon = R.drawable.ic_lucide_folder,
      title = R.string.onboarding_storage_title,
      body = R.string.onboarding_storage_body,
      done = R.string.onboarding_storage_done,
      action = StepButton(stringResource(R.string.onboarding_allow)) { onAction(SetupAction.AllowStorage) },
    )
    SetupStep.InstallPi -> installPiSpec(state, onAction)
    SetupStep.SignIn -> signInSpec(state, onAction)
  }

/** [note] names the same setting on other phones' settings apps; it shows until the step is done. */
@Composable
private fun permissionSpec(
  step: SetupStep,
  state: OnboardingState,
  @DrawableRes icon: Int,
  title: Int,
  body: Int,
  done: Int,
  action: StepButton,
  note: Int? = null,
): StepSpec {
  val status = state.status(step)
  if (status == StepStatus.Done) {
    return StepSpec(step = step, icon = icon, title = stringResource(title), body = stringResource(done), status = status)
  }
  return StepSpec(
    step = step,
    icon = icon,
    title = stringResource(title),
    body = stringResource(body),
    status = status,
    buttons = listOf(action),
    note = note?.let { stringResource(it) },
  )
}

@Composable
private fun notificationsSpec(state: OnboardingState, onAction: (SetupAction) -> Unit): StepSpec {
  val blocked = android.Manifest.permission.POST_NOTIFICATIONS in state.blockedPermissions
  val label = if (blocked) R.string.onboarding_open_settings else R.string.onboarding_allow
  return permissionSpec(
    step = SetupStep.Notifications,
    state = state,
    icon = R.drawable.ic_lucide_bell,
    title = R.string.onboarding_notifications_title,
    body = if (blocked) R.string.onboarding_notifications_blocked else R.string.onboarding_notifications_body,
    done = R.string.onboarding_notifications_done,
    action = StepButton(stringResource(label)) { onAction(SetupAction.AllowNotifications) },
    note = if (blocked) R.string.onboarding_notifications_blocked_note else null,
  )
}

@Composable
private fun runtimeSpec(state: OnboardingState, onAction: (SetupAction) -> Unit): StepSpec {
  val status = state.status(SetupStep.Runtime)
  val version = state.checks.runtimeVersion
  val done = status == StepStatus.Done
  val error = state.errorFor(SetupStep.Runtime)?.message
  return StepSpec(
    step = SetupStep.Runtime,
    icon = R.drawable.ic_lucide_square_terminal,
    title = stringResource(R.string.onboarding_runtime_title),
    body = when {
      done && version != null -> stringResource(R.string.onboarding_runtime_done, version)
      done -> stringResource(R.string.onboarding_runtime_done_unnamed)
      else -> stringResource(R.string.onboarding_runtime_body)
    },
    status = status,
    buttons = when (status) {
      StepStatus.Done -> emptyList()
      StepStatus.Running -> emptyList()
      StepStatus.Failed -> listOf(
        StepButton(stringResource(R.string.onboarding_retry)) { onAction(SetupAction.PrepareRuntime) },
      )
      StepStatus.Pending -> listOf(
        StepButton(stringResource(R.string.onboarding_runtime_action)) { onAction(SetupAction.PrepareRuntime) },
      )
    },
    error = error,
    waiting = if (status == StepStatus.Running) stringResource(R.string.onboarding_runtime_running) else null,
    note = if (done) null else stringResource(R.string.onboarding_runtime_note),
    extra = if (state.runtimeProgress.isEmpty() || done) null else ({ ProgressLines(state.runtimeProgress) }),
  )
}

@Composable
private fun installPiSpec(state: OnboardingState, onAction: (SetupAction) -> Unit): StepSpec {
  val status = state.status(SetupStep.InstallPi)
  val version = state.expectedPiVersion
  val install = StepButton(stringResource(R.string.onboarding_install_action), state.canLaunchHost) {
    onAction(SetupAction.InstallPi)
  }
  val retry =
    StepButton(stringResource(R.string.onboarding_retry), state.canLaunchHost) { onAction(SetupAction.InstallPi) }
  return StepSpec(
    step = SetupStep.InstallPi,
    icon = R.drawable.ic_lucide_bot,
    title = stringResource(R.string.onboarding_install_title),
    body = when (status) {
      StepStatus.Done -> stringResource(R.string.onboarding_install_done, version)
      else -> stringResource(R.string.onboarding_install_body, version)
    },
    status = status,
    buttons = when (status) {
      StepStatus.Pending -> listOf(install)
      StepStatus.Failed -> listOf(retry)
      else -> emptyList()
    },
    error = state.errorFor(SetupStep.InstallPi)?.let {
      it.message ?: stringResource(R.string.onboarding_install_failed)
    },
    // `ensure` reports progress only when it installs, starting with `npm ci`; until then it's checking the agent.
    waiting = when {
      status != StepStatus.Running -> null
      state.progress.isEmpty() -> stringResource(R.string.onboarding_install_checking)
      else -> stringResource(R.string.onboarding_install_running)
    },
    extra = if (state.progress.isEmpty() || status == StepStatus.Done) null else ({ ProgressLines(state.progress) }),
  )
}

@Composable
private fun signInSpec(state: OnboardingState, onAction: (SetupAction) -> Unit): StepSpec {
  val enabled = state.piReady
  return StepSpec(
    step = SetupStep.SignIn,
    icon = R.drawable.ic_lucide_key_round,
    title = stringResource(R.string.onboarding_signin_title),
    body = stringResource(if (enabled) R.string.onboarding_signin_body else R.string.onboarding_signin_needs_pi),
    status = state.status(SetupStep.SignIn),
    extra = { SignInChoices(state.authStatus, enabled, onAction) },
  )
}
