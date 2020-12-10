/* Copyright 2002-2020 CS GROUP
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
package org.orekit.files.ilrs;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.orekit.time.AbsoluteDate;
import org.orekit.time.ChronologicalComparator;
import org.orekit.time.TimeStamped;
import org.orekit.utils.ImmutableTimeStampedCache;

/**
 * This class stocks all the information of the Consolidated laser ranging Data Format (CRD) parsed
 * by CRDParser. It contains the header and a list of data records.
 * @author Bryan Cazabonne
 * @since 10.3
 */
public class CRDFile {

    /** List of comments contained in the file. */
    private List<String> comments;

    /** List of data blocks contain in the CDR file. */
    private List<CRDDataBlock> dataBlocks;

    /**
     * Constructor.
     */
    public CRDFile() {
        // Initialise empty lists
        this.comments   = new ArrayList<>();
        this.dataBlocks = new ArrayList<>();
    }

    /**
     * Add a data block to the current list of data blocks.
     * @param dataBlock data block to add
     */
    public void addDataBlock(final CRDDataBlock dataBlock) {
        dataBlocks.add(dataBlock);
    }

    /**
     * Get the comments contained in the file.
     * @return the comments contained in the file
     */
    public List<String> getComments() {
        return comments;
    }

    /**
     * Get the data blocks contain in the file.
     * @return the data blocks contain in the file
     */
    public List<CRDDataBlock> getDataBlocks() {
        return Collections.unmodifiableList(dataBlocks);
    }

    /**
     * Data block containing a set of data contain in the CRD file.
     * <p>
     * A data block consists of a header, configuration data and
     * recorded data (range, angles, meteorological, etc.).
     * </p>
     */
    public static class CRDDataBlock {

        /** Data block header. */
        private CRDHeader header;

        /** Configuration record. */
        private CRDConfiguration configurationRecords;

        /** Range records. */
        private List<RangeMeasurement> rangeData;

        /** Meteorological records. */
        private final SortedSet<TimeStamped> meteoData;

        /** Pointing angles records. */
        private List<AnglesMeasurement> anglesData;

        /**
         * Constructor.
         */
        public CRDDataBlock() {
            // Initialise empty lists
            this.rangeData  = new ArrayList<>();
            this.meteoData  = new TreeSet<>(new ChronologicalComparator());
            this.anglesData = new ArrayList<>();
        }

        /**
         * Get the header of the current data block.
         * @return the header of the current data block
         */
        public CRDHeader getHeader() {
            return header;
        }

        /**
         * Set the header for the current data block.
         * @param header the header to set
         */
        public void setHeader(final CRDHeader header) {
            this.header = header;
        }

        /**
         * Get the system configuration records.
         * @return the system configuration records
         */
        public CRDConfiguration getConfigurationRecords() {
            return configurationRecords;
        }

        /**
         * Set the configuration records for the current data block.
         * @param configurationRecords the configuration records to set
         */
        public void setConfigurationRecords(final CRDConfiguration configurationRecords) {
            this.configurationRecords = configurationRecords;
        }

        /**
         * Add an entry to the list of range data.
         * @param range entry to add
         */
        public void addRangeData(final RangeMeasurement range) {
            rangeData.add(range);
        }

        /**
         * Add an entry to the list of meteorological data.
         * @param meteorologicalMeasurement entry to add
         */
        public void addMeteoData(final MeteorologicalMeasurement meteorologicalMeasurement) {
            meteoData.add(meteorologicalMeasurement);
        }

        /**
         * Add an entry to the list of angles data.
         * @param angles entry to add
         */
        public void addAnglesData(final AnglesMeasurement angles) {
            anglesData.add(angles);
        }

        /**
         * Get the range data for the data block.
         * @return an unmodifiable list of range data
         */
        public List<RangeMeasurement> getRangeData() {
            return Collections.unmodifiableList(rangeData);
        }

        /**
         * Get the angles data for the data block.
         * @return an unmodifiable list of angles data
         */
        public List<AnglesMeasurement> getAnglesData() {
            return Collections.unmodifiableList(anglesData);
        }

        /**
         * Get the meteorological data for the data block.
         * @return an unmodifiable list of meteorological data
         */
        public Meteo getMeteoData() {
            return new Meteo(meteoData);
        }

    }

    /** Range record. */
    public static class RangeMeasurement implements TimeStamped {

        /** Data epoch. */
        private AbsoluteDate date;

        /** Time of flight [s]. */
        private final double timeOfFlight;

        /** Time event reference indicator.
         * 0 = ground receive time (at SRP) (two-way)
         * 1 = spacecraft bounce time (two-way)
         * 2 = ground transmit time (at SRP) (two-way)
         * 3 = spacecraft receive time (one-way)
         * 4 = spacecraft transmit time (one-way)
         * 5 = ground transmit time (at SRP) and spacecraft receive time (one-way)
         * 6 = spacecraft transmit time and ground receive time (at SRP) (one-way)
         * Currently, only 1 and 2 are used for laser ranging data.
         */
        private final int epochEvent;

        /** Signal to noise ration. */
        private final double snr;

        /**
         * Constructor.
         * @param date data epoch
         * @param timeOfFlight time of flight in seconds
         * @param epochEvent indicates the time event reference
         */
        public RangeMeasurement(final AbsoluteDate date,
                                final double timeOfFlight,
                                final int epochEvent) {
            this(date, timeOfFlight, epochEvent, Double.NaN);
        }

        /**
         * Constructor.
         * @param date data epoch
         * @param timeOfFlight time of flight in seconds
         * @param epochEvent indicates the time event reference
         * @param snr signal to noise ratio (can be Double.NaN if unkonwn)
         */
        public RangeMeasurement(final AbsoluteDate date,
                                final double timeOfFlight,
                                final int epochEvent, final double snr) {
            this.date         = date;
            this.timeOfFlight = timeOfFlight;
            this.epochEvent   = epochEvent;
            this.snr          = snr;
        }

        /**
         * Get the time-of-flight.
         * @return the time-of-flight in seconds
         */
        public double getTimeOfFlight() {
            return timeOfFlight;
        }

        /**
         * Get the indicator for the time event reference.
         * <ul>
         * <li>0 = ground receive time (at SRP) (two-way)</li>
         * <li>1 = spacecraft bounce time (two-way)</li>
         * <li>2 = ground transmit time (at SRP) (two-way)</li>
         * <li>3 = spacecraft receive time (one-way)</li>
         * <li>4 = spacecraft transmit time (one-way)</li>
         * <li>5 = ground transmit time (at SRP) and spacecraft receive time (one-way)</li>
         * <li>6 = spacecraft transmit time and ground receive time (at SRP) (one-way)</li>
         * </ul>
         * Currently, only 1 and 2 are used for laser ranging data
         * @return the indicator for the time event reference
         */
        public int getEpochEvent() {
            return epochEvent;
        }

        /**
         * Get the signal to noise ratio.
         * @return the signal to noise ratio
         */
        public double getSnr() {
            return snr;
        }

        /** {@inheritDoc} */
        @Override
        public AbsoluteDate getDate() {
            return date;
        }

    }

    /** This data record contains a minimal set of meteorological data. */
    public static class MeteorologicalMeasurement implements TimeStamped {

        /** Data epoch. */
        private AbsoluteDate date;

        /** Surface pressure [bar]. */
        private final double pressure;

        /** Surface temperature [K]. */
        private final double temperature;

        /** Relative humidity at the surface [%]. */
        private final double humidity;

        /**
         * Constructor.
         * @param date data epoch
         * @param pressure the surface pressure in bars
         * @param temperature the surface temperature in degrees Kelvin
         * @param humidity the relative humidity at the surface in percents
         */
        public MeteorologicalMeasurement(final AbsoluteDate date,
                                         final double pressure, final double temperature,
                                         final double humidity) {
            this.date        = date;
            this.pressure    = pressure;
            this.temperature = temperature;
            this.humidity    = humidity;
        }

        /**
         * Get the surface pressure.
         * @return the surface pressure in bars
         */
        public double getPressure() {
            return pressure;
        }

        /**
         * Get the surface temperature.
         * @return the surface temperature in degrees Kelvin
         */
        public double getTemperature() {
            return temperature;
        }

        /**
         * Get the relative humidity at the surface.
         * @return the relative humidity at the surface in percents
         */
        public double getHumidity() {
            return humidity;
        }

        /** {@inheritDoc} */
        @Override
        public AbsoluteDate getDate() {
            return date;
        }

    }

    /** Pointing angles record. */
    public static class AnglesMeasurement implements TimeStamped {

        /** Data epoch. */
        private AbsoluteDate date;

        /** Azimuth [rad]. */
        private final double azimuth;

        /** Elevation [rad]. */
        private final double elevation;

        /** Direction flag (0 = transmit &#38; receive ; 1 = transmit ; 2 = receive). */
        private final int directionFlag;

        /** Angle origin indicator.
         * 0 = unknown
         * 1 = computed
         * 2 = commanded (from predictions)
         * 3 = measured (from encoders)
         */
        private final int originIndicator;

        /** Refraction corrected. */
        private final boolean refractionCorrected;

        /** Azimuth rate [rad/sec]. */
        private final double azimuthRate;

        /** Elevation rate [rad/sec]. */
        private final double elevationRate;

        /**
         * Constructor.
         * @param date data epoch
         * @param azimuth azimuth angle in radians
         * @param elevation elevation angle in radians
         * @param directionFlag direction flag
         * @param originIndicator angle origin indicator
         * @param refractionCorrected flag to indicate if the refraction is corrected
         * @param azimuthRate azimuth rate in radians per second (equal to Double.NaN if unknown)
         * @param elevationRate elevation rate in radians per second (equal to Double.NaN if unknown)
         */
        public AnglesMeasurement(final AbsoluteDate date, final double azimuth,
                                 final double elevation, final int directionFlag,
                                 final int originIndicator,
                                 final boolean refractionCorrected,
                                 final double azimuthRate, final double elevationRate) {
            this.date                = date;
            this.azimuth             = azimuth;
            this.elevation           = elevation;
            this.directionFlag       = directionFlag;
            this.originIndicator     = originIndicator;
            this.refractionCorrected = refractionCorrected;
            this.azimuthRate         = azimuthRate;
            this.elevationRate       = elevationRate;
        }

        /**
         * Get the azimuth angle.
         * @return the azimuth angle in radians
         */
        public double getAzimuth() {
            return azimuth;
        }

        /**
         * Get the elevation angle.
         * @return the elevation angle in radians
         */
        public double getElevation() {
            return elevation;
        }

        /**
         * Get the direction flag (0 = transmit &#38; receive ; 1 = transmit ; 2 = receive).
         * @return the direction flag
         */
        public int getDirectionFlag() {
            return directionFlag;
        }

        /**
         * Get the angle origin indicator.
         * <p>
         * 0 = unknown;
         * 1 = computed;
         * 2 = commanded (from predictions);
         * 3 = measured (from encoders)
         * </p>
         * @return the angle origin indicator
         */
        public int getOriginIndicator() {
            return originIndicator;
        }

        /**
         * Get the flag indicating if the refraction is corrected.
         * @return true if refraction is corrected
         */
        public boolean isRefractionCorrected() {
            return refractionCorrected;
        }

        /**
         * Get the azimuth rate.
         * <p>
         * Is equal to Double.NaN if the value is unknown.
         * </p>
         * @return the azimuth rate in radians per second
         */
        public double getAzimuthRate() {
            return azimuthRate;
        }

        /**
         * Get the elevation rate.
         * <p>
         * Is equal to Double.NaN if the value is unknown.
         * </p>
         * @return the elevation rate in radians per second
         */
        public double getElevationRate() {
            return elevationRate;
        }

        /** {@inheritDoc} */
        @Override
        public AbsoluteDate getDate() {
            return date;
        }

    }

    /** Meteorological data. */
    public static class Meteo {

        /** Number of neighbors for meteo data interpolation. */
        private static final int N_NEIGHBORS = 2;

        /** First available date. */
        private final AbsoluteDate firstDate;

        /** Last available date. */
        private final AbsoluteDate lastDate;

        /** Previous set of meteorological parameters. */
        private transient MeteorologicalMeasurement previousParam;

        /** Next set of solar meteorological parameters. */
        private transient MeteorologicalMeasurement nextParam;

        /** List of meteo data. */
        private final transient ImmutableTimeStampedCache<TimeStamped> meteo;

        /**
         * Constructor.
         * @param meteoData list of meteo data
         */
        public Meteo(final SortedSet<TimeStamped> meteoData) {

            // Size
            final int neighborsSize = (meteoData.size() < 2) ? meteoData.size() : N_NEIGHBORS;

            // Check neighbors size
            if (neighborsSize == 0) {

                // Meteo data -> empty cache
                this.meteo = ImmutableTimeStampedCache.emptyCache();

                // Null epochs (will ne be used)
                this.firstDate = null;
                this.lastDate  = null;

            } else {

                // Meteo data
                this.meteo = new ImmutableTimeStampedCache<TimeStamped>(neighborsSize, meteoData);

                // Initialize first and last available dates
                this.firstDate = meteoData.first().getDate();
                this.lastDate  = meteoData.last().getDate();

            }

        }

        /**
         * Get the meteorological parameters at a given date.
         * @param date date when user wants the meteorological parameters
         * @return the meteorological parameters at date (can be null if
         *         meteorological data are empty).
         */
        public MeteorologicalMeasurement getMeteo(final AbsoluteDate date) {

            // Check if meteorological data are available
            if (meteo.getNeighborsSize() == 0) {
                return null;
            }

            // Interpolating two neighboring meteorological parameters
            bracketDate(date);
            if (date.durationFrom(firstDate) <= 0 || date.durationFrom(lastDate) > 0) {
                // Date is outside file range
                return previousParam;
            } else {
                // Perform interpolations
                final double pressure    = getLinearInterpolation(date, previousParam.getPressure(), nextParam.getPressure());
                final double temperature = getLinearInterpolation(date, previousParam.getTemperature(), nextParam.getTemperature());
                final double humidity    = getLinearInterpolation(date, previousParam.getHumidity(), nextParam.getHumidity());
                return new MeteorologicalMeasurement(date, pressure, temperature, humidity);
            }

        }

        /**
         * Find the data bracketing a specified date.
         * @param date date to bracket
         */
        private void bracketDate(final AbsoluteDate date) {

            // don't search if the cached selection is fine
            if ((previousParam != null) &&
                (date.durationFrom(previousParam.getDate()) > 0) &&
                (date.durationFrom(nextParam.getDate()) <= 0 )) {
                return;
            }

            // Initialize previous and next parameters
            if (date.durationFrom(firstDate) <= 0) {
                // Current date is before the first date
                previousParam = (MeteorologicalMeasurement) meteo.getEarliest();
                nextParam     = previousParam;
            } else if (date.durationFrom(lastDate) > 0) {
                // Current date is after the last date
                previousParam = (MeteorologicalMeasurement) meteo.getLatest();
                nextParam     = previousParam;
            } else {
                // Current date is between first and last date
                final List<TimeStamped> neighbors = meteo.getNeighbors(date).collect(Collectors.toList());
                previousParam = (MeteorologicalMeasurement) neighbors.get(0);
                nextParam     = (MeteorologicalMeasurement) neighbors.get(1);
            }

        }

        /**
         * Performs a linear interpolation between two values The weights are computed
         * from the time delta between previous date, current date, next date.
         * @param date the current date
         * @param previousValue the value at previous date
         * @param nextValue the value at next date
         * @return the value interpolated for the current date
         */
        private double getLinearInterpolation(final AbsoluteDate date,
                                              final double previousValue,
                                              final double nextValue) {
            // Perform a linear interpolation
            final AbsoluteDate previousDate = previousParam.getDate();
            final AbsoluteDate currentDate = nextParam.getDate();
            final double dt = currentDate.durationFrom(previousDate);
            final double previousWeight = currentDate.durationFrom(date) / dt;
            final double nextWeight = date.durationFrom(previousDate) / dt;

            // Returns the data interpolated at the date
            return previousValue * previousWeight + nextValue * nextWeight;
        }

    }

}
