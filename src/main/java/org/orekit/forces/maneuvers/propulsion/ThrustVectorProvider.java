/* Copyright 2022-2024 Romain Serra
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

package org.orekit.forces.maneuvers.propulsion;

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;

/** Interface defining thrust vectors depending on date and mass only.
 * The frame is assumed to be the satellite one.
 * @author Romain Serra
 * @since 13.0
 */
public interface ThrustVectorProvider {

    /** Get thrust vector at a specified date.
     * @param date date to consider
     * @param mass current mass
     * @return thrust at {@code date} (N)
     */
    Vector3D getThrustVector(AbsoluteDate date, double mass);

    /** Get thrust vector at a specified date.
     * @param <T> type of the field elements
     * @param date date to consider
     * @param mass current mass
     * @return thrust at {@code date} (N)
     */
    <T extends CalculusFieldElement<T>> FieldVector3D<T> getThrustVector(FieldAbsoluteDate<T> date, T mass);
}
