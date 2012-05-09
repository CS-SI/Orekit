/* Copyright 2002-2012 Space Applications Services
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
package org.orekit.files.general;

import java.util.Collection;
import java.util.List;

import org.orekit.time.AbsoluteDate;

/** Interface for orbit file representations.
 * @author Thomas Neidhart
 */
public interface OrbitFile {

    /** Time system used throughout this orbit file. */
    public enum TimeSystem {
        /** Global Positioning System. */
        GPS,
        /** GLONASS. */
        GLO,
        /** GALILEO. */
        GAL,
        /** International Atomic Time. */
        TAI,
        /** Coordinated Universal Time. */
        UTC,
        /** Quasi-Zenith System. */
        QZS
    }

    /** Returns the start epoch of the orbit file.
     * @return the start epoch
     */
    AbsoluteDate getEpoch();

    /** Returns the time interval between epochs (in seconds).
     * @return the time interval between epochs
     */
    double getEpochInterval();

    /** Returns the number of epochs contained in this orbit file.
     * @return the number of epochs
     */
    int getNumberOfEpochs();

    /** Returns the coordinate system of the entries in this orbit file.
     * @return the coordinate system
     */
    String getCoordinateSystem();

    /** Returns the {@link TimeSystem} used to time-stamp position entries.
     * @return the {@link TimeSystem} of the orbit file
     */
    TimeSystem getTimeSystem();

    /** Returns a {@link Collection} of {@link SatelliteInformation} objects for
     * all satellites contained in this orbit file.
     * @return a {@link Collection} of {@link SatelliteInformation} objects
     */
    Collection<SatelliteInformation> getSatellites();

    /** Get the number of satellites contained in this orbit file.
     * @return the number of satellites
     */
    int getSatelliteCount();

    /** Get additional information about a satellite.
     * @param satId the satellite id
     * @return a {@link SatelliteInformation} object describing the satellite if
     *         present, <code>null</code> otherwise
     */
    SatelliteInformation getSatellite(final String satId);

    /** Tests whether a satellite with the given id is contained in this orbit
     * file.
     * @param satId the satellite id
     * @return <code>true</code> if the satellite is contained in the file,
     *         <code>false</code> otherwise
     */
    boolean containsSatellite(final String satId);

    /** Get the time coordinates for the given satellite.
     * @param satId the satellite id
     * @return a {@link List} of {@link SatelliteTimeCoordinate} entries for
     *         this satellite
     */
    List<SatelliteTimeCoordinate> getSatelliteCoordinates(final String satId);
}
