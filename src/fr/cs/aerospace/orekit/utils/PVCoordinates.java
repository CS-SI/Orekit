package fr.cs.aerospace.orekit.utils;

import org.spaceroots.mantissa.geometry.Vector3D;

/** Simple container of the cinematic couple Position (m) and Velocity (m/s).
 * 
 * @author F Maussion
 */
public class PVCoordinates {

	/** Simple constructor.
	 * <p> Sets the Coordinates to default.
	 */
	public PVCoordinates() {
	    position = new Vector3D(Double.NaN, Double.NaN, Double.NaN);
	    velocity = new Vector3D(Double.NaN, Double.NaN, Double.NaN);
	}
	
	/** Builds a PVCoordinates couple.
	 * @param position the position vector (m)
	 * @param velocity the velocity vector (m/s)
	 */
	public PVCoordinates(Vector3D position, Vector3D velocity) {
		
		this.position = position;
		this.velocity = velocity;
		
	}
	
	/** Builds a PVCoordinates couple by copying an other.
	 * @param pv the {@link PVCoordinates} to copy.
	 */
	public PVCoordinates(PVCoordinates pv) {
		
		position = pv.position;
		velocity = pv.velocity;
		
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
	    sb.append(position.getX());
	    sb.append(' ');
	    sb.append(position.getY());
	    sb.append(' ');
	    sb.append(position.getZ());
	    sb.append(';');
	    sb.append(velocity.getX());
	    sb.append(' ');
	    sb.append(velocity.getY());
	    sb.append(' ');
	    sb.append(velocity.getZ());
	    sb.append('}');
	    return sb.toString();
	}
	
	/** The position. 
	 */
	private Vector3D position;
	
	/** The velocity. 
	 */
	private Vector3D velocity;
	
}
