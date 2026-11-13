package io.leloush.fang

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.SaveableStateHolder
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.IntOffset
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.leloush.fang.agent.ChatLocation
import io.leloush.fang.agent.ChatNotification
import io.leloush.fang.ui.AppNavigator
import io.leloush.fang.ui.ChatInboxOverlay
import io.leloush.fang.ui.Destination
import io.leloush.fang.ui.MainScreen
import io.leloush.fang.ui.ShellActions
import io.leloush.fang.ui.archive.ArchiveScreen
import io.leloush.fang.ui.archive.ArchivedChatScreen
import io.leloush.fang.ui.onboarding.OnboardingScreen
import io.leloush.fang.ui.onboarding.PermissionsScreen
import io.leloush.fang.ui.settings.AddModelsScreen
import io.leloush.fang.ui.settings.AddProviderScreen
import io.leloush.fang.ui.settings.AdvancedScreen
import io.leloush.fang.ui.settings.BackupsScreen
import io.leloush.fang.ui.settings.LicenseScreen
import io.leloush.fang.ui.settings.LicensesScreen
import io.leloush.fang.ui.settings.ModelEditorScreen
import io.leloush.fang.ui.settings.ProviderDrafts
import io.leloush.fang.ui.settings.ProviderEditorScreen
import io.leloush.fang.ui.settings.ProvidersScreen
import io.leloush.fang.ui.settings.SettingsScreen
import io.leloush.fang.ui.settings.SubscriptionScreen
import io.leloush.fang.ui.settings.ownsDraft
import io.leloush.fang.ui.theme.FangTheme
import io.leloush.fang.ui.theme.fangColors
import kotlinx.coroutines.flow.MutableStateFlow

class MainActivity : ComponentActivity() {
  /** The chat a tapped notification asked for, until the main screen opens it. */
  private val requestedChat = MutableStateFlow<ChatLocation?>(null)

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    // A recreated activity gets its launch intent again, but that chat was opened the first time.
    if (savedInstanceState == null) requestedChat.value = ChatNotification.location(intent)
    enableEdgeToEdge()
    val graph = appGraph
    setContent {
      val theme by graph.preferences.theme.collectAsStateWithLifecycle()
      FangTheme(theme) {
        Surface(modifier = Modifier.fillMaxSize(), color = fangColors.surface0) {
          FangApp(graph, requestedChat)
        }
      }
    }
  }

  override fun onNewIntent(intent: Intent) {
    super.onNewIntent(intent)
    requestedChat.value = ChatNotification.location(intent)
  }
}

/**
 * Shows the top of the navigator's stack; onboarding is the root until setup completes. Screens
 * swap instantly, with no slide or fade.
 */
@Composable
private fun FangApp(graph: AppGraph, requestedChat: MutableStateFlow<ChatLocation?>) {
  val navigator = rememberSaveable(saver = AppNavigator.Saver) {
    AppNavigator(listOf(if (graph.preferences.setupComplete.value) Destination.Main else Destination.Onboarding))
  }
  val actions = remember(graph) { ShellActions(graph) }
  val stack by navigator.stack.collectAsStateWithLifecycle()
  val holder = rememberSaveableStateHolder()
  val drafts = remember { ProviderDrafts() }
  val chat by requestedChat.collectAsStateWithLifecycle()
  ForgetPopped(stack, holder)
  LaunchedEffect(stack) { if (stack.none(::ownsDraft)) drafts.release() }
  // The main screen opens the requested chat once it is on top; before setup there is no main screen yet.
  LaunchedEffect(chat) {
    if (chat != null && Destination.Main in navigator.stack.value) navigator.popTo(Destination.Main)
  }
  BackHandler(enabled = stack.size > 1) { navigator.pop() }
  // The inbox's pill would cover the rows of a sidebar or explorer, so it waits until they close.
  var sidePanelOpen by remember { mutableStateOf(false) }
  Box(modifier = Modifier.fillMaxSize()) {
    AnimatedContent(
      targetState = stack,
      transitionSpec = { stackTransition(from = initialState.size, to = targetState.size) },
      contentKey = { AppNavigator.key(it.last()) },
      label = "navigation",
    ) { shown ->
      val top = shown.last()
      holder.SaveableStateProvider(AppNavigator.key(top)) {
        Screen(shown, navigator, actions, drafts, requestedChat, onSidePanelOpen = { sidePanelOpen = it })
      }
    }
    ChatInboxOverlay(
      shown = stack.last() == Destination.Main && !sidePanelOpen,
      onOpen = { requestedChat.value = it },
      above = {},
    )
  }
}

/** The top of [shown]; model sub-pages edit the draft of the subscription or provider page below. */
@Composable
private fun Screen(
  shown: List<Destination>,
  navigator: AppNavigator,
  actions: ShellActions,
  drafts: ProviderDrafts,
  requestedChat: MutableStateFlow<ChatLocation?>,
  onSidePanelOpen: (Boolean) -> Unit,
) {
  when (val top = shown.last()) {
    Destination.Main -> MainScreen(
      actions = actions,
      requestedChat = requestedChat,
      onOpenSettings = { navigator.push(Destination.Settings) },
      onOpenProviders = { navigator.push(Destination.Providers) },
      onOpenArchive = { navigator.push(Destination.Archive) },
      onOpenArchivedChat = { navigator.push(Destination.ArchivedChat(it)) },
      onOpenOnboarding = { navigator.push(Destination.Onboarding) },
      onSidePanelOpen = onSidePanelOpen,
    )

    Destination.Archive, is Destination.ArchivedChat -> ArchivePage(top, navigator, actions, requestedChat)

    is Destination.ArchivedSubagent -> ArchivePage(Destination.Archive, navigator, actions, requestedChat)

    Destination.Settings, Destination.Permissions, Destination.Backups, Destination.Advanced, Destination.Licenses,
    is Destination.License,
    -> SettingsPages(top, navigator)

    Destination.Providers -> ProvidersScreen(
      onBack = { navigator.pop() },
      onAdd = { navigator.push(Destination.AddProvider) },
      onOpenSubscription = { navigator.push(Destination.Subscription(it)) },
      onEditProvider = { navigator.push(Destination.ProviderEditor(it)) },
    )

    Destination.AddProvider -> AddProviderScreen(
      onBack = { navigator.pop() },
      onAddCompatible = { navigator.push(Destination.ProviderEditor(null)) },
      onAdded = { navigator.popTo(Destination.Providers) },
    )

    is Destination.Subscription -> SubscriptionScreen(
      form = remember { drafts.subscription(top.id) },
      onBack = { navigator.pop() },
      onAddModels = { navigator.push(Destination.AddModels) },
      onOpenModel = { navigator.push(Destination.ModelEditor(it)) },
    )

    is Destination.ProviderEditor -> ProviderEditorScreen(
      form = remember { drafts.provider(top.id) },
      onBack = { navigator.pop() },
      onDone = { navigator.popTo(Destination.Providers) },
      onAddModels = { navigator.push(Destination.AddModels) },
      onOpenModel = { navigator.push(Destination.ModelEditor(it)) },
    )

    Destination.AddModels -> AddModelsScreen(draft = remember { drafts.owner(shown) }, onBack = { navigator.pop() })

    is Destination.ModelEditor -> ModelEditorScreen(
      draft = remember { drafts.owner(shown) },
      modelId = top.modelId,
      onBack = { navigator.pop() },
    )

    Destination.Onboarding -> OnboardingScreen(
      onFinished = { navigator.replaceAll(Destination.Main) },
      onAddProvider = { navigator.push(Destination.ProviderEditor(null)) },
      // Before setup finishes the wizard is the root; opened from the main screen later, it can be closed.
      onClose = if (shown.size > 1) ({ navigator.pop() }) else null,
    )
  }
}

/** Settings, or one of the pages reached from it that needs nothing but the navigator. */
@Composable
private fun SettingsPages(top: Destination, navigator: AppNavigator) {
  when (top) {
    Destination.Permissions -> PermissionsScreen(onBack = { navigator.pop() })

    Destination.Backups -> BackupsScreen(onBack = { navigator.pop() })

    Destination.Advanced -> AdvancedScreen(onBack = { navigator.pop() })

    Destination.Licenses -> LicensesScreen(
      onBack = { navigator.pop() },
      onOpenLicense = { navigator.push(Destination.License(it)) },
    )

    is Destination.License -> LicenseScreen(path = top.path, onBack = { navigator.pop() })

    else -> SettingsScreen(
      onBack = { navigator.pop() },
      onOpenPermissions = { navigator.push(Destination.Permissions) },
      onOpenProviders = { navigator.push(Destination.Providers) },
      onOpenBackups = { navigator.push(Destination.Backups) },
      onOpenAdvanced = { navigator.push(Destination.Advanced) },
      onOpenLicenses = { navigator.push(Destination.Licenses) },
    )
  }
}

/** The archive list, or one archived chat's transcript; restoring that one reopens it on the main screen. */
@Composable
private fun ArchivePage(
  top: Destination,
  navigator: AppNavigator,
  actions: ShellActions,
  requestedChat: MutableStateFlow<ChatLocation?>,
) {
  if (top is Destination.ArchivedChat) {
    ArchivedChatScreen(
      path = top.path,
      actions = actions,
      onBack = { navigator.pop() },
      onRestored = { requestedChat.value = it },
    )
  } else {
    ArchiveScreen(
      actions = actions,
      onBack = { navigator.pop() },
      onOpenChat = { navigator.push(Destination.ArchivedChat(it)) },
    )
  }
}

/**
 * Screens swap with no animation, so navigation feels instant. The screen underneath shifts a
 * quarter width for depth; the popped screen stays on top while it leaves. Replacing the whole
 * stack cross-fades.
 *
 * AnimatedContent keeps the z-index a screen entered with, so every transition sets it to the
 * screen's stack depth: a deeper screen always draws over a shallower one, however it got there.
 */
private fun stackTransition(from: Int, to: Int): ContentTransform {
  val slide = tween<IntOffset>(durationMillis = 0)
  val fade = tween<Float>(durationMillis = 0)
  val transform = when {
    to > from -> slideInHorizontally(slide) { it } togetherWith slideOutHorizontally(slide) { -it / 4 }
    to < from -> slideInHorizontally(slide) { -it / 4 } togetherWith slideOutHorizontally(slide) { it }
    else -> fadeIn(fade) togetherWith fadeOut(fade)
  }
  return transform.apply { targetContentZIndex = to.toFloat() }
}

/** Drops the saved UI state of screens that left the stack, so reopening one starts fresh. */
@Composable
private fun ForgetPopped(stack: List<Destination>, holder: SaveableStateHolder) {
  val keys = stack.map(AppNavigator::key)
  val previous = remember { mutableListOf<String>() }
  LaunchedEffect(keys) {
    (previous - keys.toSet()).forEach(holder::removeState)
    previous.clear()
    previous.addAll(keys)
  }
}
