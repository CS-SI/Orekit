package org.orekit.propagation.semianalytical.dsst.dsstforcemodel;

import org.apache.commons.math.geometry.euclidean.threed.Vector3D;
import org.apache.commons.math.util.FastMath;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.forces.radiation.RadiationSensitive;
import org.orekit.frames.Frame;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.OrbitType;
import org.orekit.orbits.PositionAngle;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.semianalytical.dsst.DSSTPropagator;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.Constants;
import org.orekit.utils.PVCoordinates;
import org.orekit.utils.PVCoordinatesProvider;

/** Solar radiation pressure force model for {@link DSSTPropagator}.
*
*  @author Pascal Parraud
*/
public class DSSTSolarRadiationPressure implements DSSTForceModel {

    /** Sun radius (m). */
   private static final double SUN_RADIUS = Constants.SUN_RADIUS;

   /** DSST model needs equinoctial orbit as internal representation.
    *  Classical equinoctial elements have discontinuities when inclination is close to zero.
    *  In this representation, I = +1. <br>
    *  To avoid this discontinuity, another representation exists and equinoctial elements can
    *  be expressed in a different way, called "retrograde" orbit. This implies I = -1.
    *  As Orekit doesn't implement the retrograde orbit, I = +1 here.
    */
   private double I = 1;

   // Equinoctial elements according to DSST theory
   
   /** a */
   double a;
   /** ex */
   double k;
   /** ey */
   double h;
   /** hx */
   double q;
   /** hy */
   double p;

   // Equinoctial coefficients

   /** A = sqrt(&mu; * a) */
   private double A;

   /** B = sqrt(1 - ex<sup>2</sup> - ey<sup>2</sup>) */
   private double B;

   /** C = 1 + hx<sup>2</sup> + hx<sup>2</sup>) */
   private double C;

   // Direction cosines of the symmetry axis
   /** &alpha; */
   private double alpha;

   /** &beta; */
   private double beta;

   /** &gamma; */
   private double gamma;

   /** Central body attraction coefficient (m<sup>3</sup>/s<sup>2</sup>). */
   private double mu;

   /** Reference flux normalized for a 1m distance (N). */
   private final double kRef;

   /** Sun model. */
   private final PVCoordinatesProvider sun;

   /** Earth equatorial radius. */
   private final double equatorialRadius;

   /** Spacecraft. */
   private final RadiationSensitive spacecraft;

   /** Simple constructor with default reference values.
    *  <p>When this constructor is used, the reference values are:</p>
    *  <ul>
    *   <li>d<sub>ref</sub> = 149597870000.0 m</li>
    *   <li>p<sub>ref</sub> = 4.56 10<sup>-6</sup> N/m<sup>2</sup></li>
    *  </ul>
    *  @param sun Sun model
    *  @param equatorialRadius spherical shape model (for umbra/penumbra computation)
    *  @param spacecraft the object physical and geometrical information
    */
    public DSSTSolarRadiationPressure(final PVCoordinatesProvider sun, final double equatorialRadius,
                                      final RadiationSensitive spacecraft) {
        this(149597870000.0, 4.56e-6, sun, equatorialRadius, spacecraft);
    }

    /** Complete constructor.
     *  <p>Note that reference solar radiation pressure <code>pRef</code> in
     *  N/m<sup>2</sup> is linked to solar flux SF in W/m<sup>2</sup> using
     *  formula pRef = SF/c where c is the speed of light (299792458 m/s). So
     *  at 1UA a 1367 W/m<sup>2</sup> solar flux is a 4.56 10<sup>-6</sup>
     *  N/m<sup>2</sup> solar radiation pressure.</p>
     *  @param dRef reference distance for the solar radiation pressure (m)
     *  @param pRef reference solar radiation pressure at dRef (N/m<sup>2</sup>)
     *  @param sun Sun model
     *  @param equatorialRadius spherical shape model (for umbra/penumbra computation)
     *  @param spacecraft the object physical and geometrical information
     */
    public DSSTSolarRadiationPressure(final double dRef, final double pRef,
                                      final PVCoordinatesProvider sun,
                                      final double equatorialRadius,
                                      final RadiationSensitive spacecraft) {
        this.kRef = pRef * dRef * dRef;
        this.sun  = sun;
        this.equatorialRadius = equatorialRadius;
        this.spacecraft = spacecraft;
    }

    /** {@inheritDoc} */
    public double[] getMeanElementRate(final SpacecraftState state) throws OrekitException {
        double[] meanElementRate = NULL_CONTRIBUTION;
        double[] stateVector = NULL_CONTRIBUTION;
        // Store current state :
        OrbitType.EQUINOCTIAL.mapOrbitToArray(state.getOrbit(), PositionAngle.MEAN, stateVector);
        // Initialisation of A, B, C, Alpha, Beta and Gamma coefficient :
        computeABCAlphaBetaGamma(stateVector);
        // Compute drag acceleration
        double V = computeRawP(state);
        // Compute mean elements rate
        final double ab = A * B;
        final double bvsa = B * V / A;
        final double kpihq = k * p - I * h * q;
        final double vgsab = V * gamma / ab;
        final double cvgs2ab = C * vgsab / 2.;
        final double vgsabkpihq = kpihq * vgsab;
        // da/dt =  0
        meanElementRate[0] = 0.;
        // dk/dt = −B*V*β/A + (V*h*γ/A*B)*(k*p − I*h*q)
        meanElementRate[1] = -bvsa * beta + vgsabkpihq * h;
        // dh/dt = B*V*α/A − (V*k*γ/A*B)*(k*p − I*h*q)
        meanElementRate[2] =  bvsa * alpha - vgsabkpihq * k;
        // dq/dt = I*C*V*k*γ/2*A*B
        meanElementRate[3] =  I * cvgs2ab * k;
        // dp/dt = C*V*h*γ/2*A*B
        meanElementRate[4] =  cvgs2ab * h;
        // dλ/dt = −(2 + B)*V*(k*α + h*β)/A*(1 + B) − (V*γ/A*B)*(k*p − I*h*q)
        meanElementRate[5] = -(vgsabkpihq + (2. + B) * V * (k * alpha + h * beta) / (A * (1. + B)));

        return meanElementRate;
    }

    /** {@inheritDoc} */
    public double[] getShortPeriodicVariations(final AbsoluteDate date, final double[] stateVector)
        throws OrekitException {
        // Short Periodic Variations are set to null
        return NULL_CONTRIBUTION;
    }

    /** {@inheritDoc} */
    public void init(final SpacecraftState state) throws OrekitException {
        this.mu = state.getMu();
    }

    /** Get the lightning ratio ([0-1]).
     * @param position the satellite's position in the selected frame.
     * @param frame in which is defined the position
     * @param date the date
     * @return lightning ratio
     * @exception OrekitException if the trajectory is inside the Earth
     */
    public double getLightningRatio(final Vector3D position, final Frame frame,
                                    final AbsoluteDate date)
        throws OrekitException {

        final Vector3D satSunVector =
            sun.getPVCoordinates(date, frame).getPosition().subtract(position);

        // Earth apparent radius
        final double r = position.getNorm();
        if (r <= equatorialRadius) {
            throw new OrekitException(OrekitMessages.TRAJECTORY_INSIDE_BRILLOUIN_SPHERE, r);
        }

        final double alphaEarth = FastMath.asin(equatorialRadius / r);

        // Definition of the Sun's apparent radius
        final double alphaSun = FastMath.asin(SUN_RADIUS / satSunVector.getNorm());

        // Retrieve the Sat-Sun / Sat-Central body angle
        final double sunEarthAngle = Vector3D.angle(satSunVector, position.negate());

        double result = 1.0;

        // Is the satellite in complete umbra ?
        if (sunEarthAngle - alphaEarth + alphaSun < 0.0) {
            result = 0.0;
        }
        // Compute a lightning ratio in penumbra
        if ((sunEarthAngle - alphaEarth + alphaSun >= 0.0) &&
            (sunEarthAngle - alphaEarth - alphaSun <= 0.0)) {

            //result = (alphaSun + sunEarthAngle - alphaEarth) / (2*alphaSun);

            final double alpha1 =
                (sunEarthAngle * sunEarthAngle -
                        (alphaEarth - alphaSun) * (alphaSun + alphaEarth)) / (2 * sunEarthAngle);

            final double alpha2 =
                (sunEarthAngle * sunEarthAngle +
                        (alphaEarth - alphaSun) * (alphaSun + alphaEarth)) / (2 * sunEarthAngle);

            final double P1 = FastMath.PI * alphaSun * alphaSun -
                alphaSun * alphaSun * FastMath.acos(alpha1 / alphaSun) +
                alpha1 * FastMath.sqrt(alphaSun * alphaSun - alpha1 * alpha1);

            final double P2 = alphaEarth * alphaEarth * FastMath.acos(alpha2 / alphaEarth) -
                alpha2 * FastMath.sqrt(alphaEarth * alphaEarth - alpha2 * alpha2);

            result = (P1 - P2) / (FastMath.PI * alphaSun * alphaSun);
        }
        return result;
    }

    /** Compute the acceleration due to solar radiation pressure.
     *  <p>
     *  The computation includes all spacecraft specific characteristics
     *  like shape, area and coefficients.
     *  </p>
     *  @param s current state information: date, kinematics, attitude
     *  @exception OrekitException if some specific error occurs
     */
    private Vector3D getRadiationPressureAcceleration(final SpacecraftState s) throws OrekitException {

        final Vector3D satSunVector = getSatSunVector(s);
        final double r              = satSunVector.getNorm();
        final double rawP           = computeRawP(s);
        // raw radiation pressure
        return spacecraft.radiationPressureAcceleration(s, new Vector3D(-rawP / r, satSunVector));
    }

    /** Compute sat-Sun vector in spacecraft state frame.
     * @param state current spacecraft state
     * @return sat-Sun vector in spacecraft state frame
     * @exception OrekitException if sun position cannot be computed
     */
    private Vector3D getSatSunVector(final SpacecraftState state)
        throws OrekitException {
        final PVCoordinates sunPV = sun.getPVCoordinates(state.getDate(), state.getFrame());
        final PVCoordinates satPV = state.getPVCoordinates();
        return sunPV.getPosition().subtract(satPV.getPosition());
    }

    /** Compute radiation coefficient.
     * @param s spacecraft state
     * @return coefficient for acceleration computation
     * @exception OrekitException if position cannot be computed
     */
    private double computeRawP (final SpacecraftState s) throws OrekitException {
        return kRef * getLightningRatio(s.getPVCoordinates().getPosition(), s.getFrame(), s.getDate())
                    / getSatSunVector(s).getNormSq();
    }

    /**
     * @param stateVector
     */
    private void computeABCAlphaBetaGamma(double[] stateVector) {
        // Equinoctial elements
        a = stateVector[0];
        k = stateVector[1];
        h = stateVector[2];
        q = stateVector[3];
        p = stateVector[4];

        // Factors
        final double k2 = k * k;
        final double h2 = h * h;
        final double q2 = q * q;
        final double p2 = p * p;

        // Equinoctial coefficients
        A = FastMath.sqrt(mu * a);
        B = FastMath.sqrt(1 - k2 - h2);
        C = 1 + q2 + p2;

        // Direction cosines
        alpha = -(2 * I * q) / C;
        beta  =   2 * p / C;
        gamma =  (1 - q2 - p2) * I / C;
    }

}
