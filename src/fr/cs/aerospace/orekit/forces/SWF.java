package fr.cs.aerospace.orekit.forces;

import fr.cs.aerospace.orekit.attitudes.AttitudeKinematics;
import fr.cs.aerospace.orekit.errors.OrekitException;
import fr.cs.aerospace.orekit.frames.Frame;
import fr.cs.aerospace.orekit.time.AbsoluteDate;
import fr.cs.aerospace.orekit.utils.PVCoordinates;

/** This interface represents the switching function of the set of force 
 * models.
 *
 * <p>It should be implemented by all real force models before they
 * can be taken into account by the orbit extrapolation methods.</p>
 *
 * <p>Switching functions are a useful solution to meet the requirements of 
 * integrators concerning discontinuities problems.</p>
 * 
 * @author L. Maisonobe
 */


public interface SWF {

    /** Compute the value of the switching function. 
     * @param t current date
     * @param pvCoordinates the {@link PVCoordinates}
     * @param frame in which are defined the coordinates
     * @param mass the current mass (kg)
     * @param ak the attitude representation
     * @return the value of the switching function
     * @throws OrekitException
     */
    public double g(AbsoluteDate t, PVCoordinates pvCoordinates, Frame frame,
                      double mass, AttitudeKinematics ak)
      throws OrekitException;
    
    /** Handle an event and choose what to do next.
     * @param t current date
     * @param pvCoordinates the {@link PVCoordinates}
     * @param frame in which are defined the coordinates
     * @param mass the current mass (kg)
     * @param ak the attitude representation
     */
    public void eventOccurred(AbsoluteDate t, PVCoordinates pvCoordinates,
                              Frame frame, double mass, AttitudeKinematics ak);
    
    /** Get the convergence threshold in the event time search.
     */
    public double getThreshold();
    
    /** Get maximal time interval between switching function checks.
     */
    public double getMaxCheckInterval();
    
}
