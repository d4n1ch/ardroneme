import javax.microedition.lcdui.Graphics;

abstract class Joystick extends Widget implements PointerHandler {
    int bw; //ball W
    int r;  //radius of the ball moving
    static final float RATIO_MIN = (float)0.5;
    int js_x, js_y, last_x, last_y;

    public Joystick(ArCanvas arcanvas, int ox, int oy, int w) {
    	super(arcanvas, ox, oy, w, w);
    	bw = w/3;
    	r = (w - bw)/2;
    	js_x = ox;
    	js_y = oy;
    }
    
    public void joystickPaint(Graphics g) {
        g.setColor(180, 180, 180);
    	widgetPaint(g);
        g.fillArc(js_x - bw/2, js_y - bw/2, bw, bw, 0, 360);	
    }

    public void pointerPressed(int x, int y) {
	if (isInRange(x, y)) {
	    last_x = x;
	    last_y = y;
	    focused = true;
	    handlePressed();
	}
    }

    public void pointerReleased(int x, int y) {
	focused = false;	
    	handleReleased();
    }

    public void pointerDragged(int x, int y) {
	if (!focused) return;
	
	js_x += x - last_x;
	js_y += y - last_y;
	last_x = x;
	last_y = y;

	if (js_x < ox - r) js_x = ox - r;
	if (js_x > ox + r) js_x = ox + r;
	if (js_y < oy - r) js_y = oy - r;
	if (js_y > oy + r) js_y = oy + r;

	float ratio_x = (float)(js_x - ox)/r;
	float ratio_y = (float)(js_y - oy)/r;
	//System.out.println("ratio_x = " + ratio_x + ", ratio_y = " + ratio_y);

	if (-ratio_y >= RATIO_MIN && ((ratio_x <= 0 && -ratio_y > -ratio_x) 
			|| (ratio_x > 0 && -ratio_y > ratio_x))) handleUP(-ratio_y);
	if (ratio_y >= RATIO_MIN && ((ratio_x <= 0 && ratio_y > -ratio_x) 
			|| (ratio_x > 0 && ratio_y > ratio_x))) handleDOWN(ratio_y);
	if (-ratio_x >= RATIO_MIN && ((ratio_y <= 0 && -ratio_y < -ratio_x) 
			|| (ratio_y > 0 && ratio_y < -ratio_x))) handleLEFT(-ratio_x);
	if (ratio_x >= RATIO_MIN && ((ratio_y <= 0 && -ratio_y > -ratio_x) 
			|| (ratio_y > 0 && ratio_y > ratio_x))) handleRIGHT(ratio_x);
    }

    abstract void handleUP(float ratio);
    abstract void handleDOWN(float ratio);
    abstract void handleLEFT(float ratio);
    abstract void handleRIGHT(float ratio);
    abstract void handlePressed();
    abstract void handleReleased();
}
