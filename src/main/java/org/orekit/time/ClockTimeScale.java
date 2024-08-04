/* Copyright 2002-2024 Thales Alenia Space
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

/** Time scale with clock offset from another time scale.
 * @author Luc Maisonobe
 * @since 12.1
 */
public class ClockTimeScale implements TimeScale {

    /** Serializable UID. */
    private static final long serialVersionUID = 20240720L;

    /** Name of the time scale. */
    private final String name;

    /** Reference time scale. */
    private final TimeScale reference;

    /** Clock offset model. */
    private final transient ClockModel clockModel;

    /** Simple constructor.
     * @param name name of the time scale
     * @param reference reference time scale
     * @param clockModel clock offset model
     */
    public ClockTimeScale(final String name,
                          final TimeScale reference,
                          final ClockModel clockModel) {
        this.name       = name;
        this.reference  = reference;
        this.clockModel = clockModel;
    }

    /** {@inheritDoc} */
    @Override
    public SplitTime offsetFromTAI(final AbsoluteDate date) {
        return reference.offsetFromTAI(date).add(new SplitTime(clockModel.getOffset(date).getOffset()));
    }

    /** {@inheritDoc} */
    @Override
    public <T extends CalculusFieldElement<T>> T offsetFromTAI(final FieldAbsoluteDate<T> date) {
        return reference.offsetFromTAI(date).add(clockModel.getOffset(date).getOffset());
    }

    /** {@inheritDoc} */
    @Override
    public String getName() {
        return name;
    }

}
