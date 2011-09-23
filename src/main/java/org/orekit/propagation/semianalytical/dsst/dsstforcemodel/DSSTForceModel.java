package org.orekit.propagation.semianalytical.dsst.dsstforcemodel;

import org.orekit.errors.OrekitException;
import org.orekit.propagation.SpacecraftState;
import org.orekit.time.AbsoluteDate;


/** This interface represents a force modifying spacecraft motion for a {@link DSSTPropagator}.
 *
 * <p>
 * Objects implementing this interface are intended to be added to a
 * {@link DSSTPropagator semianalytical DSST propagator} before the propagation is started.
 * </p>
 * <p>
 * The propagator will call at each step the {@link #getMeanElementRate(SpacecraftState)} method.
 * The force model instance will extract all the state data needed (date, position, velocity,
 * frame, attitude, mass) to compute the mean element rates that contribute to the global state
 * derivative.
 * </p>
 * <p>
 * The propagator will call the {@link #getShortPeriodicVariations(SpacecraftState)} method at
 * the end of the propagation. The force model instance will extract all the state data it needs
 * (date, position, velocity, frame, attitude, mass) to compute the short periodic variations to
 * be added to the final state.
 * </p>
 *
 * @author Romain Di Constanzo
 * @author Pascal Parraud
 */
public interface DSSTForceModel {

    /** Compute the mean element rates
     *  @param state current state information: date, kinematics, attitude
     *  @return the mean element rates dai/dt
     *  @exception OrekitException if some specific error occurs
     */
    public abstract double[] getMeanElementRate(final SpacecraftState state) throws OrekitException;

    /** Compute the short periodic variations
     *  @param date current date
     *  @param state current state vector
     *  @return the short periodic variations
     *  @exception OrekitException if some specific error occurs
     */
    public abstract double[] getShortPeriodicVariations(final AbsoluteDate date, final double[] stateVector)
        throws OrekitException;

    /** Initialize the current computation
     *  @param state current state information: date, kinematics, attitude
     *  @exception OrekitException if some specific error occurs
     */
    public abstract void init(final SpacecraftState state) throws OrekitException;

}
