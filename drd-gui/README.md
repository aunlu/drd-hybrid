### About

DRD-UI is a simple standalone tool to analyze file with DRD races. 
Only the new style xml-based race reports are accepted (i.e. produced by DRD 0.7+)

DRD-UI is delivered zip file containing:

* _bin_ - directory with .jar files
* _run.bat_ - script for launching (simple shortcut for javaw -jar ...) 

DRD-UI was developed as "hot tool" for early DRD users, so it looks not so pleasant and its capabilities are fairly low.
Main UI races analyzer is scheduled for release in DRD 1.0.
It would provide more user-friendly races table and IntelliJ Idea plugin for in-place analysis of racing stack traces.

### Usage
1. Launch run.bat
2. Load drd-races.log file via file picker
3. Table with races will appear in new tab

Each row in the table corresponds to one race.
You may sort table by certain column by clicking its header. By default races are sorted by time of occurence.

In general the displayed information in the "Current stacktrace" and "Racing stacktrace" is limited.
Hover the mouse pointer on the certain cell to view full stacktrace in the tooltip.
Rightclick on the row to copy both traces to the clipboard for further analysis.

Use the "Show unique targets" button under the races table to view race-per-class statistics. Races are grouped by:

* Object races: how many races occurred on calls of that object
* Field races: how many races occurred on accesses to fields of this class

Common scenario:
1. Launch your project under DRD, gather somw races, open races file with DRD UI tool.
2. Open unique races dialog. You may immediately see some fake races: e.g. OBJECT races on java/lang/System or FIELD race on com/company/MyUtilsClass. Exclude these races (see [DRD configuration](https://code.devexperts.com/display/DRD/Documentation).
3. Now close dialog, sort table by target and start view races. If you find some of them interesting - copy traces and use IntelliJ Idea -> Alt+Z+S