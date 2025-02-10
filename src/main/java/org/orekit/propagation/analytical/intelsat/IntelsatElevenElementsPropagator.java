/* Copyright 2002-2025 Airbus Defence and Space
 * Licensed to CS GROUP (CS) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * Airbus Defence and Space licenses this file to You under the Apache License, Version 2.0
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
package org.orekit.propagation.analytical.intelsat;

import org.hipparchus.analysis.differentiation.UnivariateDerivative2;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.FastMath;
import org.hipparchus.util.FieldSinCos;
import org.orekit.annotation.DefaultDataContext;
import org.orekit.attitudes.Attitude;
import org.orekit.attitudes.AttitudeProvider;
import org.orekit.attitudes.FrameAlignedProvider;
import org.orekit.data.DataContext;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.orbits.CartesianOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.propagation.Propagator;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.analytical.AbstractAnalyticalPropagator;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.Constants;
import org.orekit.utils.IERSConventions;
import org.orekit.utils.PVCoordinates;
import org.orekit.utils.units.Unit;

/**
 * This class provides elements to propagate Intelsat's 11 elements.
 * <p>
 * Intelsat's 11 elements propagation is defined in ITU-R S.1525 standard.
 * </p>
 *
 * @author Bryan Cazabonne
 * @since 12.1
 */
public class IntelsatElevenElementsPropagator extends AbstractAnalyticalPropagator {

    /**
     * Intelsat's 11 elements.
     */
    private final IntelsatElevenElements elements;

    /**
     * Inertial frame for the output orbit.
     */
    private final Frame inertialFrame;

    /**
     * ECEF frame related to the Intelsat's 11 elements.
     */
    private final Frame ecefFrame;

    /**
     * Spacecraft mass in kilograms.
     */
    private final double mass;

    /**
     * Compute spacecraft's east longitude.
     */
    private UnivariateDerivative2 eastLongitudeDegrees;

    /**
     * Compute spacecraft's geocentric latitude.
     */
    private UnivariateDerivative2 geocentricLatitudeDegrees;

    /**
     * Compute spacecraft's orbit radius.
     */
    private UnivariateDerivative2 orbitRadius;

    /**
     * Default constructor.
     * <p>
     * This constructor uses the {@link DataContext#getDefault() default data context}.
     * </p>
     * <p> The attitude provider is set by default to be aligned with the inertial frame.<br>
     * The mass is set by default to the
     * {@link org.orekit.propagation.Propagator#DEFAULT_MASS DEFAULT_MASS}.<br>
     * The inertial frame is set by default to the
     * {@link org.orekit.frames.Predefined#TOD_CONVENTIONS_2010_SIMPLE_EOP TOD frame} in the default data
     * context.<br>
     * The ECEF frame is set by default to the
     * {@link org.orekit.frames.Predefined#ITRF_CIO_CONV_2010_SIMPLE_EOP
     * CIO/2010-based ITRF simple EOP} in the default data context.
     * </p>
     *
     * @param elements Intelsat's 11 elements
     */
    @DefaultDataContext
    public IntelsatElevenElementsPropagator(final IntelsatElevenElements elements) {
        this(elements, FramesFactory.getTOD(IERSConventions.IERS_2010, true), FramesFactory.getITRF(IERSConventions.IERS_2010, true));
    }

    /**
     * Constructor.
     *
     * <p> The attitude provider is set by default to be aligned with the inertial frame.<br>
     * The mass is set by default to the
     * {@link org.orekit.propagation.Propagator#DEFAULT_MASS DEFAULT_MASS}.<br>
     * </p>
     *
     * @param elements      Intelsat's 11 elements
     * @param inertialFrame inertial frame for the output orbit
     * @param ecefFrame     ECEF frame related to the Intelsat's 11 elements
     */
    public IntelsatElevenElementsPropagator(final IntelsatElevenElements elements, final Frame inertialFrame, final Frame ecefFrame) {
        this(elements, inertialFrame, ecefFrame, FrameAlignedProvider.of(inertialFrame), Propagator.DEFAULT_MASS);
    }

    /**
     * Constructor.
     *
     * @param elements         Intelsat's 11 elements
     * @param inertialFrame    inertial frame for the output orbit
     * @param ecefFrame        ECEF frame related to the Intelsat's 11 elements
     * @param attitudeProvider attitude provider
     * @param mass             spacecraft mass
     */
    public IntelsatElevenElementsPropagator(final IntelsatElevenElements elements, final Frame inertialFrame, final Frame ecefFrame, final AttitudeProvider attitudeProvider,
                                            final double mass) {
        super(attitudeProvider);
        this.elements = elements;
        this.inertialFrame = inertialFrame;
        this.ecefFrame = ecefFrame;
        this.mass = mass;
        setStartDate(elements.getEpoch());
        final Orbit orbitAtElementsDate = propagateOrbit(elements.getEpoch());
        final Attitude attitude = attitudeProvider.getAttitude(orbitAtElementsDate, elements.getEpoch(), inertialFrame);
        super.resetInitialState(new SpacecraftState(orbitAtElementsDate, attitude, mass));
    }

    /**
     * Converts the Intelsat's 11 elements into Position/Velocity coordinates in ECEF.
     *
     * @param date computation epoch
     * @return Position/Velocity coordinates in ECEF
     */
    public PVCoordinates propagateInEcef(final AbsoluteDate date) {
        final UnivariateDerivative2 tDays = new UnivariateDerivative2(date.durationFrom(elements.getEpoch()), 1.0, 0.0).divide(Constants.JULIAN_DAY);
        final double wDegreesPerDay = elements.getLm1() + IntelsatElevenElements.DRIFT_RATE_SHIFT_DEG_PER_DAY;
        final UnivariateDerivative2 wt = FastMath.toRadians(tDays.multiply(wDegreesPerDay));
        final FieldSinCos<UnivariateDerivative2> scWt = FastMath.sinCos(wt);
        final FieldSinCos<UnivariateDerivative2> sc2Wt = FastMath.sinCos(wt.multiply(2.0));
        final UnivariateDerivative2 satelliteEastLongitudeDegrees = computeSatelliteEastLongitudeDegrees(tDays, scWt, sc2Wt);
        final UnivariateDerivative2 satelliteGeocentricLatitudeDegrees = computeSatelliteGeocentricLatitudeDegrees(tDays, scWt);
        final UnivariateDerivative2 satelliteRadius = computeSatelliteRadiusKilometers(wDegreesPerDay, scWt).multiply(Unit.KILOMETRE.getScale());
        this.eastLongitudeDegrees = satelliteEastLongitudeDegrees;
        this.geocentricLatitudeDegrees = satelliteGeocentricLatitudeDegrees;
        this.orbitRadius = satelliteRadius;
        final FieldSinCos<UnivariateDerivative2> scLongitude = FastMath.sinCos(FastMath.toRadians(satelliteEastLongitudeDegrees));
        final FieldSinCos<UnivariateDerivative2> scLatitude = FastMath.sinCos(FastMath.toRadians(satelliteGeocentricLatitudeDegrees));
        final FieldVector3D<UnivariateDerivative2> positionWithDerivatives = new FieldVector3D<>(satelliteRadius.multiply(scLatitude.cos()).multiply(scLongitude.cos()),
                                                                                                 satelliteRadius.multiply(scLatitude.cos()).multiply(scLongitude.sin()),
                                                                                                 satelliteRadius.multiply(scLatitude.sin()));
        return new PVCoordinates(new Vector3D(positionWithDerivatives.getX().getValue(), //
                                              positionWithDerivatives.getY().getValue(), //
                                              positionWithDerivatives.getZ().getValue()), //
                                 new Vector3D(positionWithDerivatives.getX().getFirstDerivative(), //
                                              positionWithDerivatives.getY().getFirstDerivative(), //
                                              positionWithDerivatives.getZ().getFirstDerivative()), //
                                 new Vector3D(positionWithDerivatives.getX().getSecondDerivative(), //
                                              positionWithDerivatives.getY().getSecondDerivative(), //
                                              positionWithDerivatives.getZ().getSecondDerivative()));
    }

    /**
     * {@inheritDoc}.
     */
    @Override
    public void resetInitialState(final SpacecraftState state) {
        throw new OrekitException(OrekitMessages.NON_RESETABLE_STATE);
    }

    /**
     * {@inheritDoc}.
     */
    @Override
    protected double getMass(final AbsoluteDate date) {
        return mass;
    }

    /**
     * {@inheritDoc}.
     */
    @Override
    protected void resetIntermediateState(final SpacecraftState state, final boolean forward) {
        throw new OrekitException(OrekitMessages.NON_RESETABLE_STATE);
    }

    /**
     * {@inheritDoc}.
     */
    @Override
    public Orbit propagateOrbit(final AbsoluteDate date) {
        return new CartesianOrbit(ecefFrame.getTransformTo(inertialFrame, date).transformPVCoordinates(propagateInEcef(date)), inertialFrame, date, Constants.WGS84_EARTH_MU);
    }

    /**
     * Computes the satellite's east longitude.
     *
     * @param tDays delta time in days
     * @param scW   sin/cos of the W angle
     * @param sc2W  sin/cos of the 2xW angle
     * @return the satellite's east longitude in degrees
     */
    private UnivariateDerivative2 computeSatelliteEastLongitudeDegrees(final UnivariateDerivative2 tDays, final FieldSinCos<UnivariateDerivative2> scW,
                                                                       final FieldSinCos<UnivariateDerivative2> sc2W) {
        final UnivariateDerivative2 longitude = tDays.multiply(tDays).multiply(elements.getLm2()) //
                                                     .add(tDays.multiply(elements.getLm1())) //
                                                     .add(elements.getLm0());
        final UnivariateDerivative2 cosineLongitudeTerm = scW.cos().multiply(tDays.multiply(elements.getLonC1()).add(elements.getLonC()));
        final UnivariateDerivative2 sineLongitudeTerm = scW.sin().multiply(tDays.multiply(elements.getLonS1()).add(elements.getLonS()));
        final UnivariateDerivative2 latitudeTerm = sc2W.sin().multiply(0.5 * (elements.getLatC() * elements.getLatC() - elements.getLatS() * elements.getLatS())) //
                                                       .subtract(sc2W.cos().multiply(elements.getLatC() * elements.getLatS())) //
                                                       .multiply(IntelsatElevenElements.K);
        return longitude.add(cosineLongitudeTerm).add(sineLongitudeTerm).add(latitudeTerm);
    }

    /**
     * Computes the satellite's geocentric latitude.
     *
     * @param tDays delta time in days
     * @param scW   sin/cos of the W angle
     * @return he satellite geocentric latitude in degrees
     */
    private UnivariateDerivative2 computeSatelliteGeocentricLatitudeDegrees(final UnivariateDerivative2 tDays, final FieldSinCos<UnivariateDerivative2> scW) {
        final UnivariateDerivative2 cosineTerm = scW.cos().multiply(tDays.multiply(elements.getLatC1()).add(elements.getLatC()));
        final UnivariateDerivative2 sineTerm = scW.sin().multiply(tDays.multiply(elements.getLatS1()).add(elements.getLatS()));
        return cosineTerm.add(sineTerm);
    }

    /**
     * Computes the satellite's orbit radius.
     *
     * @param wDegreesPerDay W angle in degrees/day
     * @param scW            sin/cos of the W angle
     * @return the satellite's orbit radius in kilometers
     */
    private UnivariateDerivative2 computeSatelliteRadiusKilometers(final double wDegreesPerDay, final FieldSinCos<UnivariateDerivative2> scW) {
        final double coefficient = IntelsatElevenElements.SYNCHRONOUS_RADIUS_KM * (1.0 - (2.0 * elements.getLm1()) / (3.0 * (wDegreesPerDay - elements.getLm1())));
        return scW.sin()
                  .multiply(IntelsatElevenElements.K * elements.getLonC())
                  .add(1.0)
                  .subtract(scW.cos().multiply(IntelsatElevenElements.K * elements.getLonS()))
                  .multiply(coefficient);
    }

    /**
     * Get the computed satellite's east longitude.
     *
     * @return the satellite's east longitude in degrees
     */
    public UnivariateDerivative2 getEastLongitudeDegrees() {
        return eastLongitudeDegrees;
    }

    /**
     * Get the computed satellite's geocentric latitude.
     *
     * @return the satellite's geocentric latitude in degrees
     */
    public UnivariateDerivative2 getGeocentricLatitudeDegrees() {
        return geocentricLatitudeDegrees;
    }

    /**
     * Get the computed satellite's orbit.
     *
     * @return satellite's orbit radius in meters
     */
    public UnivariateDerivative2 getOrbitRadius() {
        return orbitRadius;
    }

    /**
     * {@inheritDoc}.
     */
    @Override
    public Frame getFrame() {
        return inertialFrame;
    }

    /**
     * Get the Intelsat's 11 elements used by the propagator.
     *
     * @return the Intelsat's 11 elements used by the propagator
     */
    public IntelsatElevenElements getIntelsatElevenElements() {
        return elements;
    }

}
