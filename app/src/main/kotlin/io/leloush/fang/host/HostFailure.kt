package io.leloush.fang.host

import androidx.annotation.StringRes
import io.leloush.fang.AppStrings
import io.leloush.fang.R
import java.io.IOException

/**
 * Why a Wolfi-side operation could not run. [message] is user-presentable, in the app's language when it is read.
 */
internal sealed class HostFailure(val kind: Kind,
  @StringRes protected val messageRes: Int,
  cause: Throwable? = null,
) :
  IOException(null, cause) {
  /** A failure whose [messageRes] has placeholders overrides this to fill them. */
  override val message: String get() = AppStrings.get(messageRes)

  enum class Kind {
    Runtime,
    Permission,
    NodeMissing,
    NodeTooOld,
    Internal,
    Crashed,
    Timeout,
    Disconnected,
  }

  /** Fixed by a step of onboarding rather than by retrying. */
  val needsSetup: Boolean get() = kind in SETUP_KINDS

  /** Fixed in setup rather than by retrying; a host connecting proves the fix. */
  val fixedBySetup: Boolean get() = needsSetup

  /** Wolfi OS is not installed or its files are incomplete. */
  class Runtime(cause: Throwable? = null) : HostFailure(Kind.Runtime, R.string.host_failure_runtime, cause)

  /** Android refused to execute PRoot, usually because the runtime files were removed. */
  class Permission(cause: Throwable? = null) : HostFailure(Kind.Permission, R.string.host_failure_permission, cause)

  class NodeMissing : HostFailure(Kind.NodeMissing, R.string.host_failure_node_missing)

  class NodeTooOld : HostFailure(Kind.NodeTooOld, R.string.host_failure_node_too_old)

  class Internal : HostFailure(Kind.Internal, R.string.host_failure_internal)

  class Crashed(val exitCode: Int) : HostFailure(Kind.Crashed, R.string.host_failure_crashed) {
    override val message: String get() = AppStrings.get(messageRes, exitCode)
  }

  class Timeout : HostFailure(Kind.Timeout, R.string.host_failure_timeout)

  class Disconnected(cause: Throwable? = null) :
    HostFailure(Kind.Disconnected, R.string.host_failure_disconnected, cause = cause)

  private companion object {
    val SETUP_KINDS = setOf(Kind.Runtime, Kind.Permission, Kind.NodeMissing, Kind.NodeTooOld)
  }
}
