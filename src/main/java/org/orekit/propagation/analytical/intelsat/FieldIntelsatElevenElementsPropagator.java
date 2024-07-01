/* Copyright 2002-2024 Airbus Defence and Space
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

import java.util.Collections;
import java.util.List;
import org.hipparchus.CalculusFieldElement;
import org.hipparchus.Field;
import org.hipparchus.analysis.differentiation.FieldUnivariateDerivative2;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.util.FastMath;
import org.hipparchus.util.FieldSinCos;
import org.orekit.annotation.DefaultDataContext;
import org.orekit.attitudes.AttitudeProvider;
import org.orekit.attitudes.FieldAttitude;
import org.orekit.attitudes.FrameAlignedProvider;
import org.orekit.data.DataContext;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.orbits.FieldCartesianOrbit;
import org.orekit.orbits.FieldOrbit;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.Propagator;
import org.orekit.propagation.analytical.FieldAbstractAnalyticalPropagator;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.utils.Constants;
import org.orekit.utils.FieldPVCoordinates;
import org.orekit.utils.IERSConventions;
import org.orekit.utils.ParameterDriver;
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
public class FieldIntelsatElevenElementsPropagator<T extends CalculusFieldElement<T>> extends FieldAbstractAnalyticalPropagator<T> {

    /**
     * Intelsat's 11 elements.
     */
    private final FieldIntelsatElevenElements<T> elements;

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
    private final T mass;

    /**
     * Compute spacecraft's east longitude.
     */
    private FieldUnivariateDerivative2<T> eastLongitudeDegrees;

    /**
     * Compute spacecraft's geocentric latitude.
     */
    private FieldUnivariateDerivative2<T> geocentricLatitudeDegrees;

    /**
     * Compute spacecraft's orbit radius.
     */
    private FieldUnivariateDerivative2<T> orbitRadius;

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
    public FieldIntelsatElevenElementsPropagator(final FieldIntelsatElevenElements<T> elements) {
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
    public FieldIntelsatElevenElementsPropagator(final FieldIntelsatElevenElements<T> elements, final Frame inertialFrame, final Frame ecefFrame) {
        this(elements, inertialFrame, ecefFrame, FrameAlignedProvider.of(inertialFrame), elements.getEpoch().getField().getZero().add(Propagator.DEFAULT_MASS));
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
    public FieldIntelsatElevenElementsPropagator(final FieldIntelsatElevenElements<T> elements, final Frame inertialFrame, final Frame ecefFrame,
                                                 final AttitudeProvider attitudeProvider, final T mass) {
        super(elements.getEpoch().getField(), attitudeProvider);
        this.elements = elements;
        this.inertialFrame = inertialFrame;
        this.ecefFrame = ecefFrame;
        this.mass = mass;
        setStartDate(elements.getEpoch());
        final FieldOrbit<T> orbitAtElementsDate = propagateOrbit(elements.getEpoch(), getParameters(elements.getEpoch().getField()));
        final FieldAttitude<T> attitude = attitudeProvider.getAttitude(orbitAtElementsDate, elements.getEpoch(), inertialFrame);
        super.resetInitialState(new FieldSpacecraftState<>(orbitAtElementsDate, attitude, mass));
    }

    /**
     * Converts the Intelsat's 11 elements into Position/Velocity coordinates in ECEF.
     *
     * @param date computation epoch
     * @return Position/Velocity coordinates in ECEF
     */
    public FieldPVCoordinates<T> propagateInEcef(final FieldAbsoluteDate<T> date) {
        final Field<T> field = date.getField();
        final FieldUnivariateDerivative2<T> tDays = new FieldUnivariateDerivative2<>(date.durationFrom(elements.getEpoch()), field.getOne(), field.getZero()).divide(
                Constants.JULIAN_DAY);
        final T wDegreesPerDay = elements.getLm1().add(IntelsatElevenElements.DRIFT_RATE_SHIFT_DEG_PER_DAY);
        final FieldUnivariateDerivative2<T> wt = FastMath.toRadians(tDays.multiply(wDegreesPerDay));
        final FieldSinCos<FieldUnivariateDerivative2<T>> scWt = FastMath.sinCos(wt);
        final FieldSinCos<FieldUnivariateDerivative2<T>> sc2Wt = FastMath.sinCos(wt.multiply(2.0));
        final FieldUnivariateDerivative2<T> satelliteEastLongitudeDegrees = computeSatelliteEastLongitudeDegrees(tDays, scWt, sc2Wt);
        final FieldUnivariateDerivative2<T> satelliteGeocentricLatitudeDegrees = computeSatelliteGeocentricLatitudeDegrees(tDays, scWt);
        final FieldUnivariateDerivative2<T> satelliteRadius = computeSatelliteRadiusKilometers(wDegreesPerDay, scWt).multiply(Unit.KILOMETRE.getScale());
        this.eastLongitudeDegrees = satelliteEastLongitudeDegrees;
        this.geocentricLatitudeDegrees = satelliteGeocentricLatitudeDegrees;
        this.orbitRadius = satelliteRadius;
        final FieldSinCos<FieldUnivariateDerivative2<T>> scLongitude = FastMath.sinCos(FastMath.toRadians(satelliteEastLongitudeDegrees));
        final FieldSinCos<FieldUnivariateDerivative2<T>> scLatitude = FastMath.sinCos(FastMath.toRadians(satelliteGeocentricLatitudeDegrees));
        final FieldVector3D<FieldUnivariateDerivative2<T>> positionWithDerivatives = new FieldVector3D<>(satelliteRadius.multiply(scLatitude.cos()).multiply(scLongitude.cos()),
                                                                                                         satelliteRadius.multiply(scLatitude.cos()).multiply(scLongitude.sin()),
                                                                                                         satelliteRadius.multiply(scLatitude.sin()));
        return new FieldPVCoordinates<>(new FieldVector3D<>(positionWithDerivatives.getX().getValue(), //
                                                            positionWithDerivatives.getY().getValue(), //
                                                            positionWithDerivatives.getZ().getValue()), //
                                        new FieldVector3D<>(positionWithDerivatives.getX().getFirstDerivative(), //
                                                            positionWithDerivatives.getY().getFirstDerivative(), //
                                                            positionWithDerivatives.getZ().getFirstDerivative()), //
                                        new FieldVector3D<>(positionWithDerivatives.getX().getSecondDerivative(), //
                                                            positionWithDerivatives.getY().getSecondDerivative(), //
                                                            positionWithDerivatives.getZ().getSecondDerivative()));
    }

    /**
     * {@inheritDoc}.
     */
    @Override
    public void resetInitialState(final FieldSpacecraftState<T> state) {
        throw new OrekitException(OrekitMessages.NON_RESETABLE_STATE);
    }

    /**
     * {@inheritDoc}.
     */
    @Override
    protected T getMass(final FieldAbsoluteDate<T> date) {
        return mass;
    }

    /**
     * {@inheritDoc}.
     */
    @Override
    protected void resetIntermediateState(final FieldSpacecraftState<T> state, final boolean forward) {
        throw new OrekitException(OrekitMessages.NON_RESETABLE_STATE);
    }

    /**
     * {@inheritDoc}.
     */
    @Override
    protected FieldOrbit<T> propagateOrbit(final FieldAbsoluteDate<T> date, final T[] parameters) {
        return new FieldCartesianOrbit<>(ecefFrame.getTransformTo(inertialFrame, date).transformPVCoordinates(propagateInEcef(date)), inertialFrame, date,
                                         date.getField().getZero().add(Constants.WGS84_EARTH_MU));
    }

    /**
     * Computes the satellite's east longitude.
     *
     * @param tDays delta time in days
     * @param scW   sin/cos of the W angle
     * @param sc2W  sin/cos of the 2xW angle
     * @return the satellite's east longitude in degrees
     */
    private FieldUnivariateDerivative2<T> computeSatelliteEastLongitudeDegrees(final FieldUnivariateDerivative2<T> tDays, final FieldSinCos<FieldUnivariateDerivative2<T>> scW,
                                                                               final FieldSinCos<FieldUnivariateDerivative2<T>> sc2W) {
        final FieldUnivariateDerivative2<T> longitude = tDays.multiply(tDays).multiply(elements.getLm2()) //
                                                             .add(tDays.multiply(elements.getLm1())) //
                                                             .add(elements.getLm0());
        final FieldUnivariateDerivative2<T> cosineLongitudeTerm = scW.cos().multiply(tDays.multiply(elements.getLonC1()).add(elements.getLonC()));
        final FieldUnivariateDerivative2<T> sineLongitudeTerm = scW.sin().multiply(tDays.multiply(elements.getLonS1()).add(elements.getLonS()));
        final FieldUnivariateDerivative2<T> latitudeTerm = sc2W.sin()
                                                               .multiply(elements.getLatC()
                                                                                 .multiply(elements.getLatC())
                                                                                 .subtract(elements.getLatS().multiply(elements.getLatS()))
                                                                                 .multiply(0.5)) //
                                                               .subtract(sc2W.cos().multiply(elements.getLatC().multiply(elements.getLatS()))) //
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
    private FieldUnivariateDerivative2<T> computeSatelliteGeocentricLatitudeDegrees(final FieldUnivariateDerivative2<T> tDays,
                                                                                    final FieldSinCos<FieldUnivariateDerivative2<T>> scW) {
        final FieldUnivariateDerivative2<T> cosineTerm = scW.cos().multiply(tDays.multiply(elements.getLatC1()).add(elements.getLatC()));
        final FieldUnivariateDerivative2<T> sineTerm = scW.sin().multiply(tDays.multiply(elements.getLatS1()).add(elements.getLatS()));
        return cosineTerm.add(sineTerm);
    }

    /**
     * Computes the satellite's orbit radius.
     *
     * @param wDegreesPerDay W angle in degrees/day
     * @param scW            sin/cos of the W angle
     * @return the satellite's orbit radius in kilometers
     */
    private FieldUnivariateDerivative2<T> computeSatelliteRadiusKilometers(final T wDegreesPerDay, final FieldSinCos<FieldUnivariateDerivative2<T>> scW) {
        final T coefficient = elements.getLm1()
                                      .multiply(2.0)
                                      .divide(wDegreesPerDay.subtract(elements.getLm1()).multiply(3.0))
                                      .negate()
                                      .add(1.0)
                                      .multiply(IntelsatElevenElements.SYNCHRONOUS_RADIUS_KM);
        return scW.sin()
                  .multiply(elements.getLonC().multiply(IntelsatElevenElements.K))
                  .add(1.0)
                  .subtract(scW.cos().multiply(elements.getLonS().multiply(IntelsatElevenElements.K)))
                  .multiply(coefficient);
    }

    /**
     * Get the computed satellite's east longitude.
     *
     * @return the satellite's east longitude in degrees
     */
    public FieldUnivariateDerivative2<T> getEastLongitudeDegrees() {
        return eastLongitudeDegrees;
    }

    /**
     * Get the computed satellite's geocentric latitude.
     *
     * @return the satellite's geocentric latitude in degrees
     */
    public FieldUnivariateDerivative2<T> getGeocentricLatitudeDegrees() {
        return geocentricLatitudeDegrees;
    }

    /**
     * Get the computed satellite's orbit.
     *
     * @return satellite's orbit radius in meters
     */
    public FieldUnivariateDerivative2<T> getOrbitRadius() {
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
     * {@inheritDoc}.
     */
    @Override
    public List<ParameterDriver> getParametersDrivers() {
        return Collections.emptyList();
    }

    /**
     * Get the Intelsat's 11 elements used by the propagator.
     *
     * @return the Intelsat's 11 elements used by the propagator
     */
    public getIntelsatElevenElements() {
        return elements;
    }
}
