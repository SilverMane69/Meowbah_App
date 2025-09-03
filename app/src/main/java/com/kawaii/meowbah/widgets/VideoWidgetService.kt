package com.kawaii.meowbah.widgets

import android.content.Intent
import android.widget.RemoteViewsService

class VideoWidgetService : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory {
        return VideoWidgetViewsFactory(this.applicationContext, intent)
    }
}
