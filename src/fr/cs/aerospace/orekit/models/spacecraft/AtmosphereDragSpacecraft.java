package fr.cs.aerospace.orekit.models.spacecraft;

import org.spaceroots.mantissa.geometry.Vector3D;
import fr.cs.aerospace.orekit.forces.perturbations.Drag;
import fr.cs.aerospace.orekit.time.AbsoluteDate;

/** Adapted container for the Atmosphere drag force model.
 * @see Drag
 * @author F. Maussion
 */
public interface AtmosphereDragSpacecraft {

//TODO Define correctly the signature of these methods (currently wrong)
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

}
