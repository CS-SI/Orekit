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
package org.orekit.estimation.measurements;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.analysis.differentiation.Gradient;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.FastMath;
import org.orekit.propagation.SpacecraftState;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.utils.Constants;
import org.orekit.utils.ParameterDriver;
import org.orekit.utils.TimeStampedFieldPVCoordinates;
import org.orekit.utils.TimeStampedPVCoordinates;

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
    private final double[] observed;

    /** Theoretical standard deviation. */
    private final double[] sigma;

    /** Base weight. */
    private final double[] baseWeight;

    /** Modifiers that apply to the measurement.*/
    private final List<EstimationModifier<T>> modifiers;

    /** Enabling status. */
    private boolean enabled;

    /** Simple constructor for mono-dimensional measurements.
     * <p>
     * At construction, a measurement is enabled.
     * </p>
     * @param date date of the measurement
     * @param observed observed value
     * @param sigma theoretical standard deviation
     * @param baseWeight base weight
     * @param satellites satellites related to this measurement
     * @since 9.3
     */
    protected AbstractMeasurement(final AbsoluteDate date, final double observed,
                                  final double sigma, final double baseWeight,
                                  final List<ObservableSatellite> satellites) {

        this.supportedParameters = new ArrayList<ParameterDriver>();

        this.date       = date;
        this.observed   = new double[] {
            observed
        };
        this.sigma      = new double[] {
            sigma
        };
        this.baseWeight = new double[] {
            baseWeight
        };

        this.satellites = satellites;

        this.modifiers = new ArrayList<EstimationModifier<T>>();
        setEnabled(true);

    }

    /** Simple constructor, for multi-dimensional measurements.
     * <p>
     * At construction, a measurement is enabled.
     * </p>
     * @param date date of the measurement
     * @param observed observed value
     * @param sigma theoretical standard deviation
     * @param baseWeight base weight
     * @param satellites satellites related to this measurement
     * @since 9.3
     */
    protected AbstractMeasurement(final AbsoluteDate date, final double[] observed,
                                  final double[] sigma, final double[] baseWeight,
                                  final List<ObservableSatellite> satellites) {
        this.supportedParameters = new ArrayList<ParameterDriver>();

        this.date       = date;
        this.observed   = observed.clone();
        this.sigma      = sigma.clone();
        this.baseWeight = baseWeight.clone();

        this.satellites = satellites;

        this.modifiers = new ArrayList<EstimationModifier<T>>();
        setEnabled(true);

    }

    /** Add a parameter driver.
     * @param driver parameter driver to add
     * @since 9.3
     */
    protected void addParameterDriver(final ParameterDriver driver) {
        supportedParameters.add(driver);
    }

    /** {@inheritDoc} */
    @Override
    public List<ParameterDriver> getParametersDrivers() {
        return Collections.unmodifiableList(supportedParameters);
    }

    /** {@inheritDoc} */
    @Override
    public void setEnabled(final boolean enabled) {
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
    protected abstract EstimatedMeasurementBase<T> theoreticalEvaluationWithoutDerivatives(int iteration, int evaluation,
                                                                                           SpacecraftState[] states);

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

    /** Compute propagation delay on a link leg (typically downlink or uplink).
     * @param adjustableEmitterPV position/velocity of emitter that may be adjusted
     * @param receiverPosition fixed position of receiver at {@code signalArrivalDate},
     * in the same frame as {@code adjustableEmitterPV}
     * @param signalArrivalDate date at which the signal arrives to receiver
     * @return <em>positive</em> delay between signal emission and signal reception dates
     */
    public static double signalTimeOfFlight(final TimeStampedPVCoordinates adjustableEmitterPV,
                                            final Vector3D receiverPosition,
                                            final AbsoluteDate signalArrivalDate) {

        // initialize emission date search loop assuming the state is already correct
        // this will be true for all but the first orbit determination iteration,
        // and even for the first iteration the loop will converge very fast
        final double offset = signalArrivalDate.durationFrom(adjustableEmitterPV.getDate());
        double delay = offset;

        // search signal transit date, computing the signal travel in inertial frame
        final double cReciprocal = 1.0 / Constants.SPEED_OF_LIGHT;
        double delta;
        int count = 0;
        do {
            final double previous   = delay;
            final Vector3D transitP = adjustableEmitterPV.shiftedBy(offset - delay).getPosition();
            delay                   = receiverPosition.distance(transitP) * cReciprocal;
            delta                   = FastMath.abs(delay - previous);
        } while (count++ < 10 && delta >= 2 * FastMath.ulp(delay));

        return delay;

    }

    /** Compute propagation delay on a link leg (typically downlink or uplink).
     * @param adjustableEmitterPV position/velocity of emitter that may be adjusted
     * @param receiverPosition fixed position of receiver at {@code signalArrivalDate},
     * in the same frame as {@code adjustableEmitterPV}
     * @param signalArrivalDate date at which the signal arrives to receiver
     * @return <em>positive</em> delay between signal emission and signal reception dates
     * @param <T> the type of the components
     */
    public static <T extends CalculusFieldElement<T>> T signalTimeOfFlight(final TimeStampedFieldPVCoordinates<T> adjustableEmitterPV,
                                                                           final FieldVector3D<T> receiverPosition,
                                                                           final FieldAbsoluteDate<T> signalArrivalDate) {

        // Initialize emission date search loop assuming the emitter PV is almost correct
        // this will be true for all but the first orbit determination iteration,
        // and even for the first iteration the loop will converge extremely fast
        final T offset = signalArrivalDate.durationFrom(adjustableEmitterPV.getDate());
        T delay = offset;

        // search signal transit date, computing the signal travel in the frame shared by emitter and receiver
        final double cReciprocal = 1.0 / Constants.SPEED_OF_LIGHT;
        double delta;
        int count = 0;
        do {
            final double previous           = delay.getReal();
            final FieldVector3D<T> transitP = adjustableEmitterPV.shiftedBy(delay.negate().add(offset)).getPosition();
            delay                           = receiverPosition.distance(transitP).multiply(cReciprocal);
            delta                           = FastMath.abs(delay.getReal() - previous);
        } while (count++ < 10 && delta >= 2 * FastMath.ulp(delay.getReal()));

        return delay;

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
                        new FieldVector3D<>(Gradient.variable(freeParameters, firstDerivative + 0, p.getX()),
                                            Gradient.variable(freeParameters, firstDerivative + 1, p.getY()),
                                            Gradient.variable(freeParameters, firstDerivative + 2, p.getZ()));

        // Velocity of the satellite expressed as a gradient
        // The components of the velocity are the 3 second derivative parameters
        final Vector3D v = state.getPVCoordinates().getVelocity();
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
