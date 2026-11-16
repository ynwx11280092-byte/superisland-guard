# Xposed 入口类不能被混淆/优化掉
-keep class com.guard.superisland.MainHook { *; }
-keep class * extends de.robv.android.xposed.IXposedHookLoadPackage { *; }
-keep class * extends de.robv.android.xposed.IXposedHookZygoteInit { *; }
