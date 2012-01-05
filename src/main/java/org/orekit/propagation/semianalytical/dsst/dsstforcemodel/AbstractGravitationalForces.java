package org.orekit.propagation.semianalytical.dsst.dsstforcemodel;

import org.orekit.propagation.semianalytical.dsst.coefficients.ModifiedNewcombOperatorLoader;

/**
 * This abstract class represent gravitational forces and contains the {@link DSSTThirdBody} and the
 * {@link DSSTCentralBody} force model. .<br>
 * As resonant central body tesseral harmonics and third body potential expressions are using
 * Modified Newcomb Operator, we mainly use this class to define a common data loader to read the
 * Modified Newcomb Operator from an internal file.
 * 
 * @author rdicosta
 */
public abstract class AbstractGravitationalForces implements DSSTForceModel {

    ModifiedNewcombOperatorLoader loader;

    /**
     * Dummy constructor
     */
    public AbstractGravitationalForces() {
        loader = null;
        // Dummy constructor
    }

    /**
     * Get the Modified Newcomb Operator loader
     * 
     * @return
     */
    public ModifiedNewcombOperatorLoader getLoader() {
        return this.loader;
    }

    /**
     * Initialize the Modified Newcomb Operator loader
     */
    public void initializeLoader() {
        if (loader == null){
            
        }

    }

}
