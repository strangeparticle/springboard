on run argv
    set commandLine to item 1 of argv
    tell application "Terminal"
        activate
        -- `do script` with no target opens a fresh window for the command.
        do script commandLine
    end tell
end run
