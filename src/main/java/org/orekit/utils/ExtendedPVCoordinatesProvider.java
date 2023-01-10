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

package org.orekit.utils;

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.Field;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.orekit.frames.Frame;
import org.orekit.time.FieldAbsoluteDate;

/**
** Interface for PV coordinates providers that also support fields.
 * @since 9.2
 * @author Luc Maisonobe
 */
public interface ExtendedPVCoordinatesProvider extends PVCoordinatesProvider {

    /** Convert to a {@link FieldPVCoordinatesProvider} with a specific type.
     * @param <T> the type of the field elements
     * @param field field for the argument and value
     * @return converted function
     */
    default <T extends CalculusFieldElement<T>> FieldPVCoordinatesProvider<T> toFieldPVCoordinatesProvider(Field<T> field) {
        return this::getPVCoordinates;
    }

    /** Get the position of the body in the selected frame.
     * @param date current date
     * @param frame the frame where to define the position
     * @param <T> type for the field elements
     * @return position of the body (m and)
     * @since 12.0
     */
    default <T extends CalculusFieldElement<T>> FieldVector3D<T> getPosition(final FieldAbsoluteDate<T> date, final Frame frame) {
        return getPVCoordinates(date, frame).getPosition();
    }

    /** Get the {@link FieldPVCoordinates} of the body in the selected frame.
     * @param date current date
     * @param frame the frame where to define the position
     * @param <T> type for the field elements
     * @return time-stamped position/velocity of the body (m and m/s)
     */
    <T extends CalculusFieldElement<T>>TimeStampedFieldPVCoordinates<T> getPVCoordinates(FieldAbsoluteDate<T> date,
                                                                                         Frame frame);

}
