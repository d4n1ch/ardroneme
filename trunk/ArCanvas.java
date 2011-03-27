import javax.microedition.lcdui.Canvas;
import javax.microedition.lcdui.Graphics;
import javax.microedition.lcdui.Font;
import java.util.Vector; 

class ArCanvas extends Canvas {
    static final int FIRE_W = 30;
    static final int FIRE_WW = 50;
    int slider_w;
    int slider_len;
    int slider_thick;
    int pointer_focus = 0;
    static final float SPEED_MAX = (float)0.9;
    static final float SPEED_MIN = (float)0.0;
    float speed = (float)0.2;
    float roll = 0, pitch = 0, gaz = 0, yaw = 0;
    boolean shift = false;
    int w, h, wh, fh, bw, bh, last_y, slider_y0, slider_y, o_x, o_y;
    Font f;
    static final String arrow1[] = {"U", "R", "D", "L"};
    static final String arrow2[] = {"F", "RR", "B", "LR"};
    boolean arrow_pressed[] = {false, false, false, false};
    String arrow[] = arrow1;
    ARDroneME ardroneme;
    int battery = 0;
    float altitude = 0;
    String status_str = "";
    Vector widgets = new Vector();
    JoystickL js_L;

    public ArCanvas(ARDroneME ardroneme) {
    	this.ardroneme = ardroneme;
    	w = getWidth();
        h = getHeight();
        wh = Math.min(w, h);
	f = Font.getDefaultFont();
	fh = f.getHeight(); //font H
	bw = f.stringWidth("Takeoff") + fh*2; //button W
	bh = fh*2; //button H
	slider_w = bh*w*2/(wh*3);
	slider_len = h*46/100;
	slider_thick = fh/3;

	slider_y0 = (h - slider_len)/2;
	slider_y = slider_y0 + (int)((slider_len - 5) * (SPEED_MAX - speed) / (SPEED_MAX - SPEED_MIN));
	o_x = w - FIRE_WW/2 - 30;
	o_y = h/2;

	js_L = new JoystickL(this, (w - slider_w)/4, h/2, w/3);
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
                if (!shift) roll = -speed;
            	else yaw = -speed;
                break;
            
            case Canvas.RIGHT:
                System.out.println("RIGHT 1");
		arrow_pressed[1] = true;
                if (!shift) roll = speed;
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
                if (!shift) roll = 0;
            	else yaw = 0;
                break;
            
            case Canvas.RIGHT:
                System.out.println("RIGHT 0");
		arrow_pressed[1] = false;
                if (!shift) roll = 0;
            	else yaw = 0;
                break;            	

            default:
            	return;
        }

	repaint();
        ardroneme.wake_thread();
    }

    public void pointerPressed(int x, int y) {
    	js_L.pointerPressed(x, y);
    	
	if (x >= (w - slider_w)/2 && x <= (w + slider_w)/2 && y >= slider_y0 && y <= slider_y0 + slider_len) {
	    last_y = y;
	    pointer_focus = 1;
	}
	
	if (x > (w - bw)/2 && x < (w + bw)/2 && y > h - fh - bh && y < h - fh) {
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
            if (!shift) roll = -speed;
            else yaw = -speed;
 	}

	if (x > o_x + FIRE_WW/2 && x < o_x + FIRE_WW + 10 && y > o_y - FIRE_W/2 && y < o_y + FIRE_W/2) {
            System.out.println("RIGHT 1");
            arrow_pressed[1] = true;
            if (!shift) roll = speed;
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
    	js_L.pointerReleased(x, y);

        System.out.println("pointerReleased");
        pointer_focus = 0;
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
    	js_L.pointerDragged(x, y);

	if (pointer_focus == 1) {
	    slider_y += y - last_y;
	    speed = SPEED_MAX - (SPEED_MAX - SPEED_MIN) * (slider_y - slider_y0 - 1) / (slider_len - slider_thick);
	    if (slider_y <= slider_y0) {
	    	slider_y = slider_y0 + 1;
	    	speed = SPEED_MAX;
	    }
	    if (slider_y > slider_y0 + slider_len - slider_thick) {
	    	slider_y = slider_y0 + slider_len - slider_thick;
	    	speed = SPEED_MIN;
	    }
	}

	last_y = y;

	repaint();
    }
    
    public void repaint_canvas() {
    	repaint();
    }

    public void paint(Graphics g) {
        g.setColor(255, 255, 255);
    	g.fillRect(0, 0, w, h);

    	js_L.paint(g);

        g.setColor(0, 0, 255);
	g.drawRect((w - slider_w)/2, slider_y0, slider_w, slider_len);
	g.setColor(255, 0, 255);
	g.fillRect((w - slider_w)/2 + 1, slider_y, slider_w - 1, slider_thick);

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
	g.fillRoundRect((w - bw)/2, h - fh - bh, bw, bh, bh, bh);
	if (ardroneme.flying_flag) {
	    //g.setColor(0, 0, 255);
	    g.setColor(255, 0, 0); //JavaFX1.2 bug: (B,G,R) format for Text
            g.drawString("Landing", w/2, h - (fh + (bh - fh)/2), Graphics.BOTTOM|Graphics.HCENTER);
	} else {
	    g.setColor(255, 0, 255);
            g.drawString("Takeoff", w/2, h - (fh + (bh - fh)/2), Graphics.BOTTOM|Graphics.HCENTER);
	}

        g.setColor(200, 200, 200);
        g.fillArc(w/2 - bh/2, fh, bh, bh, 0, 360);
	//g.setColor(255, 0, 0);
	g.setColor(0, 0, 255); //JavaFX1.2 bug: (B,G,R) format for Text
        g.drawString("E", w/2, fh + (bh + fh)/2, Graphics.BOTTOM|Graphics.HCENTER);

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

        g.drawString("Speed(%):", w/2, slider_y0, Graphics.BOTTOM|Graphics.HCENTER);
        g.drawString("" + (int)(SPEED_MAX*100), w/2, slider_y0, Graphics.TOP|Graphics.HCENTER);
        g.drawString("" + (int)(SPEED_MIN*100), w/2, slider_y0 + slider_len, Graphics.BOTTOM|Graphics.HCENTER);
        g.drawString("" + (int)(speed*100), (w + slider_w)/2 + 2, slider_y + fh*2/3, Graphics.BOTTOM|Graphics.LEFT);
        g.drawString("Battery: " + battery + "%", 2, 0, Graphics.TOP|Graphics.LEFT);
        g.drawString("Altitude: ", w - 2 - f.stringWidth("000.000m"), 0, Graphics.TOP|Graphics.RIGHT);
        g.drawString(altitude + "m", w - 2, 0, Graphics.TOP|Graphics.RIGHT);
        g.drawString("Status: " + status_str, 2, h, Graphics.BOTTOM|Graphics.LEFT);
    }
}
