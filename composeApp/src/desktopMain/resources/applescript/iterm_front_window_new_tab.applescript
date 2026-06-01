on run argv
    set commandLine to item 1 of argv
    tell application "iTerm"
        activate
        -- iTerm's AppleScript has no "respect the user's new-session preference"
        -- verb, so the least-prescriptive reliable option is a new tab in the
        -- current window (or a new window when none is open).
        --
        -- Capture the session we create and write to *that* reference, so a
        -- focus change by the user can never redirect the command elsewhere.
        if (count of windows) = 0 then
            set newWindow to (create window with default profile)
            set targetSession to current session of newWindow
        else
            tell current window
                set newTab to (create tab with default profile)
            end tell
            set targetSession to current session of newTab
        end if
        tell targetSession
            write text commandLine
        end tell
    end tell
end run
