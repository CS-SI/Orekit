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

import org.hipparchus.CalculusFieldElement;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.utils.TimeSpanMap;

/**
 * Offset clock model aggregating several other clock models.
 *
 * @param <T> type of the field elements
 * @author Luc Maisonobe
 * @since 14.0
 */
public class AggregatedFieldClockModel<T extends CalculusFieldElement<T>>
    implements FieldClockModel<T> {

    /** Underlying clock models. */
    private final TimeSpanMap<FieldClockModel<T>> models;

    /** Non-field version of the model. */
    private final AggregatedClockModel nonField;

    /** Simple constructor.
     * @param models underlying clock models
     */
    public AggregatedFieldClockModel(final TimeSpanMap<FieldClockModel<T>> models) {

        try {
            // we ignore the result, we just want to check some data is present
            models.getFirstNonNullSpan();
        } catch (OrekitException oe) {
            throw new OrekitException(oe, OrekitMessages.NOT_ENOUGH_DATA, 0);
        }

        this.models = models;

        // create non-field version
        final TimeSpanMap<ClockModel> nonFieldMap = new TimeSpanMap<>(null);
        for (TimeSpanMap.Span<FieldClockModel<T>> span = models.getFirstSpan(); span != null; span = span.next()) {
            nonFieldMap.addValidBetween(span.getData() == null ? null : span.getData().toNonField(),
                                        span.getStart(), span.getEnd());
        }
        this.nonField = new AggregatedClockModel(nonFieldMap);

    }

    /** Get the underlying models.
     * @return underlying models
     */
    public TimeSpanMap<FieldClockModel<T>> getModels() {
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
    public FieldClockOffset<T> getOffset(final FieldAbsoluteDate<T> date) {
        return getModel(date).getOffset(date);
    }

    /** {@inheritDoc} */
    @Override
    public AggregatedClockModel toNonField() {
        return nonField;
    }

    /**
     * Get the model valid at specified date.
     *
     * @param date date for which model is requested
     * @return clock model valid at date
     */
    private FieldClockModel<T> getModel(final FieldAbsoluteDate<T> date) {
        final FieldClockModel<T> clockModel = models.get(date.toAbsoluteDate());
        if (clockModel == null) {
            // this may happen if map is limited or not contiguous
            // typically for models retrieved from SP3Ephemeris
            throw new OrekitException(OrekitMessages.NO_DATA_GENERATED, date);
        }
        return clockModel;
    }

}
