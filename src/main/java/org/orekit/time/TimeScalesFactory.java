/* Copyright 2002-2008 CS Communication & Systèmes
 * Licensed to CS Communication & Systèmes (CS) under one or more
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

import java.io.Serializable;

import org.orekit.errors.OrekitException;

/** Factory for predefined time scales.
 * <p>
 * This is a utility class, so its constructor is private.
 * </p>
 * @author Luc Maisonobe
 * @version $Revision$ $Date$
 */
public class TimeScalesFactory implements Serializable {

    /** Serialiazable UID. */
    private static final long serialVersionUID = 250016254726663365L;

    /** International Atomic Time scale. */
    private static TAIScale tai = null;

    /** Universal Time Coordinate scale. */
    private static UTCScale utc = null;

    /** Terrestrial Time scale. */
    private static TTScale tt = null;

    /** Global Positioning System scale. */
    private static GPSScale gps = null;

    /** Geocentric Coordinate Time scale. */
    private static TCGScale tcg = null;

    /** Private constructor.
     * <p>This class is a utility class, it should neither have a public
     * nor a default constructor. This private constructor prevents
     * the compiler from generating one automatically.</p>
     */
    private TimeScalesFactory() {
    }

    /** Get the International Atomic Time scale. */
    public static TAIScale getTAI() {
        synchronized(TimeScalesFactory.class) {

            if (tai == null) {
                tai = new TAIScale();
            }

            return tai;

        }
    }

    /** Get the Universal Time Coordinate scale.
     * @exception OrekitException if the leap seconds cannot be read
     */
    public static UTCScale getUTC() throws OrekitException {
        synchronized(TimeScalesFactory.class) {

            if (utc == null) {
                utc = new UTCScale();
            }

            return utc;

        }
    }

    /** Get the Terrestrial Time scale. */
    public static TTScale getTT() {
        synchronized(TimeScalesFactory.class) {

            if (tt == null) {
                tt = new TTScale();
            }

            return tt;

        }
    }

    /** Get the Global Positioning System scale. */
    public static GPSScale getGPS() {
        synchronized(TimeScalesFactory.class) {

            if (gps == null) {
                gps = new GPSScale();
            }

            return gps;

        }
    }

    /** Get the Geocentric Coordinate Time scale. */
    public static TCGScale getTCG() {
        synchronized(TimeScalesFactory.class) {

            if (tcg == null) {
                tcg = new TCGScale();
            }

            return tcg;

        }
    }

}
