package com.github.weisj.darkmode.platform.linux

import com.github.weisj.darkmode.platform.LibraryUtil
import com.github.weisj.darkmode.platform.Notifications
import com.github.weisj.darkmode.platform.OneTimeAction
import com.github.weisj.darkmode.platform.settings.DefaultSettingsContainer
import com.github.weisj.darkmode.platform.settings.SettingsContainerProvider
import com.github.weisj.darkmode.platform.settings.SingletonSettingsContainerProvider
import com.github.weisj.darkmode.platform.settings.group
import com.github.weisj.darkmode.platform.settings.hidden
import com.github.weisj.darkmode.platform.settings.mirrorPreview
import com.github.weisj.darkmode.platform.settings.persistentBooleanProperty
import com.github.weisj.darkmode.platform.settings.persistentChoiceProperty
import com.github.weisj.darkmode.platform.settings.transformerOf
import com.google.auto.service.AutoService

@AutoService(SettingsContainerProvider::class)
class AdvancedLinuxSettingsProvider :
    SingletonSettingsContainerProvider(
        { AdvancedLinuxSettings },
        enabled = LibraryUtil.isLinux || true
    )

enum class ImplementationType(val displayString : String) {
    GTK_XSETTINGS("GTK (xsettings)"),
    GTK_GSETTINGS("GTK-Gnome v.<42 (gsettings)"),
    XDG_DESKTOP("Xdg-Desktop")
}

object AdvancedLinuxSettings : DefaultSettingsContainer(identifier = "advanced_linux_settings") {

    private val advancedSettingsLogAction = OneTimeAction {
        Notifications.dispatchNotification(
            """
            A guess has been made for the monitoring implementation.
            Please select an appropriate value in the settings, which works for you.
            """.trimIndent(),
            showSettingsLink = true
        )
    }

    private fun readImplType(type : ImplementationType) = type.toString()
    private fun parseImplType(typeStr : String) = runCatching {
        ImplementationType.valueOf(typeStr)
    }.getOrElse { guessImplType() }

    private fun guessImplType() = when {
        LibraryUtil.isGNOME -> ImplementationType.GTK_GSETTINGS
        LibraryUtil.isGtk -> ImplementationType.GTK_XSETTINGS
        else -> ImplementationType.XDG_DESKTOP
    }

    private fun supportedImplementations() = buildList {
        if (LibraryUtil.isGtk || overrideGtkDetection) add(ImplementationType.GTK_XSETTINGS)
        if (LibraryUtil.isGNOME || overrideGtkDetection) add(ImplementationType.GTK_GSETTINGS)
        add(ImplementationType.XDG_DESKTOP)
    }

    var implType = guessImplType()

    var overrideGtkDetection = false

    init {
        group("Advanced") {
            persistentChoiceProperty(
                description = "Implementation Type",
                value = ::implType,
                transformer = transformerOf(write = ::parseImplType, read = ::readImplType)
            ) { choicesProvider = ::supportedImplementations; renderer = ImplementationType::displayString; }

            if (!LibraryUtil.isGtk) {
                persistentBooleanProperty(
                    description = "Override Gtk detection (Enforce availability of Gtk implementations)",
                    value = ::overrideGtkDetection
                ).mirrorPreview()
            }
        }

        hidden {
            persistentBooleanProperty(value = advancedSettingsLogAction::executed)
        }
    }

    override fun onSettingsLoaded() {
        if (!overrideGtkDetection) advancedSettingsLogAction()
    }
}
