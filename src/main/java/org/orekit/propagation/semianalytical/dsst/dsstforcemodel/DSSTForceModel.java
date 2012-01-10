package org.orekit.propagation.semianalytical.dsst.dsstforcemodel;

import org.orekit.errors.OrekitException;
import org.orekit.propagation.SpacecraftState;
import org.orekit.time.AbsoluteDate;

/**
 * This interface represents a force modifying spacecraft motion for a {@link DSSTPropagator}.
 * <p>
 * Objects implementing this interface are intended to be added to a {@link DSSTPropagator
 * semianalytical DSST propagator} before the propagation is started.
 * </p>
 * <p>
 * The propagator will call at each step the {@link #getMeanElementRate(SpacecraftState)} method.
 * The force model instance will extract all the state data needed (date, position, velocity, frame,
 * attitude, mass) to compute the mean element rates that contribute to the mean state derivative.
 * </p>
 * <p>
 * The propagator will call the {@link #getShortPeriodicVariations(AbsoluteDate, double[])} method
 * at the end of the propagation in order to compute the short periodic variations to be added to
 * the mean elements to get the final state.
 * </p>
 * 
 * @author Romain Di Constanzo
 * @author Pascal Parraud
 */
public interface DSSTForceModel {

    /**
     * Compute the mean element rates
     * 
     * @param state
     *            current state information: date, kinematics, attitude
     * @return the mean element rates dai/dt
     * @exception OrekitException
     *                if some specific error occurs
     */
    double[] getMeanElementRate(final SpacecraftState state) throws OrekitException;

    /**
     * Compute the short periodic variations
     * 
     * @param date
     *            current date
     * @param meanElements
     *            current mean elements
     * @return the short periodic variations
     * @exception OrekitException
     *                if some specific error occurs
     */
    double[] getShortPeriodicVariations(final AbsoluteDate date,
                                        final double[] meanElements) throws OrekitException;
    
    /**
     * Initialize the current force model. This method has to be triggered just before the first
     * propagation, i.e, once the force model is completely defined. 
     * @throws OrekitException 
     */
    void initialize(final SpacecraftState initialState) throws OrekitException;
}
