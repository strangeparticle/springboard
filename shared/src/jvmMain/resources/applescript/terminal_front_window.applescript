on run argv
    set commandLine to item 1 of argv
    tell application "Terminal"
        activate
        -- Reuse the front window when one exists, otherwise open a new window.
        if (count of windows) > 0 then
            do script commandLine in front window
        else
            do script commandLine
        end if
    end tell
end run
