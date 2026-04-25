on run argv
    set targetPid to (item 1 of argv) as integer
    tell application "System Events"
        set visible of (first process whose unix id is targetPid) to false
    end tell
end run
