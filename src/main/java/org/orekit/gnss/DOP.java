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
package org.orekit.gnss;

import org.orekit.bodies.GeodeticPoint;
import org.orekit.time.AbsoluteDate;


/**
 * This class is a container for the result of a single DOP computation.
 *
 * @author Pascal Parraud
 * @since 8.0
 * @see <a href="http://en.wikipedia.org/wiki/Dilution_of_precision_%28GPS%29">Dilution of precision</a>
 *
 */
public class DOP {

    // Fields
    /** Location with respect to the Earth where DOP was calculated. */
    private final GeodeticPoint location;
    /** Date when all DOP was calculated. */
    private final AbsoluteDate date;
    /** Number of GNSS satellites taken into account for DOP computation. */
    private final int gnssNb;
    /** Geometric dilution of precision. */
    private final double gdop;
    /** Position dilution of precision. */
    private final double pdop;
    /** Horizontal dilution of precision. */
    private final double hdop;
    /** Vertical dilution of precision. */
    private final double vdop;
    /** Time dilution of precision. */
    private final double tdop;

    /**
     * Constructor.
     *
     * @param location location with respect to the Earth where DOP was calculated
     * @param date date when all DOP was calculated
     * @param gnssNb number of GNSS satellites taken into account for DOP computation
     * @param gdop the geometric dilution of precision
     * @param pdop the position dilution of precision
     * @param hdop the horizontal dilution of precision
     * @param vdop the vertical dilution of precision
     * @param tdop the time dilution of precision
     */
    public DOP(final GeodeticPoint location, final AbsoluteDate date, final int gnssNb,
               final double gdop, final double pdop, final double hdop, final double vdop, final double tdop) {
        this.location = location;
        this.date = date;
        this.gnssNb = gnssNb;
        this.gdop = gdop;
        this.pdop = pdop;
        this.hdop = hdop;
        this.vdop = vdop;
        this.tdop = tdop;
    }

    /** Gets the location with respect to the Earth where DOP was calculated.
     * @return the location with respect to the Earth where DOP was calculated
     */
    public GeodeticPoint getLocation() {
        return location;
    }

    /** Gets the calculation date of the DOP.
     * @return the calculation date of the DOP
     */
    public AbsoluteDate getDate() {
        return date;
    }

    /** Gets the number of GNSS satellites taken into account for DOP computation.
     * @return the number of GNSS satellites taken into account for DOP computation
     */
    public int getGnssNb() {
        return gnssNb;
    }

    /** Gets the geometric dilution of precision.
     * @return the GDOP
     */
    public double getGdop() {
        return gdop;
    }

    /** Gets the position dilution of precision.
     * @return the PDOP
     */
    public double getPdop() {
        return pdop;
    }

    /** Gets the horizontal dilution of precision.
     * @return the HDOP
     */
    public double getHdop() {
        return hdop;
    }

    /** Gets the vertical dilution of precision.
     * @return the VDOP
     */
    public double getVdop() {
        return vdop;
    }

    /** Gets the time dilution of precision.
     * @return the TDOP
     */
    public double getTdop() {
        return tdop;
    }
}
