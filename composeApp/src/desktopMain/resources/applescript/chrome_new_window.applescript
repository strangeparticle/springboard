set oldCount to 0

tell application "Google Chrome"
    activate
    set oldCount to count of windows
    make new window
end tell

repeat 40 times
    tell application "Google Chrome"
        if (count of windows) > oldCount and frontmost then
            return "ok"
        end if
    end tell
    delay 0.05
end repeat

error "Timed out waiting for Google Chrome to create a focused window."
