package com.strangeparticle.springboard.app.command

import com.strangeparticle.springboard.command.SpringboardCommand
import com.strangeparticle.springboard.command.SpringboardCommandResult

interface SpringboardCommandExecutor {
    suspend fun execute(command: SpringboardCommand): SpringboardCommandResult
}
