/* Copyright 2022-2025 Thales Alenia Space
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
package org.orekit.time;

import org.hipparchus.CalculusFieldElement;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.utils.TimeSpanMap;

/** Offset clock model aggregating several other clock models.
 * @author Luc Maisonobe
 * @since 12.1
 */
public class AggregatedClockModel implements ClockModel {

    /** Underlying clock models. */
    private final TimeSpanMap<ClockModel> models;

    /** Simple constructor.
     * @param models underlying clock models
     */
    public AggregatedClockModel(final TimeSpanMap<ClockModel> models) {
        this.models = models;
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
    public ClockOffset getOffset(final AbsoluteDate date) {
        return getModel(date).getOffset(date);
    }

    /** {@inheritDoc} */
    @Override
    public <T extends CalculusFieldElement<T>> FieldClockOffset<T> getOffset(final FieldAbsoluteDate<T> date) {
        return getModel(date.toAbsoluteDate()).getOffset(date);
    }

    /** Get the model valid at specified date.
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
