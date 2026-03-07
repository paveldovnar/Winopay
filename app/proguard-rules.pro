# ═══════════════════════════════════════════════════════════════════
# WinoPay ProGuard Rules
# ═══════════════════════════════════════════════════════════════════

# ─────────────────────────────────────────────────────────────────────
# ROOM DATABASE
# ─────────────────────────────────────────────────────────────────────
# Keep Room entities and DAOs
-keep class * extends androidx.room.RoomDatabase { *; }
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao interface * { *; }

# Keep migration classes (critical for data integrity)
-keep class * extends androidx.room.migration.Migration { *; }

# ─────────────────────────────────────────────────────────────────────
# SOLANA / MOBILE WALLET ADAPTER
# ─────────────────────────────────────────────────────────────────────
-keep class org.sol4k.** { *; }
-keep class com.funkatronics.** { *; }
-keep class com.solana.mobilewalletadapter.** { *; }

# ─────────────────────────────────────────────────────────────────────
# ZXING (QR CODE)
# ─────────────────────────────────────────────────────────────────────
-keep class com.google.zxing.** { *; }

# ─────────────────────────────────────────────────────────────────────
# COIL (IMAGE LOADING)
# ─────────────────────────────────────────────────────────────────────
-keep class coil.** { *; }

# ─────────────────────────────────────────────────────────────────────
# KOTLIN COROUTINES
# ─────────────────────────────────────────────────────────────────────
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}

# ─────────────────────────────────────────────────────────────────────
# DATASTORE
# ─────────────────────────────────────────────────────────────────────
-keep class androidx.datastore.** { *; }

# ─────────────────────────────────────────────────────────────────────
# WORKMANAGER
# ─────────────────────────────────────────────────────────────────────
-keep class * extends androidx.work.Worker { *; }
-keep class * extends androidx.work.ListenableWorker { *; }

# ─────────────────────────────────────────────────────────────────────
# JSON PARSING (org.json used in RPC calls)
# ─────────────────────────────────────────────────────────────────────
-keep class org.json.** { *; }

# ─────────────────────────────────────────────────────────────────────
# WINOPAY DATA CLASSES (for reflection/serialization safety)
# ─────────────────────────────────────────────────────────────────────
-keep class com.winopay.data.local.** { *; }
-keep class com.winopay.payments.** { *; }
