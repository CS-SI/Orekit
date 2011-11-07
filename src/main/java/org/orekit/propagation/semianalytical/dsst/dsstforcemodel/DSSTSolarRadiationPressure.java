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
        computeParameters(state);
        // Compute the coefficients of the quartic equation in cos(L) 3.5-(2)
        final double h2 = h * h;
        final double k2 = k * k;
        final double mm = cbr2 / (a * a * B * B);
        final double m2 = mm * mm;
        final Vector3D sunDir = sun.getPVCoordinates(state.getDate(), state.getFrame()).getPosition().normalize();
        final double alfa = sunDir.dotProduct(f);
        final double beta = sunDir.dotProduct(g);
        final double bet2 = beta * beta;
        final double bb = alfa * beta + mm * h * k;
        final double b2 = bb * bb;
        final double cc = alfa * alfa - bet2 + mm * (k2 - h2);
        final double dd =  1. - bet2 - mm * (1. + h2);
        final double a0 =  4. * b2 + cc * cc;
        final double a1 =  8. * bb * mm * h + 4. * cc * mm * k;
        final double a2 = -4. * b2 + 4. * m2 * h2 - 2. * cc * dd + 4. * m2 * k2;
        final double a3 = -8. * bb * mm * h - 4. * dd * mm * k;
        final double a4 = -4. * m2 * h2 + dd * dd;
        // Compute the real roots of the quartic equation
        final double[] cosL = realQuarticRoots(a0, a1, a2, a3, a4);
        // Test the roots
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

    /** Compute the real roots of the quartic equation:
     *    a<sub>0</sub> * x<sup>4</sup> + a<sub>1</sub> * x<sup>3</sup>
     *  + a<sub>2</sub> * x<sup>2</sup> + a<sub>3</sub> * x + a<sub>4</sub> = 0
     *  @param a0 1st coefficient
     *  @param a1 2nd coefficient
     *  @param a2 3rd coefficient
     *  @param a3 4th coefficient
     *  @param a4 5th coefficient
     *  @return the real roots of the quartic equation
     *  @exception OrekitException
     */
    private double[] realQuarticRoots(final double a0,
                                      final double a1,
                                      final double a2,
                                      final double a3,
                                      final double a4) {
        // Reduce all the coefficients
        final double c3 = a1 / a0;
        final double c2 = a2 / a0;
        final double c1 = a3 / a0;
        final double c0 = a4 / a0;

        // Compute the resolvent cubic coefficients
        final double cc2 = -c2;
        final double cc1 =  c1 * c3 - 4 * c0;
        final double cc0 =  4. * c2 * c0 - c1 * c1 - c3 * c3 * c0;
        // Compute a real root of the cubic equation
        final double rr3 = realCubicRoot(cc0, cc1, cc2);

        return new double[] {0., 0., 0., 0.};
    }

    /** Compute one real root of the cubic equation: 
     *    x<sup>3</sup> + a<sub>2</sub> * x<sup>2</sup> + a<sub>1</sub> * x + a<sub>0</sub> = 0
     *  @param a0 1st coefficient
     *  @param a1 2nd coefficient
     *  @param a2 3rd coefficient
     *  @return one real root of the cubic equation
     */
    private double realCubicRoot(final double a0, final double a1, final double a2) {
        double realRoot = 0.;
        final double Q  = (3. * a1 - a2 * a2) / 9.;
        final double Q3 = Q * Q * Q;
        final double R  = (9. * a1 * a2 - 27. * a0 - 2. * a2 * a2 * a2) / 54.;
        final double R2 = R * R;
        final double D  = R2 + Q3;
        if (D < 0.) {
            final double teta = FastMath.acos(R / FastMath.sqrt(-Q3));
            realRoot = 2. * FastMath.sqrt(-Q) * FastMath.cos(teta / 3.) - a2 / 3.;
        } else {
            final double oneThird = 1. / 3.;
            final double sqD = FastMath.sqrt(D);
            final double S = FastMath.pow(R + sqD, oneThird);
            final double T = FastMath.pow(R - sqD, oneThird);
            realRoot =  S + T - a2 / 3.;
        }
        return realRoot;
    }

    /** Compute the roots of the quadric equation.
     *  @param A the 5 coefficients
     *  @return the 4 roots of the quadric equation
     */
    private double[] quarticRoots(final double[] A) {

        final double[] a = {A[0] / A[4], A[1] / A[4], A[2] / A[4], A[3] / A[4]};

        final double a32 = a[3] * a[3];
        final double c0 = 4. * a[2] * a[0] - a[1] * a[1] - a32 * a[0];
        final double c1 = a[1] * a[3] - 4. * a[0];
        final double c2 = -a[2];

        final double y1 = realCubicRoot(c0, c1, c2);
        System.out.println("y1: " + y1);

        final double t0 = -a[3] / 4.;
        final double R2 = a32 / 4. - a[2] + y1;
        final double R  = FastMath.sqrt(R2);
        final double r2 = R / 2.;
        System.out.println("R: " + R);

        final double t1 = 3. * a32 / 4. - 2. * a[2];
        System.out.println("t1: " + t1);

        final double t2 = (4. * a[3] * a[2] - 8. * a[1] - a32 * a[3]) / 4.;
        System.out.println("t2: " + t2);

        final double t3 = 2. * FastMath.sqrt(y1 * y1 - 4. * a[0]);
        System.out.println("t3: " + t3);
 
        final double D  = (R != 0.) ? FastMath.sqrt(t1 - R2 + t2 / R) : FastMath.sqrt(t1 + t3);
        final double d2 = D / 2.;
        final double E  = (R != 0.) ? FastMath.sqrt(t1 - R2 - t2 / R) : FastMath.sqrt(t1 - t3);
        final double e2 = E / 2.;
        System.out.println("D: " + D);
        System.out.println("E: " + E);

        return new double[] {t0 + r2 + d2, t0 + r2 - d2, t0 - r2 + e2, t0 - r2 - e2};
    }

}
