set oldCount to 0

tell application "Safari"
    activate
    set oldCount to count of windows
    make new document
end tell

repeat 40 times
    tell application "Safari"
        if (count of windows) > oldCount and frontmost then
            return "ok"
        end if
    end tell
    delay 0.05
end repeat

error "Timed out waiting for Safari to create a focused window."
