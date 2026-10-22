package io.leloush.fang

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import io.leloush.fang.agent.AgentProcess
import io.leloush.fang.agent.AgentProcessState
import io.leloush.fang.agent.AgentRegistry
import io.leloush.fang.agent.AgentService
import io.leloush.fang.agent.AppVisibility
import io.leloush.fang.agent.ChatInbox
import io.leloush.fang.agent.DialogNotifications
import io.leloush.fang.agent.KeepAlive
import io.leloush.fang.agent.TurnNotifications
import io.leloush.fang.data.AccountRepository
import io.leloush.fang.data.ConfigBackups
import io.leloush.fang.data.ModelDefaults
import io.leloush.fang.data.NoticeStore
import io.leloush.fang.data.PiExtensionUpdate
import io.leloush.fang.data.Preferences
import io.leloush.fang.data.SessionArchive
import io.leloush.fang.data.SessionRepository
import io.leloush.fang.files.FileTabRegistry
import io.leloush.fang.git.CommitTabRegistry
import io.leloush.fang.host.FileHost
import io.leloush.fang.host.GitHost
import io.leloush.fang.host.HostClient
import io.leloush.fang.terminal.TerminalRegistry
import io.leloush.fang.ui.chat.ChatViews
import io.leloush.fang.wolfi.WolfiRuntime
import io.leloush.fang.workspace.OpenTabs
import io.leloush.fang.workspace.Panes
import io.leloush.fang.workspace.WorkspaceLayout
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/** The app's long-lived objects, built once by [FangApplication]. Reach it with [Context.appGraph]. */
internal class AppGraph(private val application: Application) {
  /** Lives as long as the process; main-thread confined. */
  val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
  val hostClient = HostClient(
    application,
    foreground = ProcessLifecycleOwner.get().lifecycle.currentStateFlow.map { it.isAtLeast(Lifecycle.State.STARTED) },
    scope = scope,
  )
  val keepAlive = KeepAlive(application)
  val wolfi = WolfiRuntime(application)
  val preferences = Preferences(application)
  val sessionRepository = SessionRepository(hostClient, NoticeStore(File(application.filesDir, "notices")), scope)
  val sessionArchive = SessionArchive(sessionRepository, preferences, scope)
  private val visibility = AppVisibility(application)

  /** The chats the user sees: those the panes show while Fang Harness is on screen and nothing covers them. */
  val chatsOnScreen = MutableStateFlow<Set<AgentProcess>>(emptySet())
  val chatInbox = ChatInbox()
  private val turnNotifications = TurnNotifications(application, visibility, chatInbox) {
    hostClient.lastHello.value?.home
  }
  val registry: AgentRegistry<AgentProcess> =
    AgentRegistry(
      scope,
      maxRunning = preferences.maxRunningChats,
      idleParkMillis = preferences.idleStop.map { it.millis },
      onWorkEnded = turnNotifications::workEnded,
    ) { cwd, sessionPath ->
      AgentProcess(hostClient, keepAlive, cwd, sessionPath, scope, sessionRepository, preferences.disabledExtensions)
    }
  val accountRepository = AccountRepository(hostClient, registry, keepAlive)
  val configBackups = ConfigBackups(hostClient, registry, keepAlive, accountRepository, sessionRepository, scope)
  val piExtensionUpdate = PiExtensionUpdate(hostClient, registry, keepAlive, sessionRepository, scope)
  val modelDefaults = ModelDefaults(hostClient)
  val panes = Panes(registry, sessionArchive::isArchived)
  val tabs = OpenTabs(panes)
  val terminals = TerminalRegistry(application, hostClient, scope, tabs)
  val fileHost = FileHost(hostClient, scope)
  val files = FileTabRegistry(fileHost, tabs)
  val gitHost = GitHost(hostClient, scope)
  val commits = CommitTabRegistry(gitHost, tabs, preferences.trustedRepositories, scope)
  val layout = WorkspaceLayout(this)

  /** What the chat screens keep of each chat, such as unsent drafts, held here so it survives rotation and folding. */
  val chatViews = ChatViews()

  init {
    // The host that answers the questions asked most is started now, in the background: loading Pi's own software
    // takes about ten seconds under PRoot, and doing it here means the screens that need it find it ready.
    hostClient.warm()
    // The app's home is the runtime's own root now; what was saved on Android's shared storage follows it, once.
    preferences.adoptHome(WolfiRuntime.WOLFI_ROOT, LEGACY_HOME)
    holdWhileWorking()
    restartChatsOnceSetupWorks()
    feedChatInbox()
    val dialogNotifications = DialogNotifications(application, registry, chatsOnScreen, visibility, chatInbox) {
      hostClient.lastHello.value?.home
    }
    scope.launch { dialogNotifications.watch() }
    ProcessLifecycleOwner.get().lifecycle.addObserver(
      object : DefaultLifecycleObserver {
        override fun onStart(owner: LifecycleOwner) {
          // Before setup, the Wolfi runtime may not be able to run the host yet.
          if (preferences.setupComplete.value) sessionRepository.refresh()
          // Events may have gone missing while Fang Harness was in the background.
          for (process in registry.processes.value.values) scope.launch { process.resync() }
        }
      },
    )
  }

  /**
   * Keeps [chatInbox] following the setting, the archived chats, and the session files of the chats on screen, which
   * Pi can name late.
   */
  @OptIn(ExperimentalCoroutinesApi::class)
  private fun feedChatInbox() {
    scope.launch { preferences.inAppNotifications.collect { chatInbox.enabled = it } }
    scope.launch { preferences.archivedSessions.collect { chatInbox.setArchived(it.keys) } }
    scope.launch {
      chatsOnScreen
        .flatMapLatest { chats ->
          if (chats.isEmpty()) {
            flowOf(emptySet())
          } else {
            combine(chats.map { it.sessionFile }) { files -> files.filterNotNullTo(HashSet()) }
          }
        }
        .collect(chatInbox::setOnScreen)
    }
  }

  /**
   * A host that connects after a failure fixed by setup proves the fix, so chats that failed on it start again instead
   * of waiting on Retry.
   */
  private fun restartChatsOnceSetupWorks() {
    scope.launch {
      var setupBroken = false
      hostClient.lastFailure.collect { failure ->
        if (failure != null) {
          setupBroken = setupBroken || failure.fixedBySetup
          return@collect
        }
        if (!setupBroken) return@collect
        setupBroken = false
        for (process in registry.processes.value.values) {
          val state = process.state.value
          if (state is AgentProcessState.Failed && state.failure?.fixedBySetup == true) process.restart()
        }
      }
    }
  }

  /**
   * One [KeepAlive] acquisition while any chat holds its Pi (it works or waits on background tasks, with a short grace
   * after; see [AgentProcess.holdsPi]) or any terminal runs a command, so switching apps does not stop them. The
   * notification follows which of them keeps Fang Harness awake, also while sign-in or a Pi install keeps it up.
   */
  private fun holdWhileWorking() {
    scope.launch {
      var hold: AutoCloseable? = null
      // Busy only changes the notification, which tells a working chat from one waiting on background tasks.
      combine(registry.busy, registry.holding, terminals.busy, ::Triple).collect { (_, chats, terminalsBusy) ->
        if (chats || terminalsBusy) {
          if (hold == null) hold = keepAlive.acquire(if (chats) "chats working" else "terminal command")
        } else {
          hold?.close()
          hold = null
        }
        // The service may still run for another holder; a no-op once it has stopped.
        AgentService.refresh(application)
      }
    }
  }
}

internal val Context.appGraph: AppGraph get() = (applicationContext as FangApplication).graph

/**
 * Android's shared storage, which was the app's home before the runtime's own root was; [Preferences.adoptHome] moves
 * what was saved there, once. Shared storage itself is untouched, and the folder browser still offers it.
 */
@SuppressLint("SdCardPath")
private const val LEGACY_HOME = "/sdcard"
