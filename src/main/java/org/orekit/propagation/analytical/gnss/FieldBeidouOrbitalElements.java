/* Copyright 2002-2021 CS GROUP
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
package org.orekit.propagation.analytical.gnss;

import org.hipparchus.RealFieldElement;

/** This interface provides the minimal set of orbital elements needed by the {@link BeidouPropagator}.
*
* @see <a href="http://www2.unb.ca/gge/Resources/beidou_icd_english_ver2.0.pdf">Beidou Interface Control Document</a>
* @author Bryan Cazabonne
* @author Nicolas Fialton (field translation)
*/
public interface FieldBeidouOrbitalElements<T extends RealFieldElement<T>> extends FieldGNSSOrbitalElements<T> {

	// Constants
    /** Earth's universal gravitational parameter for Beidou user in m³/s². */
    double BEIDOU_MU = 3.986004418e+14;

    /** Value of Pi for conversion from semicircles to radian. */
    double BEIDOU_PI = 3.1415926535898;

    /** Duration of the Beidou week in seconds. */
    double BEIDOU_WEEK_IN_SECONDS = 604800.;

    /** Number of weeks in the Beidou cycle. */
    int BEIDOU_WEEK_NB = 8192;

    /**
     * Gets the Age Of Data Clock (AODC).
     *
     * @return the Age Of Data Clock (AODC)
     */
    int getAODC();

    /**
     * Gets the Age Of Data Ephemeris (AODE).
     *
     * @return the Age Of Data Ephemeris (AODE)
     */
    int getAODE();

    /**
     * Gets the BeiDou Issue Of Data (IOD).
     *
     * @return the IOD
     */
    int getIOD();

    /**
     * Gets the estimated group delay differential TGD1 for B1I signal.
     *
     * @return the estimated group delay differential TGD1 for B1I signal (s)
     */
    T getTGD1();

    /**
     * Gets the estimated group delay differential TGD for B2I signal.
     *
     * @return the estimated group delay differential TGD2 for B2I signal (s)
     */
    T getTGD2();
}
