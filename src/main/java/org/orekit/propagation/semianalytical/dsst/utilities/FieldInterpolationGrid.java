/* Copyright 2002-2024 CS GROUP
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
package org.orekit.propagation.semianalytical.dsst.utilities;

import org.hipparchus.CalculusFieldElement;

/**  Interface for interpolation grids.
 * <p>
 * An interpolation grid provides a grid of time points
 * that can be used for interpolation processes.
 * </p>
 * <p>
 * In the context of DSST propagation, an interpolation grid is used for the
 * computation through interpolation of short periodics coefficients
 * </p>
 * @author Nicolas Bernard
 * @param <T> type of the field elements
 */
public interface FieldInterpolationGrid<T extends CalculusFieldElement<T>> {

    /** Get grid points that are within the current step.
     * <p>The step is defined by its start and its end time.
     * </p>
     * @param stepStart start of the step
     * @param stepEnd end of the step
     * @return time points between start and end
     */
    T[] getGridPoints(T stepStart, T stepEnd);
}
