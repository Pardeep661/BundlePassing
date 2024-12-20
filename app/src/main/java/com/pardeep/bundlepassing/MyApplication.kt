package com.pardeep.bundlepassing

import android.app.Application
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.storage.Storage

class MyApplication : Application() {
    lateinit var supabaseClient: SupabaseClient

    override fun onCreate() {
        super.onCreate()
        val supaBaseUrl = "https://lecfqdaoullqqrkximvq.supabase.co"
        val supaBaseKey = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImxlY2ZxZGFvdWxscXFya3hpbXZxIiwicm9sZSI6ImFub24iLCJpYXQiOjE3MzI2ODY4MDQsImV4cCI6MjA0ODI2MjgwNH0.Myd_8-Exs9LkCpNITCbL5mnz2548nsJkFooslTkmW2Y"

        supabaseClient = createSupabaseClient(
            supabaseUrl = supaBaseUrl,
            supabaseKey = supaBaseKey
        ){
            install(Storage)
        }
    }
}