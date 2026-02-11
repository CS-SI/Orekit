/* Copyright 2002-2026 Guo Xiaozhong
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
package org.orekit.bodies;

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.orekit.time.AbsoluteDate;

/** A simple container for JPL libration data.
 * <p>
 * As described in
 * <a href="https://ssd.jpl.nasa.gov/ftp/eph/planets/ascii/ascii_format.txt">
 * Internal Format of the Ephemeris Files</a> of JPL, there are three components
 * for librations : phi, theta, psi, which can be returned by {@link getLibration},
 * or individually by {@link getPhi}, {@link getTheta}, and {@link getPsi}.
 * </p>
 * @author Guo Xiaozhong
 * @since 14.0
 */
public class JPLLibration {

    /** Raw provider. */
    private final transient JPLEphemeridesLoader.RawPVProvider rawPVProvider;

    /** Build an instance.
     * @param rawPVProvider raw libration provider
     */
    JPLLibration (final JPLEphemeridesLoader.RawPVProvider rawPVProvider) {
        this.rawPVProvider  = rawPVProvider;
    }

    /** Get the libration.
     * @param date current date
     * @return libration in radians
     */
    public Vector3D getLibration(final AbsoluteDate date) {
        return rawPVProvider.getRawPosition(date);
    }

    /** Get the phi component of libration.
     * @param date current date
     * @return phi component of libration in radians
     */
    public double getPhi(final AbsoluteDate date) {
        return getLibration(date).getX();
    }

    /** Get the theta component of libration.
     * @param date current date
     * @return theta component of libration in radians
     */
    public double getTheta(final AbsoluteDate date) {
        return getLibration(date).getY();
    }

    /** Get the psi component of libration.
     * @param date current date
     * @return psi component of libration in radians
     */
    public double getPsi(final AbsoluteDate date) {
        return getLibration(date).getZ();
    }
}
