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
import javax.microedition.lcdui.ItemStateListener;
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

public class ARDroneME extends MIDlet implements Runnable, CommandListener, ItemCommandListener, ItemStateListener {
    static final int AT_PORT = 5556;
    static final int DELAY_IN_MS = 30;
    static final int INTERVAL_IN_MS = 100;  //within 250ms to avoid ARDRONE_COM_WATCHDOG_MASK (pitch and roll does not react in this state)
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
    ChoiceGroup ipCG, atCG, modeCG;

    Command setCommand = new Command("Settings", Command.ITEM, 1);
    Command exitCommand = new Command("Exit", Command.EXIT, 1);
    Command okCommand = new Command("OK", Command.OK, 1);
    Command cancelCommand = new Command("Cancel", Command.CANCEL, 1);
    Command trimCommand = new Command("Flat Trim", Command.ITEM, 1);
    Command atCommand = new Command("Send AT Command", Command.ITEM, 1);
    String[] ip_list = {"192.168.1.1", "192.168.0.100", "192.168.0.5", "169.254.2.2"};
    int ardrone_ip_idx = 0;
/*
AT*GAIN=%d,%d,%d,%d,%d,%d,%d,%d,%d,%d,%d,%d<LF>
Argument 1 :  the sequence number = 1
Argument 2 :  pq_kp : proportionnal gain for pitch (p) and roll (q) angular rate = 20000
Argument 3 :  r_kp : proportionnal gain for yaw (r) angular rate = 100000
Argument 4 :  r_ki : integral gain for yaw (r) angular rate = 10000
Argument 5 :  ea_kp : proportionnal gain for the Euler Angle = 9000
Argument 6 :  ea_ki : integral gain for the Euler Angle = 8000
Argument 7 :  alt_kp: proportionnal gain for the altitude = 2000
Argument 8 :  alt_ki: integral gain for the altitude = 400
Argument 9 :  vz_kp : proportionnal gain for the vertical speed = 100
Argument 10 : vz_ki: integral gain for the vertical speed = 50
Argument 11 : hv_kp: proportionnal gain for the hovering = 8000
Argument 12 : hv_ki: integral gain for the hovering = 8000
*/
    String[] at_name_list =    {"LED Red/Green blink 5s",
    				"WPADD alt=1m",
    				"WPADD alt=15m",
    				"WPADD alt=30m",
    				"GAIN alt_kp=0,alt_ki=0,vz_kp=0,vz_ki=0",
    				"GAIN alt_kp=0,alt_ki=0",
    				"GAIN vz_kp=0,vz_ki=0",
    				"GAIN pq_kp=0",
    				"GAIN ea_kp=0,ea_ki=0",
    				"GAIN hv_kp=0,hv_ki=0",
    				"GAIN default"    				
    				};
    String[] at_cmd_list =     {"AT*LED=1,4,1056964608,5",
    				"AT*WPADD=1,0,0,1",
    				"AT*WPADD=1,0,0,15",
    				"AT*WPADD=1,0,0,30",
    				"AT*GAIN=1,20000,100000,10000,9000,8000,0,0,0,0,8000,8000",
    				"AT*GAIN=1,20000,100000,10000,9000,8000,0,0,100,50,8000,8000",
    				"AT*GAIN=1,20000,100000,10000,9000,8000,2000,400,0,0,8000,8000",
    				"AT*GAIN=1,0,100000,10000,9000,8000,2000,400,100,50,8000,8000",
    				"AT*GAIN=1,20000,100000,10000,0,0,2000,400,100,50,8000,8000",
    				"AT*GAIN=1,20000,100000,10000,9000,8000,2000,400,100,50,0,0",
    				"AT*GAIN=1,20000,100000,10000,9000,8000,2000,400,100,50,8000,8000"
    				};
    String[] mode_list = {"Go Mode"};

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

	    modeCG = new ChoiceGroup("", ChoiceGroup.MULTIPLE, mode_list, null);
	    modeCG.setSelectedIndex(0, false);
	    f.append(modeCG);

	    f.append(" \n");
	    atCG = new ChoiceGroup("Select AT Cmd", ChoiceGroup.POPUP, at_name_list, null);
	    atCG.setSelectedIndex(0, true);
	    f.append(atCG);

	    StringItem at_item = new StringItem("", "Send AT Command", Item.BUTTON);
	    at_item.setDefaultCommand(atCommand);
	    at_item.setItemCommandListener(this);
	    f.append(at_item);
	    atCmdField = new TextField("", "AT*LED=1,4,1056964608,5", 100, TextField.ANY);
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
	    f.setItemStateListener(this);
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
    	arcanvas.go_mode = modeCG.isSelected(0);

	if (c == trimCommand) {
	    send_at_cmd("AT*FTRIM=1");
        } else if (c == atCommand) {
	    send_at_cmd(atCmdField.getString());
        }
     }
     
    public void itemStateChanged(Item src) {
	if (src == atCG) {
	    atCmdField.setString(at_cmd_list[atCG.getSelectedIndex()]);
	}
    }

    public void run() {
    	int cnt = 0, navdata_cnt_old = navdata_cnt;

	while(app_running) {
	try {
	    if (at_cmd == null) {
	    	synchronized(this) {
                    wait(INTERVAL_IN_MS); 
        	}

		if (++cnt > 3000/INTERVAL_IN_MS) { //No NavData received in 3s
		    cnt = 0;
		    if (navdata_cnt == navdata_cnt_old) {
	    	    	navdata.interrupt_receive();
	    	    } else {
	    	    	navdata_cnt_old = navdata_cnt;
	    	    }
	    	}

	        if (at_cmd == null) {
	            if (flying_flag) {
	            	if (arcanvas.direct_motor) put_motor_into_at_cmd(arcanvas.enable, arcanvas.roll, arcanvas.pitch, arcanvas.gaz, arcanvas.yaw);
	            	else put_pcmd_into_at_cmd(arcanvas.enable, arcanvas.roll, arcanvas.pitch, arcanvas.gaz, arcanvas.yaw);
		    } else put_pcmd_into_at_cmd(0, 0, 0, 0, 0);
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

    public void put_motor_into_at_cmd(int enable, float roll, float pitch, float gaz, float yaw) {
	at_cmd = "AT*MOTOR=" + get_seq() + "," + (int)(roll*50) + "," + (int)(pitch*50) + ","
					+ (int)(gaz*50) + "," + (int)(yaw*50);
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
	    Thread.sleep(DELAY_IN_MS);
	    send("AT*CONFIG=1,\"control:altitude_max\",\"10000\""); //altitude max in mm: 10000=unlimited
	    Thread.sleep(DELAY_IN_MS);
	    send("AT*CTRL=1,5,0");
	    Thread.sleep(DELAY_IN_MS);
	    send("AT*CONFIG=1,\"control:euler_angle_max\",\"0.2\""); //max radians 0.2 = 180*0.2/3.14 = 11 degrees
	    Thread.sleep(DELAY_IN_MS);
	    send("AT*CTRL=1,5,0");
	    Thread.sleep(DELAY_IN_MS);
	    send("AT*CONFIG=1,\"control:control_vz_max\",\"2000.0\""); //2000mm/s
	    Thread.sleep(DELAY_IN_MS);
	    send("AT*CTRL=1,5,0");
	    Thread.sleep(DELAY_IN_MS);
	    send("AT*CONFIG=1,\"control:control_yaw\",\"2.0\""); //radians 2.0/s
	    Thread.sleep(DELAY_IN_MS);
	    send("AT*CTRL=1,5,0");
	    Thread.sleep(DELAY_IN_MS);
	    send("AT*PCMD=1,0,0,0,0,0");
	    Thread.sleep(DELAY_IN_MS);
	} catch(Exception e) {}
    }
}
