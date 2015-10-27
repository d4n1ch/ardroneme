ARDroneME is a Java (J2ME) based application to control your AR.Drone via WiFi using a Smart Phone device.

<b>Requirements to the Phone device:</b>
<br> - WiFi<br>
<br> - Java (J2ME) CLDC-1.1 + MIDP-2.0<br>
<br>
I tested it on my Windows Mobile 6 (WM6) phone.<br>
WM6 does not come with Java support as default, so I installed a Java Emulator JavaFX1.2:<br>
<a href='http://www.javafx.com/downloads/mobile/javafx_mobile1.2_windows.zip'>http://www.javafx.com/downloads/mobile/javafx_mobile1.2_windows.zip</a>

Someone reported to have tested it OK on a Symbian phone which has Java support as default.<br>
<br>
Here is the direct link to browse the latest source code of ARDroneME:<br>
<a href='http://code.google.com/p/ardroneme/source/browse/trunk'>http://code.google.com/p/ardroneme/source/browse/trunk</a>

<img src='http://ardroneme.googlecode.com/svn/wiki/Screen008.jpg' />aaaaaaaaaaa<img src='http://ardroneme.googlecode.com/svn/wiki/Screen009.jpg' />

<img src='http://ardroneme.googlecode.com/svn/wiki/Screen010.jpg' />aaaaaaaaaaa<img src='http://ardroneme.googlecode.com/svn/wiki/Screen005.jpg' />

<b>Run ARDroneME on PC Windows XP:</b><br>
You can also run WTK 2.5.2 on your PC Windows XP to emulate a Phone device:<br>
<a href='http://www.oracle.com/technetwork/java/download-135801.html'>http://www.oracle.com/technetwork/java/download-135801.html</a>

And run ARDroneME on WTK to control AR.Drone from your PC via keyboard or mouse.<br>
Use Enter key to switch the control between the 2 Soft-Joysticks on the touch screen.<br>
<br>
To enable mouse control, you need to update file:<br>
WTK2.5.2_01\wtklib\devices\DefaultColorPhone\DefaultColorPhone.properties<br>
with:<br>
touch_screen=true<br>
<br>
<img src='http://ardroneme.googlecode.com/svn/wiki/ARDroneME_WTK.PNG' />

<b>Java Security Settings:</b><br>
To avoid too many “Yes/No” confirmations when run ARDroneME, you can change the Java Security settings on your phone device.<br>
Here is an example for JavaFx:<br>
Select ARDroneME in JavaFx --> Menu --> Set Permissions --> on the popup window select "Ask once per session" for both:<br>
<br> - Network Access<br>
<br> - Low Level Network Access<br>
<br>
<b>Flat Trim:</b><br>
To make the Flat Trim button works, you need to ensure following two files are removed from AR.Drone:<br>
/data/trims.bin<br>
/data/fact_trims.bin<br>
<br>
<b>Re-assign Camera and MediaPlayer buttons:</b><br>
For WM6 phone: Start --> Settings --> Buttons<br>
Camera button = Scrolling-UP<br>
MediaPlayer button = Scrolling-DOWN<br>
<br>
<b>Keyboad Layout:</b><br>
Takeoff/Landing: a toggle button (or MediaPlayer button)<br>
Emergency: "E" button (or Camera button), only effective after Landing button pressed first)<br>
Hovering: when the Arrow button loosed<br>
Speed(%) slider: change rudder rate in range of 0%~90%<br>
Arrow Keys and 2 Soft-Joysticks on the touch screen are linked.<br>
<br>
<pre>
Arrow Keys:<br>
Go Up<br>
^<br>
|<br>
Go Left <---+---> Go Right<br>
|<br>
v<br>
Go Down<br>
<br>
Arrow Keys with central button pressed down (Shift):<br>
Go Forward<br>
^<br>
|<br>
Rotate Left <--- ---> Rotate Right<br>
|<br>
v<br>
Go Backward