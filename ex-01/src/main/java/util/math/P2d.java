package util.math;

public class P2d {
    private double x, y;

    public static P2d copyOf(P2d point){ return new P2d(point); }
    public P2d(double x,double y){
        this.x = x;
        this.y = y;
    }
    private P2d(P2d p){
        this.x = p.x;
        this.y = p.y;
    }

    public P2d sum(V2d v) {
    	x += v.x;
    	y += v.y;
    	return this;
    }
     
    public void change(double x, double y){
    	this.x = x;
    	this.y = y;
    }

    public double getX() {
    	return x;
    }

    public double getY() {
    	return y;
    }
}