package fr.cs.aerospace.orekit.perturbations;

import fr.cs.aerospace.orekit.RDate;
import fr.cs.aerospace.orekit.Constants;

import org.spaceroots.mantissa.geometry.Vector3D;

/**
 * This class represents the effect of the central body as a third body.
 
 * @version $Id$
 * @author E. Delente
 */

public class ThirdBody_Central extends ThirdBody{
        
   /** Creates a new instance of ThirdBody_Central.
   * @param radius radius of the third body
   * @param mu mu of the third body
   * @param position position of the third body
   */
    public ThirdBody_Central() {
        super(Constants.CentralBodyradius, Constants.CentralBodymu);
    }
    
   /** Gets the position of the central body wrt the central body.
   * @param date date
   * @return CentralBody the CentralBody position vector3D
   */
    public Vector3D getPosition(RDate t) {
      // Computation of the central position wrt the central body (units: m)
      Vector3D centralBody = new Vector3D();
      centralBody.setCoordinates(0., 0., 0.);
      return centralBody;
    }
    
}
