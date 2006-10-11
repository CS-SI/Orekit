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
	
	/** Gets the position.
	 * @return the position vector (m).
	 */
	public Vector3D getPosition() {
		return position;
	}

	/** Sets the position.
	 * @param position the new position (m).
	 */
	public void setPosition(Vector3D position) {
		this.position = position;
	}

	/** Gets the velocity
	 * @return the velocity vector (m/s).
	 */
	public Vector3D getVelocity() {
		return velocity;
	}

	/** Sets the velocity
	 * @param velocity the new velocity (m/s).
	 */
	public void setVelocity(Vector3D velocity) {
		this.velocity = velocity;
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
