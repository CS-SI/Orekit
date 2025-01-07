/* Copyright 2002-2025 CS GROUP
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
package org.orekit.propagation.analytical.gnss;

import org.hipparchus.Field;
import org.hipparchus.analysis.differentiation.Gradient;
import org.hipparchus.analysis.differentiation.GradientField;
import org.hipparchus.analysis.differentiation.UnivariateDerivative2;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.linear.MatrixUtils;
import org.hipparchus.linear.QRDecomposition;
import org.hipparchus.linear.RealMatrix;
import org.hipparchus.linear.RealVector;
import org.hipparchus.util.FastMath;
import org.hipparchus.util.FieldSinCos;
import org.hipparchus.util.SinCos;
import org.orekit.attitudes.Attitude;
import org.orekit.attitudes.AttitudeProvider;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.frames.Frame;
import org.orekit.orbits.CartesianOrbit;
import org.orekit.orbits.FieldKeplerianAnomalyUtility;
import org.orekit.orbits.KeplerianAnomalyUtility;
import org.orekit.orbits.KeplerianOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.PositionAngleType;
import org.orekit.propagation.AbstractMatricesHarvester;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.analytical.AbstractAnalyticalPropagator;
import org.orekit.propagation.analytical.gnss.data.FieldGnssOrbitalElements;
import org.orekit.propagation.analytical.gnss.data.GNSSOrbitalElements;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.DoubleArrayDictionary;
import org.orekit.utils.FieldPVCoordinates;
import org.orekit.utils.PVCoordinates;
import org.orekit.utils.ParameterDriver;
import org.orekit.utils.TimeSpanMap.Span;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Common handling of {@link AbstractAnalyticalPropagator} methods for GNSS propagators.
 * <p>
 * This class allows to provide easily a subset of {@link AbstractAnalyticalPropagator} methods
 * for specific GNSS propagators.
 * </p>
 * @author Pascal Parraud
 */
public class GNSSPropagator extends AbstractAnalyticalPropagator {

    /** Maximum number of iterations for internal loops.
     * @since 13.0
     */
    private static final int MAX_ITER = 100;

    /** Tolerance on position for rebuilding orbital elements from initial state.
     * @since 13.0
     */
    private static final double TOL_P = 1.0e-6;

    /** Tolerance on velocity for rebuilding orbital elements from initial state.
     * @since 13.0
     */
    private static final double TOL_V = 1.0e-9;

    /** Number of free parameters for orbital elements.
     * @since 13.0
     */
    private static final int FREE_PARAMETERS = 6;

    /** Convergence parameter.
     * @since 13.0
     */
    private static final double EPS = 1.0e-12;

    /** The GNSS propagation model used. */
    private final GNSSOrbitalElements<?> orbitalElements;

    /** The spacecraft mass (kg). */
    private final double mass;

    /** The ECI frame used for GNSS propagation. */
    private final Frame eci;

    /** The ECEF frame used for GNSS propagation. */
    private final Frame ecef;

    /**
     * Build a new instance.
     * @param orbitalElements GNSS orbital elements
     * @param eci Earth Centered Inertial frame
     * @param ecef Earth Centered Earth Fixed frame
     * @param provider attitude provider
     * @param mass satellite mass (kg)
     */
    GNSSPropagator(final GNSSOrbitalElements<?> orbitalElements, final Frame eci,
                   final Frame ecef, final AttitudeProvider provider, final double mass) {
        super(provider);
        // Stores the GNSS orbital elements
        this.orbitalElements = orbitalElements;
        // Sets the start date as the date of the orbital elements
        setStartDate(orbitalElements.getDate());
        // Sets the mass
        this.mass = mass;
        // Sets the Earth Centered Inertial frame
        this.eci  = eci;
        // Sets the Earth Centered Earth Fixed frame
        this.ecef = ecef;
        // Sets initial state
        final Orbit orbit = propagateOrbit(getStartDate());
        final Attitude attitude = provider.getAttitude(orbit, orbit.getDate(), orbit.getFrame());

        // calling the method from base class because the one overridden below intentionally throws an exception
        super.resetInitialState(new SpacecraftState(orbit, attitude, mass));

    }

    /**
     * Build a new instance from an initial state.
     * <p>
     * The Keplerian elements already present in the {@code nonKeplerianElements} argument
     * will be ignored as it is the {@code initialState} argument that will be used to
     * build the complete {@link #getOrbitalElements() orbital elements} of the propagator
     * </p>
     * @param initialState         initial state
     * @param nonKeplerianElements non-Keplerian orbital elements (the Keplerian orbital elements will be ignored)
     * @param ecef                 Earth Centered Earth Fixed frame
     * @param provider             attitude provider
     * @param mass                 spacecraft mass
     * @since 13.0
     */
    GNSSPropagator(final SpacecraftState initialState, final GNSSOrbitalElements<?> nonKeplerianElements,
                   final Frame ecef, final AttitudeProvider provider, final double mass) {
        this(buildOrbitalElements(initialState, nonKeplerianElements, ecef, provider, mass),
             initialState.getFrame(), ecef, provider, initialState.getMass());
    }

    /**
     * Gets the Earth Centered Inertial frame used to propagate the orbit.
     *
     * @return the ECI frame
     */
    public Frame getECI() {
        return eci;
    }

    /**
     * Gets the Earth Centered Earth Fixed frame used to propagate GNSS orbits according to the
     * Interface Control Document.
     *
     * @return the ECEF frame
     */
    public Frame getECEF() {
        return ecef;
    }

    /**
     * Gets the Earth gravity coefficient used for GNSS propagation.
     *
     * @return the Earth gravity coefficient.
     */
    public double getMU() {
        return orbitalElements.getMu();
    }

    /** Get the underlying GNSS propagation orbital elements.
     * @return the underlying GNSS orbital elements
     * @since 13.0
     */
    public GNSSOrbitalElements<?> getOrbitalElements() {
        return orbitalElements;
    }

    /** {@inheritDoc}
     * @since 13.0
     */
    @Override
    protected AbstractMatricesHarvester createHarvester(final String stmName, final RealMatrix initialStm,
                                                        final DoubleArrayDictionary initialJacobianColumns) {
        // Create the harvester
        final GnssHarvester harvester = new GnssHarvester(this, stmName, initialStm, initialJacobianColumns);

        // Update the list of additional state provider
        addAdditionalStateProvider(harvester);
        // Return the configured harvester
        return harvester;
     }

    /** {@inheritDoc}
     * @since 13.0
     */
    @Override
    protected List<String> getJacobiansColumnsNames() {
        final List<String> columnsNames = new ArrayList<>();
        for (final ParameterDriver driver : orbitalElements.getParametersDrivers()) {
            if (driver.isSelected() && !columnsNames.contains(driver.getNamesSpanMap().getFirstSpan().getData())) {
                // As driver with same name should have same NamesSpanMap we only check if the first span is present,
                // if not we add all span names to columnsNames
                for (Span<String> span = driver.getNamesSpanMap().getFirstSpan(); span != null; span = span.next()) {
                    columnsNames.add(span.getData());
                }
            }
        }
        Collections.sort(columnsNames);
        return columnsNames;
    }

    /** {@inheritDoc} */
    @Override
    protected Orbit propagateOrbit(final AbsoluteDate date) {
        // Gets the PVCoordinates in ECEF frame
        final PVCoordinates pvaInECEF = propagateInEcef(date);
        // Transforms the PVCoordinates to ECI frame
        final PVCoordinates pvaInECI = ecef.getTransformTo(eci, date).transformPVCoordinates(pvaInECEF);
        // Returns the Cartesian orbit
        return new CartesianOrbit(pvaInECI, eci, date, getMU());
    }

    /**
     * Gets the PVCoordinates of the GNSS SV in {@link #getECEF() ECEF frame}.
     *
     * <p>The algorithm uses automatic differentiation to compute velocity and
     * acceleration.</p>
     *
     * @param date the computation date
     * @return the GNSS SV PVCoordinates in {@link #getECEF() ECEF frame}
     */
    public PVCoordinates propagateInEcef(final AbsoluteDate date) {
        // Duration from GNSS ephemeris Reference date
        final UnivariateDerivative2 tk = new UnivariateDerivative2(getTk(date), 1.0, 0.0);
        // Mean anomaly
        final UnivariateDerivative2 mk = tk.multiply(orbitalElements.getMeanMotion()).add(orbitalElements.getM0());
        // Eccentric Anomaly
        final UnivariateDerivative2 e  = tk.newInstance(orbitalElements.getE());
        final UnivariateDerivative2 ek = FieldKeplerianAnomalyUtility.ellipticMeanToEccentric(e, mk);
        // True Anomaly
        final UnivariateDerivative2 vk =  FieldKeplerianAnomalyUtility.ellipticEccentricToTrue(e, ek);
        // Argument of Latitude
        final UnivariateDerivative2 phik    = vk.add(orbitalElements.getPa());
        final FieldSinCos<UnivariateDerivative2> cs2phi = FastMath.sinCos(phik.multiply(2));
        // Argument of Latitude Correction
        final UnivariateDerivative2 dphik = cs2phi.cos().multiply(orbitalElements.getCuc()).add(cs2phi.sin().multiply(orbitalElements.getCus()));
        // Radius Correction
        final UnivariateDerivative2 drk = cs2phi.cos().multiply(orbitalElements.getCrc()).add(cs2phi.sin().multiply(orbitalElements.getCrs()));
        // Inclination Correction
        final UnivariateDerivative2 dik = cs2phi.cos().multiply(orbitalElements.getCic()).add(cs2phi.sin().multiply(orbitalElements.getCis()));
        // Corrected Argument of Latitude
        final FieldSinCos<UnivariateDerivative2> csuk = FastMath.sinCos(phik.add(dphik));
        // Corrected Radius
        final UnivariateDerivative2 rk = ek.cos().multiply(-orbitalElements.getE()).add(1).multiply(orbitalElements.getSma()).add(drk);
        // Corrected Inclination
        final UnivariateDerivative2 ik  = tk.multiply(orbitalElements.getIDot()).add(orbitalElements.getI0()).add(dik);
        final FieldSinCos<UnivariateDerivative2> csik = FastMath.sinCos(ik);
        // Positions in orbital plane
        final UnivariateDerivative2 xk = csuk.cos().multiply(rk);
        final UnivariateDerivative2 yk = csuk.sin().multiply(rk);
        // Corrected longitude of ascending node
        final double thetaDot = orbitalElements.getAngularVelocity();
        final FieldSinCos<UnivariateDerivative2> csomk =
            FastMath.sinCos(tk.multiply(orbitalElements.getOmegaDot() - thetaDot).
                            add(orbitalElements.getOmega0() - thetaDot * orbitalElements.getTime()));
        // returns the Earth-fixed coordinates
        final FieldVector3D<UnivariateDerivative2> positionWithDerivatives =
                        new FieldVector3D<>(xk.multiply(csomk.cos()).subtract(yk.multiply(csomk.sin()).multiply(csik.cos())),
                                            xk.multiply(csomk.sin()).add(yk.multiply(csomk.cos()).multiply(csik.cos())),
                                            yk.multiply(csik.sin()));
        return new PVCoordinates(positionWithDerivatives);
    }

    /**
     * Gets the duration from GNSS Reference epoch.
     * <p>This takes the GNSS week roll-over into account.</p>
     * @param date the considered date
     * @return the duration from GNSS orbit Reference epoch (s)
     */
    private double getTk(final AbsoluteDate date) {
        final double cycleDuration = orbitalElements.getCycleDuration();
        // Time from ephemeris reference epoch
        double tk = date.durationFrom(orbitalElements.getDate());
        // Adjusts the time to take roll over week into account
        while (tk > 0.5 * cycleDuration) {
            tk -= cycleDuration;
        }
        while (tk < -0.5 * cycleDuration) {
            tk += cycleDuration;
        }
        // Returns the time from ephemeris reference epoch
        return tk;
    }

    /** {@inheritDoc} */
    @Override
    public Frame getFrame() {
        return eci;
    }

    /** {@inheritDoc} */
    @Override
    protected double getMass(final AbsoluteDate date) {
        return mass;
    }

    /** {@inheritDoc} */
    @Override
    public void resetInitialState(final SpacecraftState state) {
        throw new OrekitException(OrekitMessages.NON_RESETABLE_STATE);
    }

    /** {@inheritDoc} */
    @Override
    protected void resetIntermediateState(final SpacecraftState state, final boolean forward) {
        throw new OrekitException(OrekitMessages.NON_RESETABLE_STATE);
    }

    /**
     * Build orbital elements from initial state.
     * <p>
     * This method is roughly the inverse of {@link #propagateInEcef(AbsoluteDate)}, except it starts from a state in
     * inertial frame
     * </p>
     *
     * @param initialState    initial state
     * @param nonKeplerianElements non-Keplerian orbital elements (the Keplerian orbital elements will be ignored)
     * @param ecef            Earth Centered Earth Fixed frame
     * @param provider        attitude provider
     * @param mass            satellite mass (kg)
     * @return orbital elements that generate the {@code initialState} when used with a propagator
     * @since 13.0
     */
    private static GNSSOrbitalElements<?> buildOrbitalElements(final SpacecraftState initialState,
                                                               final GNSSOrbitalElements<?> nonKeplerianElements,
                                                               final Frame ecef, final AttitudeProvider provider,
                                                               final double mass) {

        // build the elements with immutable parts
        final GNSSOrbitalElements<?> elements = nonKeplerianElements.copyNonKeplerian();

        // get approximate initial orbit
        final Frame frozenEcef = ecef.getFrozenFrame(initialState.getFrame(), initialState.getDate(), "frozen");
        KeplerianOrbit orbit = approximateInitialOrbit(initialState, nonKeplerianElements, frozenEcef);

        // refine orbit using simple differential correction to reach target PV
        final PVCoordinates targetPV = initialState.getPVCoordinates();
        for (int i = 0; i < MAX_ITER; ++i) {

            // get position-velocity derivatives with respect to initial orbit
            final FieldGnssOrbitalElements<Gradient, ?> gElements = convert(elements, orbit);
            final FieldGnssPropagator<Gradient> gPropagator =
                new FieldGnssPropagator<>(gElements, initialState.getFrame(), ecef, provider,
                                          gElements.getMu().newInstance(mass));
            final FieldPVCoordinates<Gradient> gPV = gPropagator.getInitialState().getPVCoordinates();

            // compute Jacobian matrix
            final RealMatrix jacobian = MatrixUtils.createRealMatrix(FREE_PARAMETERS, FREE_PARAMETERS);
            jacobian.setRow(0, gPV.getPosition().getX().getGradient());
            jacobian.setRow(1, gPV.getPosition().getY().getGradient());
            jacobian.setRow(2, gPV.getPosition().getZ().getGradient());
            jacobian.setRow(3, gPV.getVelocity().getX().getGradient());
            jacobian.setRow(4, gPV.getVelocity().getY().getGradient());
            jacobian.setRow(5, gPV.getVelocity().getZ().getGradient());

            // linear correction to get closer to target PV
            final RealVector residuals = MatrixUtils.createRealVector(FREE_PARAMETERS);
            residuals.setEntry(0, targetPV.getPosition().getX() - gPV.getPosition().getX().getValue());
            residuals.setEntry(1, targetPV.getPosition().getY() - gPV.getPosition().getY().getValue());
            residuals.setEntry(2, targetPV.getPosition().getZ() - gPV.getPosition().getZ().getValue());
            residuals.setEntry(3, targetPV.getVelocity().getX() - gPV.getVelocity().getX().getValue());
            residuals.setEntry(4, targetPV.getVelocity().getY() - gPV.getVelocity().getY().getValue());
            residuals.setEntry(5, targetPV.getVelocity().getZ() - gPV.getVelocity().getZ().getValue());
            final RealVector correction = new QRDecomposition(jacobian, EPS).getSolver().solve(residuals);

            // update initial orbit
            orbit = new KeplerianOrbit(orbit.getA()                             + correction.getEntry(0),
                                       orbit.getE()                             + correction.getEntry(1),
                                       orbit.getI()                             + correction.getEntry(2),
                                       orbit.getPerigeeArgument()               + correction.getEntry(3),
                                       orbit.getRightAscensionOfAscendingNode() + correction.getEntry(4),
                                       orbit.getMeanAnomaly()                   + correction.getEntry(5),
                                       PositionAngleType.MEAN, PositionAngleType.MEAN,
                                       frozenEcef, initialState.getDate(), initialState.getMu());

            final double deltaP = FastMath.sqrt(residuals.getEntry(0) * residuals.getEntry(0) +
                                                residuals.getEntry(1) * residuals.getEntry(1) +
                                                residuals.getEntry(2) * residuals.getEntry(2));
            final double deltaV = FastMath.sqrt(residuals.getEntry(3) * residuals.getEntry(3) +
                                                residuals.getEntry(4) * residuals.getEntry(4) +
                                                residuals.getEntry(5) * residuals.getEntry(5));

            if (deltaP < TOL_P && deltaV < TOL_V) {
                break;
            }

        }

        // store the orbital elements
        elements.setSma(orbit.getA());
        elements.setE(orbit.getE());
        elements.setI0(orbit.getI());
        elements.setPa(orbit.getPerigeeArgument());
        elements.setOmega0(orbit.getRightAscensionOfAscendingNode());
        elements.setM0(orbit.getMeanAnomaly());

        return elements;

    }

    /** Compute approximate initial orbit.
     * @param initialState         initial state
     * @param nonKeplerianElements non-Keplerian orbital elements (the Keplerian orbital elements will be ignored)
     * @param frozenEcef           inertial frame aligned with Earth Centered Earth Fixed frame at orbit date
     * @return approximate initial orbit that generate a state close to {@code initialState}
     * @since 13.0
     */
    private static KeplerianOrbit approximateInitialOrbit(final SpacecraftState initialState,
                                                          final GNSSOrbitalElements<?> nonKeplerianElements,
                                                          final Frame frozenEcef) {

        // rotate the state to a frame that is inertial but aligned with Earth frame,
        // as analytical model is expressed in Earth frame
        final PVCoordinates pv = initialState.getPVCoordinates(frozenEcef);
        final Vector3D      p  = pv.getPosition();
        final Vector3D      v  = pv.getVelocity();

        // compute Keplerian orbital parameters
        final double   rk  = p.getNorm();

        // compute orbital plane orientation
        final Vector3D normal = pv.getMomentum().normalize();
        final double   cosIk  = normal.getZ();
        final double   ik     = Vector3D.angle(normal, Vector3D.PLUS_K);

        // compute position in orbital plane
        final double   q   = FastMath.hypot(normal.getX(), normal.getY());
        final double   cos = -normal.getY() / q;
        final double   sin =  normal.getX() / q;
        final double   xk  =  p.getX() * cos + p.getY() * sin;
        final double   yk  = (p.getY() * cos - p.getX() * sin) / cosIk;

        // corrected latitude argument
        final double   uk  = FastMath.atan2(yk, xk);

        // recover latitude argument before correction, using a fixed-point method
        double phi = uk;
        for (int i = 0; i < MAX_ITER; ++i) {
            final double previous = phi;
            final SinCos cs2Phi = FastMath.sinCos(2 * phi);
            phi = uk - (cs2Phi.cos() * nonKeplerianElements.getCuc() + cs2Phi.sin() * nonKeplerianElements.getCus());
            if (FastMath.abs(phi - previous) <= EPS) {
                break;
            }
        }
        final SinCos cs2phi = FastMath.sinCos(2 * phi);

        // recover plane orientation before correction
        // here, we know that tk = 0 since our orbital elements will be at initial state date
        final double i0  = ik - (cs2phi.cos() * nonKeplerianElements.getCic() + cs2phi.sin() * nonKeplerianElements.getCis());
        final double om0 = FastMath.atan2(sin, cos) +
                           nonKeplerianElements.getAngularVelocity() * nonKeplerianElements.getTime();

        // recover eccentricity and anomaly
        final double rV2OMu           = rk * v.getNormSq() / initialState.getMu();
        final double sma              = rk / (2 - rV2OMu);
        final double eCosE            = rV2OMu - 1;
        final double eSinE            = Vector3D.dotProduct(p, v) / FastMath.sqrt(initialState.getMu() * sma);
        final double e                = FastMath.hypot(eCosE, eSinE);
        final double eccentricAnomaly = FastMath.atan2(eSinE, eCosE);
        final double aop              = phi - eccentricAnomaly;
        final double meanAnomaly      = KeplerianAnomalyUtility.ellipticEccentricToMean(e, eccentricAnomaly);

        return new KeplerianOrbit(sma, e, i0, aop, om0, meanAnomaly, PositionAngleType.MEAN,
                                  PositionAngleType.MEAN, frozenEcef,
                                  initialState.getDate(), initialState.getMu());

    }

    /** Convert orbital elements to gradient.
     * @param elements   primitive double elements
     * @param orbit      Keplerian orbit
     * @return converted elements, set up as gradient relative to Keplerian orbit
     * @since 13.0
     */
    private static FieldGnssOrbitalElements<Gradient, ?> convert(final GNSSOrbitalElements<?> elements,
                                                                 final KeplerianOrbit orbit) {

        final Field<Gradient> field = GradientField.getField(FREE_PARAMETERS);
        final FieldGnssOrbitalElements<Gradient, ?> gElements = elements.toField(field);

        // Keplerian parameters
        gElements.setSma(Gradient.variable(FREE_PARAMETERS, 0, orbit.getA()));
        gElements.setE(Gradient.variable(FREE_PARAMETERS, 1, orbit.getE()));
        gElements.setI0(Gradient.variable(FREE_PARAMETERS, 2, orbit.getI()));
        gElements.setPa(Gradient.variable(FREE_PARAMETERS, 3, orbit.getPerigeeArgument()));
        gElements.setOmega0(Gradient.variable(FREE_PARAMETERS, 4, orbit.getRightAscensionOfAscendingNode()));
        gElements.setM0(Gradient.variable(FREE_PARAMETERS, 5, orbit.getMeanAnomaly()));

        return gElements;
    }

}
