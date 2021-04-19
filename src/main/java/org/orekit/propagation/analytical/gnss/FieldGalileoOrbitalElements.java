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

/**
 * This interface provides the minimal set of orbital elements needed by the
 * {@link GalileoPropagator}.
 *
 * @see <a href=
 *      "https://www.gsc-europa.eu/system/files/galileo_documents/Galileo-OS-SIS-ICD.pdf">
 *      Galileo Interface Control Document</a>
 * @author Bryan Cazabonne
 * @author Nicolas Fialton (field translation)
 */

public interface FieldGalileoOrbitalElements<T extends RealFieldElement<T>>
    extends
    FieldGNSSOrbitalElements<T> {

    // Constants
    /** Earth's universal gravitational parameter for Galileo user in m³/s². */
    double GALILEO_MU = 3.986004418e+14;

    /** Value of Pi for conversion from semicircles to radian. */
    double GALILEO_PI = 3.1415926535898;

    /** Duration of the Galileo week in seconds. */
    double GALILEO_WEEK_IN_SECONDS = 604800.;

    /** Number of weeks in the Galileo cycle. */
    int GALILEO_WEEK_NB = 4096;

    /**
     * Gets the Issue Of Data (IOD).
     *
     * @return the Issue Of Data (IOD)
     */
    int getIODNav();

    /**
     * Gets the estimated broadcast group delay differential.
     *
     * @return the estimated broadcast group delay differential(s)
     */
    T getBGD();

    /**
     * Gets the E1/E5a broadcast group delay.
     *
     * @return the E1/E5a broadcast group delay (s)
     */
    T getBGDE1E5a();

    /**
     * Gets the Broadcast Group Delay E5b/E1.
     *
     * @return the Broadcast Group Delay E5b/E1 (s)
     */
    T getBGDE5bE1();
}
