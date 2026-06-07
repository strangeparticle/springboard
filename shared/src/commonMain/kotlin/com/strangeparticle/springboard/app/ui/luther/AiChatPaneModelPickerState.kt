package com.strangeparticle.springboard.app.ui.luther

import com.strangeparticle.springboard.app.settings.DropDownOption

internal data class AiChatPaneModelPickerState(
    val selectedModelId: String,
    val selectedModelLabel: String,
    val options: List<DropDownOption>,
    val isLoading: Boolean,
    val errorMessage: String?,
    val onRefresh: () -> Unit,
    val onSelectModel: (String) -> Unit,
)
