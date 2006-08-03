package fr.cs.aerospace.orekit.perturbations;

import fr.cs.aerospace.orekit.Attitude;
import fr.cs.aerospace.orekit.errors.OrekitException;
import fr.cs.aerospace.orekit.orbits.OrbitDerivativesAdder;
import fr.cs.aerospace.orekit.time.AbsoluteDate;

import org.spaceroots.mantissa.geometry.Vector3D;

/** This interface represents a forces model set.
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
 * @version $Id$
 * @author M. Romero
 * @author L. Maisonobe
 */


public interface ForceModel {

    /** Compute the contribution of the force model to the perturbing
     * acceleration.
     * @param t current date
     * @param position current position (m)
     * @param velocity current velocity (m/s)
     * @param adder object where the contribution should be added
     */
    public void addContribution(AbsoluteDate t, Vector3D position, Vector3D velocity, 
                                Attitude attitude, OrbitDerivativesAdder adder) throws OrekitException;

    /** Get the switching functions internally used by the model itself.
     * @return array of switching functions or null if the model doesn't need
     * any switching function by itself
     */
    public SWF[] getSwitchingFunctions();

}
