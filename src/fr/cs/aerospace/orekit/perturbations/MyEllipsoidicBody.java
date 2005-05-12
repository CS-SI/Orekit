package fr.cs.aerospace.orekit.perturbations;

import org.spaceroots.mantissa.geometry.Rotation;
import org.spaceroots.mantissa.geometry.Vector3D;
import fr.cs.aerospace.orekit.RDate;

/**
 * This class represents an ellispoidic body.
 
 * <p>The equations used hereafter have been copied from Marmottes.</p>
 
 * @version $Id$
 * @author E. Delente
 */

public class MyEllipsoidicBody extends EllipsoidicBody {
    
   /** Creates a default instance of MyEllispoidicBody.
   */
    public MyEllipsoidicBody() {
        super();
    }
    
   /** Creates a new instance of MyEllispoidicBody.
    * @param name name 
    * @param equatorialRadius equatorial radius of the ellipsoidic body
    * @param flatness Ellipsoidic body's flatness
    */
    public MyEllipsoidicBody(String name, double equatorialRadius, double flatness) {
        super(name, equatorialRadius, flatness);
    }
    
   /** Definition of the rotation from inertial frame to body frame.
   * @param R Rotation of my ellipsoidic body wrt the inertial frame
   */    
    public Rotation SideralTime(RDate t, Rotation R){
    // sidereal time upgraded wrt the CNES julian date t
    // (seconds are expressed in fraction of day)
    double a1  = 0.1746647708617871E1;
    double a3  = 0.7292115146705E-04;
    double eps = 1.7202179573714597E-02;
    double Offset = t.minus(RDate.CNES1950RDate);
    
    double r = Offset - Math.floor(Offset);
    r = a1 + eps * Offset + a3 * Offset + (Math.PI + Math.PI) * r;
    if (r < 0.0) {
      while (r < 0.0) {
         r = r + 2 * Math.PI;
      }
    }
    if (r > 2 * Math.PI) {
      while (r > 2 * Math.PI) {
         r = r - 2 * Math.PI;
      }
    }
    return new Rotation(Vector3D.plusK,r);
    }
    
}

