package com.strangeparticle.springboard.app.settings.items.core

import com.strangeparticle.springboard.app.settings.SettingsGroup
import com.strangeparticle.springboard.app.settings.items.base.StringSettingsItem

object OpenFromS3AwsProfileSetting : StringSettingsItem() {
    override val id = "open_from_s3.aws_profile"
    override val displayName = "Open from S3: Default AWS Profile"
    override val description =
        "Pre-populates the AWS profile field in the Open from S3 dialog. Leave blank to type a profile each time."
    override val group = SettingsGroup.General
    override val defaultValue = ""
}
