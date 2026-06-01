on run argv
    set commandLine to item 1 of argv
    tell application "iTerm"
        activate
        -- Hold a reference to the session we create and write to *that*, so a
        -- focus change by the user can never redirect the command elsewhere.
        set newWindow to (create window with default profile)
        set targetSession to current session of newWindow
        tell targetSession
            write text commandLine
        end tell
    end tell
end run
