package fr.cs.aerospace.orekit.forces;

import fr.cs.aerospace.orekit.attitudes.AttitudeKinematics;
import fr.cs.aerospace.orekit.errors.OrekitException;
import fr.cs.aerospace.orekit.frames.Frame;
import fr.cs.aerospace.orekit.propagation.TimeDerivativesEquations;
import fr.cs.aerospace.orekit.time.AbsoluteDate;
import fr.cs.aerospace.orekit.utils.PVCoordinates;

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
 * @version $Id: ForceModel.java 1032 2006-09-28 08:25:21 +0000 (jeu., 28 sept. 2006) fabien $
 * @author M. Romero
 * @author L. Maisonobe
 */

public interface ForceModel {

  /** Compute the contribution of the force model to the perturbing
   * acceleration.
   * @param t current date
   * @param pvCoordinates the {@link PVCoordinates}
   * @param frame in which are defined the coordinates
   * @param mass the current mass (kg)
   * @param ak the attitude representation
   * @param adder object where the contribution should be added
   */
  public void addContribution(AbsoluteDate t, PVCoordinates pvCoordinates,
                              Frame frame, double mass, AttitudeKinematics ak,
                              TimeDerivativesEquations adder)
      throws OrekitException;

  /** Get the switching functions internally used by the model itself.
   * @return array of switching functions or null if the model doesn't need
   * any switching function by itself
   */
  public SWF[] getSwitchingFunctions();

}
