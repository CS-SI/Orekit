/* Copyright 2002-2012 CS Systèmes d'Information
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
package org.orekit.frames;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.commons.math3.analysis.interpolation.HermiteInterpolator;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.errors.TimeStampedCacheException;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.ChronologicalComparator;
import org.orekit.time.TimeStamped;
import org.orekit.utils.Constants;
import org.orekit.utils.OrekitConfiguration;
import org.orekit.utils.TimeStampedCache;
import org.orekit.utils.TimeStampedGenerator;

/** This class loads any kind of Earth Orientation Parameter data throughout a large time range.
 * @author Pascal Parraud
 */
public abstract class AbstractEOPHistory implements Serializable, EOPHistory {

    /** Serializable UID. */
    private static final long serialVersionUID = 5659073889129159070L;

    /** Number of points to use in interpolation. */
    private static final int INTERPOLATION_POINTS = 4;

    /** Earth Orientation Parameter entries. */
    private final SortedSet<TimeStamped> entries;

    /** EOP history entries. */
    private final TimeStampedCache<EOPEntry> cache;

    /** Simple constructor.
     */
    protected AbstractEOPHistory() {
        entries = new TreeSet<TimeStamped>(new ChronologicalComparator());
        cache   = new TimeStampedCache<EOPEntry>(INTERPOLATION_POINTS,
                                                 OrekitConfiguration.getCacheSlotsNumber(), Constants.JULIAN_YEAR,
                                                 30 * Constants.JULIAN_DAY,
                                                 new Generator(), EOPEntry.class);
    }

    /** Add an Earth Orientation Parameters entry.
     * @param entry entry to add
     */
    public void addEntry(final EOPEntry entry) {
        entries.add(entry);
    }

    /** {@inheritDoc} */
    public Iterator<TimeStamped> iterator() {
        return entries.iterator();
    }

    /** {@inheritDoc} */
    public int size() {
        return cache.getEntries();
    }

    /** {@inheritDoc} */
    public AbsoluteDate getStartDate() {
        return entries.first().getDate();
    }

    /** {@inheritDoc} */
    public AbsoluteDate getEndDate() {
        return entries.last().getDate();
    }

    /** {@inheritDoc} */
    public double getUT1MinusUTC(final AbsoluteDate date) {
        try {
            final EOPEntry[] neighbors = getNeighbors(date);
            final HermiteInterpolator interpolator = new HermiteInterpolator();
            final double firstDUT = neighbors[0].getUT1MinusUTC();
            boolean beforeLeap = true;
            for (final EOPEntry neighbor : neighbors) {
                final double dut;
                if (neighbor.getUT1MinusUTC() - firstDUT > 0.9) {
                    // there was a leap second between the entries
                    dut = neighbor.getUT1MinusUTC() - 1.0;
                    if (neighbor.getDate().compareTo(date) <= 0) {
                        beforeLeap = false;
                    }
                } else {
                    dut = neighbor.getUT1MinusUTC();
                }
                interpolator.addSamplePoint(neighbor.getDate().durationFrom(date),
                                            new double[] {
                                                dut
                                            });
            }
            final double interpolated = interpolator.value(0)[0];
            return beforeLeap ? interpolated : interpolated + 1.0;
        } catch (TimeStampedCacheException tce) {
            // no EOP data available for this date, we use a default 0.0 offset
            return 0.0;
        }
    }

    /** Get the entries surrounding a central date.
     * @param central central date
     * @return array of cached entries surrounding specified date
     * @exception TimeStampedCacheException if EOP data cannot be retrieved
     */
    protected EOPEntry[] getNeighbors(final AbsoluteDate central) throws TimeStampedCacheException {
        return cache.getNeighbors(central);
    }

    /** {@inheritDoc} */
    public double getLOD(final AbsoluteDate date) {
        try {
            final HermiteInterpolator interpolator = new HermiteInterpolator();
            for (final EOPEntry entry : getNeighbors(date)) {
                interpolator.addSamplePoint(entry.getDate().durationFrom(date),
                                            new double[] {
                                                entry.getLOD()
                                            });
            }
            return interpolator.value(0)[0];
        } catch (TimeStampedCacheException tce) {
            // no EOP data available for this date, we use a default null correction
            return 0.0;
        }
    }

    /** Get the pole IERS Reference Pole correction.
     * <p>The data provided comes from the IERS files. It is smoothed data.</p>
     * @param date date at which the correction is desired
     * @return pole correction ({@link PoleCorrection#NULL_CORRECTION
     * PoleCorrection.NULL_CORRECTION} if date is outside covered range)
     */
    public PoleCorrection getPoleCorrection(final AbsoluteDate date) {
        try {
            final HermiteInterpolator interpolator = new HermiteInterpolator();
            for (final EOPEntry entry : getNeighbors(date)) {
                interpolator.addSamplePoint(entry.getDate().durationFrom(date),
                                            new double[] {
                                                entry.getX(), entry.getY()
                                            });
            }
            final double[] interpolated = interpolator.value(0);
            return new PoleCorrection(interpolated[0], interpolated[1]);
        } catch (TimeStampedCacheException tce) {
            // no EOP data available for this date, we use a default null correction
            return PoleCorrection.NULL_CORRECTION;
        }
    }

    /** Check Earth orientation parameters continuity.
     * @param maxGap maximal allowed gap between entries (in seconds)
     * @exception OrekitException if there are holes in the data sequence
     */
    public void checkEOPContinuity(final double maxGap) throws OrekitException {
        TimeStamped preceding = null;
        for (final TimeStamped current : entries) {

            // compare the dates of preceding and current entries
            if ((preceding != null) && ((current.getDate().durationFrom(preceding.getDate())) > maxGap)) {
                throw new OrekitException(OrekitMessages.MISSING_EARTH_ORIENTATION_PARAMETERS_BETWEEN_DATES,
                                          preceding.getDate(), current.getDate());

            }

            // prepare next iteration
            preceding = current;

        }
    }

    /** Local generator for entries. */
    private class Generator implements TimeStampedGenerator<EOPEntry> {

        /** {@inheritDoc} */
        public List<EOPEntry> generate(final EOPEntry existing, final AbsoluteDate date)
            throws TimeStampedCacheException {

            final List<EOPEntry> generated = new ArrayList<EOPEntry>();

            // depending on the user provided EOP file, entry points are expected to
            // be every day or every 5 days, using 5 days is a safe margin
            final double timeMargin = 5 * Constants.JULIAN_DAY;
            final AbsoluteDate start;
            final AbsoluteDate end;
            if (existing == null) {
                start = date.shiftedBy(-INTERPOLATION_POINTS * timeMargin);
                end   = date.shiftedBy(INTERPOLATION_POINTS * timeMargin);
            } else if (existing.getDate().compareTo(date) <= 0) {
                start = existing.getDate();
                end   = date.shiftedBy(INTERPOLATION_POINTS * timeMargin);
            } else {
                start = date.shiftedBy(-INTERPOLATION_POINTS * timeMargin);
                end   = existing.getDate();
            }

            // gather entries in the interval from existing to date (with some margins)
            for (TimeStamped ts : entries.tailSet(start).headSet(end)) {
                generated.add((EOPEntry) ts);
            }

            if (generated.isEmpty()) {
                if (entries.isEmpty()) {
                    throw new TimeStampedCacheException(OrekitMessages.UNABLE_TO_GENERATE_NEW_DATA_AFTER, date);
                } else if (entries.last().getDate().compareTo(date) < 0) {
                    throw new TimeStampedCacheException(OrekitMessages.UNABLE_TO_GENERATE_NEW_DATA_AFTER,
                                                        entries.last().getDate());
                } else {
                    throw new TimeStampedCacheException(OrekitMessages.UNABLE_TO_GENERATE_NEW_DATA_BEFORE,
                                                        entries.first().getDate());
                }
            }

            return generated;

        }

    }

}
