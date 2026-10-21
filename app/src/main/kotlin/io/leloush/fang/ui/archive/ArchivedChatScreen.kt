package io.leloush.fang.ui.archive

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.leloush.fang.R
import io.leloush.fang.agent.AgentProcessState
import io.leloush.fang.agent.ChatLocation
import io.leloush.fang.agent.ChatTimeline
import io.leloush.fang.agent.TimelineState
import io.leloush.fang.appGraph
import io.leloush.fang.host.HostRejected
import io.leloush.fang.host.SessionInfo
import io.leloush.fang.ui.ShellActions
import io.leloush.fang.ui.chat.ChatStream
import io.leloush.fang.ui.chat.ChatView
import io.leloush.fang.ui.chat.ToolCallSheet
import io.leloush.fang.ui.components.HeaderBar
import io.leloush.fang.ui.components.IconButton28
import io.leloush.fang.ui.components.LucideIcon
import io.leloush.fang.ui.components.SimmeringPot
import io.leloush.fang.ui.components.SnackbarStack
import io.leloush.fang.ui.components.SnackbarStackState
import io.leloush.fang.ui.sidebar.tildePath
import io.leloush.fang.ui.theme.FangTokens
import io.leloush.fang.ui.theme.fangColors
import io.leloush.fang.ui.workspace.sessionTitle
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONException

private sealed interface Transcript {
  data object Loading : Transcript

  data class Loaded(val state: TimelineState) : Transcript

  data class Failed(val message: String) : Transcript
}

/**
 * The archived chat in session file [path], read from its file without starting Pi: the chat's stream with no
 * composer. Restore takes it out of the archive and reopens it as a live chat through [onRestored]; Delete removes
 * its file. Leaves on its own once the chat is no longer archived.
 */
@Composable
internal fun ArchivedChatScreen(
  path: String,
  actions: ShellActions,
  onBack: () -> Unit,
  onRestored: (ChatLocation) -> Unit,
) {
  val graph = LocalContext.current.appGraph
  val archived by graph.sessionArchive.archived.collectAsStateWithLifecycle()
  val marks by graph.preferences.archivedSessions.collectAsStateWithLifecycle()
  val session = archived.firstOrNull { it.session.path == path }?.session
  val genericError = stringResource(R.string.shell_error_generic)
  val snackbar = remember { SnackbarStackState() }
  val scope = rememberCoroutineScope()
  var attempt by remember { mutableIntStateOf(0) }
  val transcript by rememberTranscript(path, attempt)
  var toolKey by remember { mutableStateOf<String?>(null) }
  var confirmingDelete by remember { mutableStateOf(false) }
  var busy by remember { mutableStateOf(false) }
  LaunchedEffect(marks) { if (path !in marks) onBack() }
  Column(modifier = Modifier.fillMaxSize().background(fangColors.surface0)) {
    TranscriptHeader(session, actions.home, deleteEnabled = !busy, onBack, onDelete = { confirmingDelete = true })
    Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
      TranscriptBody(transcript, onRetry = { attempt += 1 }, onOpenTool = { toolKey = it })
      SnackbarStack(snackbar, Modifier.align(Alignment.BottomCenter).padding(FangTokens.Spacing.s16))
    }
    ReadOnlyBar(
      session = session,
      enabled = !busy,
      onRestore = {
        val restored = session?.let { actions.restore(listOf(it)) }.orEmpty()
        restored.firstOrNull()?.let { onRestored(ChatLocation(it.cwd, it.path)) }
      },
    )
  }
  val tool = toolKey?.let { key -> (transcript as? Transcript.Loaded)?.state?.items?.firstOrNull { it.key == key } }
  // A transcript is settled history, so no tool call has arguments still streaming.
  tool?.let { ToolCallSheet(it, streamingArguments = { null }, onDismiss = { toolKey = null }) }
  if (confirmingDelete) {
    DeleteDialog(
      count = 1,
      onConfirm = {
        confirmingDelete = false
        busy = true
        // The delete outlives this screen, which leaves once the archive no longer holds the chat.
        val result = graph.scope.async { actions.deleteArchived(listOf(path)) }
        scope.launch {
          val failure = result.await() ?: return@launch
          busy = false
          snackbar.show(failure.ifBlank { genericError })
        }
      },
      onDismiss = { confirmingDelete = false },
    )
  }
}

/** The chat's messages from its session file; a new [attempt] reads them again. */
@Composable
private fun rememberTranscript(path: String, attempt: Int): State<Transcript> {
  val repository = LocalContext.current.appGraph.sessionRepository
  val failedToLoad = stringResource(R.string.archive_transcript_failed)
  return produceState<Transcript>(Transcript.Loading, path, attempt) {
    value = Transcript.Loading
    value = try {
      val timeline = ChatTimeline()
      val messages = repository.messages(path)
      val saved = withContext(Dispatchers.IO) { repository.notices.load(path) }
      timeline.restoreNotices { saved }
      // Off the main thread: a long history takes a while to turn into rows.
      withContext(Dispatchers.Default) { timeline.seed(messages) }
      Transcript.Loaded(timeline.state.value)
    } catch (error: IOException) {
      Transcript.Failed(error.message ?: failedToLoad)
    } catch (error: HostRejected) {
      Transcript.Failed(error.error)
    } catch (_: JSONException) {
      Transcript.Failed(failedToLoad)
    }
  }
}

/** Back, the chat's title over its folder, and Delete. */
@Composable
private fun TranscriptHeader(
  session: SessionInfo?,
  fallbackHome: String,
  deleteEnabled: Boolean,
  onBack: () -> Unit,
  onDelete: () -> Unit,
) {
  val hello by LocalContext.current.appGraph.hostClient.lastHello.collectAsStateWithLifecycle()
  val home = hello?.home?.takeIf { it.isNotEmpty() } ?: fallbackHome
  HeaderBar(
    title = session?.let { sessionTitle(it) } ?: stringResource(R.string.archive_title),
    subtitle = session?.let { tildePath(it.cwd, home) },
    leading = {
      IconButton28(
        icon = R.drawable.ic_lucide_chevron_left,
        contentDescription = stringResource(R.string.archive_back),
        onClick = onBack,
      )
    },
    trailing = {
      IconButton28(
        icon = R.drawable.ic_lucide_trash_2,
        contentDescription = stringResource(R.string.archive_delete),
        onClick = onDelete,
        enabled = deleteEnabled,
        tint = fangColors.destructive,
      )
    },
  )
}

@Composable
private fun TranscriptBody(transcript: Transcript, onRetry: () -> Unit, onOpenTool: (String) -> Unit) {
  val colors = fangColors
  when (transcript) {
    Transcript.Loading -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { SimmeringPot() }

    is Transcript.Failed -> Column(
      modifier = Modifier.fillMaxSize().padding(FangTokens.Spacing.s24),
      verticalArrangement = Arrangement.spacedBy(FangTokens.Spacing.s8, Alignment.CenterVertically),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      Text(text = transcript.message, color = colors.foregroundMuted, fontSize = FangTokens.FontSize.base)
      TextButton(onClick = onRetry) { Text(stringResource(R.string.archive_retry), color = colors.accent) }
    }

    is Transcript.Loaded -> {
      if (transcript.state.items.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
          Text(
            text = stringResource(R.string.archive_transcript_empty),
            color = colors.foregroundMuted,
            fontSize = FangTokens.FontSize.base,
          )
        }
        return
      }
      ChatStream(
        timeline = transcript.state,
        processState = AgentProcessState.Ready,
        view = remember { ChatView() },
        onOpenTool = onOpenTool,
        onOpenProviders = {},
        onOpenOnboarding = {},
        onRestart = {},
        backgroundTasks = 0,
      )
    }
  }
}

/** In place of the composer: the chat is archived and read only, with Restore unless its folder is gone. */
@Composable
private fun ReadOnlyBar(session: SessionInfo?, enabled: Boolean, onRestore: () -> Unit) {
  val colors = fangColors
  val restorable = session?.cwdExists == true
  Column(modifier = Modifier.fillMaxWidth().background(colors.surface0).navigationBarsPadding()) {
    HorizontalDivider(thickness = 1.dp, color = colors.border)
    Row(
      modifier = Modifier.fillMaxWidth().padding(
        horizontal = FangTokens.Spacing.s16,
        vertical = FangTokens.Spacing.s12,
      ),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(FangTokens.Spacing.s12),
    ) {
      LucideIcon(icon = R.drawable.ic_lucide_archive, contentDescription = null, tint = colors.foregroundMuted)
      Text(
        text = stringResource(
          if (session != null && !restorable) R.string.archive_read_only_missing else R.string.archive_read_only,
        ),
        modifier = Modifier.weight(1f),
        color = if (session != null && !restorable) colors.destructive else colors.foregroundMuted,
        fontSize = FangTokens.FontSize.sm,
      )
      Button(onClick = onRestore, enabled = enabled && restorable) {
        Text(stringResource(R.string.archive_restore))
      }
    }
  }
}
