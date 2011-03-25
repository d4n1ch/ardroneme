/* ARDroneME --- Java (J2ME) based AR.Drone Controller
Author: MAPGPS at
	http://www.ardrone-flyers.com/forum/viewforum.php?f=8
	http://www.rcgroups.com/forums/showthread.php?t=1401935
	https://projects.ardrone.org/projects/ardrone-api/boards
	http://www.ourdev.cn/bbs/bbs_list.jsp?bbs_id=1025
	http://bbs.5imx.com/bbs/viewthread.php?tid=415063
Initial: 2011.03.13
Updated: 2011.03.25


########## Keyboad Layout ############
Takeoff/Landing: a toggle button
Hovering: when the Arrow button loosed
Speed(%) slider: change rudder rate in range of 0%~90%

Arrow buttons:
              Go Up
                ^
                |
Rotate Left <---+---> Rotate Right
                |
                v
             Go Down

Arrow buttons with central button pressed down (Shift):
        Go Forward
            ^
            |
Go Left <--- ---> Go Right
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
import javax.microedition.midlet.MIDlet;
import javax.microedition.io.Datagram;
import javax.microedition.io.DatagramConnection;
import javax.microedition.io.Connector;
import javax.microedition.io.ConnectionNotFoundException;
import javax.microedition.io.ServerSocketConnection;


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
	            if (flying_flag) put_pcmd_into_at_cmd(1, arcanvas.roll, arcanvas.pitch, arcanvas.gaz, arcanvas.yaw);
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
	    send("AT*PCMD=1,0,0,0,0,0");
	    Thread.sleep(INTERVAL);
	} catch(Exception e) {}
    }
}

class ArCanvas extends Canvas {
    static final int FIRE_W = 30;
    static final int FIRE_WW = 50;
    static final int SLIDER_W = 40;
    static final int SLIDER_LEN = 180;
    static final float SPEED_MAX = (float)0.9;
    static final float SPEED_MIN = (float)0.0;
    float speed = (float)0.2;
    float roll = 0, pitch = 0, gaz = 0, yaw = 0;
    boolean shift = false;
    int w, h, last_y, slider_y0, slider_y, o_x, o_y;
    static final String arrow1[] = {"U", "R", "D", "L"};
    static final String arrow2[] = {"F", "R", "B", "L"};
    //static final String arrow1[] = {"上", "右", "下", "左"};
    //static final String arrow2[] = {"前", "右", "后", "左"};
    boolean arrow_pressed[] = {false, false, false, false};
    String arrow[] = arrow1;
    ARDroneME ardroneme;
    int battery = 0;
    float altitude = 0;
    String status_str = "";

    public ArCanvas(ARDroneME ardroneme) {
    	this.ardroneme = ardroneme;
    	w = getWidth();
        h = getHeight();
	slider_y0 = (h - SLIDER_LEN)/2;
	slider_y = slider_y0 + (int)((SLIDER_LEN - 5) * (SPEED_MAX - speed) / (SPEED_MAX - SPEED_MIN));
	o_x = w - FIRE_WW/2 - 30;
	o_y = h/2;

    }

    public void keyPressed(int keyCode) {
	//System.out.println("keyPressed: " + keyCode);
        switch (getGameAction(keyCode)) {
            case Canvas.FIRE:
                System.out.println("FIRE 1");
    	    	shift = !shift;
    	    	repaint();
            
                return;

            case Canvas.UP:
                System.out.println("UP 1");
		arrow_pressed[0] = true;
                if (shift) pitch = speed;
            	else gaz = speed;
                break;
            
            case Canvas.DOWN:
                System.out.println("DOWN 1");
		arrow_pressed[2] = true;
                if (shift) pitch = -speed;
            	else gaz = -speed;
                break;
            
            case Canvas.LEFT:
                System.out.println("LEFT 1");
		arrow_pressed[3] = true;
                if (shift) roll = -speed;
            	else yaw = -speed;
                break;
            
            case Canvas.RIGHT:
                System.out.println("RIGHT 1");
		arrow_pressed[1] = true;
                if (shift) roll = speed;
            	else yaw = speed;
                break; 
            
            default:/*
		if (keyCode >= 48 && keyCode <= 57) { //support Num keys (0~9)
		}*/
		
            	return;
        }
        
	repaint();
        ardroneme.put_pcmd_into_at_cmd(0, 0, 0, 0, 0); //toggle 0 and 1 for enable flag to enable Pitch and Roll
        ardroneme.wake_thread();
    }

    public void keyReleased(int keyCode) {
	//System.out.println("keyReleased: " + keyCode);
        switch (getGameAction(keyCode)) {
            case Canvas.UP:
                System.out.println("UP 0");
		arrow_pressed[0] = false;
                if (shift) pitch = 0;
            	else gaz = 0;
                break;
            
            case Canvas.DOWN:
                System.out.println("DOWN 0");
		arrow_pressed[2] = false;
                if (shift) pitch = 0;
            	else gaz = 0;
                break;
            
            case Canvas.LEFT:
                System.out.println("LEFT 0");
		arrow_pressed[3] = false;
                if (shift) roll = 0;
            	else yaw = 0;
                break;
            
            case Canvas.RIGHT:
                System.out.println("RIGHT 0");
		arrow_pressed[1] = false;
                if (shift) roll = 0;
            	else yaw = 0;
                break;            	

            default:
            	return;
        }

	repaint();
        ardroneme.wake_thread();
    }

    public void pointerPressed(int x, int y) {
	if (x <= SLIDER_W && y >= slider_y0 && y <= slider_y0 + SLIDER_LEN) {
	    last_y = y;
	}
	
	if (x > (w - 70)/2 && x < (w + 70)/2 && y > h - 40 && y < h - 20) {
	    if (!ardroneme.flying_flag) ardroneme.send_at_cmd("AT*REF=1,290718208");	//Takeoff
	    else ardroneme.send_at_cmd("AT*REF=1,290717696");			//Landing
	    ardroneme.flying_flag = !ardroneme.flying_flag;
	}

	if (x > o_x - FIRE_W/2 && x < o_x + FIRE_W/2 && y > o_y - FIRE_W/2 && y < o_y + FIRE_W/2) {
            System.out.println("FIRE 1");
	    shift = !shift;
	}

	if (x > o_x - FIRE_W/2 && x < o_x + FIRE_W/2 && y > o_y - FIRE_WW - 10 && y < o_y - FIRE_WW/2) {
            System.out.println("UP 1");
            arrow_pressed[0] = true;
            if (shift) pitch = speed;
            else gaz = speed;
	}

	if (x > o_x - FIRE_W/2 && x < o_x + FIRE_W/2 && y > o_y + FIRE_WW/2 && y < o_y + FIRE_WW + 10) {
            System.out.println("DOWN 1");
            arrow_pressed[2] = true;
            if (shift) pitch = -speed;
            else gaz = -speed;
	}

	if (x > o_x - FIRE_WW - 10 && x < o_x - FIRE_WW/2 && y > o_y - FIRE_W/2 && y < o_y + FIRE_W/2) {
            System.out.println("LEFT 1");
            arrow_pressed[3] = true;
            if (shift) roll = -speed;
            else yaw = -speed;
 	}

	if (x > o_x + FIRE_WW/2 && x < o_x + FIRE_WW + 10 && y > o_y - FIRE_W/2 && y < o_y + FIRE_W/2) {
            System.out.println("RIGHT 1");
            arrow_pressed[1] = true;
            if (shift) roll = speed;
            else yaw = speed;
	}
	
	if (!ardroneme.flying_flag && x > w/2 - 10 && x < w/2 + 10 && y > 20 && y < 40) {
	    System.out.println("E");
	    ardroneme.send_at_cmd("AT*REF=1,290717952");
	}

	repaint();
        ardroneme.wake_thread();
    }

    public void pointerReleased(int x, int y) {
        System.out.println("pointerReleased");
        roll = 0;
        pitch = 0;
        gaz = 0;
        yaw = 0;
        arrow_pressed[0] = false;
        arrow_pressed[1] = false;
        arrow_pressed[2] = false;
        arrow_pressed[3] = false;

	repaint();
        ardroneme.wake_thread();
    }


    public void pointerDragged(int x, int y) {
	if (x <= SLIDER_W && y >= slider_y0 && y <= slider_y0 + SLIDER_LEN) {
	    slider_y += y - last_y;
	    if (slider_y <= slider_y0) slider_y = slider_y0 + 1;
	    if (slider_y > slider_y0 + SLIDER_LEN - 5) slider_y = slider_y0 + SLIDER_LEN - 5;
	    speed = SPEED_MAX - (SPEED_MAX - SPEED_MIN) * (slider_y - slider_y0) / (SLIDER_LEN - 5);
	    repaint();
	}

	last_y = y;
    }
    
    public void repaint_canvas() {
    	repaint();
    }

    public void paint(Graphics g) {
        g.setColor(255, 255, 255);
    	g.fillRect(0, 0, w, h);

        g.setColor(0, 0, 255);
	g.drawRect(-1, slider_y0, SLIDER_W, SLIDER_LEN);
	g.setColor(255, 0, 255);
	g.fillRect(0, slider_y, SLIDER_W - 1, 5);

        g.setColor(180, 180, 180);
    	g.fillRect(o_x - FIRE_WW/2, o_y - FIRE_WW/2, FIRE_WW, FIRE_WW);

	//Up
	g.drawLine(o_x - FIRE_W/2, o_y - FIRE_W/2, o_x, o_y - FIRE_WW);
	g.drawLine(o_x + FIRE_W/2, o_y - FIRE_W/2, o_x, o_y - FIRE_WW);
	g.drawLine(o_x - FIRE_W/2 + 1, o_y - FIRE_W/2, o_x, o_y - FIRE_WW + 2);
	g.drawLine(o_x + FIRE_W/2 - 1, o_y - FIRE_W/2, o_x, o_y - FIRE_WW + 2);

	//Down
	g.drawLine(o_x - FIRE_W/2, o_y + FIRE_W/2, o_x, o_y + FIRE_WW);
	g.drawLine(o_x + FIRE_W/2, o_y + FIRE_W/2, o_x, o_y + FIRE_WW);
	g.drawLine(o_x - FIRE_W/2 + 1, o_y + FIRE_W/2, o_x, o_y + FIRE_WW - 2);
	g.drawLine(o_x + FIRE_W/2 - 1, o_y + FIRE_W/2, o_x, o_y + FIRE_WW - 2);

	//Left
	g.drawLine(o_x - FIRE_W/2, o_y - FIRE_W/2, o_x - FIRE_WW, o_y);
	g.drawLine(o_x - FIRE_W/2, o_y + FIRE_W/2, o_x - FIRE_WW, o_y);
	g.drawLine(o_x - FIRE_W/2, o_y - FIRE_W/2 + 1, o_x - FIRE_WW + 2, o_y);
	g.drawLine(o_x - FIRE_W/2, o_y + FIRE_W/2 - 1, o_x - FIRE_WW + 2, o_y);

	//Right
	g.drawLine(o_x + FIRE_W/2, o_y - FIRE_W/2, o_x + FIRE_WW, o_y);
	g.drawLine(o_x + FIRE_W/2, o_y + FIRE_W/2, o_x + FIRE_WW, o_y);
	g.drawLine(o_x + FIRE_W/2, o_y - FIRE_W/2 + 1, o_x + FIRE_WW - 2, o_y);
	g.drawLine(o_x + FIRE_W/2, o_y + FIRE_W/2 - 1, o_x + FIRE_WW - 2, o_y);

	if (shift) {
	    arrow = arrow2;
	    g.setColor(0, 0, 0);
	    g.drawLine(o_x - FIRE_W/2, o_y - FIRE_W/2, o_x + FIRE_W/2, o_y - FIRE_W/2);
	    g.drawLine(o_x - FIRE_W/2, o_y - FIRE_W/2, o_x - FIRE_W/2, o_y + FIRE_W/2);
	    g.setColor(255, 255, 255);
	    g.drawLine(o_x + FIRE_W/2, o_y - FIRE_W/2, o_x + FIRE_W/2, o_y + FIRE_W/2);
	    g.drawLine(o_x - FIRE_W/2, o_y + FIRE_W/2, o_x + FIRE_W/2, o_y + FIRE_W/2);
	} else {
	    arrow = arrow1;
	    g.setColor(255, 255, 255);
	    g.drawLine(o_x - FIRE_W/2, o_y - FIRE_W/2, o_x + FIRE_W/2, o_y - FIRE_W/2);
	    g.drawLine(o_x - FIRE_W/2, o_y - FIRE_W/2, o_x - FIRE_W/2, o_y + FIRE_W/2);
	    g.setColor(0, 0, 0);
	    g.drawLine(o_x + FIRE_W/2, o_y - FIRE_W/2, o_x + FIRE_W/2, o_y + FIRE_W/2);
	    g.drawLine(o_x - FIRE_W/2, o_y + FIRE_W/2, o_x + FIRE_W/2, o_y + FIRE_W/2);
	}
	
	
        g.setColor(200, 200, 200);
	g.fillRoundRect((w - 70)/2, h - 40, 70, 20, 20, 20);
	if (ardroneme.flying_flag) {
	    //g.setColor(0, 0, 255);
	    g.setColor(255, 0, 0); //JavaFX1.2 bug: (B,G,R) format for Text
            g.drawString("Landing", (w - 70)/2 + 16, h - 27, Graphics.BASELINE|Graphics.LEFT);
	} else {
	    g.setColor(255, 0, 255);
            g.drawString("Takeoff", (w - 70)/2 + 16, h - 27, Graphics.BASELINE|Graphics.LEFT);
	}

        g.setColor(200, 200, 200);
        g.fillArc(w/2 - 10, 20, 20, 20, 0, 360);
	//g.setColor(255, 0, 0);
	g.setColor(0, 0, 255); //JavaFX1.2 bug: (B,G,R) format for Text
        g.drawString("E", w/2 - 2, 33, Graphics.BASELINE|Graphics.LEFT);

        if (arrow_pressed[0]) g.setColor(0, 255, 0);
        else g.setColor(0, 0, 0);
        g.drawString(arrow[0], o_x - 3, o_y - FIRE_WW/2 - 2, Graphics.BASELINE|Graphics.LEFT);
        if (arrow_pressed[2]) g.setColor(0, 255, 0);
        else g.setColor(0, 0, 0);
        g.drawString(arrow[2], o_x - 3, o_y + FIRE_WW/2 + 13, Graphics.BASELINE|Graphics.LEFT);
        if (arrow_pressed[1]) g.setColor(0, 255, 0);
        else g.setColor(0, 0, 0);
        g.drawString(arrow[1], o_x + FIRE_WW/2 + 2, o_y + 4, Graphics.BASELINE|Graphics.LEFT);
        if (arrow_pressed[3]) g.setColor(0, 255, 0);
        else g.setColor(0, 0, 0);
        g.drawString(arrow[3], o_x - FIRE_WW/2 - 8, o_y + 4, Graphics.BASELINE|Graphics.LEFT);
        g.setColor(0, 0, 0);
        g.drawString("Shift", o_x - 10, o_y + 4, Graphics.BASELINE|Graphics.LEFT);
        /*
        if (arrow_pressed[0]) g.setColor(0, 255, 0);
        else g.setColor(0, 0, 0);
        g.drawString(arrow[0], o_x - 5, o_y - FIRE_WW/2 - 2, Graphics.BASELINE|Graphics.LEFT);
        if (arrow_pressed[2]) g.setColor(0, 255, 0);
        else g.setColor(0, 0, 0);
        g.drawString(arrow[2], o_x - 5, o_y + FIRE_WW/2 + 11, Graphics.BASELINE|Graphics.LEFT);
        if (arrow_pressed[1]) g.setColor(0, 255, 0);
        else g.setColor(0, 0, 0);
        g.drawString(arrow[1], o_x + FIRE_WW/2, o_y + 4, Graphics.BASELINE|Graphics.LEFT);
        if (arrow_pressed[3]) g.setColor(0, 255, 0);
        else g.setColor(0, 0, 0);
        g.drawString(arrow[3], o_x - FIRE_WW/2 - 10, o_y + 4, Graphics.BASELINE|Graphics.LEFT);
        g.setColor(0, 0, 0);
        g.drawString("切换", o_x - 10, o_y + 4, Graphics.BASELINE|Graphics.LEFT);
        */

        g.drawString("Speed(%): ", 2, slider_y0 - 4, Graphics.BASELINE|Graphics.LEFT);
        g.drawString("" + (int)(SPEED_MAX*100), 13, slider_y0 + 10, Graphics.BASELINE|Graphics.LEFT);
        g.drawString("" + (int)(SPEED_MIN*100), 16, slider_y0 + SLIDER_LEN - 2, Graphics.BASELINE|Graphics.LEFT);
        g.drawString("" + (int)((speed + 0.005)*100), SLIDER_W + 2, slider_y + 6, Graphics.BASELINE|Graphics.LEFT);
        g.drawString("Battery: " + battery + "%", 2, 10, Graphics.BASELINE|Graphics.LEFT);
        g.drawString("Altitude: " + altitude + "m", w - 90, 10, Graphics.BASELINE|Graphics.LEFT);
        g.drawString("Status: " + status_str, 2, h - 2, Graphics.BASELINE|Graphics.LEFT);
    }
}

class NavData extends Thread { 
    static final int NAV_PORT = 5554;
    static final int NAV_STATE_OFFSET	=  4;
    static final int NAV_BATTERY_OFFSET	= 24;
    static final int NAV_PITCH_OFFSET	= 28;
    static final int NAV_ROLL_OFFSET	= 32;
    static final int NAV_YAW_OFFSET	= 36;
    static final int NAV_ALTITUDE_OFFSET= 40;

    static final int MYKONOS_TRIM_COMMAND_MASK   = 1 <<  7; /*!< Trim command ACK : (0) None, (1) one received */
    static final int MYKONOS_TRIM_RUNNING_MASK   = 1 <<  8; /*!< Trim running : (0) none, (1) running */
    static final int MYKONOS_TRIM_RESULT_MASK    = 1 <<  9; /*!< Trim result : (0) failed, (1) succeeded */
    static final int MYKONOS_ANGLES_OUT_OF_RANGE = 1 << 19; /*!< Angles : (0) Ok, (1) out of range */
    static final int MYKONOS_WIND_MASK           = 1 << 20; /*!< Wind : (0) Ok, (1) too much to fly */
    static final int MYKONOS_ULTRASOUND_MASK     = 1 << 21; /*!< Ultrasonic sensor : (0) Ok, (1) deaf */
    static final int MYKONOS_CUTOUT_MASK         = 1 << 22; /*!< Cutout system detection : (0) Not detected, (1) detected */
    static final int MYKONOS_COM_WATCHDOG_MASK   = 1 << 30; /*!< Communication Watchdog : (1) com problem, (0) Com is ok */
    static final int MYKONOS_EMERGENCY_MASK      = 1 << 31; /*!< Emergency landing : (0) no emergency, (1) emergency */

    ARDroneME ardroneme;
    ArCanvas arcanvas;
    byte[] trigger_bytes = {0x01, 0x00, 0x00, 0x00};
    String local_ip = "", ardrone_ip_cur = "";
    float attitude_pitch, attitude_roll, attitude_yaw;
    DatagramConnection dc_nav = null;
    Datagram dg = null;

    public NavData(ARDroneME ardroneme) {
    	this.ardroneme = ardroneme;
    	this.arcanvas = ardroneme.arcanvas;
    }

   public int get_int(byte[] data, int offset) {
	int tmp = 0, n = 0;

	for (int i=3; i>=0; i--) {   
	    n <<= 8;
	    tmp = data[offset + i] & 0xFF;   
	    n |= tmp;   
	}

        return n;   
    }

    public float get_float(byte[] data, int offset) {
   	return Float.intBitsToFloat(get_int(data, offset));
    }

    //Workaround to interrupt dc_nav.receive(dg) if no UDP packet received for a period 
    public synchronized void interrupt_receive() {
	try {
	    DatagramConnection dc_nav = (DatagramConnection) Connector.open("datagram://"
	    					+ local_ip + ":" + NAV_PORT);
	    Datagram dg = dc_nav.newDatagram(trigger_bytes, trigger_bytes.length);
	    dc_nav.send(dg);
    	    System.out.println("Sent Interrupt bytes to local port " + NAV_PORT);
	} catch(Exception e) { 
	    e.printStackTrace(); 
	}
    }
    
    public void reconnect_nav() {
    	try {
	    if (dc_nav != null) dc_nav.close();
	    dc_nav = (DatagramConnection)Connector.open("datagram://"
	    			+ ardrone_ip_cur + ":" + NAV_PORT);
	    dg = dc_nav.newDatagram(trigger_bytes, trigger_bytes.length);
	    dc_nav.send(dg);
    	    System.out.println("Sent NavData trigger bytes to " + ardrone_ip_cur + ":" + NAV_PORT);
	    arcanvas.status_str = local_ip + " -> " + ardrone_ip_cur;
 	    arcanvas.repaint_canvas();
	    Thread.sleep(ARDroneME.INTERVAL);
	    ardroneme.send("AT*CONFIG=1,\"general:navdata_demo\",\"TRUE\"");
	    Thread.sleep(ARDroneME.INTERVAL);
	    ardroneme.send("AT*CTRL=1,5,0");
    	    dc_nav.close();
    	    
	    dc_nav = (DatagramConnection)Connector.open("datagram://:" + NAV_PORT);
    	    dg = dc_nav.newDatagram(10240);
	} catch(Exception e) {}
    }

    public void run() {
    	byte[] navdata;
    	int print_cnt = 0, timeout_cnt = 0, state, len;
    	boolean blink_r = true;

	try {
	    ServerSocketConnection ssc = (ServerSocketConnection) Connector.open("socket://:1234");
	    local_ip = ssc.getLocalAddress();
	    ssc.close();
	    System.out.println("Local IP: " + local_ip);
	} catch(Exception e) {}	
	
	while(ardroneme.app_running) {
	try {

	    if (!ardrone_ip_cur.equals(ardroneme.ardrone_ip)) {
		ardrone_ip_cur = ardroneme.ardrone_ip;
		reconnect_nav();
		print_cnt = -10;
    	    }

	    //System.out.println("NavData receive() ...");
	    dc_nav.receive(dg);
	    ardroneme.navdata_cnt++;
	    len = dg.getLength();
	    arcanvas.status_str = "NavData received: " + len + " bytes";
	    
	    if (len == trigger_bytes.length) {
		System.out.println("Interrupt bytes received, ignore it");
		arcanvas.status_str = "NavData Timeout";
		arcanvas.repaint_canvas();
		
		if (++timeout_cnt <= 2) continue;
		timeout_cnt = 0;
		ardroneme.reconnect_at();
		reconnect_nav();
	    }

	    if (len == 24) {
	    	System.out.println("In BOOTSTRAP mode, reconnect to switch to DEMO mode");
		reconnect_nav();
	    	Thread.sleep(1000);
 		continue;
	    }

	    navdata = dg.getData();
	    state = get_int(navdata, NAV_STATE_OFFSET);

	    if ((state & MYKONOS_COM_WATCHDOG_MASK) > 0) {
	    	System.out.println("MYKONOS_COM_WATCHDOG_MASK");
		arcanvas.status_str = "MYKONOS_COM_WATCHDOG_MASK";
		arcanvas.repaint_canvas();
		print_cnt = -10;
		ardroneme.send("AT*COMWDG=1");
		Thread.sleep(ARDroneME.INTERVAL);		
	    }

	    if ((state & MYKONOS_TRIM_COMMAND_MASK) > 0) {
	    	System.out.println("MYKONOS_TRIM_COMMAND_MASK");
		ardroneme.send("AT*LED=1,4,1056964608,5");
		Thread.sleep(ARDroneME.INTERVAL);		
	    }

	    if ((state & MYKONOS_TRIM_RESULT_MASK) > 0) {
	    	System.out.println("MYKONOS_TRIM_RESULT_MASK");
	    }

	    if ((state & MYKONOS_ANGLES_OUT_OF_RANGE) > 0) {
		arcanvas.status_str = "MYKONOS_ANGLES_OUT_OF_RANGE";
	    }

	    if ((state & MYKONOS_WIND_MASK) > 0) {
		arcanvas.status_str = "MYKONOS_WIND_MASK";
	    }

	    if ((state & MYKONOS_ULTRASOUND_MASK) > 0) {
		arcanvas.status_str = "MYKONOS_ULTRASOUND_MASK";
	    }

	    if ((state & MYKONOS_CUTOUT_MASK) > 0) {
		arcanvas.status_str = "MYKONOS_CUTOUT_MASK";
	    }

	    if ((state & MYKONOS_EMERGENCY_MASK) > 0) {
		arcanvas.status_str = "MYKONOS_EMERGENCY_MASK";
	    }

	    arcanvas.battery = get_int(navdata, NAV_BATTERY_OFFSET);
	    arcanvas.altitude = ((float)get_int(navdata, NAV_ALTITUDE_OFFSET)/1000);
	    attitude_pitch = get_float(navdata, NAV_PITCH_OFFSET)/1000;
	    attitude_roll = get_float(navdata, NAV_ROLL_OFFSET)/1000;
	    attitude_yaw = get_float(navdata, NAV_YAW_OFFSET)/1000;
	    
	    
	    if (++print_cnt > 5) {
	    	print_cnt = 0;
	    	if (blink_r) arcanvas.status_str += " ...R";
	    	blink_r = !blink_r;
	    	arcanvas.repaint_canvas();
	    	
	    	ardroneme.pitchItem.setText("" + attitude_pitch);
	    	ardroneme.rollItem.setText("" + attitude_roll);
	    	ardroneme.yawItem.setText("" + attitude_yaw);
	    }
	} catch(Exception e) { 
	    e.printStackTrace(); 
	}
	}

	try {
	    dc_nav.close();
	} catch (Exception e) {}
    }
}
