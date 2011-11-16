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

    /** A value smaller than ALMOST_ZERO is considered to be zero (0.0). */
    private final static double ALMOST_ZERO = FastMath.ulp(1.0);

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
        final double[] a = new double[5];
        a[0] =  4. * b2 + cc * cc;
        a[1] =  8. * bb * mm * h + 4. * cc * mm * k;
        a[2] = -4. * b2 + 4. * m2 * h2 - 2. * cc * dd + 4. * m2 * k2;
        a[3] = -8. * bb * mm * h - 4. * dd * mm * k;
        a[4] = -4. * m2 * h2 + dd * dd;
        // Compute the real roots of the quartic equation
        final double[] cosL = new double[4];
        final int nbRoots = realQuarticRoots(a, cosL);
        if (nbRoots > 0) {
            // Test the roots

        }
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

    /** Compute the real roots of a quartic equation:
     *  <pre>a[0] * x<sup>4</sup> + a[1] * x<sup>3</sup> + a[2] * x<sup>2</sup> + a[3] * x + a[4] = 0.</pre>
     *  @param a the 5 coefficients
     *  @param y the real roots
     *  @return the number of real roots
     */
    private int realQuarticRoots(final double[] a, final double[] y)
    {
        /* Treat the degenerate quartic as cubic */
        if (a[0] == 0.0) {
            final double[] aa = new double[a.length - 1];
            System.arraycopy(a, 1, aa, 0, aa.length);
            return(realCubicRoots(aa, y));
        }
        
        double a0 = a[0];
        double a1 = a[1];
        double a2 = a[2];
        double a3 = a[3];
        double a4 = a[4];
        /* Set the leading coefficient to 1.0 */
        if (a0 != 1.0) {
            a1 /= a0;
            a2 /= a0;
            a3 /= a0;
            a4 /= a0;
        }

        /* Compute the cubic resolvant */
        final double a12 = a1 * a1;
        final double p = -0.375 * a12 + a2;
        final double q =  0.125 * a12 * a1 - 0.5 * a1 * a2 + a3;
        final double r = -0.01171875 * a12 * a12 + 0.0625 * a12 * a2 - 0.25 * a1 * a3 + a4;

        double[] y3 = new double[3];
        final int i3 = realCubicRoots(new double[] {1.0, -0.5 * p, -r, 0.5 * r * p - 0.125 * q * q}, y3);

        if (i3 == 0) {
            return(0);
        }
        final double z = y3[0];

        double d1 = 2.0 * z - p;
        if (d1 < 0.0) {
            if (d1 > -ALMOST_ZERO) {
                d1 = 0.0;
            } else {
                return(0);
            }
        }
        double d2;
        if (d1 < ALMOST_ZERO) {
            d2 = z * z - r;
            if (d2 < 0.0) {
                return(0);
            }
            d2 = FastMath.sqrt(d2);
        } else {
            d1 = FastMath.sqrt(d1);
            d2 = 0.5 * q / d1;
        }

        /* Set up useful values for the quadratic factors */
        final double q1 = d1 * d1;
        final double q2 = -0.25 * a1;

        int i4 = 0;
        /* Solve the first quadratic */
        double p1 = q1 - 4.0 * (z - d2);
        if (p1 == 0) {
            y[i4++] = -0.5 * d1 - q2;
        } else if (p1 > 0) {
            p1 = FastMath.sqrt(p1);
            y[i4++] = -0.5 * (d1 + p1) + q2;
            y[i4++] = -0.5 * (d1 - p1) + q2;
        }

        /* Solve the second quadratic */
        double p2 = q1 - 4.0 * (z + d2);
        if (p2 == 0) {
            y[i4++] = 0.5 * d1 - q2;
        } else if (p2 > 0) {
            p2 = FastMath.sqrt(p2);
            y[i4++] = 0.5 * (d1 + p2) + q2;
            y[i4++] = 0.5 * (d1 - p2) + q2;
        }

        return(i4);
    }

    /** Compute the real roots of a cubic equation:
     *  <pre>a[0] * x<sup>3</sup> + a[1] * x<sup>2</sup> + a[2] * x + a[3] = 0.</pre>
     *  @param a the 4 coefficients
     *  @param y the real roots
     *  @return the number of real roots
     */
    private int realCubicRoots(final double[] a, final double[] y)
    {
        /* Treat the degenerate cubic as quadratic */
        if (a[0] == 0.0) {
            final double[] aa = new double[a.length - 1];
            System.arraycopy(a, 1, aa, 0, aa.length);
            return(realQuadraticRoots(aa, y));
        }
        
        double a0 = a[0];
        double a1 = a[1];
        double a2 = a[2];
        double a3 = a[3];
        /* Make sure the cubic has a leading coefficient of 1.0 */
        if (a0 != 1.0) {
            a1 /= a0;
            a2 /= a0;
            a3 /= a0;
        }

        final double a12 = a1 * a1;
        final double q  = (a12 - 3.0 * a2) / 9.0;
        final double q3 = q * q * q;
        final double r  = (a1 * (a12 - 4.5 * a2) + 13.5 * a3) / 27.0;
        final double r2 = r * r;
        final double d  = q3 - r2;
        final double a1o3 = a1 / 3.0;

        if (d >= 0.0) {
            /* 3 real roots. */
            final double roq3 = r / FastMath.sqrt(q3);
            final double teta = FastMath.acos(roq3) / 3.0;
            final double sqrq = -2.0 * FastMath.sqrt(q);

            y[0] = sqrq * FastMath.cos(teta) - a1o3;
            y[1] = sqrq * FastMath.cos(teta + FastMath.PI * 2. / 3.) - a1o3;
            y[2] = sqrq * FastMath.cos(teta + FastMath.PI * 4. / 3.) - a1o3;

            return(3);

        } else {
            /* 1 real root. */
            final double tmp = FastMath.pow(FastMath.sqrt(-d) + FastMath.abs(r), 1.0 / 3.0);
            if (r < 0) {
                y[0] =  (tmp + q / tmp) - a1o3;
            } else {
                y[0] = -(tmp + q / tmp) - a1o3;
            }

            return(1);
        }
    }

    /** Compute the real roots of a quadratic equation:
     *  <pre>a[0] * x<sup>2</sup> + a[1] * x + a[2] = 0.</pre>
     *  @param a the 3 coefficients
     *  @param y the real roots
     *  @return the number of real roots
     */
    private int realQuadraticRoots(final double[] a, final double[] y)
    {
        final double aa =  a[0];
        final double bb = -a[1];
        final double cc =  a[2];

        if (aa == 0.0) {
            if (bb == 0.0) {
                return(0);
            }
            y[0] = cc / bb;
            return(1);
        }

        final double d = bb * bb - 4.0 * aa * cc;

        /* Treat values of d around 0 as 0. */
        if (FastMath.abs(d) < ALMOST_ZERO)
        {
            y[0] = 0.5 * bb / aa;
            return(1);
        } else {
            if (d < 0.0) {
                return(0);
            }
        }

        final double oo2a = 0.5 / aa;
        final double sqrd = FastMath.sqrt(d);

        y[0] = (bb + sqrd) * oo2a;
        y[1] = (bb - sqrd) * oo2a;

        return(2);
    }

}
