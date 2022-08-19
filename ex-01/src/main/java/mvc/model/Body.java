package mvc.model;

import util.exception.InfiniteForceException;
import util.math.P2d;
import util.math.V2d;

import java.util.Objects;

/**
 * This class represents a body.
 */
public class Body {
	private static final double REPULSIVE_CONST = 0.01;
	private static final double FRICTION_CONST = 1;

    private P2d pos;
    private V2d vel;
    private V2d acc;
    private double mass;
    private int id;

    public static Body copyOf(Body body){ return new Body(body); }
    public Body(int id, P2d pos, V2d vel, V2d acc, double mass){
    	this.id = id;
        this.pos = pos;
        this.vel = vel;
        this.acc = acc;
        this.mass = mass;
    }
    private Body(Body b){ this(b.id, P2d.copyOf(b.pos), V2d.copyOf(b.vel), V2d.copyOf(b.acc), b.mass); }

    public double getMass() {
    	return mass;
    }
    public P2d getPos(){
        return pos;
    }
    public V2d getVel(){
        return vel;
    }
    public V2d getAcc(){ return acc; }
    public int getId() {
    	return id;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Body body = (Body) o;
        return id == body.id;
    }
    @Override
    public int hashCode() { return Objects.hash(id); }

    /**
     * Update the position, according to current velocity.
     * @param dt time elapsed
     */
    public void updatePosition(double dt){
    	pos.sum(V2d.copyOf(vel).scalarMul(dt));
    }
    /**
     * Update the velocity, given the instant acceleration.
     * @param dt time elapsed
     */
    public void updateVelocity(double dt){
    	vel.sum(V2d.copyOf(this.acc).scalarMul(dt));
    }
    /**
     * Update the acceleration, given the received force.
     * @param receivedForce the received force
     */
    public void updateAcceleration(V2d receivedForce){
        this.acc = V2d.copyOf(receivedForce).scalarMul(1.0 / this.mass);
    }
    /**
     * Change the velocity.
     * @param vx the new x velocity
     * @param vy the new y velocity
     */
    public void changeVelocity(double vx, double vy){
    	vel.change(vx, vy);
    }
    /**
     * @param b the specified body
     * @return the distance from the specified body
     */
    public double getDistanceFrom(Body b) {
    	double dx = pos.getX() - b.getPos().getX();
    	double dy = pos.getY() - b.getPos().getY();
    	return Math.sqrt(dx*dx + dy*dy);
    }
    /**
     * @param b the specified body
     * @return the repulsive force exerted by the specified body
     * @throws InfiniteForceException if the distance between the two bodies is null
     */
    public V2d computeRepulsiveForceBy(Body b) throws InfiniteForceException {
		double dist = getDistanceFrom(b);
		if (dist > 0) {
			try {
				return new V2d(b.getPos(), pos)
					.normalize()
					.scalarMul(b.getMass()*REPULSIVE_CONST/(dist*dist));
			} catch (Exception ex) {
				throw new InfiniteForceException();
			}
		} else {
			throw new InfiniteForceException();
		}
    }
    /**
     * @param bodies the specified bodies
     * @return the total force exerted on this body, as the sum of the total repulsive
     *         force with the specified bodies and the total friction force.
     */
    public V2d computeTotalForceOnSelf(Iterable<Body> bodies) {
        V2d totalForce = new V2d(0, 0);
        for (Body otherBody: bodies) {                     //total repulsive force
            if (!this.equals(otherBody)) {
                try {
                    V2d forceByOtherBody = this.computeRepulsiveForceBy(otherBody);
                    totalForce.sum(forceByOtherBody);
                } catch (Exception ex) { ex.printStackTrace(); }
            }
        }
        totalForce.sum(this.getCurrentFrictionForce());    //total friction force
        return totalForce;
    }
    /**
     * Compute current friction force, given the current velocity.
     */
    public V2d getCurrentFrictionForce() {
        return V2d.copyOf(vel).scalarMul(-FRICTION_CONST);
    }
    /**
     * Check if there are collisions with the specified boundary and update the
     * position and velocity accordingly.
     * @param bounds the specified boundary
     */
    public void checkAndSolveBoundaryCollision(Boundary bounds){
    	double x = pos.getX();
    	double y = pos.getY();

    	if (x > bounds.getX1()){
            pos.change(bounds.getX1(), pos.getY());
            vel.change(-vel.getX(), vel.getY());
        } else if (x < bounds.getX0()){
            pos.change(bounds.getX0(), pos.getY());
            vel.change(-vel.getX(), vel.getY());
        }

        if (y > bounds.getY1()){
            pos.change(pos.getX(), bounds.getY1());
            vel.change(vel.getX(), -vel.getY());
        } else if (y < bounds.getY0()){
            pos.change(pos.getX(), bounds.getY0());
            vel.change(vel.getX(), -vel.getY());
        }
    }
}