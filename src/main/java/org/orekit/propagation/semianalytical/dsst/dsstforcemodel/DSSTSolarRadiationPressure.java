package org.orekit.propagation.semianalytical.dsst.dsstforcemodel;

import org.apache.commons.math.geometry.euclidean.threed.Vector3D;
import org.apache.commons.math.util.FastMath;
import org.orekit.errors.OrekitException;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.semianalytical.dsst.DSSTPropagator;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.PVCoordinates;
import org.orekit.utils.PVCoordinatesProvider;

/** Solar radiation pressure contribution for {@link DSSTPropagator}.
 * <p>
 *  The solar radiation pressure acceleration is computed as follows:<br>
 *  &gamma; = (1/2 C<sub>R</sub> A / m) * (p<sub>ref</sub> * d<sup>2</sup><sub>ref</sub>) * (r<sub>sat</sub> - R<sub>sun</sub>) / |r<sub>sat</sub> - R<sub>sun</sub>|<sup>3</sup>
 * </p>
 *
 *  @author Pascal Parraud
 */
public class DSSTSolarRadiationPressure extends AbstractDSSTGaussianContribution {

    // Quadrature parameters
    /** Number of points desired for quadrature (must be between 2 and 5 inclusive). */
    private final static int[] NB_POINTS = {2, 5, 5, 5, 5, 5};
    /** Relative accuracy of the result. */
    private final static double[] RELATIVE_ACCURACY = {1.e-5, 1.e-3, 1.e-3, 1.e-3, 1.e-3, 1.e-3};
    /** Absolute accuracy of the result. */
    private final static double[] ABSOLUTE_ACCURACY = {1.e-18, 1.e-20, 1.e-20, 1.e-20, 1.e-20, 1.e-20};
    /** Maximum number of evaluations. */
    private final static int[] MAX_EVAL = {1000000, 1000000, 1000000, 1000000, 1000000, 1000000};

   /** Flux on satellite: kRef = 0.5 * C<sub>R</sub> * Area * P<sub>Ref</sub> * D<sub>Ref</sub><sup>2</sup>. */
   private final double kRef;

   /** Sun model. */
   private final PVCoordinatesProvider sun;

   /** Square of Central Body radius: (R<sub>+</sub>)<sup>2</sup>. */
   private final double cbr2;

   /** Simple constructor with default reference values.
    *  <p>When this constructor is used, the reference values are:</p>
    *  <ul>
    *   <li>d<sub>ref</sub> = 149597870000.0 m</li>
    *   <li>p<sub>ref</sub> = 4.56 10<sup>-6</sup> N/m<sup>2</sup></li>
    *  </ul>
    *  @param cr satellite radiation pressure coefficient (assuming total specular reflection)
    *  @param area cross sectionnal area of satellite
    *  @param sun Sun model
    *  @param equatorialRadius spherical shape model (for shadow computation)
    */
    public DSSTSolarRadiationPressure(final double cr, final double area,
                                      final PVCoordinatesProvider sun,
                                      final double equatorialRadius) {
        this(149597870000.0, 4.56e-6, cr, area, sun, equatorialRadius);
    }

    /** Complete constructor.
     *  <p>Note that reference solar radiation pressure <code>pRef</code> in
     *  N/m<sup>2</sup> is linked to solar flux SF in W/m<sup>2</sup> using
     *  formula pRef = SF/c where c is the speed of light (299792458 m/s). So
     *  at 1UA a 1367 W/m<sup>2</sup> solar flux is a 4.56 10<sup>-6</sup>
     *  N/m<sup>2</sup> solar radiation pressure.</p>
     *  @param dRef reference distance for the solar radiation pressure (m)
     *  @param pRef reference solar radiation pressure at dRef (N/m<sup>2</sup>)
     *  @param cr satellite radiation pressure coefficient (assuming total specular reflection)
     *  @param area cross sectionnal area of satellite
     *  @param sun Sun model
     *  @param equatorialRadius spherical shape model (for shadow computation)
     */
    public DSSTSolarRadiationPressure(final double dRef, final double pRef,
                                      final double cr, final double area,
                                      final PVCoordinatesProvider sun,
                                      final double equatorialRadius) {
        this.kRef = 0.5 * pRef * dRef * dRef * cr * area;
        this.sun  = sun;
        this.cbr2 = equatorialRadius * equatorialRadius;
    }

    /** {@inheritDoc} */
    public double[] getShortPeriodicVariations(final AbsoluteDate date, final double[] meanElements)
        throws OrekitException {
        // TODO: not implemented yet
        // Short Periodic Variations are set to null
        return new double[] {0.,0.,0.,0.,0.,0.};
    }

    /** {@inheritDoc} */
    public void init(final SpacecraftState state) throws OrekitException {
    }

    /** {@inheritDoc} */
    protected Vector3D getAcceleration(final SpacecraftState state,
                                       final Vector3D position,
                                       final Vector3D velocity) throws OrekitException {
    
        final Vector3D sunSat = getSunSatVector(state, position);
        final double R        = sunSat.getNorm();
        final double R3       = R * R * R;
        final double T        = kRef / state.getMass();
        // raw radiation pressure
        return new Vector3D(T / R3, sunSat);
    }

    /** {@inheritDoc} */
    protected double[] getLLimits(SpacecraftState state) throws OrekitException {
        double[] ll = {-FastMath.PI, FastMath.PI};
        // TODO: cylinder or conical modeling for shadow computation
        final double M = cbr2;
        return ll;
    }

    /** {@inheritDoc} */
    protected int getNbPoints(int element) {
        return NB_POINTS[element];
    }

    /** {@inheritDoc} */
    protected double getRelativeAccuracy(int element) {
        return RELATIVE_ACCURACY[element];
    }

    /** {@inheritDoc} */
    protected double getAbsoluteAccuracy(int element) {
        return ABSOLUTE_ACCURACY[element];
    }

    /** {@inheritDoc} */
    protected int getMaxEval(int element) {
        return MAX_EVAL[element];
    }

    /** Compute Sun-sat vector in SpacecraftState frame.
     *  @param state current spacecraft state
     *  @param position spacecraft position
     *  @return Sun-sat vector in SpacecraftState frame
     *  @exception OrekitException if sun position cannot be computed
     */
    private Vector3D getSunSatVector(final SpacecraftState state, final Vector3D position)
        throws OrekitException {
        final PVCoordinates sunPV = sun.getPVCoordinates(state.getDate(), state.getFrame());
        return position.subtract(sunPV.getPosition());
    }

}
