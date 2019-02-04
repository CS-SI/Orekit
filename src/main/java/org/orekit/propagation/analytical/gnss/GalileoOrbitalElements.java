/* Copyright 2002-2019 CS Systèmes d'Information
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
package org.orekit.propagation.analytical.gnss;

/** This interface provides the minimal set of orbital elements needed by the {@link GalileoPropagator}.
*
* @see <a href="https://www.gsc-europa.eu/system/files/galileo_documents/Galileo-OS-SIS-ICD.pdf">
*         Galileo Interface Control Document</a>
*
* @author Bryan Cazabonne
*
*/
public interface GalileoOrbitalElements extends GNSSOrbitalElements {

    // Constants
    /** Earth's universal gravitational parameter for Galileo user in m³/s². */
    double GALILEO_MU = 3.986004418e+14;

    /** Value of Pi for conversion from semicircles to radian. */
    double GALILEO_PI = 3.1415926535898;

    /** Duration of the Galileo week in seconds. */
    double GALILEO_WEEK_IN_SECONDS = 604800.;

    /** Number of weeks in the Galileo cycle. */
    int GALILEO_WEEK_NB = 4096;

}
