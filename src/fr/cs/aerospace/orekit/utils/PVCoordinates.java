package fr.cs.aerospace.orekit.utils;

import java.io.Serializable;

import org.spaceroots.mantissa.geometry.Vector3D;

/** Simple container of the cinematic couple Position (m) and Velocity (m/s).
 * 
 * @author F Maussion
 */
public class PVCoordinates implements Serializable {

    /** Simple constructor.
	 * <p> Sets the Coordinates to default : (0 0 0) (0 0 0).
	 */
	public PVCoordinates() {
	    position = new Vector3D();
	    velocity = new Vector3D();
	}
	
	/** Builds a PVCoordinates couple.
	 * @param position the position vector (m)
	 * @param velocity the velocity vector (m/s)
	 */
	public PVCoordinates(Vector3D position, Vector3D velocity) {
		
		this.position = position;
		this.velocity = velocity;
		
	}
	
	/** Gets the position.
	 * @return the position vector (m).
	 */
	public Vector3D getPosition() {
		return position;
	}

	/** Gets the velocity
	 * @return the velocity vector (m/s).
	 */
	public Vector3D getVelocity() {
		return velocity;
	}
	
    public String toString() {
	    StringBuffer sb = new StringBuffer();
	    sb.append('{');
        sb.append("X: ");
	    sb.append(position.getX());
        sb.append("; Y: ");
	    sb.append(position.getY());
        sb.append("; Z: ");
	    sb.append(position.getZ());
        sb.append("; XDot: ");
        sb.append(velocity.getX());
        sb.append("; YDot: ");
	    sb.append(velocity.getY());
        sb.append("; ZDot: ");
	    sb.append(velocity.getZ());
        sb.append(";}");
	    return sb.toString();
	}
	
	/** The position. 
	 */
	private final Vector3D position;
	
	/** The velocity. 
	 */
	private final Vector3D velocity;

    private static final long serialVersionUID = -8311737465010015024L;
}
