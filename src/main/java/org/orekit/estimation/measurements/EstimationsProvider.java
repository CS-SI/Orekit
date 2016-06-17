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
package org.orekit.estimation.measurements;

import org.orekit.errors.OrekitException;


/** Interface for retrieving estimated measurements during orbit determination.
 * <p>
 * Implementations of this interface are provided by the orbit
 * determination engine to user so they can {@link
 * org.orekit.estimation.leastsquares.BatchLSObserver monitor}
 * the orbit determination process.
 * </p>
 * @see org.orekit.estimation.leastsquares.BatchLSObserver
 * @author Luc Maisonobe
 * @since 8.0
 */
public interface EstimationsProvider {

    /** Get the number of evaluations available.
     * @return number of evaluations available
     */
    int getNumber();

    /** Get one estimated measurement.
     * @param index index of the estimated measurement, must be between 0
     * and {@link #getNumber() getNumber()} - 1, chronologically
     * sorted
     * @return estimated measurement at specified index
     * @exception OrekitException if number is out of range
     */
    EstimatedMeasurement<?> getEstimatedMeasurement(int index)
        throws OrekitException;

}
