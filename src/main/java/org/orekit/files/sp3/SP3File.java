/* Licensed to CS Communication & Systèmes (CS) under one or more
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
package org.orekit.files.sp3;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.orekit.files.OrbitFile;
import org.orekit.files.SatelliteInformation;
import org.orekit.files.SatelliteTimeCoordinate;
import org.orekit.time.AbsoluteDate;

/** Represents a parsed SP3 orbit file.
 * @author Thomas Neidhart
 */
public class SP3File implements OrbitFile, Serializable {

    /** Serializable UID. */
    private static final long serialVersionUID = 3333652174843017654L;

    /** File type indicator. */
    public enum FileType {
        /** GPS only file. */
        GPS,
        /** Mixed file. */
        MIXED,
        /** GLONASS only file. */
        GLONASS,
        /** LEO only file. */
        LEO,
        /** Galileo only file. */
        GALILEO,
        /** COMPASS only file. */
        COMPASS,
        /** QZSS only file. */
        QZSS,
        /** undefined file format. */
        UNDEFINED
    }

    /** Orbit type indicator. */
    public enum OrbitType {
        /** fitted. */
        FIT,
        /** extrapolated or predicted. */
        EXT,
        /** broadcast. */
        BCT,
        /** fitted after applying a Helmert transformation. */
        HLM
    }

    /** File type. */
    private FileType type;

    /** Time system. */
    private TimeSystem timeSystem;

    /** Epoch of the file. */
    private AbsoluteDate epoch;

    /** GPS week. */
    private int gpsWeek;

    /** Seconds of the current GPS week. */
    private double secondsOfWeek;

    /** Julian day. */
    private int julianDay;

    /** Day fraction. */
    private double dayFraction;

    /** Time-interval between epochs. */
    private double epochInterval;

    /** Number of epochs. */
    private int numberOfEpochs;

    /** Coordinate system. */
    private String coordinateSystem;

    /** Data used indicator. */
    private String dataUsed;

    /** Orbit type. */
    private OrbitType orbitType;

    /** Agency providing the file. */
    private String agency;

    /** A list containing additional satellite information. */
    private List<SatelliteInformation> satellites;

    /** A map containing all satellite coordinates. */
    private Map<String, List<SatelliteTimeCoordinate>> satelliteCoords;

    /** Create a new SP3 file object. */
    public SP3File() {
        satellites = new ArrayList<SatelliteInformation>();
        satelliteCoords = new HashMap<String, List<SatelliteTimeCoordinate>>();
    }

    /** Returns the {@link FileType} associated with this SP3 file.
     * @return the file type for this SP3 file
     */
    public FileType getType() {
        return type;
    }

    /** Set the file type for this SP3 file.
     * @param fileType the file type to be set
     */
    public void setType(final FileType fileType) {
        this.type = fileType;
    }

    /** {@inheritDoc} */
    public TimeSystem getTimeSystem() {
        return timeSystem;
    }

    /** Set the time system used in this SP3 file.
     * @param system the time system to be set
     */
    public void setTimeSystem(final TimeSystem system) {
        this.timeSystem = system;
    }

    /** Returns the data used indicator from the SP3 file.
     * @return the data used indicator (unparsed)
     */
    public String getDataUsed() {
        return dataUsed;
    }

    /** Set the data used indicator for this SP3 file.
     * @param data the data used indicator to be set
     */
    public void setDataUsed(final String data) {
        this.dataUsed = data;
    }

    /** {@inheritDoc} */
    public AbsoluteDate getEpoch() {
        return epoch;
    }

    /** Set the epoch of the SP3 file.
     * @param time the epoch to be set
     */
    public void setEpoch(final AbsoluteDate time) {
        this.epoch = time;
    }

    /** Returns the GPS week as contained in the SP3 file.
     * @return the GPS week of the SP3 file
     */
    public int getGpsWeek() {
        return gpsWeek;
    }

    /** Set the GPS week of the SP3 file.
     * @param week the GPS week to be set
     */
    public void setGpsWeek(final int week) {
        this.gpsWeek = week;
    }

    /** Returns the seconds of the GPS week as contained in the SP3 file.
     * @return the seconds of the GPS week
     */
    public double getSecondsOfWeek() {
        return secondsOfWeek;
    }

    /** Set the seconds of the GPS week for this SP3 file.
     * @param seconds the seconds to be set
     */
    public void setSecondsOfWeek(final double seconds) {
        this.secondsOfWeek = seconds;
    }

    /** Returns the julian day for this SP3 file.
     * @return the julian day
     */
    public int getJulianDay() {
        return julianDay;
    }

    /** Set the julian day for this SP3 file.
     * @param day the julian day to be set
     */
    public void setJulianDay(int day) {
        this.julianDay = day;
    }

    /** Returns the day fraction for this SP3 file.
     * @return the day fraction
     */
    public double getDayFraction() {
        return dayFraction;
    }

    /** Set the day fraction for this SP3 file.
     * @param fraction the day fraction to be set
     */
    public void setDayFraction(final double fraction) {
        this.dayFraction = fraction;
    }

    /** {@inheritDoc} */
    public double getEpochInterval() {
        return epochInterval;
    }

    /** Set the epoch interval for this SP3 file.
     * @param interval the interval between orbit entries
     */
    public void setEpochInterval(final double interval) {
        this.epochInterval = interval;
    }

    /** {@inheritDoc} */
    public int getNumberOfEpochs() {
        return numberOfEpochs;
    }

    /** Set the number of epochs as contained in the SP3 file.
     * @param epochCount the number of epochs to be set
     */
    public void setNumberOfEpochs(final int epochCount) {
        this.numberOfEpochs = epochCount;
    }

    /** {@inheritDoc} */
    public String getCoordinateSystem() {
        return coordinateSystem;
    }

    /** Set the coordinate system used for the orbit entries.
     * @param system the coordinate system to be set
     */
    public void setCoordinateSystem(final String system) {
        this.coordinateSystem = system;
    }

    /** Returns the {@link OrbitType} for this SP3 file.
     * @return the orbit type
     */
    public OrbitType getOrbitType() {
        return orbitType;
    }

    /** Set the {@link OrbitType} for this SP3 file.
     * @param type the orbit type to be set
     */
    public void setOrbitType(final OrbitType type) {
        this.orbitType = type;
    }

    /** Returns the agency that prepared this SP3 file.
     * @return the agency
     */
    public String getAgency() {
        return agency;
    }

    /** Set the agency string for this SP3 file.
     * @param agencyStr the agency string to be set
     */
    public void setAgency(final String agencyStr) {
        this.agency = agencyStr;
    }

    /** Add a new satellite with a given identifier to the list of
     * stored satellites.
     * @param satId the satellite identifier
     */
    public void addSatellite(final String satId) {
        satellites.add(new SatelliteInformation(satId));
        satelliteCoords.put(satId, new LinkedList<SatelliteTimeCoordinate>());
    }

    /** {@inheritDoc} */
    public Collection<SatelliteInformation> getSatellites() {
        return Collections.unmodifiableCollection(satellites);
    }

    /** {@inheritDoc} */
    public int getSatelliteCount() {
        return satellites.size();
    }

    /** {@inheritDoc} */
    public SatelliteInformation getSatellite(final String satId) {
        if (satId == null) {
            return null;
        }

        // TODO: the satellite info is currently stored in an ArrayList
        //       for quick access to the nth satellite.
        //       Consider using a Set/Map to quickly access satellites by id
        for (SatelliteInformation sat : satellites) {
            if (satId.equalsIgnoreCase(sat.getSatelliteId())) {
                return sat;
            }
        }

        return null;
    }

    /** Returns the nth satellite as contained in the SP3 file.
     * @param n the index of the satellite
     * @return a {@link SatelliteInformation} object for the nth satellite
     */
    public SatelliteInformation getSatellite(final int n) {
        return satellites.get(n);
    }

    /** {@inheritDoc} */
    public boolean containsSatellite(final String satId) {
        return satelliteCoords.containsKey(satId);
    }

    /** {@inheritDoc} */
    public List<SatelliteTimeCoordinate> getSatelliteCoordinates(final String satId) {
        return satelliteCoords.get(satId);
    }

    /** Adds a new P/V coordinate for a given satellite.
     * @param satId the satellite identifier
     * @param coord the P/V coordinate of the satellite
     */
    public void addSatelliteCoordinate(final String satId,
            final SatelliteTimeCoordinate coord) {
        List<SatelliteTimeCoordinate> coords = satelliteCoords.get(satId);
        if (coords != null) {
            coords.add(coord);
        }
    }
}
