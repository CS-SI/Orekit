package fr.cs.aerospace.orekit.models.spacecraft;

import org.spaceroots.mantissa.geometry.Vector3D;
import fr.cs.aerospace.orekit.time.AbsoluteDate;

public interface AtmosphereDragSpacecraft extends Spacecraft{
	  /** Get the mass.
	   * @return mass (kg)
	   */
	  public double getMass();

	  /** Get the surface.
	   * @param direction direction of the flux
	   * @return surface (m<sup>2</sup>)
	   */
	  public double getSurface(Vector3D direction, AbsoluteDate t);

	  /** Get the drag coefficients vector.
	   * @param direction direction of the atmospheric flux
	   * @return drag coefficients vector
	   */
	  public Vector3D getDragCoef(Vector3D direction, AbsoluteDate t);

	  /** Set the mass.
	   * @param mass new mass (kg)
	   */
	  public void setMass(double mass);
}
