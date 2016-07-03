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
package org.orekit.bodies;

import java.io.Serializable;

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.orekit.time.AbsoluteDate;

/** Interface for IAU pole and prime meridian orientations.
 * <p>
 * This interface defines methods compliant with the report of the
 * IAU/IAG Working Group on Cartographic Coordinates and Rotational
 * Elements of the Planets and Satellites (WGCCRE). These definitions
 * are common for all recent versions of this report published every
 * three years.
 * </p>
 * <p>
 * The precise values of pole direction and W angle coefficients may vary
 * from publication year as models are adjusted. The latest value of
 * constants for implementing this interface can be found in the <a
 * href="http://astrogeology.usgs.gov/Projects/WGCCRE/">working group
 * site</a>.
 * </p>
 * @see CelestialBodyFactory
 * @author Luc Maisonobe
 */
public interface IAUPole extends Serializable {

    /** Get the body North pole direction in ICRF frame.
     * @param date current date
     * @return body North pole direction in ICRF frame
     */
    Vector3D getPole(AbsoluteDate date);

    /** Get the prime meridian angle.
     * <p>
     * The prime meridian angle is the angle between the Q node and the
     * prime meridian. represents the body rotation.
     * </p>
     * @param date current date
     * @return prime meridian vector
     */
    double getPrimeMeridianAngle(AbsoluteDate date);

}
