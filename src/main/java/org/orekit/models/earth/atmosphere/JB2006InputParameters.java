/* Copyright 2002-2025 CS GROUP
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
package org.orekit.models.earth.atmosphere;

import java.io.Serializable;
import org.orekit.time.AbsoluteDate;

/**
 * Interface for solar activity and magnetic activity data.
 *
 * @author Fabien Maussion
 */
public interface JB2006InputParameters extends Serializable {

    /**
     * Get the value of the instantaneous solar flux index
     * (1e<sup>-22</sup>*Watt/(m²*Hertz)).
     * Tabular time 1.0 day earlier.
     *
     * @param date the current date
     * @return the instantaneous F10.7 index
     */
    double getF10(AbsoluteDate date);

    /**
     * Get the value of the mean solar flux.
     * Averaged 81-day centered F10.7 B index on the input time.
     *
     * @param date the current date
     * @return the mean solar flux F10.7B index
     */
    double getF10B(AbsoluteDate date);

    /**
     * Get the EUV index (26-34 nm) scaled to F10.
     * Tabular time 1 day earlier.
     *
     * @param date the current date
     * @return the the EUV S10 index
     */
    double getS10(AbsoluteDate date);

    /**
     * Get the EUV 81-day averaged centered index.
     *
     * @param date the current date
     * @return the the mean EUV S10B index
     */
    double getS10B(AbsoluteDate date);

    /**
     * Get the MG2 index scaled to F10.
     *
     * @param date the current date
     * @return the the EUV S10 index
     */
    double getXM10(AbsoluteDate date);

    /**
     * Get the MG2 81-day average centered index.
     * Tabular time 5.0 days earlier.
     *
     * @param date the current date
     * @return the the mean EUV S10B index
     */
    double getXM10B(AbsoluteDate date);

    /**
     * Get the Geomagnetic planetary 3-hour index A<sub>p</sub>.
     * Tabular time 6.7 hours earlier.
     *
     * @param date the current date
     * @return the A<sub>p</sub> index
     */
    double getAp(AbsoluteDate date);

    /**
     * Gets the available data range minimum date.
     *
     * @return the minimum date.
     */
    AbsoluteDate getMinDate();

    /**
     * Gets the available data range maximum date.
     *
     * @return the maximum date.
     */
    AbsoluteDate getMaxDate();
}
