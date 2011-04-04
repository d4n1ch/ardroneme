/* ARDroneME --- Java (J2ME) based AR.Drone Controller
Author: MAPGPS at
	http://www.ardrone-flyers.com/forum/viewforum.php?f=8
	http://www.rcgroups.com/forums/showthread.php?t=1401935
	https://projects.ardrone.org/projects/ardrone-api/boards
	http://www.ourdev.cn/bbs/bbs_list.jsp?bbs_id=1025
	http://bbs.5imx.com/bbs/viewthread.php?tid=415063
Initial: 2011.03.13


########## Keyboad Layout ############
Takeoff/Landing: a toggle button (or MediaPlayer button)
Emergency: "E" button (or Camera button), only effective after Landing button pressed first)
Hovering: when the Arrow button loosed
Speed(%) slider: change rudder rate in range of 0%~90%
Arrow Keys and 2 Soft-Joysticks on the touch screen are linked.

Arrow Keys:
              Go Up
                ^
                |
    Go Left <---+---> Go Right
                |
                v
             Go Down

Arrow Keys with central button pressed down (Shift):
           Go Forward
                ^
                |
Rotate Left <--- ---> Rotate Right
                |
                v
           Go Backward

UI_BIT:
00010001010101000000000000000000
   |   | | | |        || | ||||+--0: Button turn to left
   |   | | | |        || | |||+---1: Button altitude down (ah - ab)
   |   | | | |        || | ||+----2: Button turn to right
   |   | | | |        || | |+-----3: Button altitude up (ah - ab)
   |   | | | |        || | +------4: Button - z-axis (r1 - l1)
   |   | | | |        || +--------6: Button + z-axis (r1 - l1)
   |   | | | |        |+----------8: Button emergency reset all
   |   | | | |        +-----------9: Button Takeoff / Landing
   |   | | | +-------------------18: y-axis trim +1 (Trim increase at +/- 1??/s)
   |   | | +---------------------20: x-axis trim +1 (Trim increase at +/- 1??/s)
   |   | +-----------------------22: z-axis trim +1 (Trim increase at +/- 1??/s)
   |   +-------------------------24: x-axis +1
   +-----------------------------28: y-axis +1

AT*REF=<sequence>,<UI>
AT*PCMD=<sequence>,<enable>,<roll>,<pitch>,<gaz>,<yaw>
	(float)0.05 = (int)1028443341		(float)-0.05 = (int)-1119040307
	(float)0.1  = (int)1036831949		(float)-0.1  = (int)-1110651699
	(float)0.2  = (int)1045220557		(float)-0.2  = (int)-1102263091
	(float)0.5  = (int)1056964608		(float)-0.5  = (int)-1090519040
AT*ANIM=<sequence>,<animation>,<duration>
AT*CONFIG=<sequence>,\"<name>\",\"<value>\"

########## AT Commands ############
altitude max2m:	AT*CONFIG=1,\"control:altitude_max\",\"2000\"   //10000=unlimited
Takeoff:	AT*REF=1,290718208
Landing:	AT*REF=1,290717696
Hovering:	AT*PCMD=1,1,0,0,0,0
gaz 0.1:	AT*PCMD=1,1,0,0,1036831949,0
gaz -0.1:	AT*PCMD=1,1,0,0,-1110651699,0
roll 0.1:	AT*PCMD=1,1,1036831949,0,0,0
roll -0.1:	AT*PCMD=1,1,-1110651699,0,0,0
yaw 0.1:	AT*PCMD=1,1,0,0,0,1036831949
yaw -0.1:	AT*PCMD=1,1,0,0,0,-1110651699
pitch 0.1:	AT*PCMD=1,1,0,1036831949,0,0
pitch -0.1:	AT*PCMD=1,1,0,-1110651699,0,0
pitch -30 deg:	AT*ANIM=1,0,1000
pitch 30 deg:	AT*ANIM=1,1,1000
Emergency	AT*REF=1,290717952
Flat Trim:	AT*FTRIM=1
*/


import javax.microedition.lcdui.Canvas;
import javax.microedition.lcdui.Graphics;
import javax.microedition.lcdui.Choice;
import javax.microedition.lcdui.ChoiceGroup;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.ItemCommandListener;
import javax.microedition.lcdui.Display;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Form;
import javax.microedition.lcdui.TextField;
import javax.microedition.lcdui.Item;
import javax.microedition.lcdui.StringItem;
import javax.microedition.lcdui.Font;
import javax.microedition.midlet.MIDlet;
import javax.microedition.io.Datagram;
import javax.microedition.io.Connector;
import javax.microedition.io.DatagramConnection;

public class ARDroneME extends MIDlet implements Runnable, CommandListener, ItemCommandListener {
    static final int AT_PORT = 5556;
    static final int INTERVAL = 30; //within 50ms to avoid ARDRONE_COM_WATCHDOG_MASK (pitch and roll does not react in this state)
    String ardrone_ip = "192.168.1.1";
    //String ardrone_ip = "192.168.0.100";
    String ardrone_ip_bak = "";
    String ardrone_ip_cur = "";
    boolean isPaused;
    boolean app_running = true, flying_flag = false;
    DatagramConnection dc_at = null;
    String at_cmd = null;
    int seq = 1; //Send AT command with sequence number 1 will reset the counter
    ArCanvas arcanvas;
    NavData navdata;
    int navdata_cnt = 0;

    static Display display;
    TextField atCmdField;
    StringItem pitchItem, rollItem, yawItem;
    ChoiceGroup ipCG;

    Command setCommand = new Command("Settings", Command.ITEM, 1);
    Command exitCommand = new Command("Exit", Command.EXIT, 1);
    Command okCommand = new Command("OK", Command.OK, 1);
    Command cancelCommand = new Command("Cancel", Command.CANCEL, 1);
    Command trimCommand = new Command("Flat Trim", Command.ITEM, 1);
    Command atCommand = new Command("Send AT Command", Command.ITEM, 1);
    String[] ip_list = {"192.168.1.1", "192.168.0.100", "192.168.0.5", "169.254.2.2"};
    int ardrone_ip_idx = 0;

    public ARDroneME() {
	display = Display.getDisplay(this);
	arcanvas = new ArCanvas(this);
	arcanvas.addCommand(setCommand);
	arcanvas.addCommand(exitCommand);
	arcanvas.setCommandListener(this);
	display.setCurrent(arcanvas);
	
	navdata = new NavData(this);
	navdata.start();
	
	Thread t = new Thread(this);
	t.start();
    }

    public static Display getDisplay() {
	return display;
    }

    public boolean isPaused() {
	return isPaused;
    }

    public void startApp() {
    	app_running = true;
	isPaused = false;
    }

    public void pauseApp() {
	isPaused = true;
    }

    public void destroyApp(boolean unconditional) {
    	app_running = false;
    }

    public void commandAction(Command c, Displayable s) {
	if (c == exitCommand) {
	    destroyApp(true);
	    notifyDestroyed();
	} else if (c == setCommand) {
    	    ardrone_ip_bak = ardrone_ip;
	    Form f = new Form("Settings");

	    ipCG = new ChoiceGroup("Select AR.Drone IP", ChoiceGroup.POPUP, ip_list, null);
	    ipCG.setSelectedIndex(ardrone_ip_idx, true);
	    f.append(ipCG);

	    f.append(" \n");
	    StringItem at_item = new StringItem("", "Send AT Command", Item.BUTTON);
	    at_item.setDefaultCommand(atCommand);
	    at_item.setItemCommandListener(this);
	    f.append(at_item);
	    atCmdField = new TextField("", "AT*LED=1,4,1056964608,5", 30, TextField.ANY);
	    f.append(atCmdField);

	    f.append(" \n");
	    StringItem trim_item = new StringItem("", "Flat Trim", Item.BUTTON);
	    trim_item.setDefaultCommand(trimCommand);
	    trim_item.setItemCommandListener(this);
	    f.append(trim_item);

	    pitchItem = new StringItem("Pitch: ", "0.0");
	    f.append(pitchItem);
	    rollItem = new StringItem("Roll: ", "0.0");
	    f.append(rollItem);
	    yawItem = new StringItem("Yaw: ", "0.0");
	    f.append(yawItem);

	    f.addCommand(okCommand);
	    f.addCommand(cancelCommand);
	    f.setCommandListener(this);
	    display.setCurrent(f);
        } else if (c == okCommand) {
            ardrone_ip_idx = ipCG.getSelectedIndex();
            ardrone_ip = ipCG.getString(ardrone_ip_idx);
            if (!ardrone_ip.equals(ardrone_ip_bak)) navdata.interrupt_receive();
            display.setCurrent(arcanvas);
        } else if (c == cancelCommand) {
            ardrone_ip = ardrone_ip_bak;
            display.setCurrent(arcanvas);
        }
    }
    
    public void commandAction(Command c, Item item) {
    	ardrone_ip = ipCG.getString(ipCG.getSelectedIndex());

	if (c == trimCommand) {
	    send_at_cmd("AT*FTRIM=1");
        } else if (c == atCommand) {
	    send_at_cmd(atCmdField.getString());
        }
     }

    public void run() {
    	int cnt = 0, navdata_cnt_old = navdata_cnt;

	while(app_running) {
	try {
	    if (at_cmd == null) {
	    	synchronized(this) {
                    wait(INTERVAL);
        	}

		if (++cnt > 100) { //3s
		    cnt = 0;
		    if (navdata_cnt == navdata_cnt_old) {
	    	    	navdata.interrupt_receive();
	    	    } else {
	    	    	navdata_cnt_old = navdata_cnt;
	    	    }
	    	}

	        if (at_cmd == null) {
	            if (flying_flag) put_pcmd_into_at_cmd(arcanvas.enable, arcanvas.roll, arcanvas.pitch, arcanvas.gaz, arcanvas.yaw);
		    else put_pcmd_into_at_cmd(0, 0, 0, 0, 0);
	    	}
	    }
            
	    if (!ardrone_ip_cur.equals(ardrone_ip)) {
	    	ardrone_ip_cur = ardrone_ip;
		reconnect_at();
	    }

	    send(at_cmd);
	    at_cmd = null;
	} catch (Exception e) {
	    e.printStackTrace();
	}
	}
	
	try {
	    dc_at.close();
	} catch (Exception e) {}
    }

    public void stop() {
    }

    public synchronized void wake_thread() {
	notify();
    }
    
    public synchronized int get_seq() {
    	return seq++;
    }

    public void put_pcmd_into_at_cmd(int enable, float roll, float pitch, float gaz, float yaw) {
	at_cmd = "AT*PCMD=" + get_seq() + "," + enable + "," + Float.floatToIntBits(roll) + ","
		+ Float.floatToIntBits(pitch) + "," + Float.floatToIntBits(gaz) + "," + Float.floatToIntBits(yaw);
    }
    
    public synchronized void send_at_cmd(String cmd) {
    	at_cmd = cmd;
	notify();
    }
    
    public void send(String msg) throws Exception {
    	byte[] bytes = (msg + "\r").getBytes();
	Datagram dg = dc_at.newDatagram(bytes, bytes.length);
	dc_at.send(dg);
    }

    public void reconnect_at() {
    	try {
            if (dc_at != null) dc_at.close();
            dc_at = (DatagramConnection) Connector.open("datagram://"
	   				+ ardrone_ip_cur + ":" + AT_PORT);
	    send("AT*COMWDG=1");
	    Thread.sleep(INTERVAL);
	    send("AT*CONFIG=1,\"control:altitude_max\",\"10000\""); //altitude max in mm: 10000=unlimited
	    Thread.sleep(INTERVAL);
	    send("AT*CTRL=1,5,0");
	    Thread.sleep(INTERVAL);
	    send("AT*CONFIG=1,\"control:euler_angle_max\",\"0.2\""); //max radians 0.2 = 180*0.2/3.14 = 11 degrees
	    Thread.sleep(INTERVAL);
	    send("AT*CTRL=1,5,0");
	    Thread.sleep(INTERVAL);
	    send("AT*CONFIG=1,\"control:control_vz_max\",\"2000.0\""); //2000mm/s
	    Thread.sleep(INTERVAL);
	    send("AT*CTRL=1,5,0");
	    Thread.sleep(INTERVAL);
	    send("AT*CONFIG=1,\"control:control_yaw\",\"2.0\""); //radians 2.0/s
	    Thread.sleep(INTERVAL);
	    send("AT*CTRL=1,5,0");
	    Thread.sleep(INTERVAL);
	    send("AT*PCMD=1,0,0,0,0,0");
	    Thread.sleep(INTERVAL);
	} catch(Exception e) {}
    }
}
