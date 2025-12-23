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
import org.orekit.attitudes.AttitudeProvider;
import org.orekit.attitudes.FieldAttitude;
import org.orekit.attitudes.FrameAlignedProvider;
import org.orekit.frames.Frame;
import org.orekit.orbits.FieldCartesianOrbit;
import org.orekit.orbits.FieldKeplerianAnomalyUtility;
import org.orekit.orbits.FieldKeplerianOrbit;
import org.orekit.orbits.FieldOrbit;
import org.orekit.orbits.PositionAngleType;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.Propagator;
import org.orekit.propagation.analytical.FieldAbstractAnalyticalPropagator;
import org.orekit.propagation.analytical.gnss.data.FieldGnssOrbitalElements;
import org.orekit.propagation.analytical.gnss.data.GNSSOrbitalElements;
import org.orekit.propagation.analytical.gnss.data.GNSSOrbitalElementsFactory;
import org.orekit.propagation.analytical.gnss.data.NonKeplerianDriversFactory;
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
 * @param <O> type of the orbital elements (non-field version)
 * @param <P> type of the orbital elements (field version)
 * @since 13.0
 */
public class FieldGnssPropagator<T extends CalculusFieldElement<T>,
                                 O extends GNSSOrbitalElements<O>,
                                 P extends FieldGnssOrbitalElements<T, O, P>>
    extends FieldAbstractAnalyticalPropagator<T> {

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
    private FieldGnssOrbitalElements<T, O, P> orbitalElements;

    /** Factory for non-Keplerian elements drivers.
     * @since 14.0
     */
    private final NonKeplerianDriversFactory driversFactory;

    /** The ECI frame used for GNSS propagation. */
    private final Frame eci;

    /** The ECEF frame used for GNSS propagation. */
    private final Frame ecef;

    /** Build a new instance.
     * <p>
     * The attitude provider is set by default to be aligned with the provided inertial frame.
     * This can be changed (typically to {@link org.orekit.gnss.attitude.GenericGNSS}) after
     * construction by calling {@link #setAttitudeProvider(org.orekit.attitudes.AttitudeProvider)
     * setAttitudeProvider}
     * </p>
     * <p>
     * The mass is set to the {@link org.orekit.propagation.Propagator#DEFAULT_MASS DEFAULT_MASS}.
     * </p>
     * @param factory factory for the elements and frames
     * @since 14.0
     */
    public FieldGnssPropagator(final Field<T> field, final GNSSOrbitalElementsFactory<O> factory) {
        this(factory.createFromDrivers().toField(field),
             factory.getInertial(), factory.getBodyFixed(),
             FrameAlignedProvider.of(factory.getInertial()),
             field.getZero().newInstance(Propagator.DEFAULT_MASS));
    }

    /**
     * Build a new instance.
     * @param orbitalElements GNSS orbital elements
     * @param eci Earth Centered Inertial frame
     * @param ecef Earth Centered Earth Fixed frame
     * @param provider Attitude provider
     * @param mass Satellite mass (kg)
     */
    public FieldGnssPropagator(final P orbitalElements,
                               final Frame eci, final Frame ecef,
                               final AttitudeProvider provider, final T mass) {
        super(orbitalElements.getDate().getField(), provider);
        // Stores the GNSS orbital elements
        this.orbitalElements = orbitalElements;
        this.driversFactory  = new NonKeplerianDriversFactory();
        driversFactory.reset(orbitalElements);
       // Sets the Earth Centered Inertial frame
        this.eci  = eci;
        // Sets the Earth Centered Earth Fixed frame
        this.ecef = ecef;
        // Sets initial state
        final FieldOrbit<T> orbit = propagateOrbit(orbitalElements.getDate(),
                                                   getParameters(orbitalElements.getDate().getField()));
        final FieldAttitude<T> attitude = provider.getAttitude(orbit, orbit.getDate(), orbit.getFrame());

        // calling the method from constructor because the one overridden below recomputes the orbital elements
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
    public FieldGnssPropagator(final FieldSpacecraftState<T> initialState,
                               final P nonKeplerianElements,
                               final Frame ecef, final AttitudeProvider provider, final T mass) {
        this(buildOrbitalElements(initialState, nonKeplerianElements, new NonKeplerianDriversFactory(),
                        ecef, provider, mass),
             initialState.getFrame(), ecef, provider, initialState.getMass());
    }

    /** {@inheritDoc} */
    @Override
    public List<ParameterDriver> getParametersDrivers() {
        return driversFactory.getParametersDrivers();
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
        return orbitalElements.getOrbit().getMu();
    }

    /** Get the underlying GNSS propagation orbital elements.
     * @return the underlying GNSS orbital elements
     * @since 14.0
     */
    public FieldGnssOrbitalElements<T, O, P> getOrbitalElements() {
        return orbitalElements;
    }

    /** {@inheritDoc} */
    @Override
    public FieldOrbit<T> propagateOrbit(final FieldAbsoluteDate<T> date, final T[] parameters) {
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

        final FieldKeplerianOrbit<T> orbit = orbitalElements.getOrbit();

        // Duration from GNSS ephemeris Reference date
        final FieldUnivariateDerivative2<T> tk = new FieldUnivariateDerivative2<>(getTk(date),
                                                                                  date.getField().getOne(),
                                                                                  date.getField().getZero());

        // Semi-major axis
        final FieldUnivariateDerivative2<T> ak = tk.multiply(parameters[NonKeplerianDriversFactory.A_DOT_INDEX]).
                                                 add(orbit.getA());
        // Mean motion
        final FieldUnivariateDerivative2<T> nA = tk.multiply(parameters[NonKeplerianDriversFactory.DELTA_N0_DOT_INDEX].multiply(0.5)).
                                                 add(parameters[NonKeplerianDriversFactory.DELTA_N0_INDEX]).
                                                 add(orbit.getKeplerianMeanMotion());
        // Mean anomaly
        final FieldUnivariateDerivative2<T> mk = tk.multiply(nA).add(orbit.getMeanAnomaly());
        // Eccentric Anomaly
        final FieldUnivariateDerivative2<T> e  = tk.newInstance(orbit.getE());
        final FieldUnivariateDerivative2<T> ek = FieldKeplerianAnomalyUtility.ellipticMeanToEccentric(e, mk);
        // True Anomaly
        final FieldUnivariateDerivative2<T> vk = FieldKeplerianAnomalyUtility.ellipticEccentricToTrue(e, ek);
        // Argument of Latitude
        final FieldUnivariateDerivative2<T> phik    = vk.add(orbit.getPerigeeArgument());
        final FieldSinCos<FieldUnivariateDerivative2<T>> cs2phi = FastMath.sinCos(phik.multiply(2));
        // Argument of Latitude Correction
        final FieldUnivariateDerivative2<T> dphik = cs2phi.cos().multiply(parameters[NonKeplerianDriversFactory.CUC_INDEX]).
                                                add(cs2phi.sin().multiply(parameters[NonKeplerianDriversFactory.CUS_INDEX]));
        // Radius Correction
        final FieldUnivariateDerivative2<T> drk = cs2phi.cos().multiply(parameters[NonKeplerianDriversFactory.CRC_INDEX]).
                                              add(cs2phi.sin().multiply(parameters[NonKeplerianDriversFactory.CRS_INDEX]));
        // Inclination Correction
        final FieldUnivariateDerivative2<T> dik = cs2phi.cos().multiply(parameters[NonKeplerianDriversFactory.CIC_INDEX]).
                                              add(cs2phi.sin().multiply(parameters[NonKeplerianDriversFactory.CIS_INDEX]));
        // Corrected Argument of Latitude
        final FieldSinCos<FieldUnivariateDerivative2<T>> csuk = FastMath.sinCos(phik.add(dphik));
        // Corrected Radius
        final FieldUnivariateDerivative2<T> rk = ek.cos().multiply(e.negate()).add(1).multiply(ak).add(drk);
        // Corrected Inclination
        final FieldUnivariateDerivative2<T> ik  = tk.multiply(parameters[NonKeplerianDriversFactory.I_DOT_INDEX]).
                                                  add(orbit.getI()).add(dik);
        final FieldSinCos<FieldUnivariateDerivative2<T>> csik = FastMath.sinCos(ik);
        // Positions in orbital plane
        final FieldUnivariateDerivative2<T> xk = csuk.cos().multiply(rk);
        final FieldUnivariateDerivative2<T> yk = csuk.sin().multiply(rk);
        // Corrected longitude of ascending node
        final FieldSinCos<FieldUnivariateDerivative2<T>> csomk =
            FastMath.sinCos(tk.multiply(parameters[NonKeplerianDriversFactory.OMEGA_DOT_INDEX].
                            subtract(orbitalElements.getAngularVelocity())).
                            add(orbit.getRightAscensionOfAscendingNode().
                            subtract(parameters[NonKeplerianDriversFactory.TIME_INDEX].multiply(orbitalElements.getAngularVelocity()))));
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
        T tk = date.durationFrom(orbitalElements.getGnssDate());
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
        orbitalElements = buildOrbitalElements(state, orbitalElements, driversFactory,
                ecef, getAttitudeProvider(), state.getMass());
        final FieldOrbit<T> orbit = propagateOrbit(orbitalElements.getDate(),
                                                   getParameters(orbitalElements.getDate().getField()));
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
     * @param <O> type of the orbital elements (non-field version)
     * @param <P> type of the orbital elements (field version)
     * @param <Q> type of the orbital elements (field gradient version)
     * @param initialState         initial state
     * @param nonKeplerianElements non-Keplerian orbital elements (the Keplerian orbital elements will be overridden)
     * @param driversFactory       factory for non-Keplerian drivers
     * @param ecef                 Earth Centered Earth Fixed frame
     * @param provider             attitude provider
     * @param mass                 satellite mass (kg)
     * @return orbital elements that generate the {@code initialState} when used with a propagator
     */
    private static <T extends CalculusFieldElement<T>,
                    O extends GNSSOrbitalElements<O>,
                    P extends FieldGnssOrbitalElements<T, O, P>,
                    Q extends FieldGnssOrbitalElements<FieldGradient<T>, O, Q>>
       P buildOrbitalElements(final FieldSpacecraftState<T> initialState,
                              final FieldGnssOrbitalElements<T, O, P> nonKeplerianElements,
                              final NonKeplerianDriversFactory driversFactory,
                              final Frame ecef, final AttitudeProvider provider,
                              final T mass) {

        final Field<T> field = initialState.getDate().getField();

        // get approximate initial orbit
        final Frame frozenEcef = ecef.getFrozenFrame(initialState.getFrame(),
                                                     initialState.getDate().toAbsoluteDate(),
                                                     GNSSOrbitalElementsFactory.FROZEN + ecef.getName());
        final FieldKeplerianOrbit<T> orbit = approximateInitialOrbit(initialState, nonKeplerianElements, frozenEcef);
        driversFactory.reset(nonKeplerianElements);

        // refine orbit using simple differential correction to reach target PV
        final FieldPVCoordinates<T> targetPV = initialState.getPVCoordinates(frozenEcef);
        Q gElements = convert(nonKeplerianElements, orbit, driversFactory);
        for (int i = 0; i < MAX_ITER; ++i) {

            // get position-velocity derivatives with respect to initial orbit
            final FieldGnssPropagator<FieldGradient<T>, O, Q> gPropagator =
                new FieldGnssPropagator<>(gElements, frozenEcef, ecef, provider,
                                          gElements.getOrbit().getMu().newInstance(mass));
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

            // prevent correction to produce invalid values
            final FieldKeplerianOrbit<FieldGradient<T>> previous = gElements.getOrbit();
            T updatedA;
            T updatedE;
            double factor = 2;
            do {
                // loop until eccentricity is valid
                factor *= 0.5;
                updatedA = previous.getA().getValue().add(correction.getEntry(0).multiply(factor));
                updatedE = previous.getE().getValue().add(correction.getEntry(1).multiply(factor));
            } while (updatedA.getReal() < 0 || updatedE.getReal() < 0 || updatedE.getReal() >= 1);

            // update initial orbit
            final FieldKeplerianOrbit<T> updated =
                new FieldKeplerianOrbit<>(updatedA,
                                          updatedE,
                                          previous.getI().getValue().add(correction.getEntry(2).multiply(factor)),
                                          previous.getPerigeeArgument().getValue().add(correction.getEntry(3).multiply(factor)),
                                          previous.getRightAscensionOfAscendingNode().getValue().add(correction.getEntry(4).multiply(factor)),
                                          previous.getMeanAnomaly().getValue().add(correction.getEntry(5).multiply(factor)),
                                          PositionAngleType.MEAN, PositionAngleType.MEAN,
                                          previous.getFrame(),
                                          new FieldAbsoluteDate<>(previous.getMu().getValue().getField(),
                                                                  previous.getDate().toAbsoluteDate()),
                                          previous.getMu().getValue());
            gElements = convert(nonKeplerianElements, updated, driversFactory);

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

        final FieldKeplerianOrbit<FieldGradient<T>> initialOrbit = gElements.getOrbit();
        final T zero = initialState.getOrbit().getMu().getField().getZero();
        return gElements.toField(new FieldKeplerianOrbit<>(initialOrbit.getA().getValue(),
                                                           initialOrbit.getE().getValue(),
                                                           initialOrbit.getI().getValue(),
                                                           initialOrbit.getPerigeeArgument().getValue(),
                                                           initialOrbit.getRightAscensionOfAscendingNode().getValue(),
                                                           initialOrbit.getMeanAnomaly().getValue(),
                                                           PositionAngleType.MEAN, PositionAngleType.MEAN,
                                                           initialOrbit.getFrame(),
                                                           new FieldAbsoluteDate<>(initialOrbit.getMu().getValue().getField(),
                                                                                   initialOrbit.getDate().toAbsoluteDate()),
                                                           initialOrbit.getMu().getValue()),
                                 driversFactory.toArray(zero.getField(), zero::newInstance),
                                 FieldGradient::getValue);

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
                                final FieldGnssOrbitalElements<T, ?, ?> nonKeplerianElements,
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
        final double toe = nonKeplerianElements.getGnssDate().getGnssDate().getSecondsInWeek();
        final T om0 = FastMath.atan2(sin, cos).
                      add(nonKeplerianElements.getAngularVelocity() * toe);

        // recover eccentricity and anomaly
        final T mu = initialState.getOrbit().getMu();
        final T rV2OMu           = rk.multiply(v.getNorm2Sq()).divide(mu);
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
     * @param <O> type of the orbital elements (non-field version)
     * @param <P> type of the orbital elements (field version)
     * @param <Q> type of the orbital elements (field gradient version)
     * @param elements       primitive double elements
     * @param orbit          Keplerian orbit
     * @param driversFactory factory for non-Kepleria drivers
     * @return converted elements, set up as gradient relative to Keplerian orbit
     */
    private static <T extends CalculusFieldElement<T>,
                    O extends GNSSOrbitalElements<O>,
                    P extends FieldGnssOrbitalElements<T, O, P>,
                    Q extends FieldGnssOrbitalElements<FieldGradient<T>, O, Q>>
        Q convert(final FieldGnssOrbitalElements<T, O, P> elements,
                  final FieldKeplerianOrbit<T> orbit,
                  final NonKeplerianDriversFactory driversFactory) {
        return elements.toField(new FieldKeplerianOrbit<>(FieldGradient.variable(FREE_PARAMETERS, 0,
                                                                            orbit.getA()),
                                                          FieldGradient.variable(FREE_PARAMETERS, 1,
                                                                            orbit.getE()),
                                                          FieldGradient.variable(FREE_PARAMETERS, 2,
                                                                            orbit.getI()),
                                                          FieldGradient.variable(FREE_PARAMETERS, 3,
                                                                            orbit.getPerigeeArgument()),
                                                          FieldGradient.variable(FREE_PARAMETERS, 4,
                                                                            orbit.getRightAscensionOfAscendingNode()),
                                                          FieldGradient.variable(FREE_PARAMETERS, 5,
                                                                            orbit.getMeanAnomaly()),
                                                          PositionAngleType.MEAN, PositionAngleType.MEAN,
                                                          orbit.getFrame(),
                                                          new FieldAbsoluteDate<>(FieldGradient.constant(FREE_PARAMETERS,
                                                                                                         orbit.getMu()).
                                                                                  getField(),
                                                                                  orbit.getDate().toAbsoluteDate()),
                                                          FieldGradient.constant(FREE_PARAMETERS, orbit.getMu())),
                                driversFactory.toGradients(orbit.getMu().getField(), FREE_PARAMETERS),
                                d -> FieldGradient.constant(FREE_PARAMETERS, d));
    }

}
