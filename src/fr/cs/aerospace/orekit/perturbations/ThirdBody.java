package fr.cs.aerospace.orekit.perturbations;

import fr.cs.aerospace.orekit.RDate;
import fr.cs.aerospace.orekit.OrekitException;

import org.spaceroots.mantissa.geometry.Vector3D;


/**
 * This class represents the effect of a third body.
 
 * @version $Id$
 * @author E. Delente
 */

public abstract class ThirdBody {
        
    /** Equatorial radius of the Third Body. */
    protected double radius;
    
    /** Central body attraction coefficient. */
    protected double mu;
    
   /** Creates a new instance of ThirdBody.
   * @param radius radius of the third body
   * @param mu mu of the third body
   * @param position position of the third body
   */
    protected ThirdBody(double radius, double mu) {
    this.radius = radius;
    this.mu = mu;
    }
    
    /** Get the position of the third body.
    * @return a vector3D 
    */
    public abstract Vector3D getPosition(RDate t) throws OrekitException;
    
    /** Get the radius of the third body.
    * @return a double 
    */
    public double getRadius() {
        return radius;
    }
    
    /** Get the mu of the third body.
    * @return a double
    */
    public double getMu() {
        return mu;
    }
}

