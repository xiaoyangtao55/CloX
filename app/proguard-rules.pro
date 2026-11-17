# CloX ProGuard / R8 rules.
#
# R8 renames classes and members aggressively. Everything below is reached by
# *name at runtime* rather than through a static reference, so none of it may be
# renamed — the failures are silent or late (UnsatisfiedLinkError, or a hook
# that simply never fires), which is why the list is explicit.

# JNI: the native side binds by fully-qualified name, e.g.
#   Java_com_yzmm_clox_cocos_CocosManager_nativeInit
#   Java_com_yzmm_clox_crash_NativeCrashHandler_nativeInstall
-keep class com.yzmm.clox.cocos.** { *; }
-keep class com.yzmm.clox.crash.NativeCrashHandler { *; }

# Axmol's Java layer is called from C++ through JNI the same way
# (Java_dev_axmol_lib_AxmolRenderer_*).
-keep class dev.axmol.lib.** { *; }

# LSPosed reads the entry-point class name from assets/xposed_init; renaming
# this class means the PERM_GRANT hook silently stops loading.
-keep class com.yzmm.clox.xposed.** { *; }

# Shizuku loads this Binder service in another process and constructs it by reflection.
# The class literal alone does not keep its otherwise uncalled public constructor.
-keep class com.yzmm.clox.qs.QsUserService { *; }

# Bound to the notification listener service declared in the manifest.
-keep class com.yzmm.clox.overlay.notifications.CloXNotificationListenerService { *; }

# Inflated by class name from res/layout.
-keep class com.google.android.material.slider.** { *; }
-keep class com.google.android.material.internal.** { *; }

# Compile-only dependencies: the real classes come from the platform / LSPosed
# at runtime, so R8 must not warn about them being absent at build time.
-dontwarn org.cocos2dx.**
-dontwarn ax.**
-dontwarn dev.axmol.**
-dontwarn de.robv.android.xposed.**
