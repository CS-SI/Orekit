package fr.cs.aerospace.orekit.forces;

import java.io.Serializable;

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
 * <p>  Switching functions are a useful solution to meet the requirements 
 * of integrators concerning discontinuities problems. The value of the 
 * switching function is asked by the integrator at each step. When the 
 * value of the g function changes of sign, the step is rejected and reduced,
 * until the roots of the function is reached with the {@link #getThreshold()}
 * precision. </p>
 * 
 * <p> Once the g function root is reached, we are sure the integrator
 *  won't miss the event relative to this date : a discontinuity in
 *  acceleration, a change in the state... This event can be initiated
 *  by the {@link #eventOccurred(AbsoluteDate, PVCoordinates, Frame, double, AttitudeKinematics)}
 *  method, wich is called when the step is placed on the wanted date. <p> 
 *
 * @author L. Maisonobe
 */


public interface SWF extends Serializable {

    /** Compute the value of the switching function. 
     * This function must be continuous (at least in its roots neighborhood),
     * as the integrator will need to find its roots to locate the events.
     * @param t current date
     * @param pvCoordinates the {@link PVCoordinates}
     * @param frame in which are defined the coordinates
     * @param mass the current mass (kg)
     * @param ak the attitude representation
     * @return the value of the switching function
     * @throws OrekitException if some specific error occurs
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
