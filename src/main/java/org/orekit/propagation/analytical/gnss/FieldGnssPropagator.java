/* Copyright 2022-2025 Luc Maisonobe
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

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.Field;
import org.hipparchus.analysis.differentiation.FieldGradient;
import org.hipparchus.analysis.differentiation.FieldUnivariateDerivative2;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.linear.FieldMatrix;
import org.hipparchus.linear.FieldQRDecomposition;
import org.hipparchus.linear.FieldVector;
import org.hipparchus.linear.MatrixUtils;
import org.hipparchus.util.FastMath;
import org.hipparchus.util.FieldSinCos;
import org.hipparchus.util.MathArrays;
import org.orekit.attitudes.AttitudeProvider;
import org.orekit.attitudes.FieldAttitude;
import org.orekit.frames.Frame;
import org.orekit.orbits.FieldCartesianOrbit;
import org.orekit.orbits.FieldKeplerianAnomalyUtility;
import org.orekit.orbits.FieldKeplerianOrbit;
import org.orekit.orbits.FieldOrbit;
import org.orekit.orbits.PositionAngleType;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.analytical.FieldAbstractAnalyticalPropagator;
import org.orekit.propagation.analytical.gnss.data.FieldGnssOrbitalElements;
import org.orekit.propagation.analytical.gnss.data.GNSSOrbitalElements;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.utils.FieldPVCoordinates;
import org.orekit.utils.ParameterDriver;

import java.util.List;

/** Common handling of {@link FieldAbstractAnalyticalPropagator} methods for GNSS propagators.
 * <p>
 * This class allows to provide easily a subset of {@link FieldAbstractAnalyticalPropagator} methods
 * for specific GNSS propagators.
 * </p>
 * @author Pascal Parraud
 * @author Luc Maisonobe
 * @param <T> type of the field elements
 * @since 13.0
 */
public class FieldGnssPropagator<T extends CalculusFieldElement<T>> extends FieldAbstractAnalyticalPropagator<T> {

    /** Maximum number of iterations for internal loops. */
    private static final int MAX_ITER = 100;

    /** Tolerance on position for rebuilding orbital elements from initial state. */
    private static final double TOL_P = 1.0e-6;

    /** Tolerance on velocity for rebuilding orbital elements from initial state. */
    private static final double TOL_V = 1.0e-9;

    /** Number of free parameters for orbital elements. */
    private static final int FREE_PARAMETERS = 6;

    /** Convergence parameter. */
    private static final double EPS = 1.0e-12;

    /** The GNSS propagation model used. */
    private FieldGnssOrbitalElements<T, ?> orbitalElements;

    /** The ECI frame used for GNSS propagation. */
    private final Frame eci;

    /** The ECEF frame used for GNSS propagation. */
    private final Frame ecef;

    /**
     * Build a new instance.
     * @param orbitalElements GNSS orbital elements
     * @param eci Earth Centered Inertial frame
     * @param ecef Earth Centered Earth Fixed frame
     * @param provider Attitude provider
     * @param mass Satellite mass (kg)
     */
    FieldGnssPropagator(final FieldGnssOrbitalElements<T, ?> orbitalElements,
                        final Frame eci, final Frame ecef,
                        final AttitudeProvider provider, final T mass) {
        super(orbitalElements.getDate().getField(), provider);
        // Stores the GNSS orbital elements
        this.orbitalElements = orbitalElements;
       // Sets the Earth Centered Inertial frame
        this.eci  = eci;
        // Sets the Earth Centered Earth Fixed frame
        this.ecef = ecef;
        // Sets initial state
        final FieldOrbit<T> orbit = propagateOrbit(orbitalElements.getDate(), defaultParameters());
        final FieldAttitude<T> attitude = provider.getAttitude(orbit, orbit.getDate(), orbit.getFrame());

        // calling the method from base class because the one overridden below recomputes the orbital elements
        super.resetInitialState(new FieldSpacecraftState<>(orbit, attitude).withMass(mass));

    }

    /**
     * Build a new instance from an initial state.
     * <p>
     * The Keplerian elements already present in the {@code nonKeplerianElements} argument
     * will be ignored as it is the {@code initialState} argument that will be used to
     * build the complete orbital elements of the propagator
     * </p>
     * @param initialState         initial state
     * @param nonKeplerianElements non-Keplerian orbital elements (the Keplerian orbital elements will be ignored)
     * @param ecef                 Earth Centered Earth Fixed frame
     * @param provider             attitude provider
     * @param mass                 spacecraft mass
     */
    FieldGnssPropagator(final FieldSpacecraftState<T> initialState,
                        final FieldGnssOrbitalElements<T, ?> nonKeplerianElements,
                        final Frame ecef, final AttitudeProvider provider, final T mass) {
        this(buildOrbitalElements(initialState, nonKeplerianElements, ecef, provider, mass),
             initialState.getFrame(), ecef, provider, initialState.getMass());
    }

    /** Build default parameters.
     * @return default parameters
     */
    private T[] defaultParameters() {
        final T[] parameters = MathArrays.buildArray(orbitalElements.getDate().getField(), GNSSOrbitalElements.SIZE);
        parameters[GNSSOrbitalElements.TIME_INDEX]      = getMU().newInstance(orbitalElements.getTime());
        parameters[GNSSOrbitalElements.I_DOT_INDEX]     = getMU().newInstance(orbitalElements.getIDot());
        parameters[GNSSOrbitalElements.OMEGA_DOT_INDEX] = getMU().newInstance(orbitalElements.getOmegaDot());
        parameters[GNSSOrbitalElements.CUC_INDEX]       = getMU().newInstance(orbitalElements.getCuc());
        parameters[GNSSOrbitalElements.CUS_INDEX]       = getMU().newInstance(orbitalElements.getCus());
        parameters[GNSSOrbitalElements.CRC_INDEX]       = getMU().newInstance(orbitalElements.getCrc());
        parameters[GNSSOrbitalElements.CRS_INDEX]       = getMU().newInstance(orbitalElements.getCrs());
        parameters[GNSSOrbitalElements.CIC_INDEX]       = getMU().newInstance(orbitalElements.getCic());
        parameters[GNSSOrbitalElements.CIS_INDEX]       = getMU().newInstance(orbitalElements.getCis());
        return parameters;
    }

    /** {@inheritDoc} */
    @Override
    public List<ParameterDriver> getParametersDrivers() {
        return orbitalElements.getParametersDrivers();
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
    public T getMU() {
        return orbitalElements.getMu();
    }

    /** {@inheritDoc} */
    @Override
    public FieldOrbit<T> propagateOrbit(final FieldAbsoluteDate<T> date,
                                        final T[] parameters) {
        // Gets the PVCoordinates in ECEF frame
        final FieldPVCoordinates<T> pvaInECEF = propagateInEcef(date, parameters);
        // Transforms the PVCoordinates to ECI frame
        final FieldPVCoordinates<T> pvaInECI = ecef.getTransformTo(eci, date).transformPVCoordinates(pvaInECEF);
        // Returns the Cartesian orbit
        return new FieldCartesianOrbit<>(pvaInECI, eci, date, getMU());
    }

    /**
     * Gets the PVCoordinates of the GNSS SV in {@link #getECEF() ECEF frame}.
     *
     * <p>The algorithm uses automatic differentiation to compute velocity and
     * acceleration.</p>
     *
     * @param date the computation date
     * @param parameters propagation parameters
     * @return the GNSS SV PVCoordinates in {@link #getECEF() ECEF frame}
     */
    public FieldPVCoordinates<T> propagateInEcef(final FieldAbsoluteDate<T> date, final T[] parameters) {
        // Duration from GNSS ephemeris Reference date
        final FieldUnivariateDerivative2<T> tk = new FieldUnivariateDerivative2<>(getTk(date),
                                                                                  date.getField().getOne(),
                                                                                  date.getField().getZero());

        // Semi-major axis
        final FieldUnivariateDerivative2<T> ak = tk.multiply(orbitalElements.getADot()).add(orbitalElements.getSma());
        // Mean motion
        final FieldUnivariateDerivative2<T> nA = tk.multiply(orbitalElements.getDeltaN0Dot().multiply(0.5)).
                                                 add(orbitalElements.getDeltaN0()).
                                                 add(orbitalElements.getMeanMotion0());
        // Mean anomaly
        final FieldUnivariateDerivative2<T> mk = tk.multiply(nA).add(orbitalElements.getM0());
        // Eccentric Anomaly
        final FieldUnivariateDerivative2<T> e  = tk.newInstance(orbitalElements.getE());
        final FieldUnivariateDerivative2<T> ek = FieldKeplerianAnomalyUtility.ellipticMeanToEccentric(e, mk);
        // True Anomaly
        final FieldUnivariateDerivative2<T> vk = FieldKeplerianAnomalyUtility.ellipticEccentricToTrue(e, ek);
        // Argument of Latitude
        final FieldUnivariateDerivative2<T> phik    = vk.add(orbitalElements.getPa());
        final FieldSinCos<FieldUnivariateDerivative2<T>> cs2phi = FastMath.sinCos(phik.multiply(2));
        // Argument of Latitude Correction
        final FieldUnivariateDerivative2<T> dphik = cs2phi.cos().multiply(parameters[GNSSOrbitalElements.CUC_INDEX]).
                                                add(cs2phi.sin().multiply(parameters[GNSSOrbitalElements.CUS_INDEX]));
        // Radius Correction
        final FieldUnivariateDerivative2<T> drk = cs2phi.cos().multiply(parameters[GNSSOrbitalElements.CRC_INDEX]).
                                              add(cs2phi.sin().multiply(parameters[GNSSOrbitalElements.CRS_INDEX]));
        // Inclination Correction
        final FieldUnivariateDerivative2<T> dik = cs2phi.cos().multiply(parameters[GNSSOrbitalElements.CIC_INDEX]).
                                              add(cs2phi.sin().multiply(parameters[GNSSOrbitalElements.CIS_INDEX]));
        // Corrected Argument of Latitude
        final FieldSinCos<FieldUnivariateDerivative2<T>> csuk = FastMath.sinCos(phik.add(dphik));
        // Corrected Radius
        final FieldUnivariateDerivative2<T> rk = ek.cos().multiply(e.negate()).add(1).multiply(ak).add(drk);
        // Corrected Inclination
        final FieldUnivariateDerivative2<T> ik  = tk.multiply(parameters[GNSSOrbitalElements.I_DOT_INDEX]).
                                                  add(orbitalElements.getI0()).add(dik);
        final FieldSinCos<FieldUnivariateDerivative2<T>> csik = FastMath.sinCos(ik);
        // Positions in orbital plane
        final FieldUnivariateDerivative2<T> xk = csuk.cos().multiply(rk);
        final FieldUnivariateDerivative2<T> yk = csuk.sin().multiply(rk);
        // Corrected longitude of ascending node
        final FieldSinCos<FieldUnivariateDerivative2<T>> csomk =
            FastMath.sinCos(tk.multiply(parameters[GNSSOrbitalElements.OMEGA_DOT_INDEX].
                            subtract(orbitalElements.getAngularVelocity())).
                            add(orbitalElements.getOmega0()).
                            subtract(parameters[GNSSOrbitalElements.TIME_INDEX].multiply(orbitalElements.getAngularVelocity())));
        // returns the Earth-fixed coordinates
        final FieldVector3D<FieldUnivariateDerivative2<T>> positionWithDerivatives =
                        new FieldVector3D<>(xk.multiply(csomk.cos()).subtract(yk.multiply(csomk.sin()).multiply(csik.cos())),
                                            xk.multiply(csomk.sin()).add(yk.multiply(csomk.cos()).multiply(csik.cos())),
                                            yk.multiply(csik.sin()));
        return new FieldPVCoordinates<>(positionWithDerivatives);

    }

    /**
     * Gets the duration from GNSS Reference epoch.
     * <p>This takes the GNSS week roll-over into account.</p>
     * @param date the considered date
     * @return the duration from GNSS orbit Reference epoch (s)
     */
    private T getTk(final FieldAbsoluteDate<T> date) {
        // Time from ephemeris reference epoch
        T tk = date.durationFrom(orbitalElements.getDate());
        // Adjusts the time to take roll over week into account
        while (tk.getReal() > 0.5 * orbitalElements.getCycleDuration()) {
            tk = tk.subtract(orbitalElements.getCycleDuration());
        }
        while (tk.getReal() < -0.5 * orbitalElements.getCycleDuration()) {
            tk = tk.add(orbitalElements.getCycleDuration());
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
    protected T getMass(final FieldAbsoluteDate<T> date) {
        return getInitialState().getMass();
    }

    /** {@inheritDoc} */
    @Override
    public void resetInitialState(final FieldSpacecraftState<T> state) {
        orbitalElements = buildOrbitalElements(state, orbitalElements, ecef, getAttitudeProvider(), state.getMass());
        final FieldOrbit<T> orbit = propagateOrbit(orbitalElements.getDate(), defaultParameters());
        final FieldAttitude<T> attitude = getAttitudeProvider().getAttitude(orbit, orbit.getDate(), orbit.getFrame());
        super.resetInitialState(new FieldSpacecraftState<>(orbit, attitude).withMass(state.getMass()));
    }

    /** {@inheritDoc} */
    @Override
    protected void resetIntermediateState(final FieldSpacecraftState<T> state, final boolean forward) {
        resetInitialState(state);
    }

    /**
     * Build orbital elements from initial state.
     * <p>
     * This method is roughly the inverse of {@link #propagateInEcef(FieldAbsoluteDate, CalculusFieldElement[])},
     * except it starts from a state in inertial frame
     * </p>
     *
     * @param <T> type of the field elements
     * @param initialState    initial state
     * @param nonKeplerianElements non-Keplerian orbital elements (the Keplerian orbital elements will be overridden)
     * @param ecef            Earth Centered Earth Fixed frame
     * @param provider        attitude provider
     * @param mass            satellite mass (kg)
     * @return orbital elements that generate the {@code initialState} when used with a propagator
     */
    private static <T extends CalculusFieldElement<T>> FieldGnssOrbitalElements<T, ?>
        buildOrbitalElements(final FieldSpacecraftState<T> initialState,
                             final FieldGnssOrbitalElements<T, ?> nonKeplerianElements,
                             final Frame ecef, final AttitudeProvider provider,
                             final T mass) {

        final Field<T> field = initialState.getDate().getField();

        // get approximate initial orbit
        final Frame frozenEcef = ecef.getFrozenFrame(initialState.getFrame(),
                                                     initialState.getDate().toAbsoluteDate(),
                                                     "frozen");
        final FieldKeplerianOrbit<T> orbit = approximateInitialOrbit(initialState, nonKeplerianElements, frozenEcef);

        // refine orbit using simple differential correction to reach target PV
        final FieldPVCoordinates<T> targetPV = initialState.getPVCoordinates();
        final FieldGnssOrbitalElements<FieldGradient<T>, ?> gElements = convert(nonKeplerianElements, orbit);
        for (int i = 0; i < MAX_ITER; ++i) {

            // get position-velocity derivatives with respect to initial orbit
            final FieldGnssPropagator<FieldGradient<T>> gPropagator =
                new FieldGnssPropagator<>(gElements, initialState.getFrame(), ecef, provider,
                                          gElements.getMu().newInstance(mass));
            final FieldPVCoordinates<FieldGradient<T>> gPV = gPropagator.getInitialState().getPVCoordinates();

            // compute Jacobian matrix
            final FieldMatrix<T> jacobian = MatrixUtils.createFieldMatrix(field, FREE_PARAMETERS, FREE_PARAMETERS);
            jacobian.setRow(0, gPV.getPosition().getX().getGradient());
            jacobian.setRow(1, gPV.getPosition().getY().getGradient());
            jacobian.setRow(2, gPV.getPosition().getZ().getGradient());
            jacobian.setRow(3, gPV.getVelocity().getX().getGradient());
            jacobian.setRow(4, gPV.getVelocity().getY().getGradient());
            jacobian.setRow(5, gPV.getVelocity().getZ().getGradient());

            // linear correction to get closer to target PV
            final FieldVector<T> residuals = MatrixUtils.createFieldVector(field, FREE_PARAMETERS);
            residuals.setEntry(0, targetPV.getPosition().getX().subtract(gPV.getPosition().getX().getValue()));
            residuals.setEntry(1, targetPV.getPosition().getY().subtract(gPV.getPosition().getY().getValue()));
            residuals.setEntry(2, targetPV.getPosition().getZ().subtract(gPV.getPosition().getZ().getValue()));
            residuals.setEntry(3, targetPV.getVelocity().getX().subtract(gPV.getVelocity().getX().getValue()));
            residuals.setEntry(4, targetPV.getVelocity().getY().subtract(gPV.getVelocity().getY().getValue()));
            residuals.setEntry(5, targetPV.getVelocity().getZ().subtract(gPV.getVelocity().getZ().getValue()));
            final FieldVector<T> correction = new FieldQRDecomposition<>(jacobian, field.getZero().newInstance(EPS)).
                                              getSolver().
                                              solve(residuals);

            // update initial orbit
            gElements.setSma(gElements.getSma().add(correction.getEntry(0)));
            gElements.setE(gElements.getE().add(correction.getEntry(1)));
            gElements.setI0(gElements.getI0().add(correction.getEntry(2)));
            gElements.setPa(gElements.getPa().add(correction.getEntry(3)));
            gElements.setOmega0(gElements.getOmega0().add(correction.getEntry(4)));
            gElements.setM0(gElements.getM0().add(correction.getEntry(5)));

            final double deltaP = FastMath.sqrt(residuals.getEntry(0).getReal() * residuals.getEntry(0).getReal() +
                                                residuals.getEntry(1).getReal() * residuals.getEntry(1).getReal() +
                                                residuals.getEntry(2).getReal() * residuals.getEntry(2).getReal());
            final double deltaV = FastMath.sqrt(residuals.getEntry(3).getReal() * residuals.getEntry(3).getReal() +
                                                residuals.getEntry(4).getReal() * residuals.getEntry(4).getReal() +
                                                residuals.getEntry(5).getReal() * residuals.getEntry(5).getReal());

            if (deltaP < TOL_P && deltaV < TOL_V) {
                break;
            }

        }

        return gElements.changeField(FieldGradient::getValue);

    }

    /** Compute approximate initial orbit.
     * @param <T> type of the field elements
     * @param initialState         initial state
     * @param nonKeplerianElements non-Keplerian orbital elements (the Keplerian orbital elements will be ignored)
     * @param frozenEcef           inertial frame aligned with Earth Centered Earth Fixed frame at orbit date
     * @return approximate initial orbit that generate a state close to {@code initialState}
     */
    private static <T extends CalculusFieldElement<T>> FieldKeplerianOrbit<T>
        approximateInitialOrbit(final FieldSpacecraftState<T> initialState,
                                final FieldGnssOrbitalElements<T, ?> nonKeplerianElements,
                                final Frame frozenEcef) {

        // rotate the state to a frame that is inertial but aligned with Earth frame,
        // as analytical model is expressed in Earth frame
        final FieldPVCoordinates<T> pv = initialState.getPVCoordinates(frozenEcef);
        final FieldVector3D<T> p  = pv.getPosition();
        final FieldVector3D<T>      v  = pv.getVelocity();

        // compute Keplerian orbital parameters
        final T   rk  = p.getNorm();

        // compute orbital plane orientation
        final FieldVector3D<T> normal = pv.getMomentum().normalize();
        final T   cosIk  = normal.getZ();
        final T   ik     = FieldVector3D.angle(normal, Vector3D.PLUS_K);

        // compute position in orbital plane
        final T   q   = FastMath.hypot(normal.getX(), normal.getY());
        final T   cos = normal.getY().negate().divide(q);
        final T   sin =  normal.getX().divide(q);
        final T   xk  =  p.getX().multiply(cos).add(p.getY().multiply(sin));
        final T   yk  = p.getY().multiply(cos).subtract(p.getX().multiply(sin)).divide(cosIk);

        // corrected latitude argument
        final T   uk  = FastMath.atan2(yk, xk);

        // recover latitude argument before correction, using a fixed-point method
        T phi = uk;
        for (int i = 0; i < MAX_ITER; ++i) {
            final T previous = phi;
            final FieldSinCos<T> cs2Phi = FastMath.sinCos(phi.multiply(2));
            phi = uk.subtract(cs2Phi.cos().multiply(nonKeplerianElements.getCuc()).add(cs2Phi.sin().multiply(nonKeplerianElements.getCus())));
            if (FastMath.abs(phi.subtract(previous).getReal()) <= EPS) {
                break;
            }
        }
        final FieldSinCos<T> cs2phi = FastMath.sinCos(phi.multiply(2));

        // recover plane orientation before correction
        // here, we know that tk = 0 since our orbital elements will be at initial state date
        final T i0  = ik.subtract(cs2phi.cos().multiply(nonKeplerianElements.getCic()).add(cs2phi.sin().multiply(nonKeplerianElements.getCis())));
        final T om0 = FastMath.atan2(sin, cos).
                      add(nonKeplerianElements.getAngularVelocity() * nonKeplerianElements.getTime());

        // recover eccentricity and anomaly
        final T mu = initialState.getOrbit().getMu();
        final T rV2OMu           = rk.multiply(v.getNormSq()).divide(mu);
        final T sma              = rk.divide(rV2OMu.negate().add(2));
        final T eCosE            = rV2OMu.subtract(1);
        final T eSinE            = FieldVector3D.dotProduct(p, v).divide(FastMath.sqrt(mu.multiply(sma)));
        final T e                = FastMath.hypot(eCosE, eSinE);
        final T eccentricAnomaly = FastMath.atan2(eSinE, eCosE);
        final T aop              = phi.subtract(eccentricAnomaly);
        final T meanAnomaly      = FieldKeplerianAnomalyUtility.ellipticEccentricToMean(e, eccentricAnomaly);

        return new FieldKeplerianOrbit<>(sma, e, i0, aop, om0, meanAnomaly, PositionAngleType.MEAN,
                                         PositionAngleType.MEAN, frozenEcef,
                                         initialState.getDate(), mu);

    }

    /** Convert orbital elements to gradient.
     * @param <T> type of the field elements
     * @param elements   primitive double elements
     * @param orbit      Keplerian orbit
     * @return converted elements, set up as gradient relative to Keplerian orbit
     */
    private static <T extends CalculusFieldElement<T>> FieldGnssOrbitalElements<FieldGradient<T>, ?>
        convert(final FieldGnssOrbitalElements<T, ?> elements, final FieldKeplerianOrbit<T> orbit) {

        final FieldGnssOrbitalElements<FieldGradient<T>, ?> gElements =
            elements.changeField(t -> FieldGradient.constant(FREE_PARAMETERS, t));

        // Keplerian parameters
        gElements.setSma(FieldGradient.variable(FREE_PARAMETERS, 0, orbit.getA()));
        gElements.setE(FieldGradient.variable(FREE_PARAMETERS, 1, orbit.getE()));
        gElements.setI0(FieldGradient.variable(FREE_PARAMETERS, 2, orbit.getI()));
        gElements.setPa(FieldGradient.variable(FREE_PARAMETERS, 3, orbit.getPerigeeArgument()));
        gElements.setOmega0(FieldGradient.variable(FREE_PARAMETERS, 4, orbit.getRightAscensionOfAscendingNode()));
        gElements.setM0(FieldGradient.variable(FREE_PARAMETERS, 5, orbit.getMeanAnomaly()));

        return gElements;

    }

}
