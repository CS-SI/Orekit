package fr.cs.orekit.forces;

import fr.cs.orekit.errors.OrekitException;
import fr.cs.orekit.propagation.SpacecraftState;
import fr.cs.orekit.propagation.TimeDerivativesEquations;

/** This interface represents a force model set.
 *
 * <p>It should be implemented by all real force models before they
 * can be taken into account by the orbit extrapolation methods.</p>
 *
 * <p>For real problems, and according to the kind of forces we want to
 * represent (gravitational or non-gravitational perturbations), the
 * contribution of the perturbing acceleration is added like a disturbing term
 * in the partial derivatives coming from the Gauss equations or the Lagrange's
 * planetary equations.</p>
 *
 * @author M. Romero
 * @author L. Maisonobe
 */

public interface ForceModel {

    /** Compute the contribution of the force model to the perturbing
     * acceleration.
     * @param s the current state information : date, cinematics, attitude
     * @param adder object where the contribution should be added
     * @param mu central gravitation coefficient
     * @throws OrekitException if some specific error occurs
     */
    public void addContribution(SpacecraftState s, TimeDerivativesEquations adder, double mu)
    throws OrekitException;

    /** Get the switching functions internally used by the model itself.
     * @return array of switching functions or null if the model doesn't need
     * any switching function by itself
     */
    public SWF[] getSwitchingFunctions();

}
