/* Copyright 2002-2023 CS GROUP
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

/**
 * Class that associates a field with a date.
 *
 * @author Vincent Cucchietti
 * @see FieldAbsoluteDate
 * @see CalculusFieldElement
 * @param <KK> type of the field elements
 */
public class TimeStampedField<KK extends CalculusFieldElement<KK>> implements FieldTimeStamped<KK> {

    /** Date. */
    private final FieldAbsoluteDate<KK> date;

    /** Value. */
    private final KK value;

    /**
     * Constructor with normal date.
     *
     * @param value value
     * @param date date associated to value
     */
    public TimeStampedField(final KK value, final AbsoluteDate date) {
        this(value, new FieldAbsoluteDate<>(value.getField(), date));
    }

    /**
     * Constructor.
     *
     * @param value value
     * @param date date associated to value
     */
    public TimeStampedField(final KK value, final FieldAbsoluteDate<KK> date) {
        this.date  = date;
        this.value = value;
    }

    /** {@inheritDoc} */
    @Override
    public FieldAbsoluteDate<KK> getDate() {
        return date;
    }

    /** Get value.
     * @return value
     */
    public KK getValue() {
        return value;
    }

}
