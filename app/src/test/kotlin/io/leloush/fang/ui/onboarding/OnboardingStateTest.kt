package io.leloush.fang.ui.onboarding

import io.leloush.fang.host.HostFailure
import io.leloush.fang.host.HostHello
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class OnboardingStateTest {
  @Test
  fun runtimeIsTheFirstStepAndNotificationsAreHiddenWithoutThePermission() {
    assertEquals(SetupStep.Runtime, state(checks(runtime = false)).nextOpenStep())
    assertEquals(SetupStep.Notifications, state(checks()).nextOpenStep())
    assertFalse(SetupStep.Notifications in state(checks(notifications = null)).visibleSteps)
    // Below Android 13 the step is gone, so background comes next.
    assertEquals(SetupStep.Background, state(checks(notifications = null)).nextOpenStep())
  }

  @Test
  fun setupResumesAtTheFirstStepStillOpen() {
    assertEquals(SetupStep.Runtime, state(checks(runtime = false)).nextOpenStep())
    assertEquals(SetupStep.InstallPi, state(checks(notifications = true, background = true, storage = true)).nextOpenStep())
    assertEquals(SetupStep.SignIn, state(checks(notifications = true, background = true, storage = true), hello = READY).nextOpenStep())
  }

  @Test
  fun skippedStepsStaySkippedButRequiredOnesComeBackWhenTheyBreak() {
    val passed = setOf(SetupStep.Notifications.name, SetupStep.Background.name, SetupStep.Storage.name)
    assertEquals(SetupStep.SignIn, state(checks(background = true, storage = true), passed = passed, hello = READY).nextOpenStep())
    // Removing the runtime reopens its required step even though later steps were skipped.
    assertEquals(SetupStep.Runtime, state(checks(runtime = false), passed = passed, hello = READY).nextOpenStep())
    assertNull(
      state(
        checks(background = true, storage = true),
        passed = passed + SetupStep.SignIn.name,
        hello = READY,
      ).nextOpenStep(),
    )
  }

  @Test
  fun theAgentInstallsByItselfOnlyOnceTheRuntimeIsReady() {
    assertFalse(state(checks(runtime = false), complete = true).shouldAutoInstall)
    assertTrue(state(checks(), complete = true).shouldAutoInstall)
    assertFalse(state(checks(), complete = true, hello = READY).shouldAutoInstall)
  }

  @Test
  fun aRuntimeFailureStopsTheAutomaticInstallUntilItIsFixed() {
    val missing = HostFailure.Runtime().toSetupError()
    assertEquals(missing, state(checks(runtime = false), error = missing).currentError)
    assertNull(state(checks(runtime = true), error = missing).currentError)
    assertTrue(state(checks(runtime = true), error = missing).shouldAutoInstall)
  }

  @Test
  fun storageIsDoneOnlyWhenAndroidGrantsAllFilesAccess() {
    assertEquals(StepStatus.Done, state(checks(storage = true)).status(SetupStep.Storage))
    assertEquals(StepStatus.Pending, state(checks(storage = false)).status(SetupStep.Storage))
  }

  @Test
  fun aRunningRuntimeInstallShowsOnItsStep() {
    val installing = state(checks(runtime = false), runtimeInstalling = true)
    assertEquals(StepStatus.Running, installing.status(SetupStep.Runtime))
    assertEquals(SetupStep.Runtime, installing.nextOpenStep())
  }

  @Test
  fun theAgentStepWaitsForTheRuntimeAndSignInWaitsForTheAgent() {
    val noRuntime = state(checks(runtime = false))
    assertEquals(StepStatus.Pending, noRuntime.status(SetupStep.InstallPi))
    assertEquals(StepStatus.Pending, state(checks()).status(SetupStep.SignIn))
    assertEquals(StepStatus.Done, state(checks(), hello = READY).status(SetupStep.InstallPi))
  }

  @Test
  fun aFailureFixedBySetupPinsItselfToTheStepThatFixesIt() {
    assertEquals(SetupStep.Runtime, HostFailure.Runtime().toSetupError().step)
    assertEquals(SetupStep.Runtime, HostFailure.Permission().toSetupError().step)
    assertEquals(SetupStep.Runtime, HostFailure.NodeMissing().toSetupError().step)
    assertEquals(SetupStep.Runtime, HostFailure.NodeTooOld().toSetupError().step)
    assertEquals(SetupStep.InstallPi, HostFailure.Crashed(1).toSetupError().step)
    assertEquals(SetupStep.InstallPi, HostFailure.Disconnected().toSetupError().step)
  }

  @Test
  fun aRuntimeFailureOpensItsStepEvenAfterSetupFinished() {
    val missing = HostFailure.Runtime().toSetupError()
    val state = state(checks(runtime = false), passed = setOf(SetupStep.Notifications.name), complete = true, error = missing)
    assertEquals(StepStatus.Failed, state.status(SetupStep.Runtime))
    assertEquals(SetupStep.Runtime, state.nextOpenStep())
  }

  private fun checks(
    runtime: Boolean = true,
    notifications: Boolean? = false,
    background: Boolean = false,
    storage: Boolean = false,
  ) = DeviceChecks(
    runtimeInstalled = runtime,
    runtimeVersion = if (runtime) "Wolfi OS 20230201" else null,
    notificationsGranted = notifications,
    backgroundAllowed = background,
    allFilesAllowed = storage,
  )

  private fun state(
    checks: DeviceChecks,
    passed: Set<String> = emptySet(),
    complete: Boolean = false,
    installing: Boolean = false,
    runtimeInstalling: Boolean = false,
    hello: HostHello? = null,
    error: SetupError? = null,
  ) = OnboardingState(
    checks = checks,
    hello = hello,
    expectedPiVersion = PI_VERSION,
    setupComplete = complete,
    passedPages = passed,
    blockedPermissions = emptySet(),
    installing = installing,
    runtimeInstalling = runtimeInstalling,
    progress = emptyList(),
    runtimeProgress = emptyList(),
    error = error,
    authStatus = null,
  )

  private companion object {
    const val PI_VERSION = "1.3.0"
    val READY =
      HostHello(piVersion = PI_VERSION, nodeVersion = "22.19.0", configDir = "", home = "/sdcard", sharedStorage = true)
  }
}
