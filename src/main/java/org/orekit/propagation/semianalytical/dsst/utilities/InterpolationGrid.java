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
package org.orekit.propagation.semianalytical.dsst.utilities;

/**  Interface for interpolation grids.
 * <p>
 * An interpolation grid provides a grid of time points
 * that can be used for interpolation processes.
 * </p><p>
 * In the context of DSST propagation, an interpolation grid is used for the
 * computation through interpolation of short periodics coefficients
 * <p>
 * @author Nicolas Bernard
 */
public interface InterpolationGrid {

    /** Get grid points that are within the current step.
     * <p>The step is defined by its start and its end time.
     * </p>
     * @param stepStart start of the step
     * @param stepEnd end of the step
     * @return time points between start and end
     */
    double[] getGridPoints(double stepStart, double stepEnd);
}
