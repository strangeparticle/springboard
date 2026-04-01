package com.strangeparticle.springboard.app.acceptance

import kotlin.test.Test

class SettingsTests {

    @Test fun `status bar gear icon opens settings screen`() =
        SettingsTestScenarios.statusBarGearIconOpensSettingsScreen()

    @Test fun `settings back button returns to main screen`() =
        SettingsTestScenarios.settingsBackButtonReturnsToMainScreen()

    @Test fun `active settings back button returns to settings when opened from settings link`() =
        SettingsTestScenarios.activeSettingsBackButtonReturnsToSettingsWhenOpenedFromSettingsLink()

    @Test fun `active settings back button returns to previous screen when opened directly`() =
        SettingsTestScenarios.activeSettingsBackButtonReturnsToPreviousScreenWhenOpenedDirectly()

    @Test fun `overridden settings disabled with warning - overridden settings appear visually disabled`() =
        SettingsTestScenarios.overriddenSettingsAppearVisuallyDisabled()

    @Test fun `overridden settings disabled with warning - overridden settings show override warning`() =
        SettingsTestScenarios.overriddenSettingsShowOverrideWarning()

    @Test fun `override warning link navigates to active settings`() =
        SettingsTestScenarios.overrideWarningLinkNavigatesToActiveSettings()

    @Test fun `active settings resolved source labels - active settings shows default source label`() =
        SettingsTestScenarios.activeSettingsShowsDefaultSourceLabel()

    @Test fun `active settings resolved source labels - active settings shows user source label`() =
        SettingsTestScenarios.activeSettingsShowsUserSourceLabel()

    @Test fun `active settings resolved source labels - active settings shows env-var source label`() =
        SettingsTestScenarios.activeSettingsShowsEnvVarSourceLabel()

    @Test fun `active settings resolved source labels - active settings shows command-line source label`() =
        SettingsTestScenarios.activeSettingsShowsCommandLineSourceLabel()

    @Test fun `active settings startup springboard path display - active settings shows path for startup springboard`() =
        SettingsTestScenarios.activeSettingsShowsPathForStartupSpringboard()

    @Test fun `active settings startup springboard path display - hover tooltip reveals full startup springboard path`() =
        SettingsTestScenarios.hoverTooltipRevealsFullStartupSpringboardPath()

    @Test fun `startup springboard from command line - startup-springboard flag sets startup springboard`() =
        SettingsTestScenarios.startupSpringboardFlagSetsStartupSpringboard()

    @Test fun `startup springboard from command line - active settings shows CLI as source for startup springboard`() =
        SettingsTestScenarios.activeSettingsShowsCliAsSourceForStartupSpringboard()

    @Test fun `positional CLI path does not count as startup springboard override`() =
        SettingsTestScenarios.positionalCliPathDoesNotCountAsStartupSpringboardOverride()

    @Test fun `use current file stores current file as startup springboard`() =
        SettingsTestScenarios.useCurrentFileStoresCurrentFileAsStartupSpringboard()

    @Test fun `clear removes user-configured startup springboard`() =
        SettingsTestScenarios.clearRemovesUserConfiguredStartupSpringboard()

    @Test fun `settings persistence - startup springboard setting persists across relaunch`() =
        SettingsTestScenarios.startupSpringboardSettingPersistsAcrossRelaunch()

    @Test fun `settings persistence - surface AppleScript errors setting persists across relaunch`() =
        SettingsTestScenarios.surfaceAppleScriptErrorsSettingPersistsAcrossRelaunch()

    @Test fun `settings persistence - reset keynav-after-keynav-activation setting persists across relaunch`() =
        SettingsTestScenarios.resetKeyNavAfterKeyNavActivationSettingPersistsAcrossRelaunch()

    @Test fun `settings persistence - reset keynav-after-gridnav-activation setting persists across relaunch`() =
        SettingsTestScenarios.resetKeyNavAfterGridNavActivationSettingPersistsAcrossRelaunch()
}
