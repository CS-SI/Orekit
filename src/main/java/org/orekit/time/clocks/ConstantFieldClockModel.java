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
import org.orekit.time.FieldAbsoluteDate;

/** Field Clock model for a clock with constant offset.
 *
 * @author Brian Carter
 * @since 14.0
 */
public class ConstantFieldClockModel<T extends CalculusFieldElement<T>> extends AbstractFieldClockModel<T> {
    /**
     * Simple constructor.
     *
     * @param referenceDate reference date
     */
    public ConstantFieldClockModel(final FieldAbsoluteDate<T> referenceDate) {
        super(referenceDate);
    }

    /** {@inheritDoc} */
    @Override
    public FieldClockOffset<T> getOffset(final FieldAbsoluteDate<T> date) {
        final T zero = date.getField().getZero();
        return new FieldClockOffset<>(date, zero, zero, zero);
    }

}
