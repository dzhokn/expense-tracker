# Room
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-keep @androidx.room.Dao class * { *; }
-dontwarn androidx.room.paging.**

# kotlin-csv
-keep class com.github.doyaaaaaken.** { *; }

# Glance widget
-keep class * extends androidx.glance.appwidget.GlanceAppWidget
-keep class * extends androidx.glance.appwidget.GlanceAppWidgetReceiver

# WorkManager
-keep class * extends androidx.work.Worker
-keep class * extends androidx.work.ListenableWorker

# Compose - keep data classes used in state
-keepclassmembers class * {
    @androidx.compose.runtime.Composable <methods>;
}

# App entities
-keep class com.example.expensetracker.data.entity.** { *; }
