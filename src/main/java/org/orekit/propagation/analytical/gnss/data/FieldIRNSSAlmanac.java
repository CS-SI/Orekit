/* Copyright 2002-2024 Luc Maisonobe
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
package org.orekit.propagation.analytical.gnss.data;

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.Field;

/**
 * Class for IRNSS almanac.
 *
 * @see "Indian Regional Navigation Satellite System, Signal In Space ICD
 *       for standard positioning service, version 1.1 - Table 28"
 *
 * @param <T> type of the field elements
 * @author Luc Maisonobe
 * @since 13.0
 *
 */
public class FieldIRNSSAlmanac<T extends CalculusFieldElement<T>>
    extends FieldAbstractAlmanac<T, IRNSSAlmanac> {

    /** Constructor from non-field instance.
     * @param field    field to which elements belong
     * @param original regular non-field instance
     */
    public FieldIRNSSAlmanac(final Field<T> field, final IRNSSAlmanac original) {
        super(field, original);
    }

    /** {@inheritDoc} */
    @Override
    public IRNSSAlmanac toNonField() {
        return new IRNSSAlmanac(this);
    }

    /**
     * Setter for the Square Root of Semi-Major Axis (m^1/2).
     * <p>
     * In addition, this method set the value of the Semi-Major Axis.
     * </p>
     * @param sqrtA the Square Root of Semi-Major Axis (m^1/2)
     */
    public void setSqrtA(final T sqrtA) {
        setSma(sqrtA.square());
    }

}
