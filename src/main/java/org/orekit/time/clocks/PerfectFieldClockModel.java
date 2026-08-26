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
import org.hipparchus.Field;

/** Clock model for perfect clock with constant zero offset.
 * @param <T> type of the field elements
 * @author Luc Maisonobe
 * @since 14.0
 */
public class PerfectFieldClockModel<T extends CalculusFieldElement<T>>
    extends ConstantFieldClockModel<T> {

    /** Simple constructor.
     * @param field field to which offset belong
     */
    public PerfectFieldClockModel(final Field<T> field) {
        super(field.getZero());
    }

    /** {@inheritDoc} */
    @Override
    public PerfectClockModel toNonField() {
        return new PerfectClockModel();
    }

}
