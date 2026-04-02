# Add project-specific ProGuard rules here.
#
# For more details, see:
#   http://developer.android.com/guide/developing/tools/proguard.html

# Room-entityt — room-runtime keepaa vain RoomDatabase-aliluokan,
# entity-luokat täytyy keepata erikseen
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao class * { *; }
