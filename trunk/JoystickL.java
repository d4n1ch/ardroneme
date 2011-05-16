import javax.microedition.lcdui.Graphics;

class JoystickL extends Joystick {
    public JoystickL(ArCanvas arcanvas, int ox, int oy, int w) {
    	super(arcanvas, ox, oy, w);
    }
    
    void handleUP(float ratio) {
    	System.out.println("handleUP(): " + ratio);
    	arcanvas.enable = 1;
    	arcanvas.roll = 0;
    	arcanvas.pitch = 0;
    	arcanvas.gaz = arcanvas.speed;
    	arcanvas.yaw = 0;

    	if (ratio == 1 && js_y == oy) js_y = oy - r;
    }

    void handleDOWN(float ratio) {
    	System.out.println("handleDOWN(): " + ratio);
    	arcanvas.enable = 1;
    	arcanvas.roll = 0;
    	arcanvas.pitch = 0;
    	arcanvas.gaz = -arcanvas.speed;
    	arcanvas.yaw = 0;

    	if (ratio == 1 && js_y == oy) js_y = oy + r;
    }

    void handleLEFT(float ratio) {
    	System.out.println("handleLEFT(): " + ratio);
    	arcanvas.enable = 1;
    	if (arcanvas.go_mode) {
    	    arcanvas.roll = -arcanvas.speed;
    	    arcanvas.yaw = 0;
	} else {
    	    arcanvas.roll = 0;
    	    arcanvas.yaw = -arcanvas.speed;
	}

    	arcanvas.pitch = 0;
    	arcanvas.gaz = 0;

    	if (ratio == 1 && js_x == ox) js_x = ox - r;
    }

    void handleRIGHT(float ratio) {
    	System.out.println("handleRIGHT(): " + ratio);
    	arcanvas.enable = 1;
    	if (arcanvas.go_mode) {
    	    arcanvas.roll = arcanvas.speed;
    	    arcanvas.yaw = 0;
	} else {
    	    arcanvas.roll = 0;
    	    arcanvas.yaw = arcanvas.speed;
	}
    	arcanvas.pitch = 0;
    	arcanvas.gaz = 0;

    	if (ratio == 1 && js_x == ox) js_x = ox + r;
    }

    void handlePressed() {
    	System.out.println("handlePressed()");
    	arcanvas.shift = false;
    }
    
    void handleReleased() {
    	System.out.println("handleReleased()");
    	arcanvas.enable = 0;
    	arcanvas.roll = 0;
    	arcanvas.pitch = 0;
    	arcanvas.gaz = 0;
    	arcanvas.yaw = 0;

    	js_x = ox;
    	js_y = oy;
    }

    public void paint(Graphics g) {
    	joystickPaint(g);

        if (!arcanvas.shift) {
            if (arcanvas.direct_motor) g.setColor(255, 0, 0);
            else g.setColor(100, 100, 100);
            g.fillArc(js_x - bw/4, js_y - bw/4, bw/2, bw/2, 0, 360);
        }

	//UP (Gaz+) arrow
	if (arcanvas.gaz > 0) g.setColor(0, 255, 0);
	else g.setColor(100, 100, 100);
	g.drawLine(ox - bw, oy - w/2 - bw/5, ox + bw, oy - w/2 - bw/5);
    	g.fillTriangle(ox, oy - w/2 - bw/2, ox - bw/2, oy - w/2 - bw/5, ox + bw/2, oy - w/2 - bw/5);

	//DOWN (Gaz-) arrow
	if (arcanvas.gaz < 0) g.setColor(0, 255, 0);
	else g.setColor(100, 100, 100);
	g.drawLine(ox - bw, oy + w/2 + bw/5, ox + bw, oy + w/2 + bw/5);
    	g.fillTriangle(ox, oy + w/2 + bw/2, ox - bw/2, oy + w/2 + bw/5, ox + bw/2, oy + w/2 + bw/5);

	if (arcanvas.go_mode) {
	    //LEFT (Roll-) arrow
	    if (arcanvas.roll < 0) g.setColor(0, 255, 0);
	    else g.setColor(100, 100, 100);
    	    g.fillTriangle(ox - w/2 - bw/2, oy, ox - w/2 - bw/5, oy - bw/2, ox - w/2 - bw/5, oy + bw/2);
            
	    //Right (Roll+) arrow
	    if (arcanvas.roll > 0) g.setColor(0, 255, 0);
	    else g.setColor(100, 100, 100);
    	    g.fillTriangle(ox + w/2 + bw/2, oy, ox + w/2 + bw/5, oy - bw/2, ox + w/2 + bw/5, oy + bw/2);
	} else {
	    //LEFT (Yaw-) arrow
	    if (arcanvas.yaw < 0) g.setColor(0, 255, 0);
	    else g.setColor(100, 100, 100);
	    g.drawArc(ox - w/2 - bw*4/10, oy - bw/2, bw, bw, 120, 120);
	    g.drawArc(ox - w/2 - bw*4/10 -1, oy - bw/2, bw, bw, 120, 120);
	    g.drawLine(ox - w/2 - bw*4/10, oy + bw*4/10, ox - w/2 - bw*2/10, oy + bw*4/10);
	    g.drawLine(ox - w/2 - bw*2/10, oy + bw*2/10, ox - w/2 - bw*2/10, oy + bw*4/10);
            
	    //Right (Yaw+) arrow
	    if (arcanvas.yaw > 0) g.setColor(0, 255, 0);
	    else g.setColor(100, 100, 100);
	    g.drawArc(ox + w/2 - bw*6/10, oy - bw/2, bw, bw, -60, 120);
	    g.drawArc(ox + w/2 - bw*6/10 - 1, oy - bw/2, bw, bw, -60, 120);
	    g.drawLine(ox + w/2 + bw*4/10, oy + bw*4/10, ox + w/2 + bw*2/10, oy + bw*4/10);
	    g.drawLine(ox + w/2 + bw*2/10, oy + bw*2/10, ox + w/2 + bw*2/10, oy + bw*4/10);
	}
    }
}
