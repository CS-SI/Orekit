package fr.cs.aerospace.orekit.perturbations;

import org.spaceroots.mantissa.geometry.Rotation;
import fr.cs.aerospace.orekit.RDate;

/**
 * This class represents the central body shape.
  
 * @version $Id$
 * @author E. Delente
 */

public abstract class EllipsoidicBody {
    
    /** Name */
    String name;
    
    /** Equatorial radius of the Central Body. */
    protected double equatorialRadius;

    /** Flatness. */
    protected double flatness;
    
   /** Creates a default instance of EllispoidicBody.
   */
    protected EllipsoidicBody() {
        this.name = "";
        this.equatorialRadius = 0.0;
        this.flatness = 0.0;
    }

    /** Creates a new instance of EllispoidicBody.
   * @param equatorialRadius equatorial radius of the ellipsoidic body
   * @param flatness Ellipsoidic body's flatness
   */
    protected EllipsoidicBody(String name, double equatorialRadius, double flatness) {
        this.name = name;
        this.equatorialRadius = equatorialRadius;
        this.flatness = flatness;
    }
    
   /** Extracts the sideral time at a given date.
   * @param date object given date
   */
    
    public abstract Rotation SideralTime(RDate t, Rotation R);
}

