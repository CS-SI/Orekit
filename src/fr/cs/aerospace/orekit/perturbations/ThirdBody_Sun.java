package fr.cs.aerospace.orekit.perturbations;

import org.spaceroots.mantissa.geometry.Vector3D;
import fr.cs.aerospace.orekit.Constants;
import fr.cs.aerospace.orekit.OrekitException;
import fr.cs.aerospace.orekit.RDate;

/**
 * This class represents the effect of the Sun as a third body.
 
 * <p>The equations used hereunder have been copied from Marmottes.</p>
 
 * @version $Id$
 * @author E. Delente
 */

public class ThirdBody_Sun extends ThirdBody{
            
   /** Creates a new instance of ThirdBody_Sun.
   */
    public ThirdBody_Sun() {
        super(Constants.Sunradius, Constants.Sunmu);
    }
    
   /** Gets the position of the Sun wrt to the central body.
   * @param date date
   * @return SunBody the Sun's position vector3D
   */
    
    public Vector3D getPosition(RDate t) throws OrekitException {

      // computation of the Sun position wrt the central body (units: m)
      // ===============================================================

      double date   = t.getOffset() - 10000.0;
      double f   = Math.toRadians(225.768 + 13.2293505 * date);
      double d   = Math.toRadians (11.786 + 12.190749 * date);
      double xlp = Math.toRadians (134.003 + 0.9856 * date);
      double g   = Math.toRadians (282.551 + 0.000047 * date);
      double e   = Math.toRadians (23.44223 - 3.5626E-07 * date);
      double ce  = Math.cos(e);
      double se  = Math.sin(e);
      double rot = 0.6119022E-06 * t.getOffset();
      double cr  = Math.cos(rot);
      double sr  = Math.sin(rot);

      // Newcomb's theory

      double cl = 99972.0 * Math.cos(xlp + g) + 1671.0 * Math.cos(xlp + xlp + g) 
                    - 1678.0 * Math.cos(g);
      cl = cl + 32.0 * Math.cos(3.0 * xlp + g) + Math.cos(4.0 * xlp + g) 
             + 2.0 * Math.cos(xlp + d + g);
      cl = cl - 4.0 * Math.cos(g - xlp) - 2.0 * Math.cos(xlp - d + g) 
             + 4.0 * Math.cos(f - d);
      cl = cl - 4.0 * Math.cos(xlp + xlp - f + d + g + g);
      cl = cl * 1.E-05;

      double sl = 99972.0 * Math.sin(xlp + g) + 1671.0 * 
                  Math.sin(xlp + xlp + g) - 1678.0 * Math.sin(g);
      sl = sl + 32.0 * Math.sin(3.0 * xlp + g) + Math.sin(4.0 * xlp + g) 
           + 2.0 * Math.sin(xlp + d + g);
      sl = sl - 4.0 * Math.sin(g - xlp) - 2.0 * Math.sin(xlp - d + g) 
           + 4.0 * Math.sin(f - d);
      sl = sl - 4.0 * Math.sin(xlp + xlp - f + d + g + g);
      sl = sl * 1.E-05;
      
      Vector3D centralSun = new Vector3D();
      double q = Math.sqrt(cl * cl + sl * sl);
      if (q < Constants.Epsilon) {throw new OrekitException("variable 'q' is equal to 0");}
      double sx = cl / q;
      double sy = sl * ce / q;
      centralSun.setCoordinates(sx * cr + sy * sr, sy * cr - sx * sr, sl * se / q);      
      double dasr = 1672.2 * Math.cos(xlp) + 28.0 * Math.cos(xlp + xlp) 
                    - 0.35 * Math.cos(d);

      // units: km
      Vector3D sunBody = new Vector3D();
      double denom = 1.0 + 1.E-05 * dasr;
      if (denom < Constants.Epsilon) 
          {throw new OrekitException("variable 'factor' is equal to 0");}
      double factor = 1000.0 * 149597870.0 / denom;
      sunBody.setCoordinates(factor * centralSun.getX(), 
                             factor * centralSun.getY(),
                             factor * centralSun.getZ());

      return sunBody;
    }
    
    

}

