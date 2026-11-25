GreenWave (Java Swing version): Smart Traffic Signal & Emergency Vehicle Management System
==========================================================================================
A desktop application. No browser, no web server, no database, no extra libraries.

SETUP (Windows)
1. Install a JDK (Java 11 or newer). Check in a terminal:  javac -version
   If "not recognized", add your jdk\bin folder for this terminal:
     Command Prompt : set PATH=C:\path\to\jdk\bin;%PATH%
     PowerShell     : $env:Path = "C:\path\to\jdk\bin;" + $env:Path
2. Open a terminal INSIDE this folder (the one that has src and run.bat).
3. Run:  run.bat        (Mac/Linux: ./run.sh)
   The GreenWave window opens. Close the window to stop the program.

LOGINS
  admin    / admin123  : everything
  operator / op123     : everything except Reset city
  driver   / drive123  : dispatch emergency vehicles only
  (button) Continue as public viewer : watch only

SCREENS
  Dashboard, Live map (click a junction for its controls), Signals, Emergency, Logs, About
  Time of day (top right): Morning, Noon, Evening, Night.   Space bar = pause / resume.

FILES (src)
  Main, LoginFrame, MainFrame, MainView, MapPanel, Ui, Theme   : the Swing screens
  City, Junction, Vehicles, TrafficSim, User, Errors           : the model (application logic)
