import javax.microedition.lcdui.Graphics;

class JoystickR extends Joystick {
    public JoystickR(ArCanvas arcanvas, int ox, int oy, int w) {
    	super(arcanvas, ox, oy, w);
    }
    
    void handleUP(float ratio) {
    	System.out.println("handleUP(): " + ratio);
    	arcanvas.roll = 0;
    	arcanvas.pitch = arcanvas.speed;
    	arcanvas.gaz = 0;
    	arcanvas.yaw = 0;
    	
    	if (ratio == 1) {
    	    js_x = ox;
    	    js_y = oy - r;
	}
    }

    void handleDOWN(float ratio) {
    	System.out.println("handleDOWN(): " + ratio);
    	arcanvas.roll = 0;
    	arcanvas.pitch = -arcanvas.speed;
    	arcanvas.gaz = 0;
    	arcanvas.yaw = 0;

    	if (ratio == 1) {
    	    js_x = ox;
    	    js_y = oy + r;
	}
    }

    void handleLEFT(float ratio) {
    	System.out.println("handleLEFT(): " + ratio);
    	arcanvas.roll = 0;
    	arcanvas.pitch = 0;
    	arcanvas.gaz = 0;
    	arcanvas.yaw = -arcanvas.speed;

    	if (ratio == 1) {
    	    js_x = ox - r;
    	    js_y = oy;
	}
    }

    void handleRIGHT(float ratio) {
    	System.out.println("handleRIGHT(): " + ratio);
    	arcanvas.roll = 0;
    	arcanvas.pitch = 0;
    	arcanvas.gaz = 0;
    	arcanvas.yaw = arcanvas.speed;

    	if (ratio == 1) {
    	    js_x = ox + r;
    	    js_y = oy;
	}
    }
    
    void handlePressed() {
    	System.out.println("handlePressed()");
    	arcanvas.shift = true;
    }

    void handleReleased() {
    	System.out.println("handleReleased()");
    	arcanvas.roll = 0;
    	arcanvas.pitch = 0;
    	arcanvas.gaz = 0;
    	arcanvas.yaw = 0;

    	js_x = ox;
    	js_y = oy;
    }

    public void paint(Graphics g) {
    	joystickPaint(g);

        if (arcanvas.shift) {
            g.setColor(100, 100, 100);
            g.fillArc(js_x - bw/4, js_y - bw/4, bw/2, bw/2, 0, 360);
        }

	//UP (Pitch+) arrow
	if (arcanvas.pitch > 0) g.setColor(0, 255, 0);
	else g.setColor(100, 100, 100);
    	g.fillTriangle(ox, oy - w/2 - bw/2, ox - bw/2, oy - w/2 - bw/5, ox + bw/2, oy - w/2 - bw/5);

	//DOWN (Pitch-) arrow
	if (arcanvas.pitch < 0) g.setColor(0, 255, 0);
	else g.setColor(100, 100, 100);
    	g.fillTriangle(ox, oy + w/2 + bw/2, ox - bw/2, oy + w/2 + bw/5, ox + bw/2, oy + w/2 + bw/5);

	//LEFT (Yaw-) arrow
	if (arcanvas.yaw < 0) g.setColor(0, 255, 0);
	else g.setColor(100, 100, 100);
	g.drawArc(ox - w/2 - bw*4/10, oy - bw/2, bw, bw, 120, 120);
	g.drawArc(ox - w/2 - bw*4/10 -1, oy - bw/2, bw, bw, 120, 120);

	//Right (Yaw+) arrow
	if (arcanvas.yaw > 0) g.setColor(0, 255, 0);
	else g.setColor(100, 100, 100);
	g.drawArc(ox + w/2 - bw*6/10, oy - bw/2, bw, bw, -60, 120);
	g.drawArc(ox + w/2 - bw*6/10 - 1, oy - bw/2, bw, bw, -60, 120);
    }
}
