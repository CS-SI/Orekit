/* Copyright 2022-2026 Thales Alenia Space
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
package org.orekit.time.clocks;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.DoubleFunction;

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.Field;
import org.hipparchus.analysis.differentiation.Gradient;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.drivers.ParameterDriver;
import org.orekit.utils.TimeSpanMap;

/**
 * Offset clock model aggregating several other clock models.
 *
 * @author Luc Maisonobe
 * @since 12.1
 */
public class AggregatedClockModel implements ClockModel {

    /** Underlying clock models. */
    private final TimeSpanMap<ClockModel> models;

    /** Cached field-based models.
     * @since 14.0
     */
    private final Map<Field<? extends CalculusFieldElement<?>>, AggregatedFieldClockModel<?>> fieldModels;

    /** Simple constructor.
     * @param models underlying clock models
     */
    public AggregatedClockModel(final TimeSpanMap<ClockModel> models) {

        try {
            // we ignore the result, we just want to check some data is present
            models.getFirstNonNullSpan();
        } catch (OrekitException oe) {
            throw new OrekitException(oe, OrekitMessages.NOT_ENOUGH_DATA, 0);
        }

        this.models      = models;
        this.fieldModels = new HashMap<>();
    }

    /** Get the underlying models.
     * @return underlying models
     */
    public TimeSpanMap<ClockModel> getModels() {
        return models;
    }

    /** {@inheritDoc} */
    @Override
    public AbsoluteDate getValidityStart() {
        return models.getFirstNonNullSpan().getStart();
    }

    /** {@inheritDoc} */
    @Override
    public AbsoluteDate getValidityEnd() {
        return models.getLastNonNullSpan().getEnd();
    }

    /** {@inheritDoc} */
    @Override
    public List<ParameterDriver> getParametersDrivers() {
        final List<ParameterDriver> drivers = new ArrayList<>();
        models.forEach(model -> drivers.addAll(model.getParametersDrivers()));
        return drivers;
    }

    /** {@inheritDoc} */
    @Override
    public ClockOffset getOffset(final AbsoluteDate date) {
        return getModel(date).getOffset(date);
    }

    /** {@inheritDoc} */
    @Override
    @SuppressWarnings("unchecked")
    public <T extends CalculusFieldElement<T>> AggregatedFieldClockModel<T> toField(final DoubleFunction<T> converter) {
        // build aggregated models may be costly, so we cache the results
        return (AggregatedFieldClockModel<T>) fieldModels.computeIfAbsent(converter.apply(0.0).getField(),
                                                                          f -> buildFieldModel(converter));
    }

    /**
     * Build a field model.
     * @param <T> type of the field elements
     * @param converter converter to field elements
     * @return field version of the instance
     * @since 14.0
     */
    private <T extends CalculusFieldElement<T>> AggregatedFieldClockModel<T> buildFieldModel(final DoubleFunction<T> converter) {
        final TimeSpanMap<FieldClockModel<T>> fieldMap = new TimeSpanMap<>(null);
        for (TimeSpanMap.Span<ClockModel> span = models.getFirstSpan(); span != null; span = span.next()) {
            fieldMap.addValidBetween(span.getData() == null ? null : span.getData().toField(converter),
                                     span.getStart(), span.getEnd());
        }
        return new AggregatedFieldClockModel<>(fieldMap);
    }

    /** {@inheritDoc} */
    @Override
    public AggregatedFieldClockModel<Gradient> toGradient(final int freeParameters, final Map<String, Integer> indices) {
        final TimeSpanMap<FieldClockModel<Gradient>> fieldMap = new TimeSpanMap<>(null);
        for (TimeSpanMap.Span<ClockModel> span = models.getFirstSpan(); span != null; span = span.next()) {
            fieldMap.addValidBetween(span.getData() == null ?
                                     null :
                                     span.getData().toGradient(freeParameters, indices),
                                     span.getStart(), span.getEnd());
        }
        return new AggregatedFieldClockModel<>(fieldMap);
    }

    /**
     * Get the model valid at specified date.
     *
     * @param date date for which model is requested
     * @return clock model valid at date
     */
    private ClockModel getModel(final AbsoluteDate date) {
        final ClockModel clockModel = models.get(date);
        if (clockModel == null) {
            // this may happen if map is limited or not contiguous
            // typically for models retrieved from SP3Ephemeris
            throw new OrekitException(OrekitMessages.NO_DATA_GENERATED, date);
        }
        return clockModel;
    }

}
