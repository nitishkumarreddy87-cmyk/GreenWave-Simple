GreenWave (simple Java Swing version)
Smart Traffic Signal & Emergency Vehicle Management System
=============================================================
A small desktop program. Only the standard Java library is used.

RUN (Windows)
1. Install a JDK (Java 11 or newer). Check in a terminal:  javac -version
   If "not recognized", add your jdk\bin folder for this terminal:
     Command Prompt : set PATH=C:\path\to\jdk\bin;%PATH%
     PowerShell     : $env:Path = "C:\path\to\jdk\bin;" + $env:Path
2. Open a terminal INSIDE this folder (the one with src and run.bat).
3. Run:  run.bat        (Mac/Linux: ./run.sh)
4. The GreenWave window opens. Choose a role and press Enter.

ROLES
  Admin             : everything
  Operator          : everything except Reset city
  Emergency driver  : dispatch emergency vehicles only
  Public viewer     : watch only

HOW TO USE
  Live Map  : choose a route, press Ambulance / Fire truck / Police car. Signals ahead turn green.
              Click a junction on the map for a pop-up menu (Auto, East-West green, North-South green, Mark fault).
              Choose Normal or Night mode, tick Pause (or press the Space bar), or Reset city.
  Signals   : a table of all junctions; select a row and choose an override.
  Event Log : every event; "Save log to file" writes greenwave-log.txt.
  Menus     : File (Save log, Sign out, Exit) and Simulation (Reset city).

FILES (src)
  Screens : Main, AppFrame, LoginPanel, MainPanel, MapPanel
  Model   : City, Junction, Vehicle, User, Light, Errors
