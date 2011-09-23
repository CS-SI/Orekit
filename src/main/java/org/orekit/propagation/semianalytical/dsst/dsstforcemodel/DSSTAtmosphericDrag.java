package org.orekit.propagation.semianalytical.dsst.dsstforcemodel;

import org.apache.commons.math.geometry.euclidean.threed.Vector3D;
import org.apache.commons.math.util.FastMath;
import org.orekit.errors.OrekitException;
import org.orekit.forces.drag.Atmosphere;
import org.orekit.forces.drag.DragSensitive;
import org.orekit.frames.Frame;
import org.orekit.orbits.OrbitType;
import org.orekit.orbits.PositionAngle;
import org.orekit.propagation.SpacecraftState;
import org.orekit.time.AbsoluteDate;


/** Atmospheric drag force model for {@link DSSTPropagator}.
 *
 * <p>
 *  The drag acceleration is computed as follows :
 *
 *  &gamma; = (1/2 * Ro * V<sup>2</sup> * S / Mass) * DragCoefVector
 *
 *  With DragCoefVector = {Cx, Cy, Cz} and S given by the user through the interface
 *  {@link DragSensitive}
 * </p>
 *
 *  @author Pascal Parraud
 */
public class DSSTAtmosphericDrag implements DSSTForceModel {

    /** Null contribution. */
    private final static double[] NULL_CONTRIBUTION = {0.,0.,0.,0.,0.,0.};

    /** Atmospheric model. */
    private final Atmosphere atmosphere;

    /** Drag sensitive spacecraft. */
    private final DragSensitive spacecraft;

    /** Critical distance from the center of the central body for entering/leaving the atmosphere. */
    private double rbar;

    /** Simple constructor.
     * @param atmosphere atmospheric model
     * @param spacecraft the object physical and geometrical information
     */
    public DSSTAtmosphericDrag(final Atmosphere atmosphere, final DragSensitive spacecraft) {
        this.atmosphere = atmosphere;
        this.spacecraft = spacecraft;
        this.rbar = Double.NEGATIVE_INFINITY;
    }

    /** Get the critical distance.
     *  @return the critical distance from the center of the central body
     */
    public double getRbar() {
        return rbar;
    }

    /** Set the critical distance.
     *  The critical distance from the center of the central body
     *  aims at defining the atmosphere entry/exit.
     *  @param rbar the critical distance to set
     */
    public void setRbar(double rbar) {
        this.rbar = rbar;
    }

    /** {@inheritDoc} */
    public double[] getMeanElementRate(final SpacecraftState state) throws OrekitException {
        final double[]   mer = NULL_CONTRIBUTION;
        final double[][] jac = new double[6][6];
        double coef = 1.;
        // Analyse critical distance from center of central body
        double r = state.getOrbit().getPVCoordinates().getPosition().getNorm();
        if (r < rbar) {
            // compute the integral limits
            double[] f = getFLimits(state);
            coef -= state.getEquinoctialEx() * (FastMath.sin(f[1]) - FastMath.sin(f[0]));
            coef -= state.getEquinoctialEy() * (FastMath.cos(f[0]) - FastMath.cos(f[1]));
        }
        // Compute drag acceleration
        Vector3D drag = getDragAcceleration(state);
        // Compute jacobian
        OrbitType.EQUINOCTIAL.convertType(state.getOrbit()).getJacobianWrtCartesian(PositionAngle.ECCENTRIC, jac);
        // Compute mean elements rate
        for (int i = 0; i < 6; i++) {
            final Vector3D aisrp = new Vector3D(jac[i][3],jac[i][4],jac[i][5]);
            mer[i] = coef * Vector3D.dotProduct(aisrp, drag);
        }
        return mer;
    }

    /** {@inheritDoc} */
    public double[] getShortPeriodicVariations(final AbsoluteDate date, final double[] stateVector)
        throws OrekitException {
        // Short Periodic Variations are set to null
        return NULL_CONTRIBUTION;
    }

    /** {@inheritDoc} */
    public void init(final SpacecraftState state) throws OrekitException {
        // Nothing to do here
    }

    /** Compute the acceleration due to drag.
     *  <p>
     *  The computation includes all spacecraft specific characteristics
     *  like shape, area and coefficients.
     *  </p>
     *  @param s current state information: date, kinematics, attitude
     *  @exception OrekitException if some specific error occurs
     */
    private Vector3D getDragAcceleration(final SpacecraftState s) throws OrekitException {

        final AbsoluteDate date     = s.getDate();
        final Frame        frame    = s.getFrame();
        final Vector3D     position = s.getPVCoordinates().getPosition();

        final double rho    = atmosphere.getDensity(date, position, frame);
        final Vector3D vAtm = atmosphere.getVelocity(date, position, frame);
        final Vector3D relativeVelocity = vAtm.subtract(s.getPVCoordinates().getVelocity());
        // compute drag for the given drag sensitive spacecraft
        return spacecraft.dragAcceleration(s, rho, relativeVelocity);
    }

    /** Compute the limits for the mean elements rate integral.
     *  @param  s current state information: date, kinematics, attitude
     *  @return the limits for the integral
     */
    private double[] getFLimits(final SpacecraftState s) {

        final double a  = s.getA();
        final double e  = s.getE();
        final double eb = FastMath.acos((1. - rbar/a)/e);
        final double ww = FastMath.atan2(s.getEquinoctialEx(), s.getEquinoctialEy());

        double f[] = {-eb + ww, eb + ww};
        
        return f;
    }

}
