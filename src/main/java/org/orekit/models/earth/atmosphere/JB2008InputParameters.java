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
package org.orekit.models.earth.atmosphere;

import java.io.Serializable;

import org.orekit.time.AbsoluteDate;


/** Interface for solar activity and magnetic activity data.
 * <p>Those data are needed by the JB2008 atmosphere model.</p>
 * @author Pascal Parraud
 */
public interface JB2008InputParameters extends Serializable {

    /** Gets the available data range minimum date.
     * @return the minimum date.
     */
    AbsoluteDate getMinDate();

    /** Gets the available data range maximum date.
     * @return the maximum date.
     */
    AbsoluteDate getMaxDate();

    /** Get the value of the instantaneous solar flux index
     *  (1e<sup>-22</sup>*Watt/(m²*Hertz)).
     * <p>Tabular time 1.0 day earlier.</p>
     * @param date the current date
     * @return the instantaneous F10.7 index
     */
    double getF10(AbsoluteDate date);

    /** Get the value of the mean solar flux.
     * Averaged 81-day centered F10.7 B index on the input time.
     * <p>Tabular time 1.0 day earlier.</p>
     * @param date the current date
     * @return the mean solar flux F10.7B index
     */
    double getF10B(AbsoluteDate date);

    /** Get the EUV index (26-34 nm) scaled to F10.
     * <p>Tabular time 1.0 day earlier.</p>
     * @param date the current date
     * @return the the EUV S10 index
     */
    double getS10(AbsoluteDate date);

    /** Get the EUV 81-day averaged centered index.
     * <p>Tabular time 1.0 day earlier.</p>
     * @param date the current date
     * @return the the mean EUV S10B index
     */
    double getS10B(AbsoluteDate date);

    /** Get the MG2 index scaled to F10.
     * <p>Tabular time 2.0 days earlier.</p>
     * @param date the current date
     * @return the the MG2 index
     */
    double getXM10(AbsoluteDate date);

    /** Get the MG2 81-day average centered index.
     * <p>Tabular time 2.0 days earlier.</p>
     * @param date the current date
     * @return the the mean MG2 index
     */
    double getXM10B(AbsoluteDate date);

    /** Get the Solar X-Ray &amp; Lya index scaled to F10.
     * <p>Tabular time 5.0 days earlier.</p>
     * @param date the current date
     * @return the Solar X-Ray &amp; Lya index scaled to F10
     */
    double getY10(AbsoluteDate date);

    /** Get the Solar X-Ray &amp; Lya 81-day ave. centered index.
     * <p>Tabular time 5.0 days earlier.</p>
     * @param date the current date
     * @return the Solar X-Ray &amp; Lya 81-day ave. centered index
     */
    double getY10B(AbsoluteDate date);

    /** Get the temperature change computed from Dst index.
     * @param date the current date
     * @return the temperature change computed from Dst index
     */
    double getDSTDTC(AbsoluteDate date);

}
