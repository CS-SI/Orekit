/* Copyright 2002-2026 CS GROUP
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
package org.orekit.estimation.measurements;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.hipparchus.analysis.differentiation.Gradient;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.orekit.estimation.measurements.signal.SignalTravelTimeModel;
import org.orekit.propagation.SpacecraftState;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.ParameterDriver;
import org.orekit.utils.TimeStampedFieldPVCoordinates;

/** Abstract class handling measurements boilerplate.
 * @param <T> the type of the measurement
 * @author Luc Maisonobe
 * @since 8.0
 */
public abstract class AbstractMeasurement<T extends ObservedMeasurement<T>> implements ObservedMeasurement<T> {

    /** List of the supported parameters. */
    private final List<ParameterDriver> supportedParameters;

    /** Satellites related to this measurement.
     * @since 9.3
     */
    private final List<ObservableSatellite> satellites;

    /** Date of the measurement. */
    private final AbsoluteDate date;

    /** Observed value. */
    private double[] observed;

    /** Theoretical standard deviation. */
    private final double[] sigma;

    /** Base weight. */
    private final double[] baseWeight;

    /** Modifiers that apply to the measurement.*/
    private final List<EstimationModifier<T>> modifiers;

    /** Whether measurement is two-way or not (true for two-way). */
    private final boolean isTwoWay;

    /** Signal travel time model. */
    private final SignalTravelTimeModel signalTravelTimeModel;

    /** Enabling status. */
    private boolean enabled;

    /** Simple constructor for mono-dimensional measurements.
     * <p>
     * At construction, a measurement is enabled.
     * </p>
     * @param date date of the measurement
     * @param isTwoWay true for two-way measurement
     * @param observed observed value
     * @param sigma theoretical standard deviation
     * @param baseWeight base weight
     * @param satellites satellites related to this measurement
     * @since 14.0
     */
    protected AbstractMeasurement(final AbsoluteDate date, final boolean isTwoWay, final double observed,
                                  final double sigma, final double baseWeight,
                                  final List<ObservableSatellite> satellites) {
        this(date, isTwoWay, new double[] {observed}, new double[] {sigma}, new double[] {baseWeight}, satellites);
    }

    /** Simple constructor, for multi-dimensional measurements.
     * <p>
     * At construction, a measurement is enabled.
     * </p>
     * @param date date of the measurement
     * @param isTwoWay true for two-way measurement
     * @param observed observed value
     * @param sigma theoretical standard deviation
     * @param baseWeight base weight
     * @param satellites satellites related to this measurement
     * @since 14.0
     */
    protected AbstractMeasurement(final AbsoluteDate date, final boolean isTwoWay, final double[] observed,
                                  final double[] sigma, final double[] baseWeight,
                                  final List<ObservableSatellite> satellites) {
        this(date, isTwoWay, observed, sigma, baseWeight, new SignalTravelTimeModel(), satellites);
    }

    /** Simple constructor, for multi-dimensional measurements.
     * <p>
     * At construction, a measurement is enabled.
     * </p>
     * @param date date of the measurement
     * @param isTwoWay true for two-way measurement
     * @param observed observed value
     * @param sigma theoretical standard deviation
     * @param baseWeight base weight
     * @param signalTravelTimeModel signal travel time model
     * @param satellites satellites related to this measurement
     * @since 14.0
     */
    protected AbstractMeasurement(final AbsoluteDate date, final boolean isTwoWay, final double[] observed,
                                  final double[] sigma, final double[] baseWeight,
                                  final SignalTravelTimeModel signalTravelTimeModel,
                                  final List<ObservableSatellite> satellites) {
        this.supportedParameters = new ArrayList<>();

        this.date       = date;
        this.isTwoWay   = isTwoWay;
        this.observed   = observed.clone();
        this.sigma      = sigma.clone();
        this.baseWeight = baseWeight.clone();
        this.signalTravelTimeModel = signalTravelTimeModel;
        this.satellites = satellites;

        // Add parameter drivers
        satellites.forEach(s -> addParametersDrivers(s.getParametersDrivers()));

        this.modifiers = new ArrayList<>();
        setEnabled(true);

    }

    /** {@inheritDoc} */
    @Override
    public boolean isTwoWay() {
        return isTwoWay;
    }

    /** {@inheritDoc} */
    @Override
    public void setObservedValue(final double[] newObserved) {
        this.observed = newObserved.clone();
    }

    /** Add a parameter driver.
     * @param driver parameter driver to add
     * @since 9.3
     */
    protected void addParameterDriver(final ParameterDriver driver) {
        supportedParameters.add(driver);
    }

    /** Add a list of parameter drivers all at once.
     * @param drivers list of parameter drivers to add
     * @since 14.0
     */
    protected void addParametersDrivers(final List<ParameterDriver> drivers) {
        for (final ParameterDriver driver : drivers) {
            addParameterDriver(driver);
        }
    }

    /** {@inheritDoc} */
    @Override
    public List<ParameterDriver> getParametersDrivers() {
        return Collections.unmodifiableList(supportedParameters);
    }

    /**
     * Getter for the signal travel time model.
     * @return signal model
     * @since 14.0
     */
    public SignalTravelTimeModel getSignalTravelTimeModel() {
        return signalTravelTimeModel;
    }

    /** {@inheritDoc} */
    @Override
    public final void setEnabled(final boolean enabled) {
        this.enabled = enabled;
    }

    /** {@inheritDoc} */
    @Override
    public boolean isEnabled() {
        return enabled;
    }

    /** {@inheritDoc} */
    @Override
    public int getDimension() {
        return observed.length;
    }

    /** {@inheritDoc} */
    @Override
    public double[] getTheoreticalStandardDeviation() {
        return sigma.clone();
    }

    /** {@inheritDoc} */
    @Override
    public double[] getBaseWeight() {
        return baseWeight.clone();
    }

    /** {@inheritDoc} */
    @Override
    public List<ObservableSatellite> getSatellites() {
        return satellites;
    }

    /** Estimate the theoretical value without derivatives.
     * The default implementation uses the computation with derivatives and ought to be overwritten for performance.
     * <p>
     * The theoretical value does not have <em>any</em> modifiers applied.
     * </p>
     * @param iteration iteration number
     * @param evaluation evaluation number
     * @param states orbital states at measurement date
     * @return theoretical value
     * @see #estimate(int, int, SpacecraftState[])
     * @since 12.0
     */
    protected EstimatedMeasurementBase<T> theoreticalEvaluationWithoutDerivatives(final int iteration,
                                                                                  final int evaluation,
                                                                                  final SpacecraftState[] states) {
        final EstimatedMeasurement<T> estimatedMeasurement = theoreticalEvaluation(iteration, evaluation, states);
        final EstimatedMeasurementBase<T> estimatedMeasurementBase = new EstimatedMeasurementBase<>(estimatedMeasurement.getObservedMeasurement(),
                iteration, evaluation, states, estimatedMeasurement.getParticipants());
        estimatedMeasurementBase.setEstimatedValue(estimatedMeasurement.getEstimatedValue());
        estimatedMeasurementBase.setStatus(estimatedMeasurement.getStatus());
        return estimatedMeasurementBase;
    }

    /** Estimate the theoretical value.
     * <p>
     * The theoretical value does not have <em>any</em> modifiers applied.
     * </p>
     * @param iteration iteration number
     * @param evaluation evaluation number
     * @param states orbital states at measurement date
     * @return theoretical value
     * @see #estimate(int, int, SpacecraftState[])
     */
    protected abstract EstimatedMeasurement<T> theoreticalEvaluation(int iteration, int evaluation, SpacecraftState[] states);

    /** {@inheritDoc} */
    @Override
    public EstimatedMeasurementBase<T> estimateWithoutDerivatives(final int iteration, final int evaluation, final SpacecraftState[] states) {

        // compute the theoretical value
        final EstimatedMeasurementBase<T> estimation = theoreticalEvaluationWithoutDerivatives(iteration, evaluation, states);

        // apply the modifiers
        for (final EstimationModifier<T> modifier : modifiers) {
            modifier.modifyWithoutDerivatives(estimation);
        }

        return estimation;

    }

    /** {@inheritDoc} */
    @Override
    public EstimatedMeasurement<T> estimate(final int iteration, final int evaluation, final SpacecraftState[] states) {

        // compute the theoretical value
        final EstimatedMeasurement<T> estimation = theoreticalEvaluation(iteration, evaluation, states);

        // apply the modifiers
        for (final EstimationModifier<T> modifier : modifiers) {
            modifier.modify(estimation);
        }

        return estimation;

    }

    /** {@inheritDoc} */
    @Override
    public AbsoluteDate getDate() {
        return date;
    }

    /** {@inheritDoc} */
    @Override
    public double[] getObservedValue() {
        return observed.clone();
    }

    /** {@inheritDoc} */
    @Override
    public void addModifier(final EstimationModifier<T> modifier) {

        // combine the measurement parameters and the modifier parameters
        supportedParameters.addAll(modifier.getParametersDrivers());

        modifiers.add(modifier);

    }

    /** {@inheritDoc} */
    @Override
    public List<EstimationModifier<T>> getModifiers() {
        return Collections.unmodifiableList(modifiers);
    }

    /** Get Cartesian coordinates as derivatives.
     * <p>
     * The position will correspond to variables {@code firstDerivative},
     * {@code firstDerivative + 1} and {@code firstDerivative + 2}.
     * The velocity will correspond to variables {@code firstDerivative + 3},
     * {@code firstDerivative + 4} and {@code firstDerivative + 5}.
     * The acceleration will correspond to constants.
     * </p>
     * @param state state of the satellite considered
     * @param firstDerivative index of the first derivative
     * @param freeParameters total number of free parameters in the gradient
     * @return Cartesian coordinates as derivatives
     * @since 10.2
     */
    public static TimeStampedFieldPVCoordinates<Gradient> getCoordinates(final SpacecraftState state,
                                                                         final int firstDerivative,
                                                                         final int freeParameters) {

        // Position of the satellite expressed as a gradient
        // The components of the position are the 3 first derivative parameters
        final Vector3D p = state.getPosition();
        final FieldVector3D<Gradient> pDS =
                        new FieldVector3D<>(Gradient.variable(freeParameters, firstDerivative,     p.getX()),
                                            Gradient.variable(freeParameters, firstDerivative + 1, p.getY()),
                                            Gradient.variable(freeParameters, firstDerivative + 2, p.getZ()));

        // Velocity of the satellite expressed as a gradient
        // The components of the velocity are the 3 second derivative parameters
        final Vector3D v = state.getVelocity();
        final FieldVector3D<Gradient> vDS =
                        new FieldVector3D<>(Gradient.variable(freeParameters, firstDerivative + 3, v.getX()),
                                            Gradient.variable(freeParameters, firstDerivative + 4, v.getY()),
                                            Gradient.variable(freeParameters, firstDerivative + 5, v.getZ()));

        // Acceleration of the satellite
        // The components of the acceleration are not derivative parameters
        final Vector3D a = state.getPVCoordinates().getAcceleration();
        final FieldVector3D<Gradient> aDS =
                        new FieldVector3D<>(Gradient.constant(freeParameters, a.getX()),
                                            Gradient.constant(freeParameters, a.getY()),
                                            Gradient.constant(freeParameters, a.getZ()));

        return new TimeStampedFieldPVCoordinates<>(state.getDate(), pDS, vDS, aDS);

    }

}
