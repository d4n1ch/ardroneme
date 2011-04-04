import javax.microedition.io.Datagram;
import javax.microedition.io.DatagramConnection;
import javax.microedition.io.Connector;
import javax.microedition.io.ConnectionNotFoundException;
import javax.microedition.io.ServerSocketConnection;
import java.io.IOException;

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
	} catch(Exception e) {
	    e.printStackTrace();
	}
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
		arcanvas.repaint_canvas();
		print_cnt = -10;
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
	} catch(IOException e1) { 
	    e1.printStackTrace();
	    ardrone_ip_cur = ""; //trigger a reconnect
	    try {
	    	Thread.sleep(1000);
	    } catch(Exception e2) {}
	} catch(Exception e) { 
	    e.printStackTrace(); 
	}
	}

	try {
	    dc_nav.close();
	} catch (Exception e) {}
    }
}
