/* Copyright 2002-2016 CS Systèmes d'Information
 * Licensed to CS Systèmes d'Information (CS) under one or more
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

import org.orekit.errors.OrekitException;
import org.orekit.propagation.SpacecraftState;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.ParameterDriver;

/** Abstract class handling measurements boilerplate.
 * @param <T> the type of the measurement
 * @author Luc Maisonobe
 * @since 8.0
 */
public abstract class AbstractMeasurement<T extends ObservedMeasurement<T>>
    implements ObservedMeasurement<T> {

    /** List of the supported parameters. */
    private final List<ParameterDriver> supportedParameters;

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
     * @param supportedParameters supported parameters
     */
    protected AbstractMeasurement(final AbsoluteDate date, final double observed,
                                  final double sigma, final double baseWeight,
                                  final ParameterDriver ... supportedParameters) {

        this.supportedParameters = new ArrayList<ParameterDriver>(supportedParameters.length);
        for (final ParameterDriver parameterDriver : supportedParameters) {
            this.supportedParameters.add(parameterDriver);
        }

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
     * @param supportedParameters supported parameters
     */
    protected AbstractMeasurement(final AbsoluteDate date, final double[] observed,
                                  final double[] sigma, final double[] baseWeight,
                                  final ParameterDriver ... supportedParameters) {
        this.supportedParameters = new ArrayList<ParameterDriver>(supportedParameters.length);
        for (final ParameterDriver parameterDriver : supportedParameters) {
            this.supportedParameters.add(parameterDriver);
        }

        this.date       = date;
        this.observed   = observed.clone();
        this.sigma      = sigma.clone();
        this.baseWeight = baseWeight.clone();

        this.modifiers = new ArrayList<EstimationModifier<T>>();
        setEnabled(true);

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

    /** Estimate the theoretical value.
     * <p>
     * The theoretical value does not have <em>any</em> modifiers applied.
     * </p>
     * @param iteration iteration number
     * @param evaluation evaluation number
     * @param state orbital state at measurement date
     * @return theoretical value
     * @exception OrekitException if value cannot be computed
     * @see #estimate(int, int, SpacecraftState)
     */
    protected abstract EstimatedMeasurement<T> theoreticalEvaluation(int iteration, int evaluation,
                                                                     SpacecraftState state)
        throws OrekitException;

    /** {@inheritDoc} */
    @Override
    public EstimatedMeasurement<T> estimate(final int iteration, final int evaluation,
                                            final SpacecraftState state)
        throws OrekitException {

        // compute the theoretical value
        final EstimatedMeasurement<T> estimation = theoreticalEvaluation(iteration, evaluation, state);

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
    public void addModifier(final EstimationModifier<T> modifier)
        throws OrekitException {

        // combine the measurement parameters and the modifier parameters
        supportedParameters.addAll(modifier.getParametersDrivers());

        modifiers.add(modifier);

    }

    /** {@inheritDoc} */
    @Override
    public List<EstimationModifier<T>> getModifiers() {
        return Collections.unmodifiableList(modifiers);
    }

}
