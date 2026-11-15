package com.yzmm.clox.settings.compose

/** Settings navigation graph routes (string routes, Wear Navigation 1.6). */
object SettingsRoutes {
    const val ROOT = "root"
    const val NOTIFICATIONS = "notifications"
    const val APP_LIST = "app_list"
    const val ANIMATIONS = "animations"
    const val WATCHFACE = "watchface"
    const val GESTURES = "gestures"
    const val APPEARANCE = "appearance"
    const val TAP_ACTIONS = "tap_actions"
    const val WEATHER = "weather"
    const val LOCATION = "location"
    const val HEALTH = "health"
    const val SENSORS = "sensors"
    const val HIDE_APPS = "hide_apps"
    const val FOLDERS = "folders"
    const val FOLDER_MEMBERS = "folder_members/{folderId}"
    const val RTOS = "rtos"
    const val DEBUG = "debug"
    const val ABOUT = "about"

    fun folderMembers(folderId: String) = "folder_members/$folderId"
}
