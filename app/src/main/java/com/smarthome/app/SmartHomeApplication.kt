package com.smarthome.app

import android.app.Application
import com.google.firebase.FirebaseApp

class SmartHomeApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        FirebaseApp.initializeApp(this)
    }
}
