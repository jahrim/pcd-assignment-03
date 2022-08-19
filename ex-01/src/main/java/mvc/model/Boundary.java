package mvc.model;

/**
 * Boundary of the field where bodies move. 
 *
 */
public class Boundary {
	private final double x0;
	private final double y0;
	private final double x1;
	private final double y1;

	public static Boundary copyOf(Boundary boundary){ return new Boundary(boundary); }
	public Boundary(double x0, double y0, double x1, double y1){
		this.x0=x0;
		this.y0=y0;
		this.x1=x1;
		this.y1=y1;
	}
	private Boundary(Boundary boundary){ this(boundary.x0, boundary.y0, boundary.x1, boundary.y1); }

	public double getX0(){
		return x0;
	}
	public double getX1(){
		return x1;
	}
	public double getY0(){
		return y0;
	}
	public double getY1(){
		return y1;
	}
}