/* Copyright 2002-2025 CS GROUP
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

package org.orekit.utils;

import org.hipparchus.CalculusFieldElement;
import org.orekit.time.FieldAbsoluteDate;

/**
 * Interface for bounded, Field PV coordinates providers.
 *
 * @author Romain Serra
 * @since 13.1
 * @see FieldPVCoordinatesProvider
 */
public interface FieldBoundedPVCoordinatesProvider<T extends CalculusFieldElement<T>>
        extends FieldPVCoordinatesProvider<T> {

    /** Get the first date of the range.
     * @return the first date of the range
     */
    FieldAbsoluteDate<T> getMinDate();

    /** Get the last date of the range.
     * @return the last date of the range
     */
    FieldAbsoluteDate<T> getMaxDate();
}
