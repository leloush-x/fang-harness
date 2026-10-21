package io.leloush.fang

import android.app.Application

class FangApplication : Application() {
  internal lateinit var graph: AppGraph
    private set

  override fun onCreate() {
    super.onCreate()
    AppStrings.init(this)
    graph = AppGraph(this)
  }
}
