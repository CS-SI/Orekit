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
package org.orekit.estimation.measurements.gnss;

import java.io.Serializable;
import java.util.Comparator;

/** Comparator for {@link IntegerLeastSquareSolution} instance.
 * @see IntegerLeastSquareSolution
 * @author Bryan Cazabonne
 * @since 11.0
 */
public class IntegerLeastSquareComparator implements Comparator<IntegerLeastSquareSolution>, Serializable {

    /** Serializable UID. */
    private static final long serialVersionUID = 20210813;

    /** Simple constructor.
     */
    public IntegerLeastSquareComparator() {
        // nothing to do
    }

    /** {@inheritDoc}
     * The comparison is based on the squared
     * distance to the float solution.
     */
    @Override
    public int compare(final IntegerLeastSquareSolution ilss1,
                       final IntegerLeastSquareSolution ilss2) {

        if (ilss1 == null) {
            return ilss2 == null ? 0 : -1;
        } else if (ilss2 == null) {
            return 1;
        }

        return Double.compare(ilss1.getSquaredDistance(), ilss2.getSquaredDistance());
    }

}
