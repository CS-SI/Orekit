/* Copyright 2002-2017 CS Systèmes d'Information
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
package org.orekit.time;

import org.hipparchus.RealFieldElement;

/** This interface represents a scalar function of time.
 * @author Luc Maisonobe
 */
public interface TimeScalarFunction {

    /** Compute a function of time.
     * @param date date
     * @return value of the function
     */
    double value(AbsoluteDate date);

    /** Compute a function of time.
     * @param date date
     * @param <T> type of the filed elements
     * @return value of the function
     */
    <T extends RealFieldElement<T>> T value(FieldAbsoluteDate<T> date);

}
