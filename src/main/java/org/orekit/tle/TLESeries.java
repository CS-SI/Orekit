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
package org.orekit.tle;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.zip.GZIPInputStream;

import org.orekit.errors.OrekitException;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.ChronologicalComparator;
import org.orekit.time.TimeStamped;
import org.orekit.utils.PVCoordinates;

/** This class reads and handles series of TLEs, that have to be (for the moment)
 *  TLE from the same space object. It provides bounded ephemerides
 *  by finding the best initial TLE to propagate and then handling the
 *  propagation.
 *
 * @author Fabien Maussion
 * @version $Revision:1665 $ $Date:2008-06-11 12:12:59 +0200 (mer., 11 juin 2008) $
 */
public class TLESeries implements Serializable {

    /** Serializable UID. */
    private static final long serialVersionUID = -6657661179006608529L;

    /** Set containing all TLE entries. */
    private SortedSet<TimeStamped> tles;

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

    /** The satellite id. */
    private int satelliteNumber;

    /** International designator. */
    private String internationalDesignator;

    /** Simple constructor with a TLE file.
     * <p>Read TLE entries, if they match, are stored for later use. <p>
     * @param in the input to read (it can be compressed)
     * @exception IOException when the {@link InputStream} cannot be buffered.
     * @exception OrekitException when a file format error occurs
     */
    public TLESeries(final InputStream in)
        throws IOException, OrekitException {
        tles = new TreeSet<TimeStamped>(ChronologicalComparator.getInstance());
        internationalDesignator = null;
        satelliteNumber = 0;
        previous = null;
        next = null;
        read(in);
    }

    /** Read a TLE file.
     * <p> The read TLE entries, if they match, are stored into a treeset for later use. <p>
     * @param in the input to read (it can be compressed)
     * @exception IOException when the {@link InputStream} cannot be buffered.
     * @exception OrekitException when a format error occurs
     */
    private void read(final InputStream in)
        throws IOException, OrekitException {

        // TODO support additional formats, not portable enough
        final BufferedReader r = new BufferedReader(new InputStreamReader(checkCompressed(in)));
        try {
            for (String line1 = r.readLine(); line1 != null; line1 = r.readLine()) {

                // add second line
                final String line2 = r.readLine();
                if (line2 == null) {
                    throw new OrekitException("Missing second line in TLE", new Object[0]);
                }

                // safety checks
                if (!TLE.isFormatOK(line1, line2)) {
                    r.close();
                    throw new OrekitException("Non-TLE line in TLE data file", new Object[0]);
                }

                final int satNum = Integer.parseInt(line1.substring(2, 7).replace(' ', '0'));
                final String iD = line1.substring(9, 17);
                if (satelliteNumber == 0 && internationalDesignator == null) {
                    satelliteNumber = satNum;
                    internationalDesignator = iD;
                } else {
                    if ((satNum != satelliteNumber) || !iD.equals(internationalDesignator)) {
                        throw new OrekitException("The TLE's are not representing the same object.",
                                                  new Object[0]);
                    }
                }

                // everything seems OK
                tles.add(new TLE(line1, line2));

            }
        } finally {
            if (r != null) {
                try {
                    r.close();
                } catch (IOException ioe) {
                    throw new OrekitException(ioe.getMessage(), ioe);
                }
            }
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
        if ((lastTLE == null) || (toExtrapolate.getDate().compareTo(lastTLE.getDate()) != 0)) {
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
        if ((previous != null) && (date.minus(previous.getDate()) >= 0) &&
            (next     != null) && (date.minus(next.getDate())     <= 0)) {
            // the current selection is already good
            if (next.getDate().minus(date) > date.minus(previous.getDate())) {
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

        if (next.getDate().minus(date) > date.minus(previous.getDate())) {
            return previous;
        } else {
            return next;
        }
    }

    /** Get the start date of the serie.
     * @return the first date
     */
    public AbsoluteDate getFirstDate() {
        if (firstDate == null) {
            firstDate = ((TLE) tles.first()).getDate();
        }
        return firstDate;
    }

    /** Get the last date of the serie.
     * @return the end date
     */
    public AbsoluteDate getLastDate() {
        if (lastDate == null) {
            lastDate = ((TLE) tles.last()).getDate();
        }
        return lastDate;
    }

    /** checks if a file is compressed or not.
     * @param in the file to check.
     * @return a readable file.
     * @exception IOException if the file format is not understood.
     */
    private BufferedInputStream checkCompressed(final InputStream in)
        throws IOException {

        BufferedInputStream filter = new BufferedInputStream(in);
        filter.mark(1024 * 1024);

        boolean isCompressed = false;
        try {
            isCompressed = new GZIPInputStream(filter).read() != -1;
        } catch (IOException e) {
            isCompressed = false;
        }
        filter.reset();

        if (isCompressed) {
            filter = new BufferedInputStream(new GZIPInputStream(filter));
        }
        filter.mark(1024 * 1024);
        return filter;
    }

}
