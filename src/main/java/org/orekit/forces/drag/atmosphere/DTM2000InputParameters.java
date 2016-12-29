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
package org.orekit.forces.drag.atmosphere;

import java.io.Serializable;

import org.orekit.errors.OrekitException;
import org.orekit.time.AbsoluteDate;


/** Container for solar activity data, compatible with DTM2000 Atmosphere model.
 *
 * This model needs mean and instantaneous solar flux and geomagnetic incides to
 * compute the local density. Mean solar flux is (for the moment) represented by
 * the F10.7 indices. Instantaneous flux can be set to the mean value if the
 * data is not available. Geomagnetic acivity is represented by the Kp indice,
 * which goes from 1 (very low activity) to 9 (high activity).
 * <p>
 * All needed solar activity data can be found on the <a
 * href="http://sec.noaa.gov/Data/index.html">
 * NOAA (National Oceanic and Atmospheric
 * Administration) website.</a>
 *</p>
 *
 * @author Fabien Maussion
 */
public interface DTM2000InputParameters extends Serializable {

    /** Gets the available data range minimum date.
     * @return the minimum date.
     * @exception OrekitException if data cannot be loaded
     */
    AbsoluteDate getMinDate() throws OrekitException;

    /** Gets the available data range maximum date.
     * @return the maximum date.
     * @exception OrekitException if data cannot be loaded
     */
    AbsoluteDate getMaxDate() throws OrekitException;

    /** Get the value of the instantaneous solar flux.
     * @param date the current date
     * @return the instantaneous solar flux
     * @exception OrekitException if the date is out of range of available data
     */
    double getInstantFlux(AbsoluteDate date) throws OrekitException;

    /** Get the value of the mean solar flux.
     * @param date the current date
     * @return the mean solar flux
     * @exception OrekitException if the date is out of range of available data
     */
    double getMeanFlux(AbsoluteDate date) throws OrekitException;

    /** Get the value of the 3 hours geomagnetic index.
     * With a delay of 3 hours at pole to 6 hours at equator using:
     * delay=6-abs(lat)*0.033 (lat in deg.)
     * @param date the current date
     * @return the 3H geomagnetic index
     * @exception OrekitException if the date is out of range of available data
     */
    double getThreeHourlyKP(AbsoluteDate date) throws OrekitException;

    /** Get the last 24H mean geomagnetic index.
     * @param date the current date
     * @return the 24H geomagnetic index
     * @exception OrekitException if the date is out of range of available data
     */
    double get24HoursKp(AbsoluteDate date) throws OrekitException;

}
