/* Copyright 2002-2016 CS Systèmes d'Information
 * Licensed to CS Systèmes d'Information (CS) under one or more
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

import org.orekit.errors.OrekitIllegalArgumentException;
import org.orekit.errors.OrekitMessages;

/** Enumerate for selecting which derivatives to use in {@link TimeStampedAngularCoordinates}
 * and {@link TimeStampedFieldAngularCoordinates} interpolation.
 * @see TimeStampedAngularCoordinates#interpolate(org.orekit.time.AbsoluteDate, AngularDerivativesFilter, java.util.Collection)
 * @see TimeStampedFieldAngularCoordinates#interpolate(org.orekit.time.AbsoluteDate, AngularDerivativesFilter, java.util.Collection)
 * @see CartesianDerivativesFilter
 * @author Luc Maisonobe
 * @since 7.0
 */
public enum AngularDerivativesFilter {

    /** Use only rotations, ignoring rotation rates. */
    USE_R(0),

    /** Use rotations and rotation rates. */
    USE_RR(1),

    /** Use rotations, rotation rates and acceleration. */
    USE_RRA(2);

    /** Maximum derivation order. */
    private final int maxOrder;

    /** Simple constructor.
     * @param maxOrder maximum derivation order
     */
    AngularDerivativesFilter(final int maxOrder) {
        this.maxOrder = maxOrder;
    }

    /** Get the maximum derivation order.
     * @return maximum derivation order
     */
    public int getMaxOrder() {
        return maxOrder;
    }

    /** Get the filter corresponding to a maximum derivation order.
     * @param order maximum derivation order
     * @return the filter corresponding to derivation order
     * @exception IllegalArgumentException if the order is out of range
     */
    public static AngularDerivativesFilter getFilter(final int order)
        throws IllegalArgumentException {
        for (final AngularDerivativesFilter filter : values()) {
            if (filter.getMaxOrder() == order) {
                return filter;
            }
        }
        throw new OrekitIllegalArgumentException(OrekitMessages.OUT_OF_RANGE_DERIVATION_ORDER, order);
    }

}
