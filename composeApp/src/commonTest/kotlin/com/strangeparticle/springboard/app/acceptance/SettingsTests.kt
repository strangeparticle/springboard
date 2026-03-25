package com.strangeparticle.springboard.app.acceptance

import kotlin.test.Ignore
import kotlin.test.Test

class SettingsTests {

    class GearIconOpensSettings {
        @Ignore @Test fun `status bar gear icon opens settings screen`() =
            SettingsTestScenarios.statusBarGearIconOpensSettingsScreen()
    }

    class SettingsBackButtonReturnsToMain {
        @Ignore @Test fun `settings back button returns to main screen`() =
            SettingsTestScenarios.settingsBackButtonReturnsToMainScreen()
    }

    class ActiveSettingsBackButtonReturnsToSettings {
        @Ignore @Test fun `active settings back button returns to settings when opened from settings link`() =
            SettingsTestScenarios.activeSettingsBackButtonReturnsToSettingsWhenOpenedFromSettingsLink()
    }

    class ActiveSettingsBackButtonReturnsToPreviousScreen {
        @Ignore @Test fun `active settings back button returns to previous screen when opened directly`() =
            SettingsTestScenarios.activeSettingsBackButtonReturnsToPreviousScreenWhenOpenedDirectly()
    }

    class OverriddenSettingsDisabledWithWarning {
        @Ignore @Test fun `overridden settings appear visually disabled`() =
            SettingsTestScenarios.overriddenSettingsAppearVisuallyDisabled()

        @Ignore @Test fun `overridden settings show override warning`() =
            SettingsTestScenarios.overriddenSettingsShowOverrideWarning()
    }

    class OverrideWarningLinkOpensActiveSettings {
        @Ignore @Test fun `override warning link navigates to active settings`() =
            SettingsTestScenarios.overrideWarningLinkNavigatesToActiveSettings()
    }

    class ActiveSettingsResolvedSourceLabels {
        @Ignore @Test fun `active settings shows default source label`() =
            SettingsTestScenarios.activeSettingsShowsDefaultSourceLabel()

        @Ignore @Test fun `active settings shows user source label`() =
            SettingsTestScenarios.activeSettingsShowsUserSourceLabel()

        @Ignore @Test fun `active settings shows env-var source label`() =
            SettingsTestScenarios.activeSettingsShowsEnvVarSourceLabel()

        @Ignore @Test fun `active settings shows command-line source label`() =
            SettingsTestScenarios.activeSettingsShowsCommandLineSourceLabel()
    }

    class ActiveSettingsStartupSpringboardPathDisplay {
        @Ignore @Test fun `active settings shows path for startup springboard`() =
            SettingsTestScenarios.activeSettingsShowsPathForStartupSpringboard()

        @Ignore @Test fun `hover tooltip reveals full startup springboard path`() =
            SettingsTestScenarios.hoverTooltipRevealsFullStartupSpringboardPath()
    }

    class StartupSpringboardFromCommandLine {
        @Ignore @Test fun `startup-springboard flag sets startup springboard`() =
            SettingsTestScenarios.startupSpringboardFlagSetsStartupSpringboard()

        @Ignore @Test fun `active settings shows CLI as source for startup springboard`() =
            SettingsTestScenarios.activeSettingsShowsCliAsSourceForStartupSpringboard()
    }

    class PositionalPathNotStartupOverride {
        @Ignore @Test fun `positional CLI path does not count as startup springboard override`() =
            SettingsTestScenarios.positionalCliPathDoesNotCountAsStartupSpringboardOverride()
    }

    class StartupSpringboardUseCurrentFile {
        @Ignore @Test fun `use current file stores current file as startup springboard`() =
            SettingsTestScenarios.useCurrentFileStoresCurrentFileAsStartupSpringboard()
    }

    class StartupSpringboardClear {
        @Ignore @Test fun `clear removes user-configured startup springboard`() =
            SettingsTestScenarios.clearRemovesUserConfiguredStartupSpringboard()
    }

    class SettingsPersistence {
        @Ignore @Test fun `startup springboard setting persists across relaunch`() =
            SettingsTestScenarios.startupSpringboardSettingPersistsAcrossRelaunch()

        @Ignore @Test fun `surface AppleScript errors setting persists across relaunch`() =
            SettingsTestScenarios.surfaceAppleScriptErrorsSettingPersistsAcrossRelaunch()

        @Ignore @Test fun `reset keynav-after-keynav-activation setting persists across relaunch`() =
            SettingsTestScenarios.resetKeyNavAfterKeyNavActivationSettingPersistsAcrossRelaunch()

        @Ignore @Test fun `reset keynav-after-gridnav-activation setting persists across relaunch`() =
            SettingsTestScenarios.resetKeyNavAfterGridNavActivationSettingPersistsAcrossRelaunch()
    }
}
