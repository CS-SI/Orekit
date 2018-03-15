package org.orekit.propagation.semianalytical.dsst;

import java.util.Map;

import org.hipparchus.RealFieldElement;
import org.orekit.errors.OrekitException;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.utils.ParameterDriver;
import org.orekit.utils.ParameterDriversList;

public class DSSTPartialDerivativeEquations implements DSSTAdditionalEquations {
	
	/** Propagator computing state evolution. */
    private final DSSTPropagator propagator;
    
    /** Selected parameters for Jacobian computation. */
    private ParameterDriversList selected;

    /** Parameters map. */
    private Map<ParameterDriver, Integer> map;

    /** Name. */
    private final String name;

    /** Flag for Jacobian matrices initialization. */
    private boolean initialized;
    
    public DSSTPartialDerivativeEquations(final String name, final DSSTPropagator propagator)
        throws OrekitException {
    	this.name                   = name;
        this.selected               = null;
        this.map                    = null;
        this.propagator             = propagator;
        this.initialized            = false;    

    }

	@Override
	public String getName() {
		return name;
	}

	@Override
	public <T extends RealFieldElement<T>> T[] computeDerivatives(FieldSpacecraftState<T> s, T[] pDot)
		throws OrekitException {
        return null;
	}
    
}
