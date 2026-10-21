package io.leloush.fang.host

import android.content.Context
import android.util.Log
import io.leloush.fang.wolfi.WolfiRuntime
import java.io.IOException
import java.security.SecureRandom
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Starts one short-lived `host.mjs` in the app's own Wolfi OS and waits for it to connect back to [HostServer].
 *
 * The host is a child process of Fang Harness, run through the bundled PRoot and Node.js, so no other app, terminal,
 * background service or boot script has to be installed. The token still travels only through the environment, and
 * the loopback handshake is unchanged.
 */
internal object HostLauncher {
  private const val HOST_ASSET = "fang-host.mjs"
  private const val CONNECT_TIMEOUT_MILLIS = 30_000L
  private const val TOKEN_BYTES = 32
  private val random = SecureRandom()
  private val monitorScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

  suspend fun launch(context: Context, mode: String): HostChannel = withContext(Dispatchers.IO) {
    val runtime = WolfiRuntime(context)
    if (!runtime.status().installed) throw HostFailure.Runtime()
    try {
      runtime.installHost(context.assets.open(HOST_ASSET))
    } catch (failure: IOException) {
      Log.e(TAG, "Could not write the Wolfi host: ${failure.message}")
      throw HostFailure.Runtime()
    }
    val server = HostServer.shared
    val token = ByteArray(TOKEN_BYTES).also(random::nextBytes).toHexString()
    val connected = server.register(token)
    var process: Process? = null
    var channel: HostChannel? = null
    try {
      val started = try {
        runtime.startHost(mode, server.port, token)
      } catch (failure: IOException) {
        Log.e(TAG, "Could not start the Wolfi host: ${failure.message}", failure)
        throw if (failure.message?.contains("denied", ignoreCase = true) == true) {
          HostFailure.Permission()
        } else {
          HostFailure.Runtime()
        }
      } catch (_: SecurityException) {
        throw HostFailure.Permission()
      }
      process = started
      val exitMonitor = monitorScope.launch {
        val code = started.waitFor()
        if (!connected.isCompleted) {
          Log.e(TAG, "Host $mode exited with $code before connecting: ${runtime.hostLogTail()}")
          connected.completeExceptionally(HostFailure.Crashed(code))
        }
      }
      val result = try {
        withTimeoutOrNull(CONNECT_TIMEOUT_MILLIS) { connected.await() }
      } finally {
        exitMonitor.cancel()
      }
      if (result == null) {
        Log.e(TAG, "Host $mode did not connect: ${runtime.hostLogTail()}")
        throw HostFailure.Timeout()
      }
      channel = result
      result
    } finally {
      server.unregister(token)
      if (channel == null) {
        connected.cancel()
        process?.destroyForcibly()
      }
    }
  }

  private const val TAG = "HostLauncher"
}
