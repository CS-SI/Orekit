/* Copyright 2002-2015 CS Systèmes d'Information
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
import org.orekit.errors.OrekitMessages;
import org.orekit.estimation.Parameter;
import org.orekit.propagation.SpacecraftState;
import org.orekit.time.AbsoluteDate;

/** Abstract class handling measurements boilerplate.
 * @author Luc Maisonobe
 * @since 7.1
 */
public abstract class AbstractMeasurement implements Measurement {

    /** List of the supported parameters. */
    private final List<Parameter> supportedParameters;

    /** Date of the measurement. */
    private final AbsoluteDate date;

    /** Observed value. */
    private final double[] observed;

    /** Theoretical standard deviation. */
    private final double[] sigma;

    /** Base weight. */
    private final double[] baseWeight;

    /** Modifiers that apply to the measurement.*/
    private final List<EvaluationModifier> modifiers;

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
     */
    protected AbstractMeasurement(final AbsoluteDate date, final double observed,
                                  final double sigma, final double baseWeight) {
        this.supportedParameters = new ArrayList<Parameter>();
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
        this.modifiers = new ArrayList<EvaluationModifier>();
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
     */
    protected AbstractMeasurement(final AbsoluteDate date, final double[] observed,
                                  final double[] sigma, final double[] baseWeight) {
        this.supportedParameters = new ArrayList<Parameter>();
        this.date       = date;
        this.observed   = observed.clone();
        this.sigma      = sigma.clone();
        this.baseWeight = baseWeight.clone();
        this.modifiers = new ArrayList<EvaluationModifier>();
        setEnabled(true);
    }

    /** Add a supported parameter.
     * @param parameter supported parameter
     * @exception OrekitException if a parameter with the same name already exists
     */
    protected void addSupportedParameter(final Parameter parameter)
        throws OrekitException {

        // compare against existing parameters
        for (final Parameter existing : supportedParameters) {
            if (existing.getName().equals(parameter.getName())) {
                if (existing == parameter) {
                    // the parameter was already known
                    return;
                } else {
                    // we have two different parameters sharing the same name
                    throw new OrekitException(OrekitMessages.DUPLICATED_PARAMETER_NAME,
                                              parameter.getName());
                }
            }
        }

        // it is a new parameter
        supportedParameters.add(parameter);

    }

    /** {@inheritDoc} */
    @Override
    public List<Parameter> getSupportedParameters() {
        if (modifiers.isEmpty()) {
            // no modifiers, we already know all the parameters
            return Collections.unmodifiableList(supportedParameters);
        } else {
            // we have to combine the measurement parameters and the modifiers parameters
            final List<Parameter> parameters = new ArrayList<Parameter>();
            parameters.addAll(supportedParameters);
            for (final EvaluationModifier modifier : modifiers) {
                parameters.addAll(modifier.getSupportedParameters());
            }
            return parameters;
        }
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

    /** Compute the theoretical value.
     * <p>
     * The theoretical value does not have <em>any</em> modifiers applied.
     * </p>
     * @param iteration iteration number
     * @param state orbital state at measurement date
     * @return theoretical value
     * @exception OrekitException if value cannot be computed
     * @see #evaluate(SpacecraftStatet)
     */
    protected abstract Evaluation theoreticalEvaluation(final int iteration,
                                                        final SpacecraftState state)
        throws OrekitException;

    /** {@inheritDoc} */
    @Override
    public Evaluation evaluate(final int iteration, final SpacecraftState state)
        throws OrekitException {

        // compute the theoretical value
        final Evaluation evaluation = theoreticalEvaluation(iteration, state);

        // apply the modifiers
        // FIXME we should check the modifier is consistent with the current measure. E.g do not mix a range-rate modifier with a range measurement.
        for (final EvaluationModifier modifier : modifiers) {
            modifier.modify(evaluation);
        }

        return evaluation;

    }

    /** {@inheritDoc} */
    @Override
    public AbsoluteDate getDate() {
        return date;
    }

    /** {@inheritDoc} */
    @Override
    public double[] getObservedValue() {
        return observed;
    }

    /** {@inheritDoc} */
    @Override
    public void addModifier(final EvaluationModifier modifier) {
        modifiers.add(modifier);
    }

    /** {@inheritDoc} */
    @Override
    public List<EvaluationModifier> getModifiers() {
        return Collections.unmodifiableList(modifiers);
    }

}
