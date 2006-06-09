package fr.cs.aerospace.orekit.bodies;

import org.spaceroots.mantissa.geometry.Rotation;
import org.spaceroots.mantissa.geometry.Vector3D;

import fr.cs.aerospace.orekit.RDate;

/** Simple earth rotation model with a linear sidereal time.
 * @author Luc Maisonobe
 * @version $Id$
 */
public class FixedPoleEarth implements RotatingBody {

  public Rotation getOrientation(RDate date) {

    // sidereal time upgraded wrt the CNES julian date t
    // (seconds are expressed in fraction of day)
    double a1  = 0.1746647708617871E1;
    double a3  = 0.7292115146705E-04;
    double eps = 1.7202179573714597E-02;
    double offset = date.minus(RDate.CNES1950RDate);
    
    double r = offset - Math.floor(offset);
    r = a1 + eps * offset + a3 * offset + (Math.PI + Math.PI) * r;
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
