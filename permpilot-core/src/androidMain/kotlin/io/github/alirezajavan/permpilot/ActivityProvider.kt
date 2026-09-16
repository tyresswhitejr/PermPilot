package io.github.alirezajavan.permpilot

import android.app.Activity

class ActivityProvider private constructor() {

    companion object {
        fun create() = ActivityProvider()
    }

    private var activity: Activity? = null

    fun current(): Activity? = activity

    fun update(activity: Activity) {
        this.activity = activity
    }

    fun clear(activity: Activity) {
        if(this.activity === activity) {
            this.activity = null
        }
    }
}