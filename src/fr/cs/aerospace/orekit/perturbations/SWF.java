package fr.cs.aerospace.orekit.perturbations;

import fr.cs.aerospace.orekit.RDate;
import fr.cs.aerospace.orekit.OrekitException;

import org.spaceroots.mantissa.geometry.Vector3D;

/** This interface represents the switching function of the set of force 
 * models.
 *
 * <p>It should be implemented by all real force models before they
 * can be taken into account by the orbit extrapolation methods.</p>
 *
 * <p>Switching functions are a useful solution to meet the requirements of 
 * integrators concerning discontinuities problems.</p>
 *
 * @version $Id$
 * @author E. Delente
 */


public interface SWF {

    /** Compute the value of the switching function. 
     */    
    public double g(RDate t, Vector3D position, Vector3D velocity) throws OrekitException;
    
    /** Handle an event and choose what to do next.
     */
    public void eventOccurred(RDate t, Vector3D position, Vector3D velocity);
    
    /** Get the convergence threshold in the event time search.
     */
    public double getThreshold();
    
    /** Get maximal time interval between switching function checks.
     */
    public double getMaxCheckInterval();
    
}
