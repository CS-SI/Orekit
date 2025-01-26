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

/** The Global Pressure and Temperature 2 (GPT2) model.
 * @author Luc Maisonobe
 * @since 12.1
 */
public class GlobalPressureTemperature2 extends AbstractGlobalPressureTemperature {

    /**
     * Constructor with source of GPT2 auxiliary data given by user.
     *
     * @param source grid data source (files with extra columns like GPT2w or GPT3 can be used here)
     * @exception IOException if grid data cannot be read
     */
    public GlobalPressureTemperature2(final DataSource source)
        throws IOException {
        super(source,
              SeasonalModelType.PRESSURE,
              SeasonalModelType.TEMPERATURE,
              SeasonalModelType.QV,
              SeasonalModelType.DT,
              SeasonalModelType.AH,
              SeasonalModelType.AW);
    }

    /** {@inheritDoc} */
    @Override
    protected double deltaRef(final AbsoluteDate date) {
        return date.durationFrom(AbsoluteDate.J2000_EPOCH);
    }

    /** {@inheritDoc} */
    @Override
    protected <T extends CalculusFieldElement<T>> T deltaRef(final FieldAbsoluteDate<T> date) {
        return date.durationFrom(FieldAbsoluteDate.getJ2000Epoch(date.getField()));
    }

}
