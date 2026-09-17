/* Copyright 2022-2026 Luc Maisonobe
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
package org.orekit.utils.drivers;

import org.hipparchus.CalculusFieldElement;
import org.orekit.utils.TimeSpanMap;

/** Sequence of {@link FieldParameterDriver parameter drivers} along a timeline.
 * @param <T> type of the field elements
 * @see FieldParameterDriversSequenceBuilder
 * @author Luc Maisonobe
 * @since 14.0
 */
public class FieldParameterDriversSequence<T extends CalculusFieldElement<T>>
    extends BaseParameterDriversSequence<FieldParameterDriver<T>, FieldParameterObserver<T>> {

    /** Simple constructor.
     * <p>
     * The content of the provided map will be <em>copied</em> into the instance.
     * Further modifications of the argument (adding or removing entries, resetting dates)
     * will therefore have no effect on the instance.
     * </p>
     * @param timeSpanDrivers drivers for drag coefficients valid on specified time spans
     */
    FieldParameterDriversSequence(final TimeSpanMap<FieldParameterDriver<T>> timeSpanDrivers) {
        super(timeSpanDrivers);
    }

}
