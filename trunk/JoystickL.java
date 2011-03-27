class JoystickL extends Joystick {
    public JoystickL(ArCanvas arcanvas, int ox, int oy, int w) {
    	super(arcanvas, ox, oy, w);
    }
    
    void handleUP(float ratio) {
    	System.out.println("handleUP(): " + ratio);

    }

    void handleDOWN(float ratio) {
    	System.out.println("handleDOWN(): " + ratio);
    	
    }

    void handleLEFT(float ratio) {
    	System.out.println("handleLEFT(): " + ratio);
    	
    }

    void handleRIGHT(float ratio) {
    	System.out.println("handleRIGHT(): " + ratio);
    	
    }
}
