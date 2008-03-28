package fr.cs.orekit.propagation.numerical;

import java.io.Serializable;

import fr.cs.orekit.errors.OrekitException;
import fr.cs.orekit.propagation.SpacecraftState;

/** This interface represents space-dynamics aware switching functions.
 * 
 * <p>It mirrors the {@link org.apache.commons.math.ode.SwitchingFunction
 * SwitchingFunction} interface from <a href="http://commons.apache.org/math/"
 * commons-math</a> but provides a space-dynamics interface to the methods.</p>
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
 *  by the {@link #eventOccurred(SpacecraftState, double)}
 *  method, wich is called when the step is placed on the wanted date. <p>
 *
 * @author L. Maisonobe
 */


public interface OrekitSwitchingFunction extends Serializable {

    /** Compute the value of the switching function.
     * This function must be continuous (at least in its roots neighborhood),
     * as the integrator will need to find its roots to locate the events.
     * @param s the current state information: date, kinematics, attitude
     * @param mu central body attraction coefficient
     * @return value of the switching function
     * @throws OrekitException if some specific error occurs
     */
    public double g(SpacecraftState s, double mu) throws OrekitException;

    /** Handle an event and choose what to do next.
     * @param s the current state information : date, cinematics, attitude
     * @param mu central gravtitation coefficient
     * @throws OrekitException if some specific error occurs
     */
    public void eventOccurred(SpacecraftState s, double mu) throws OrekitException;

    /** Get the convergence threshold in the event time search.
     * @return convergence threshold
     */
    public double getThreshold();

    /** Get maximal time interval between switching function checks.
     * @return maximal time interval between switching function checks
     */
    public double getMaxCheckInterval();

    /** Get maximal number of iterations in the event time search.
     * @return maximal number of iterations in the event time search
     */
    public int getMaxIterationCount();

}
