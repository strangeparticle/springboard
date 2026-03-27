package com.strangeparticle.springboard.app.acceptance

import kotlin.test.Ignore
import kotlin.test.Test

class SettingsTests {

    @Ignore @Test fun `status bar gear icon opens settings screen`() =
        SettingsTestScenarios.statusBarGearIconOpensSettingsScreen()

    @Ignore @Test fun `settings back button returns to main screen`() =
        SettingsTestScenarios.settingsBackButtonReturnsToMainScreen()

    @Ignore @Test fun `active settings back button returns to settings when opened from settings link`() =
        SettingsTestScenarios.activeSettingsBackButtonReturnsToSettingsWhenOpenedFromSettingsLink()

    @Ignore @Test fun `active settings back button returns to previous screen when opened directly`() =
        SettingsTestScenarios.activeSettingsBackButtonReturnsToPreviousScreenWhenOpenedDirectly()

    @Ignore @Test fun `overridden settings disabled with warning - overridden settings appear visually disabled`() =
        SettingsTestScenarios.overriddenSettingsAppearVisuallyDisabled()

    @Ignore @Test fun `overridden settings disabled with warning - overridden settings show override warning`() =
        SettingsTestScenarios.overriddenSettingsShowOverrideWarning()

    @Ignore @Test fun `override warning link navigates to active settings`() =
        SettingsTestScenarios.overrideWarningLinkNavigatesToActiveSettings()

    @Ignore @Test fun `active settings resolved source labels - active settings shows default source label`() =
        SettingsTestScenarios.activeSettingsShowsDefaultSourceLabel()

    @Ignore @Test fun `active settings resolved source labels - active settings shows user source label`() =
        SettingsTestScenarios.activeSettingsShowsUserSourceLabel()

    @Ignore @Test fun `active settings resolved source labels - active settings shows env-var source label`() =
        SettingsTestScenarios.activeSettingsShowsEnvVarSourceLabel()

    @Ignore @Test fun `active settings resolved source labels - active settings shows command-line source label`() =
        SettingsTestScenarios.activeSettingsShowsCommandLineSourceLabel()

    @Ignore @Test fun `active settings startup springboard path display - active settings shows path for startup springboard`() =
        SettingsTestScenarios.activeSettingsShowsPathForStartupSpringboard()

    @Ignore @Test fun `active settings startup springboard path display - hover tooltip reveals full startup springboard path`() =
        SettingsTestScenarios.hoverTooltipRevealsFullStartupSpringboardPath()

    @Ignore @Test fun `startup springboard from command line - startup-springboard flag sets startup springboard`() =
        SettingsTestScenarios.startupSpringboardFlagSetsStartupSpringboard()

    @Ignore @Test fun `startup springboard from command line - active settings shows CLI as source for startup springboard`() =
        SettingsTestScenarios.activeSettingsShowsCliAsSourceForStartupSpringboard()

    @Ignore @Test fun `positional CLI path does not count as startup springboard override`() =
        SettingsTestScenarios.positionalCliPathDoesNotCountAsStartupSpringboardOverride()

    @Ignore @Test fun `use current file stores current file as startup springboard`() =
        SettingsTestScenarios.useCurrentFileStoresCurrentFileAsStartupSpringboard()

    @Ignore @Test fun `clear removes user-configured startup springboard`() =
        SettingsTestScenarios.clearRemovesUserConfiguredStartupSpringboard()

    @Ignore @Test fun `settings persistence - startup springboard setting persists across relaunch`() =
        SettingsTestScenarios.startupSpringboardSettingPersistsAcrossRelaunch()

    @Ignore @Test fun `settings persistence - surface AppleScript errors setting persists across relaunch`() =
        SettingsTestScenarios.surfaceAppleScriptErrorsSettingPersistsAcrossRelaunch()

    @Ignore @Test fun `settings persistence - reset keynav-after-keynav-activation setting persists across relaunch`() =
        SettingsTestScenarios.resetKeyNavAfterKeyNavActivationSettingPersistsAcrossRelaunch()

    @Ignore @Test fun `settings persistence - reset keynav-after-gridnav-activation setting persists across relaunch`() =
        SettingsTestScenarios.resetKeyNavAfterGridNavActivationSettingPersistsAcrossRelaunch()
}
