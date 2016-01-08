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
package org.orekit.propagation.analytical.tle;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.orekit.data.DataLoader;
import org.orekit.data.DataProvidersManager;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.ChronologicalComparator;
import org.orekit.time.TimeStamped;
import org.orekit.utils.PVCoordinates;

/** This class reads and handles series of TLEs for one space object.
 *  <p>
 *  TLE data is read using the standard Orekit mechanism based on a configured
 *  {@link DataProvidersManager DataProvidersManager}. This means TLE data may
 *  be retrieved from many different storage media (local disk files, remote servers,
 *  database ...).
 *  </p>
 *  <p>
 *  This class provides bounded ephemerides by finding the best initial TLE to
 *  propagate and then handling the propagation.
 *  </p>
 *
 * @see TLE
 * @see DataProvidersManager
 * @author Fabien Maussion
 * @author Luc Maisonobe
 */
public class TLESeries implements DataLoader {

    /** Default supported files name pattern. */
    private static final String DEFAULT_SUPPORTED_NAMES = ".*\\.tle$";

    /** Regular expression for supported files names. */
    private final String supportedNames;

    /** Available satellite numbers. */
    private final Set<Integer> availableSatNums;

    /** Set containing all TLE entries. */
    private final SortedSet<TimeStamped> tles;

    /** Satellite number used for filtering. */
    private int filterSatelliteNumber;

    /** Launch year used for filtering (all digits). */
    private int filterLaunchYear;

    /** Launch number used for filtering. */
    private int filterLaunchNumber;

    /** Launch piece used for filtering. */
    private String filterLaunchPiece;

    /** Previous TLE in the cached selection. */
    private TLE previous;

    /** Next TLE in the cached selection. */
    private TLE next;

    /** Last used TLE. */
    private TLE lastTLE;

    /** Associated propagator. */
    private TLEPropagator lastPropagator;

    /** Date of the first TLE. */
    private AbsoluteDate firstDate;

    /** Date of the last TLE. */
    private AbsoluteDate lastDate;

    /** Indicator for non-TLE extra lines. */
    private final boolean ignoreNonTLELines;

    /** Simple constructor with a TLE file.
     * <p>This constructor does not load any data by itself. Data must be
     * loaded later on by calling one of the {@link #loadTLEData()
     * loadTLEData()} method, the {@link #loadTLEData(int)
     * loadTLEData(filterSatelliteNumber)} method or the {@link #loadTLEData(int,
     * int, String) loadTLEData(filterLaunchYear, filterLaunchNumber, filterLaunchPiece)} method.<p>
     * @param supportedNames regular expression for supported files names
     * (if null, a default pattern matching files with a ".tle" extension will be used)
     * @param ignoreNonTLELines if true, extra non-TLE lines are silently ignored,
     * if false an exception will be generated when such lines are encountered
     * @see #loadTLEData()
     * @see #loadTLEData(int)
     * @see #loadTLEData(int, int, String)
     */
    public TLESeries(final String supportedNames, final boolean ignoreNonTLELines) {

        this.supportedNames    = (supportedNames == null) ? DEFAULT_SUPPORTED_NAMES : supportedNames;
        availableSatNums       = new TreeSet<Integer>();
        this.ignoreNonTLELines = ignoreNonTLELines;
        filterSatelliteNumber  = -1;
        filterLaunchYear       = -1;
        filterLaunchNumber     = -1;
        filterLaunchPiece      = null;

        tles     = new TreeSet<TimeStamped>(new ChronologicalComparator());
        previous = null;
        next     = null;

    }

    /** Load TLE data for a specified object.
     * <p>The TLE data already loaded in the instance will be discarded
     * and replaced by the newly loaded data.</p>
     * <p>The filtering values will be automatically set to the first loaded
     * satellite. This feature is useful when the satellite selection is
     * already set up by either the instance configuration (supported file
     * names) or by the {@link DataProvidersManager data providers manager}
     * configuration and the local filtering feature provided here can be ignored.</p>
     * @exception OrekitException if some data can't be read, some
     * file content is corrupted or no TLE data is available
     * @see #loadTLEData(int)
     * @see #loadTLEData(int, int, String)
     */
    public void loadTLEData() throws OrekitException {

        availableSatNums.clear();

        // set the filtering parameters
        filterSatelliteNumber = -1;
        filterLaunchYear      = -1;
        filterLaunchNumber    = -1;
        filterLaunchPiece     = null;

        // load the data from the configured data providers
        tles.clear();
        previous = null;
        next     = null;
        DataProvidersManager.getInstance().feed(supportedNames, this);
        if (tles.isEmpty()) {
            throw new OrekitException(OrekitMessages.NO_TLE_DATA_AVAILABLE);
        }

    }

    /** Get the available satellite numbers.
     * @return available satellite numbers
     * @throws OrekitException if some data can't be read, some
     * file content is corrupted or no TLE data is available
     */
    public Set<Integer> getAvailableSatelliteNumbers() throws OrekitException {
        if (availableSatNums.isEmpty()) {
            loadTLEData();
        }
        return availableSatNums;
    }

    /** Load TLE data for a specified object.
     * <p>The TLE data already loaded in the instance will be discarded
     * and replaced by the newly loaded data.</p>
     * <p>Calling this method with the satellite number set to a negative value,
     * is equivalent to call {@link #loadTLEData()}.</p>
     * @param satelliteNumber satellite number
     * @exception OrekitException if some data can't be read, some
     * file content is corrupted or no TLE data is available for the selected object
     * @see #loadTLEData()
     * @see #loadTLEData(int, int, String)
     */
    public void loadTLEData(final int satelliteNumber) throws OrekitException {

        if (satelliteNumber < 0) {
            // no filtering at all
            loadTLEData();
        } else {
            // set the filtering parameters
            filterSatelliteNumber = satelliteNumber;
            filterLaunchYear      = -1;
            filterLaunchNumber    = -1;
            filterLaunchPiece     = null;

            // load the data from the configured data providers
            tles.clear();
            previous = null;
            next     = null;
            DataProvidersManager.getInstance().feed(supportedNames, this);
            if (tles.isEmpty()) {
                throw new OrekitException(OrekitMessages.NO_TLE_FOR_OBJECT, satelliteNumber);
            }
        }

    }

    /** Load TLE data for a specified object.
     * <p>The TLE data already loaded in the instance will be discarded
     * and replaced by the newly loaded data.</p>
     * <p>Calling this method with either the launch year or the launch number
     * set to a negative value, or the launch piece set to null or an empty
     * string are all equivalent to call {@link #loadTLEData()}.</p>
     * @param launchYear launch year (all digits)
     * @param launchNumber launch number
     * @param launchPiece launch piece
     * @exception OrekitException if some data can't be read, some
     * file content is corrupted or no TLE data is available for the selected object
     * @see #loadTLEData()
     * @see #loadTLEData(int)
     */
    public void loadTLEData(final int launchYear, final int launchNumber,
                            final String launchPiece) throws OrekitException {

        if ((launchYear < 0) || (launchNumber < 0) ||
            (launchPiece == null) || (launchPiece.length() == 0)) {
            // no filtering at all
            loadTLEData();
        } else {
            // set the filtering parameters
            filterSatelliteNumber = -1;
            filterLaunchYear      = launchYear;
            filterLaunchNumber    = launchNumber;
            filterLaunchPiece     = launchPiece;

            // load the data from the configured data providers
            tles.clear();
            previous = null;
            next     = null;
            DataProvidersManager.getInstance().feed(supportedNames, this);
            if (tles.isEmpty()) {
                throw new OrekitException(OrekitMessages.NO_TLE_FOR_LAUNCH_YEAR_NUMBER_PIECE,
                                          launchYear, launchNumber, launchPiece);
            }
        }

    }

    /** {@inheritDoc} */
    public boolean stillAcceptsData() {
        return tles.isEmpty();
    }

    /** {@inheritDoc} */
    public void loadData(final InputStream input, final String name)
        throws IOException, OrekitException {

        final BufferedReader r = new BufferedReader(new InputStreamReader(input, "UTF-8"));
        try {

            int lineNumber     = 0;
            String pendingLine = null;
            for (String line = r.readLine(); line != null; line = r.readLine()) {

                ++lineNumber;

                if (pendingLine == null) {

                    // we must wait for the second line
                    pendingLine = line;

                } else {

                    // safety checks
                    if (!TLE.isFormatOK(pendingLine, line)) {
                        if (ignoreNonTLELines) {
                            // just shift one line
                            pendingLine = line;
                            continue;
                        } else {
                            throw new OrekitException(OrekitMessages.NOT_TLE_LINES,
                                                      lineNumber - 1, lineNumber, pendingLine, line);
                        }
                    }

                    final TLE tle = new TLE(pendingLine, line);

                    if (filterSatelliteNumber < 0) {
                        if ((filterLaunchYear < 0) ||
                            ((tle.getLaunchYear()   == filterLaunchYear) &&
                             (tle.getLaunchNumber() == filterLaunchNumber) &&
                             tle.getLaunchPiece().equals(filterLaunchPiece))) {
                            // we now know the number of the object to load
                            filterSatelliteNumber = tle.getSatelliteNumber();
                        }
                    }

                    availableSatNums.add(tle.getSatelliteNumber());

                    if (tle.getSatelliteNumber() == filterSatelliteNumber) {
                        // accept this TLE
                        tles.add(tle);
                    }

                    // we need to wait for two new lines
                    pendingLine = null;

                }

            }

            if ((pendingLine != null) && !ignoreNonTLELines) {
                // there is an unexpected last line
                throw new OrekitException(OrekitMessages.MISSING_SECOND_TLE_LINE,
                                          lineNumber, pendingLine);
            }

        } finally {
            r.close();
        }

    }

    /** Get the extrapolated position and velocity from an initial date.
     * For a good precision, this date should not be too far from the range :
     * [{@link #getFirstDate() first date} ; {@link #getLastDate() last date}].
     * @param date the final date
     * @return the final PVCoordinates
     * @exception OrekitException if the underlying propagator cannot be initialized
     */
    public PVCoordinates getPVCoordinates(final AbsoluteDate date)
        throws OrekitException {
        final TLE toExtrapolate = getClosestTLE(date);
        if (toExtrapolate != lastTLE) {
            lastTLE = toExtrapolate;
            lastPropagator = TLEPropagator.selectExtrapolator(lastTLE);
        }
        return lastPropagator.getPVCoordinates(date);
    }

    /** Get the closest TLE to the selected date.
     * @param date the date
     * @return the TLE that will suit the most for propagation.
     */
    public TLE getClosestTLE(final AbsoluteDate date) {

        //  don't search if the cached selection is fine
        if ((previous != null) && (date.durationFrom(previous.getDate()) >= 0) &&
            (next     != null) && (date.durationFrom(next.getDate())     <= 0)) {
            // the current selection is already good
            if (next.getDate().durationFrom(date) > date.durationFrom(previous.getDate())) {
                return previous;
            } else {
                return next;
            }
        }
        // reset the selection before the search phase
        previous  = null;
        next      = null;
        final SortedSet<TimeStamped> headSet = tles.headSet(date);
        final SortedSet<TimeStamped> tailSet = tles.tailSet(date);


        if (headSet.isEmpty()) {
            return (TLE) tailSet.first();
        }
        if (tailSet.isEmpty()) {
            return (TLE) headSet.last();
        }
        previous = (TLE) headSet.last();
        next = (TLE) tailSet.first();

        if (next.getDate().durationFrom(date) > date.durationFrom(previous.getDate())) {
            return previous;
        } else {
            return next;
        }
    }

    /** Get the start date of the series.
     * @return the first date
     */
    public AbsoluteDate getFirstDate() {
        if (firstDate == null) {
            firstDate = tles.first().getDate();
        }
        return firstDate;
    }

    /** Get the last date of the series.
     * @return the end date
     */
    public AbsoluteDate getLastDate() {
        if (lastDate == null) {
            lastDate = tles.last().getDate();
        }
        return lastDate;
    }

    /** Get the first TLE.
     * @return first TLE
     */
    public TLE getFirst() {
        return (TLE) tles.first();
    }

    /** Get the last TLE.
     * @return last TLE
     */
    public TLE getLast() {
        return (TLE) tles.last();
    }

}
