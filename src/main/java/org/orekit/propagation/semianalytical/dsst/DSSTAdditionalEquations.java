package org.orekit.propagation.semianalytical.dsst;

import org.hipparchus.RealFieldElement;
import org.orekit.errors.OrekitException;
import org.orekit.propagation.FieldSpacecraftState;

public interface DSSTAdditionalEquations {

	/** Get the name of the additional state.
	 * @return name of the additional state
	 */
	String getName();
	   
	/** Compute the derivatives related to the additional state parameters.
	 * <p>
	 * When this method is called, the spacecraft state contains the main
	 * state (orbit, attitude and mass), all the states provided through
	 * the {@link org.orekit.propagation.AdditionalStateProvider additional
	 * state providers} registered to the propagator, and the additional state
	 * integrated using this equation. It does <em>not</em> contains any other
	 * states to be integrated alongside during the same propagation. 
	 * </p>
	 * @param s current state information: date, kinematics, attitude, and
	 * additional state
	 * @param pDot placeholder where the derivatives of the additional parameters
	 * should be put
	 * @return cumulative effect of the equations on the main state (may be null if
	 * equations do not change main state at all)
	 * @exception OrekitException if some specific error occurs
	 */
	
	<T extends RealFieldElement<T>> T[] computeDerivatives(FieldSpacecraftState<T> s,  T[] pDot)
	        throws OrekitException;

}
