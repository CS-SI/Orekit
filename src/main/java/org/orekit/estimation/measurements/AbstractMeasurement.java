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
import java.util.SortedSet;

import org.orekit.errors.OrekitException;
import org.orekit.estimation.Parameter;
import org.orekit.propagation.SpacecraftState;
import org.orekit.time.AbsoluteDate;

/** Abstract class handling measurements boilerplate.
 * @author Luc Maisonobe
 * @since 7.1
 */
public abstract class AbstractMeasurement implements Measurement {

    /** Date of the measurement. */
    private final AbsoluteDate date;

    /** Observed value. */
    private final double[] observed;

    /** Modifiers that apply to the measurement.*/
    private final List<MeasurementModifier> modifiers;

    /** Enabling status. */
    private boolean enabled;

    /** Simple constructor.
     * <p>
     * At construction, a measurement is enabled.
     * </p>
     * @param date date of the measurement
     * @param observed observed value
     */
    public AbstractMeasurement(final AbsoluteDate date, final double[] observed) {
        this.date      = date;
        this.observed  = observed.clone();
        this.modifiers = new ArrayList<MeasurementModifier>();
        setEnabled(true);
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

    /** Get the theoretical value.
     * <p>
     * The theoretical value does not have <em>any</em> modifiers applied.
     * </p>
     * @param state orbital state at measurement date
     * @param parameters model parameters set
     * @return theoretical value (array of size {@link #getDimension()}
     * @exception OrekitException if value cannot be computed
     * @see #getSimulatedValue(SpacecraftState, SortedSet)
     */
    protected abstract double[] getTheoreticalValue(final SpacecraftState state,
                                                    final SortedSet<Parameter> parameters)
        throws OrekitException;

    /** {@inheritDoc} */
    @Override
    public double[] getSimulatedValue(final SpacecraftState state, final SortedSet<Parameter> parameters)
        throws OrekitException {

        // compute the theoretical value
        double[] value = getTheoreticalValue(state, parameters);

        // apply the modifiers
        for (final MeasurementModifier modifier : modifiers) {
            value = modifier.apply(state, parameters, value, this);
        }

        return value;

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
    public void addModifier(final MeasurementModifier modifier) {
        modifiers.add(modifier);
    }

    /** {@inheritDoc} */
    @Override
    public List<MeasurementModifier> getModifiers() {
        return Collections.unmodifiableList(modifiers);
    }

}
