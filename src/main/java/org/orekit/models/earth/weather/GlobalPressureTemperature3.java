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
package org.orekit.models.earth.weather;

import java.io.IOException;

import org.hipparchus.CalculusFieldElement;
import org.orekit.data.DataSource;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.time.TimeScale;

/** The Global Pressure and Temperature 3 (GPT3) model.
 * <p>
 * This model adds horizontal gradient data to {@link GlobalPressureTemperature2w GPT2w}.
 * </p>
 * @author Luc Maisonobe
 * @since 12.1
 */
public class GlobalPressureTemperature3 extends AbstractGlobalPressureTemperature {

    /** UTC time scale. */
    private final TimeScale utc;

    /**
     * Constructor with source of GPT3 auxiliary data given by user.
     *
     * @param source grid data source
     * @param utc UTC time scale.
     * @exception IOException if grid data cannot be read
     */
    public GlobalPressureTemperature3(final DataSource source, final TimeScale utc)
        throws IOException {
        super(source,
              SeasonalModelType.PRESSURE,
              SeasonalModelType.TEMPERATURE,
              SeasonalModelType.QV,
              SeasonalModelType.DT,
              SeasonalModelType.AH,
              SeasonalModelType.AW,
              SeasonalModelType.LAMBDA,
              SeasonalModelType.TM,
              SeasonalModelType.GN_H,
              SeasonalModelType.GE_H,
              SeasonalModelType.GN_W,
              SeasonalModelType.GE_W);

        this.utc = utc;

    }

    /** {@inheritDoc} */
    @Override
    protected double deltaRef(final AbsoluteDate date) {
        final AbsoluteDate reference =
            new AbsoluteDate(date.getComponents(utc).getDate().getYear() - 1, 12, 31, utc);
        return date.durationFrom(reference);
    }

    /** {@inheritDoc} */
    @Override
    protected <T extends CalculusFieldElement<T>> T deltaRef(final FieldAbsoluteDate<T> date) {
        final FieldAbsoluteDate<T> reference =
            new FieldAbsoluteDate<>(date.getField(), date.getComponents(utc).getDate().getYear() - 1, 12, 31, utc);
        return date.durationFrom(reference);
    }

}
