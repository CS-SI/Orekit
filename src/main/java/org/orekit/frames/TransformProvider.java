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

package org.orekit.frames;

import java.io.Serializable;

import org.hipparchus.CalculusFieldElement;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;

/** Interface for Transform providers.
 * <p>The transform provider interface is mainly used to define the
 * transform between a frame and its parent frame.
 * </p>
 * @author Luc Maisonobe
 */
public interface TransformProvider extends Serializable {

    /** Get the {@link Transform} corresponding to specified date.
     * @param date current date
     * @return transform at specified date
     */
    Transform getTransform(AbsoluteDate date);

    /** Get the {@link FieldTransform} corresponding to specified date.
     * @param date current date
     * @param <T> type of the field elements
     * @return transform at specified date
     * @since 9.0
     */
    <T extends CalculusFieldElement<T>> FieldTransform<T> getTransform(FieldAbsoluteDate<T> date);

    /**
     * Get a transform for only rotations and translations on the specified date.
     *
     * <p>The default implementation returns {@link #getTransform(AbsoluteDate)}
     * but implementations may override it for better performance.
     *
     * @param date current date.
     * @return the static transform.
     */
    default StaticTransform getStaticTransform(AbsoluteDate date) {
        return getTransform(date).toStaticTransform();
    }

    /**
     * Get a transform for only rotations and translations on the specified date.
     *
     * <p>The default implementation returns {@link #getTransform(AbsoluteDate)}
     * but implementations may override it for better performance.
     *
     * @param <T> type of the elements
     * @param date current date.
     * @return the static transform.
     * @since 12.0
     */
    default <T extends CalculusFieldElement<T>> FieldStaticTransform<T> getStaticTransform(FieldAbsoluteDate<T> date) {
        return getTransform(date).toStaticTransform();
    }

}
