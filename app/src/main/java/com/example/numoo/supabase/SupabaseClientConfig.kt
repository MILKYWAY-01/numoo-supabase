package com.example.numoo.supabase

import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.gotrue.Auth
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.realtime.Realtime

object SupabaseClientConfig {
    val supabaseUrl = "https://yqpmqzixztibjaycfqgr.supabase.co"
    // Use Supabase "Publishable key" (anon key) from your project settings.
    // If you rotate keys, update this value as well.
    // NOTE: The Supabase client expects the JWT-style anon key (eyJ...).
    // Using the "sb_publishable_*" key can cause auth failures depending on the SDK.
    val supabaseKey = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InlxcG1xeml4enRpYmpheWNmcWdyIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NzQ3NzAxNzEsImV4cCI6MjA5MDM0NjE3MX0.yEgvmUxXe4d6RH5MrqF7UieLwP1qRy9mHlTIDks6tYc"

    val client = createSupabaseClient(
        supabaseUrl = supabaseUrl,
        supabaseKey = supabaseKey
    ) {
        install(Auth)
        install(Postgrest)
        install(Realtime)
    }
}
