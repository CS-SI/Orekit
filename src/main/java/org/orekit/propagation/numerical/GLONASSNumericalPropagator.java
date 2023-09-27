/* Copyright 2002-2023 CS GROUP
 * Licensed to CS GROUP (CS) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * CS licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.orekit.propagation.numerical;

import java.util.Arrays;

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.ode.ODEIntegrator;
import org.hipparchus.ode.nonstiff.ClassicalRungeKuttaIntegrator;
import org.hipparchus.util.FastMath;
import org.hipparchus.util.MathUtils;
import org.hipparchus.util.SinCos;
import org.orekit.attitudes.Attitude;
import org.orekit.attitudes.AttitudeProvider;
import org.orekit.data.DataContext;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.frames.Frame;
import org.orekit.orbits.CartesianOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.OrbitType;
import org.orekit.orbits.PositionAngleType;
import org.orekit.propagation.PropagationType;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.analytical.gnss.data.GLONASSAlmanac;
import org.orekit.propagation.analytical.gnss.data.GLONASSNavigationMessage;
import org.orekit.propagation.analytical.gnss.data.GLONASSOrbitalElements;
import org.orekit.propagation.analytical.gnss.data.GNSSConstants;
import org.orekit.propagation.integration.AbstractIntegratedPropagator;
import org.orekit.propagation.integration.StateMapper;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.GLONASSDate;
import org.orekit.utils.AbsolutePVCoordinates;
import org.orekit.utils.Constants;
import org.orekit.utils.IERSConventions;
import org.orekit.utils.PVCoordinates;
import org.orekit.utils.TimeStampedPVCoordinates;

/**
 * This class propagates GLONASS orbits using numerical integration.
 * <p>
 * As recommended by the GLONASS Interface Control Document (ICD),
 * a {@link ClassicalRungeKuttaIntegrator  4th order Runge-Kutta technique}
 * shall be used to integrate the equations.
 * </p>
 * <p>
 * Classical used of this orbit propagator is to compute GLONASS satellite
 * coordinates from the navigation message.
 * </p>
 * <p>
 * If the projections of luni-solar accelerations to axes of
 * Greenwich geocentric coordinates {@link GLONASSOrbitalElements#getXDotDot() X''(tb)},
 * {@link GLONASSOrbitalElements#getYDotDot() Y''(tb)} and {@link GLONASSOrbitalElements#getZDotDot() Z''(tb)}
 * are available in the navigation message; a transformation is performed to convert these
 * accelerations into the correct coordinate system. In the case where they are not
 * available into the navigation message, these accelerations are computed.
 * </p>
 * <p>
 * <b>Caution:</b> The Glonass numerical propagator can only be used with {@link GLONASSNavigationMessage}.
 * Using this propagator with a {@link GLONASSAlmanac} is prone to error.
 * </p>
 *
 * @see <a href="http://russianspacesystems.ru/wp-content/uploads/2016/08/ICD-GLONASS-CDMA-General.-Edition-1.0-2016.pdf">
 *       GLONASS Interface Control Document</a>
 *
 * @author Bryan Cazabonne
 *
 */
public class GLONASSNumericalPropagator extends AbstractIntegratedPropagator {

    /** Second degree coefficient of normal potential. */
    private static final double GLONASS_J20 = 1.08262575e-3;

    /** Equatorial radius of Earth (m). */
    private static final double GLONASS_EARTH_EQUATORIAL_RADIUS = 6378136;

    /** Value of the Earth's rotation rate in rad/s (See Ref). */
    private static final double GLONASS_AV = 7.2921151467e-5;

    // Data used to solve Kepler's equation
    /** First coefficient to compute Kepler equation solver starter. */
    private static final double A;

    /** Second coefficient to compute Kepler equation solver starter. */
    private static final double B;

    static {
        final double k1 = 3 * FastMath.PI + 2;
        final double k2 = FastMath.PI - 1;
        final double k3 = 6 * FastMath.PI - 1;
        A  = 3 * k2 * k2 / k1;
        B  = k3 * k3 / (6 * k1);
    }

    /** The GLONASS orbital elements used. */
    private final GLONASSOrbitalElements glonassOrbit;

    /** Initial date in GLONASS form. */
    private final GLONASSDate initDate;

    /** The spacecraft mass (kg). */
    private final double mass;

    /** The ECI frame used for GLONASS propagation. */
    private final Frame eci;

    /** Direction cosines and distance of perturbing body: Moon.
     * <p>
     * <ul>
     * <li>double[0] = ξ<sub>m</sub></li>
     * <li>double[1] = η<sub>m</sub></li>
     * <li>double[2] = ψ<sub>m</sub></li>
     * <li>double[3] = r<sub>m</sub></li>
     * </ul>
     * </p>
     */
    private double[] moonElements;

    /** Direction cosines and distance of perturbing body: Sun.
     * <p>
     * <ul>
     * <li>double[0] = ξ<sub>s</sub></li>
     * <li>double[1] = η<sub>s</sub></li>
     * <li>double[2] = ψ<sub>s</sub></li>
     * <li>double[3] = r<sub>s</sub></li>
     * </ul>
     * </p>
     */
    private double[] sunElements;

    /** Flag for availability of projections of acceleration transmitted within the navigation message. */
    private final boolean isAccAvailable;

    /** Data context used for propagation. */
    private final DataContext dataContext;

    /**
     * Private constructor.
     * @param integrator Runge-Kutta integrator
     * @param glonassOrbit Glonass orbital elements
     * @param eci Earth Centered Inertial frame
     * @param provider Attitude provider
     * @param mass Satellite mass (kg)
     * @param context Data context
     * @param isAccAvailable true if the acceleration  is transmitted within the navigation message
     */
    public GLONASSNumericalPropagator(final ClassicalRungeKuttaIntegrator integrator,
                                      final GLONASSOrbitalElements glonassOrbit,
                                      final Frame eci, final AttitudeProvider provider,
                                      final double mass, final DataContext context,
                                      final boolean isAccAvailable) {
        super(integrator, PropagationType.OSCULATING);
        this.dataContext = context;
        this.isAccAvailable = isAccAvailable;
        // Stores the GLONASS orbital elements
        this.glonassOrbit = glonassOrbit;
        // Sets the Earth Centered Inertial frame
        this.eci = eci;
        // Sets the mass
        this.mass = mass;
        this.initDate = new GLONASSDate(
                glonassOrbit.getDate(),
                dataContext.getTimeScales().getGLONASS());

        // Initialize state mapper
        initMapper();
        setInitialState();
        setAttitudeProvider(provider);
        setOrbitType(OrbitType.CARTESIAN);
        // It is not meaningful for propagation in Cartesian parameters
        setPositionAngleType(PositionAngleType.TRUE);
        setMu(GNSSConstants.GLONASS_MU);

        // As recommended by GLONASS ICD (2016), the direction cosines and distance
        // of perturbing body are calculated one time (at tb).
        if (!isAccAvailable) {
            computeMoonElements(initDate);
            computeSunElements(initDate);
        }
    }

    /**
     * Gets the underlying GLONASS orbital elements.
     *
     * @return the underlying GLONASS orbital elements
     */
    public GLONASSOrbitalElements getGLONASSOrbitalElements() {
        return glonassOrbit;
    }

    /** {@inheritDoc} */
    @Override
    public SpacecraftState propagate(final AbsoluteDate date) {
        // Spacecraft state in inertial frame
        final SpacecraftState stateInInertial = super.propagate(date);

        // Build the spacecraft state in inertial frame
        final PVCoordinates pvInPZ90 = getPVInPZ90(stateInInertial);
        final AbsolutePVCoordinates absPV = new AbsolutePVCoordinates(
                dataContext.getFrames().getPZ9011(IERSConventions.IERS_2010, true),
                stateInInertial.getDate(), pvInPZ90);
        final TimeStampedPVCoordinates pvInInertial = absPV.getPVCoordinates(eci);
        final SpacecraftState transformedState = new SpacecraftState(new CartesianOrbit(pvInInertial, eci, pvInInertial.getDate(), GNSSConstants.GLONASS_MU),
                                                                     stateInInertial.getAttitude(),
                                                                     stateInInertial.getMass(),
                                                                     stateInInertial.getAdditionalStatesValues(),
                                                                     stateInInertial.getAdditionalStatesDerivatives());

        return transformedState;
    }

    /**
     * Set the initial state.
     * <p>
     * The initial conditions on position and velocity are in the ECEF coordinate system PZ-90.
     * Previous to orbit integration, they must be transformed to an absolute inertial coordinate system.
     * </p>
     */
    private void setInitialState() {

        // Transform initial PV coordinates to an absolute inertial coordinate system.
        final PVCoordinates pvInInertial = getPVInInertial(initDate);

        // Create a new orbit
        final Orbit orbit = new CartesianOrbit(pvInInertial,
                                               eci, initDate.getDate(),
                                               GNSSConstants.GLONASS_MU);

        // Reset the initial state to apply the transformation
        resetInitialState(new SpacecraftState(orbit, mass));
    }

    /**
     * This method computes the direction cosines and the distance used to
     * compute the gravitational perturbations of the Moon.
     *
     * @param date the computation date in GLONASS scale
     */
    private void computeMoonElements(final GLONASSDate date) {

        moonElements = new double[4];

        // Constants
        // Semi-major axis of the Moon's orbit (m)
        final double am = 3.84385243e8;
        // The Moon's orbit eccentricity
        final double em = 0.054900489;
        // Mean inclination of the Moon's orbit to the ecliptic (rad)
        final double im = 0.0898041080;

        // Computed parameters
        // Time from epoch 2000 to the instant tb of GLONASS ephemeris broadcast
        final double dtoJD = (glonassOrbit.getTime() - 10800.) / Constants.JULIAN_DAY;
        final double t  = (date.getJD0() + dtoJD - 2451545.0) / 36525;
        final double t2 = t * t;
        // Mean inclination of Earth equator to ecliptic (rad)
        final double eps = 0.4090926006 - 0.0002270711 * t;
        // Mean longitude of the Moon's orbit perigee (rad)
        final double gammaM = 1.4547885346 + 71.0176852437 * t - 0.0001801481 * t2;
        // Mean longitude of the ascending node of the Moon (rad)
        final double omegaM = 2.1824391966 - 33.7570459536 * t + 0.0000362262 * t2;
        // Mean anomaly of the Moon (rad)
        final double qm = 2.3555557435 + 8328.6914257190 * t + 0.0001545547 * t2;

        // Commons parameters
        final SinCos scOm  = FastMath.sinCos(omegaM);
        final SinCos scIm  = FastMath.sinCos(im);
        final SinCos scEs  = FastMath.sinCos(eps);
        final SinCos scGm  = FastMath.sinCos(gammaM);
        final double cosOm = scOm.cos();
        final double sinOm = scOm.sin();
        final double cosIm = scIm.cos();
        final double sinIm = scIm.sin();
        final double cosEs = scEs.cos();
        final double sinEs = scEs.sin();
        final double cosGm = scGm.cos();
        final double sinGm = scGm.sin();

        // Intermediate parameters
        final double psiStar = cosOm * sinIm;
        final double etaStar = sinOm * sinIm;
        final double epsStar = 1. - cosOm * cosOm * (1. - cosIm);
        final double eps11 = sinOm * cosOm * (1. - cosIm);
        final double eps12 = 1. - sinOm * sinOm * (1. - cosIm);
        final double eta11 = epsStar * cosEs - psiStar * sinEs;
        final double eta12 = eps11 * cosEs + etaStar * sinEs;
        final double psi11 = epsStar * sinEs + psiStar * cosEs;
        final double psi12 = eps11 * sinEs - etaStar * cosEs;

        // Eccentric Anomaly
        final double ek = getEccentricAnomaly(qm, em);

        // True Anomaly
        final double vk    = getTrueAnomaly(ek, em);
        final SinCos scVk  = FastMath.sinCos(vk);
        final double sinVk = scVk.sin();
        final double cosVk = scVk.cos();

        // Direction cosine
        final double epsM = eps11 * (sinVk * cosGm + cosVk * sinGm) + eps12 * (cosVk * cosGm - sinVk * sinGm);
        final double etaM = eta11 * (sinVk * cosGm + cosVk * sinGm) + eta12 * (cosVk * cosGm - sinVk * sinGm);
        final double psiM = psi11 * (sinVk * cosGm + cosVk * sinGm) + psi12 * (cosVk * cosGm - sinVk * sinGm);

        // Distance
        final double rm = am * (1. - em * FastMath.cos(ek));

        moonElements[0] = epsM;
        moonElements[1] = etaM;
        moonElements[2] = psiM;
        moonElements[3] = rm;

    }

    /**
     * This method computes the direction cosines and the distance used to
     * compute the gravitational perturbations of the Sun.
     *
     * @param date the computation date in GLONASS scale
     */
    private void computeSunElements(final GLONASSDate date) {

        sunElements = new double[4];

        // Constants
        //  Major semi-axis of the Earth’s orbit around the Sun (m)
        final double as = 1.49598e11;
        // The eccentricity of the Earth’s orbit around the Sun
        final double es = 0.016719;

        // Computed parameters
        // Time from epoch 2000 to the instant tb of GLONASS ephemeris broadcast
        final double dtoJD = (glonassOrbit.getTime() - 10800.) / Constants.JULIAN_DAY;
        final double t  = (date.getJD0() + dtoJD - 2451545.0) / 36525;
        final double t2 = t * t;
        // Mean inclination of Earth equator to ecliptic (rad)
        final double eps = 0.4090926006 - 0.0002270711 * t;
        // Mean tropic longitude of the Sun orbit perigee (rad)
        final double ws = -7.6281824375 + 0.0300101976 * t + 0.0000079741 * t2;
        // Mean anomaly of the Sun (rad)
        final double qs = 6.2400601269 + 628.3019551714 * t - 0.0000026820 * t2;

        // Eccentric Anomaly
        final double ek = getEccentricAnomaly(qs, es);

        // True Anomaly
        final double vk    =  getTrueAnomaly(ek, es);
        final SinCos scVk  = FastMath.sinCos(vk);
        final double sinVk = scVk.sin();
        final double cosVk = scVk.cos();

        // Commons parameters
        final SinCos scWs  = FastMath.sinCos(ws);
        final SinCos scEs  = FastMath.sinCos(eps);
        final double cosWs = scWs.cos();
        final double sinWs = scWs.sin();
        final double cosEs = scEs.cos();
        final double sinEs = scEs.sin();

        // Direction cosine
        final double epsS = cosVk * cosWs - sinVk * sinWs;
        final double etaS = cosEs * (sinVk * cosWs + cosVk * sinWs);
        final double psiS = sinEs * (sinVk * cosWs + cosVk * sinWs);

        // Distance
        final double rs = as * (1. - es * FastMath.cos(ek));

        sunElements[0] = epsS;
        sunElements[1] = etaS;
        sunElements[2] = psiS;
        sunElements[3] = rs;

    }

    /**
     * Computes the elliptic eccentric anomaly from the mean anomaly.
     * <p>
     * The algorithm used here for solving Kepler equation has been published
     * in: "Procedures for  solving Kepler's Equation", A. W. Odell and
     * R. H. Gooding, Celestial Mechanics 38 (1986) 307-334
     * </p>
     * <p>It has been copied from the OREKIT library (KeplerianOrbit class).</p>
     *
     * @param M mean anomaly (rad)
     * @param e eccentricity
     * @return E the eccentric anomaly
     */
    private double getEccentricAnomaly(final double M, final double e) {

        // reduce M to [-PI PI) interval
        final double reducedM = MathUtils.normalizeAngle(M, 0.0);

        // compute start value according to A. W. Odell and R. H. Gooding S12 starter
        double E;
        if (FastMath.abs(reducedM) < 1.0 / 6.0) {
            E = reducedM + e * (FastMath.cbrt(6 * reducedM) - reducedM);
        } else {
            if (reducedM < 0) {
                final double w = FastMath.PI + reducedM;
                E = reducedM + e * (A * w / (B - w) - FastMath.PI - reducedM);
            } else {
                final double w = FastMath.PI - reducedM;
                E = reducedM + e * (FastMath.PI - A * w / (B - w) - reducedM);
            }
        }

        final double e1 = 1 - e;
        final boolean noCancellationRisk = (e1 + E * E / 6) >= 0.1;

        // perform two iterations, each consisting of one Halley step and one Newton-Raphson step
        for (int j = 0; j < 2; ++j) {
            final double f;
            double fd;
            final SinCos scE  = FastMath.sinCos(E);
            final double fdd  = e * scE.sin();
            final double fddd = e * scE.cos();
            if (noCancellationRisk) {
                f  = (E - fdd) - reducedM;
                fd = 1 - fddd;
            } else {
                f  = eMeSinE(E, e) - reducedM;
                final double s = FastMath.sin(0.5 * E);
                fd = e1 + 2 * e * s * s;
            }
            final double dee = f * fd / (0.5 * f * fdd - fd * fd);

            // update eccentric anomaly, using expressions that limit underflow problems
            final double w = fd + 0.5 * dee * (fdd + dee * fddd / 3);
            fd += dee * (fdd + 0.5 * dee * fddd);
            E  -= (f - dee * (fd - w)) / fd;

        }

        // expand the result back to original range
        E += M - reducedM;

        return E;

    }

    /**
     * Accurate computation of E - e sin(E).
     *
     * @param E eccentric anomaly
     * @param e eccentricity
     * @return E - e sin(E)
     */
    private static double eMeSinE(final double E, final double e) {
        double x = (1 - e) * FastMath.sin(E);
        final double mE2 = -E * E;
        double term = E;
        double d    = 0;
        // the inequality test below IS intentional and should NOT be replaced by a check with a small tolerance
        for (double x0 = Double.NaN; !Double.valueOf(x).equals(Double.valueOf(x0));) {
            d += 2;
            term *= mE2 / (d * (d + 1));
            x0 = x;
            x = x - term;
        }
        return x;
    }

    /**
     * Get true anomaly from eccentric anomaly and eccentricity.
     *
     * @param ek the eccentric anomaly (rad)
     * @param ecc the eccentricity
     * @return the true anomaly (rad)
     */
    private double getTrueAnomaly(final double ek, final double ecc) {
        final SinCos scek = FastMath.sinCos(ek);
        final double svk  = FastMath.sqrt(1. - ecc * ecc) * scek.sin();
        final double cvk  = scek.cos() - ecc;
        return FastMath.atan2(svk, cvk);
    }

    /**
     * This method transforms the PV coordinates obtained after the numerical
     * integration in the ECEF PZ-90.
     *
     * @param state spacecraft state after integration
     * @return the PV coordinates in the ECEF PZ-90.
     */
    private PVCoordinates getPVInPZ90(final SpacecraftState state) {

        // Compute time difference between start date and end date
        final double dt = state.getDate().durationFrom(initDate.getDate());

        // Position and velocity vectors
        final PVCoordinates pv = state.getPVCoordinates();
        final Vector3D pos = pv.getPosition();
        final Vector3D vel = pv.getVelocity();

        // Components of position and velocity vectors
        final double x0 = pos.getX();
        final double y0 = pos.getY();
        final double z0 = pos.getZ();
        final double vx0 = vel.getX();
        final double vy0 = vel.getY();
        final double vz0 = vel.getZ();

        // Greenwich Mean Sidereal Time (GMST)
        final GLONASSDate gloDate = new GLONASSDate(
                state.getDate(),
                dataContext.getTimeScales().getGLONASS());
        final double gmst = gloDate.getGMST();

        final double ti = glonassOrbit.getTime() + dt;
        // We use the GMST instead of the GMT as it is recommended into GLONASS ICD (2016)
        final double s = gmst + GLONASS_AV * (ti - 10800.);

        // Commons Parameters
        final SinCos scS  = FastMath.sinCos(s);
        final double cosS = scS.cos();
        final double sinS = scS.sin();

        // Transformed coordinates
        final double x = x0 * cosS + y0 * sinS;
        final double y = -x0 * sinS + y0 * cosS;
        final double z = z0;
        final double vx = vx0 * cosS + vy0 * sinS + GLONASS_AV * y;
        final double vy = -vx0 * sinS + vy0 * cosS - GLONASS_AV * x;
        final double vz = vz0;

        // Transformed orbit
        return new PVCoordinates(new Vector3D(x, y, z),
                                 new Vector3D(vx, vy, vz));
    }

    /**
     * This method computes the PV coordinates of the spacecraft center of mass.
     * The returned PV are expressed in inertial coordinates system at the instant tb.
     *
     * @param date the computation date in GLONASS scale
     * @return the PV Coordinates in inertial coordinates system
     */
    private PVCoordinates getPVInInertial(final GLONASSDate date) {

        // Greenwich Mean Sidereal Time (GMST)
        final double gmst = date.getGMST();

        final double time = glonassOrbit.getTime();
        final double dt   = time - 10800.;
        // We use the GMST instead of the GMT as it is recommended into GLONASS ICD (2016)
        final double s = gmst + GLONASS_AV * dt;

        // Commons Parameters
        final SinCos scS  = FastMath.sinCos(s);
        final double cosS = scS.cos();
        final double sinS = scS.sin();

        // PV coordinates in inertial frame
        final double x0  = glonassOrbit.getX() * cosS - glonassOrbit.getY() * sinS;
        final double y0  = glonassOrbit.getX() * sinS + glonassOrbit.getY() * cosS;
        final double z0  = glonassOrbit.getZ();
        final double vx0 = glonassOrbit.getXDot() * cosS - glonassOrbit.getYDot() * sinS - GLONASS_AV * y0;
        final double vy0 = glonassOrbit.getXDot() * sinS + glonassOrbit.getYDot() * cosS + GLONASS_AV * x0;
        final double vz0 = glonassOrbit.getZDot();
        return new PVCoordinates(new Vector3D(x0, y0, z0),
                                 new Vector3D(vx0, vy0, vz0));
    }

    @Override
    protected StateMapper createMapper(final AbsoluteDate referenceDate, final double mu,
                                       final OrbitType orbitType, final PositionAngleType positionAngleType,
                                       final AttitudeProvider attitudeProvider, final Frame frame) {
        return new Mapper(referenceDate, mu, orbitType, positionAngleType, attitudeProvider, frame);
    }

    /** Internal mapper. */
    private static class Mapper extends StateMapper {

        /**
         * Simple constructor.
         *
         * @param referenceDate reference date
         * @param mu central attraction coefficient (m³/s²)
         * @param orbitType orbit type to use for mapping
         * @param positionAngleType angle type to use for propagation
         * @param attitudeProvider attitude provider
         * @param frame inertial frame
         */
        Mapper(final AbsoluteDate referenceDate, final double mu,
               final OrbitType orbitType, final PositionAngleType positionAngleType,
               final AttitudeProvider attitudeProvider, final Frame frame) {
            super(referenceDate, mu, orbitType, positionAngleType, attitudeProvider, frame);
        }

        @Override
        public SpacecraftState mapArrayToState(final AbsoluteDate date, final double[] y,
                                               final double[] yDot, final PropagationType type) {
            // The parameter meanOnly is ignored for the GLONASS Propagator
            final double mass = y[6];
            if (mass <= 0.0) {
                throw new OrekitException(OrekitMessages.NOT_POSITIVE_SPACECRAFT_MASS, mass);
            }

            final Orbit orbit       = getOrbitType().mapArrayToOrbit(y, yDot, getPositionAngleType(), date, getMu(), getFrame());
            final Attitude attitude = getAttitudeProvider().getAttitude(orbit, date, getFrame());

            return new SpacecraftState(orbit, attitude, mass);
        }

        @Override
        public void mapStateToArray(final SpacecraftState state, final double[] y,
                                    final double[] yDot) {
            getOrbitType().mapOrbitToArray(state.getOrbit(), getPositionAngleType(), y, yDot);
            y[6] = state.getMass();
        }

    }

    @Override
    protected MainStateEquations getMainStateEquations(final ODEIntegrator integ) {
        return new Main();
    }

    /** Internal class for orbital parameters integration. */
    private class Main implements MainStateEquations {

        /** Derivatives array. */
        private final double[] yDot;

        /**
         * Simple constructor.
         */
        Main() {
            yDot = new double[7];
        }

        @Override
        public double[] computeDerivatives(final SpacecraftState state) {

            // Date in Glonass form
            final GLONASSDate gloDate = new GLONASSDate(
                    state.getDate(),
                    dataContext.getTimeScales().getGLONASS());

            // Position and Velocity vectors
            final Vector3D vel = state.getPVCoordinates().getVelocity();
            final Vector3D pos = state.getPosition();

            Arrays.fill(yDot, 0.0);

            // dPos/dt = Vel
            yDot[0] += vel.getX();
            yDot[1] += vel.getY();
            yDot[2] += vel.getZ();

            // Components of position and velocity vectors
            final double x0 = pos.getX();
            final double y0 = pos.getY();
            final double z0 = pos.getZ();

            // Normalized values
            final double r  = pos.getNorm();
            final double r2 = r * r;
            final double oor = 1. / r;
            final double oor2 = 1. / r2;
            final double x = x0 * oor;
            final double y = y0 * oor;
            final double z = z0 * oor;
            final double g = GNSSConstants.GLONASS_MU * oor2;
            final double ro = GLONASS_EARTH_EQUATORIAL_RADIUS * oor;

            yDot[3] += x * (-g + (-1.5 * GLONASS_J20 * g * ro * ro * (1. - 5. * z * z)));
            yDot[4] += y * (-g + (-1.5 * GLONASS_J20 * g * ro * ro * (1. - 5. * z * z)));
            yDot[5] += z * (-g + (-1.5 * GLONASS_J20 * g * ro * ro * (3. - 5. * z * z)));

            // Luni-Solar contribution
            final Vector3D acc;
            if (isAccAvailable) {
                acc = getLuniSolarPerturbations(gloDate);
            } else {
                final Vector3D accMoon = computeLuniSolarPerturbations(
                        state, moonElements[0], moonElements[1], moonElements[2],
                        moonElements[3],
                        dataContext.getCelestialBodies().getMoon().getGM());
                final Vector3D accSun = computeLuniSolarPerturbations(
                        state,
                        sunElements[0], sunElements[1], sunElements[2],
                        sunElements[3],
                        dataContext.getCelestialBodies().getSun().getGM());
                acc = accMoon.add(accSun);
            }

            yDot[3] += acc.getX();
            yDot[4] += acc.getY();
            yDot[5] += acc.getZ();

            return yDot.clone();
        }

        /**
         * This method computes the accelerations induced by gravitational
         * perturbations of the Sun and the Moon if they are not available into
         * the navigation message data.
         *
         * @param state current state
         * @param eps first direction cosine
         * @param eta second direction cosine
         * @param psi third direction cosine
         * @param r distance of perturbing body
         * @param g body gravitational field constant
         * @return a vector containing the accelerations
         */
        private Vector3D computeLuniSolarPerturbations(final SpacecraftState state, final double eps,
                                                       final double eta, final double psi,
                                                       final double r, final double g) {

            // Current pv coordinates
            final PVCoordinates pv = state.getPVCoordinates();

            final double oor = 1. / r;
            final double oor2 = oor * oor;

            // Normalized variable
            final double x = pv.getPosition().getX() * oor;
            final double y = pv.getPosition().getY() * oor;
            final double z = pv.getPosition().getZ() * oor;
            final double gm = g * oor2;

            final double epsmX  = eps - x;
            final double etamY  = eta - y;
            final double psimZ  = psi - z;
            final Vector3D vector = new Vector3D(epsmX, etamY, psimZ);
            final double d2 = vector.getNormSq();
            final double deltaM = FastMath.sqrt(d2) * d2;

            // Accelerations
            final double accX = gm * ((epsmX / deltaM) - eps);
            final double accY = gm * ((etamY / deltaM) - eta);
            final double accZ = gm * ((psimZ / deltaM) - psi);

            return new Vector3D(accX, accY, accZ);
        }

        /**
         * Get the accelerations induced by gravitational
         * perturbations of the Sun and the Moon in a geocentric
         * coordinate system.
         * <p>
         * The accelerations are obtained using projections of accelerations
         * transmitted within navigation message data.
         * </p>
         *
         * @param date the computation date in GLONASS scale
         * @return a vector containing the sum of both accelerations
         */
        private Vector3D getLuniSolarPerturbations(final GLONASSDate date) {

            // Greenwich Mean Sidereal Time (GMST)
            final double gmst = date.getGMST();

            final double time = glonassOrbit.getTime();
            final double dt   = time - 10800.;
            // We use the GMST instead of the GMT as it is recommended into GLONASS ICD (see Ref)
            final double s = gmst + GLONASS_AV * dt;

            // Commons Parameters
            final SinCos scS  = FastMath.sinCos(s);
            final double cosS = scS.cos();
            final double sinS = scS.sin();

            // Accelerations
            final double accX = glonassOrbit.getXDotDot() * cosS - glonassOrbit.getYDotDot() * sinS;
            final double accY = glonassOrbit.getXDotDot() * sinS + glonassOrbit.getYDotDot() * cosS;
            final double accZ = glonassOrbit.getZDotDot();

            return new Vector3D(accX, accY, accZ);
        }

    }

}
