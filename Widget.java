import javax.microedition.lcdui.Graphics;

class Widget {
    ArCanvas arcanvas;
    int ox, oy, w, h;
    boolean focused = false;

    public Widget(ArCanvas arcanvas, int ox, int oy, int w, int h) {
    	this.arcanvas = arcanvas;
    	this.ox = ox;
    	this.oy = oy;
    	this.w = w;
    	this.h = h;   	
    }
    
    public boolean isInRange(int x, int y) {
    	return (x >= ox - w/2 && x <= ox + w/2 && y >= oy - w/2 && y <= oy + w/2);
    }
    
    public void widgetPaint(Graphics g) {
    	g.drawRect(ox - w/2, oy - w/2, w, h);  	
    }
}
