on run argv
    set commandLine to item 1 of argv
    -- Bake the command into the session's initial process instead of creating a
    -- session and then racing to "write text" into it. On machines with heavy
    -- shell configs, a "write text" sent before .zshrc finishes loading gets
    -- swallowed into the login banner and never runs. Launching the session
    -- *with* the command eliminates the race: the command is the session's
    -- process. After it runs, "exec /bin/zsh -l" replaces the process with a
    -- fresh interactive login shell so the user lands in their normal environment.
    set shellCmd to "/bin/zsh -c " & quoted form of (commandLine & " && exec /bin/zsh -l")
    tell application "iTerm"
        activate
        create window with default profile command shellCmd
    end tell
end run
