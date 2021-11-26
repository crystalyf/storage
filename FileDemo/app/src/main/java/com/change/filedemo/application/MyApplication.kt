package com.change.filedemo.application

import android.app.Activity
import android.app.Application
import android.content.Context
import android.os.Bundle
import kotlin.properties.Delegates

/**
 * Created by xingjunchao on 2020/08/24.
 */
class MyApplication : Application(), Application.ActivityLifecycleCallbacks {

    override fun onCreate() {
        super.onCreate()
        instance = this
        registerActivityLifecycleCallbacks(this)
    }

    companion object {
        @get:Synchronized
        var instance: MyApplication by Delegates.notNull()

    }

    override fun onActivityPaused(activity: Activity) {

    }

    override fun onActivityStarted(activity: Activity) {

    }

    override fun onActivityDestroyed(activity: Activity) {

    }

    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {

    }

    override fun onActivityStopped(activity: Activity) {

    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {

    }

    override fun onActivityResumed(activity: Activity) {

    }

}