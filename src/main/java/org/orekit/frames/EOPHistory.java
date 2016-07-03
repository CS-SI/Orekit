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
package org.orekit.frames;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.hipparchus.analysis.interpolation.HermiteInterpolator;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitInternalError;
import org.orekit.errors.OrekitMessages;
import org.orekit.errors.TimeStampedCacheException;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeFunction;
import org.orekit.time.TimeStamped;
import org.orekit.utils.Constants;
import org.orekit.utils.GenericTimeStampedCache;
import org.orekit.utils.IERSConventions;
import org.orekit.utils.ImmutableTimeStampedCache;
import org.orekit.utils.OrekitConfiguration;
import org.orekit.utils.TimeStampedCache;
import org.orekit.utils.TimeStampedGenerator;

/** This class loads any kind of Earth Orientation Parameter data throughout a large time range.
 * @author Pascal Parraud
 */
public class EOPHistory implements Serializable {

    /** Serializable UID. */
    private static final long serialVersionUID = 20131010L;

    /** Number of points to use in interpolation. */
    private static final int INTERPOLATION_POINTS = 4;

    /**
     * If this history has any EOP data.
     *
     * @see #hasDataFor(AbsoluteDate)
     */
    private final boolean hasData;

    /** EOP history entries. */
    private final transient ImmutableTimeStampedCache<EOPEntry> cache;

    /** IERS conventions to which EOP refers. */
    private final IERSConventions conventions;

    /** Correction to apply to EOP (may be null). */
    private final transient TimeFunction<double[]> tidalCorrection;

    /** Simple constructor.
     * @param conventions IERS conventions to which EOP refers
     * @param data the EOP data to use
     * @param simpleEOP if true, tidal effects are ignored when interpolating EOP
     * @exception OrekitException if tidal correction model cannot be loaded
     */
    protected EOPHistory(final IERSConventions conventions,
                         final Collection<EOPEntry> data,
                         final boolean simpleEOP)
        throws OrekitException {
        this(conventions, data, simpleEOP ? null : new CachedCorrection(conventions.getEOPTidalCorrection()));
    }

    /** Simple constructor.
     * @param conventions IERS conventions to which EOP refers
     * @param data the EOP data to use
     * @param tidalCorrection correction to apply to EOP
     * @exception OrekitException if tidal correction model cannot be loaded
     */
    private EOPHistory(final IERSConventions conventions,
                         final Collection<EOPEntry> data,
                         final TimeFunction<double[]> tidalCorrection)
        throws OrekitException {
        this.conventions      = conventions;
        this.tidalCorrection  = tidalCorrection;
        if (data.size() >= INTERPOLATION_POINTS) {
            // enough data to interpolate
            cache = new ImmutableTimeStampedCache<EOPEntry>(INTERPOLATION_POINTS, data);
            hasData = true;
        } else {
            // not enough data to interpolate -> always use null correction
            cache = ImmutableTimeStampedCache.emptyCache();
            hasData = false;
        }
    }

    /** Get non-interpolating version of the instance.
     * @return non-interpolatig version of the instance
     * @exception OrekitException if tidal correction model cannot be loaded
     */
    public EOPHistory getNonInterpolatingEOPHistory()
        throws OrekitException {
        return new EOPHistory(conventions, getEntries(), conventions.getEOPTidalCorrection());
    }

    /** Check if the instance uses interpolation on tidal corrections.
     * @return true if the instance uses interpolation on tidal corrections
     */
    public boolean usesInterpolation() {
        return tidalCorrection != null && tidalCorrection instanceof CachedCorrection;
    }

    /** Get the IERS conventions to which these EOP apply.
     * @return IERS conventions to which these EOP apply
     */
    public IERSConventions getConventions() {
        return conventions;
    }

    /** Get the date of the first available Earth Orientation Parameters.
     * @return the start date of the available data
     */
    public AbsoluteDate getStartDate() {
        return this.cache.getEarliest().getDate();
    }

    /** Get the date of the last available Earth Orientation Parameters.
     * @return the end date of the available data
     */
    public AbsoluteDate getEndDate() {
        return this.cache.getLatest().getDate();
    }

    /** Get the UT1-UTC value.
     * <p>The data provided comes from the IERS files. It is smoothed data.</p>
     * @param date date at which the value is desired
     * @return UT1-UTC in seconds (0 if date is outside covered range)
     */
    public double getUT1MinusUTC(final AbsoluteDate date) {
        //check if there is data for date
        if (!this.hasDataFor(date)) {
            // no EOP data available for this date, we use a default 0.0 offset
            return (tidalCorrection == null) ? 0.0 : tidalCorrection.value(date)[2];
        }
        //we have EOP data -> interpolate offset
        try {
            final List<EOPEntry> neighbors = getNeighbors(date);
            final HermiteInterpolator interpolator = new HermiteInterpolator();
            final double firstDUT = neighbors.get(0).getUT1MinusUTC();
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
            double interpolated = interpolator.value(0)[0];
            if (tidalCorrection != null) {
                interpolated += tidalCorrection.value(date)[2];
            }
            return beforeLeap ? interpolated : interpolated + 1.0;
        } catch (TimeStampedCacheException tce) {
            //this should not happen because of date check above
            throw new OrekitInternalError(tce);
        }
    }

    /**
     * Get the entries surrounding a central date.
     * <p>
     * See {@link #hasDataFor(AbsoluteDate)} to determine if the cache has data
     * for {@code central} without throwing an exception.
     *
     * @param central central date
     * @return array of cached entries surrounding specified date
     * @exception TimeStampedCacheException if EOP data cannot be retrieved
     */
    protected List<EOPEntry> getNeighbors(final AbsoluteDate central) throws TimeStampedCacheException {
        return cache.getNeighbors(central);
    }

    /** Get the LoD (Length of Day) value.
     * <p>The data provided comes from the IERS files. It is smoothed data.</p>
     * @param date date at which the value is desired
     * @return LoD in seconds (0 if date is outside covered range)
     */
    public double getLOD(final AbsoluteDate date) {
        //check if there is data for date
        if (!this.hasDataFor(date)) {
            // no EOP data available for this date, we use a default null correction
            return (tidalCorrection == null) ? 0.0 : tidalCorrection.value(date)[3];
        }
        //we have EOP data for date -> interpolate correction
        try {
            final HermiteInterpolator interpolator = new HermiteInterpolator();
            for (final EOPEntry entry : getNeighbors(date)) {
                interpolator.addSamplePoint(entry.getDate().durationFrom(date),
                                            new double[] {
                                                entry.getLOD()
                                            });
            }
            double interpolated = interpolator.value(0)[0];
            if (tidalCorrection != null) {
                interpolated += tidalCorrection.value(date)[3];
            }
            return interpolated;
        } catch (TimeStampedCacheException tce) {
            // this should not happen because of date check above
            throw new OrekitInternalError(tce);
        }
    }

    /** Get the pole IERS Reference Pole correction.
     * <p>The data provided comes from the IERS files. It is smoothed data.</p>
     * @param date date at which the correction is desired
     * @return pole correction ({@link PoleCorrection#NULL_CORRECTION
     * PoleCorrection.NULL_CORRECTION} if date is outside covered range)
     */
    public PoleCorrection getPoleCorrection(final AbsoluteDate date) {
        // check if there is data for date
        if (!this.hasDataFor(date)) {
            // no EOP data available for this date, we use a default null correction
            if (tidalCorrection == null) {
                return PoleCorrection.NULL_CORRECTION;
            } else {
                final double[] correction = tidalCorrection.value(date);
                return new PoleCorrection(correction[0], correction[1]);
            }
        }
        //we have EOP data for date -> interpolate correction
        try {
            final HermiteInterpolator interpolator = new HermiteInterpolator();
            for (final EOPEntry entry : getNeighbors(date)) {
                interpolator.addSamplePoint(entry.getDate().durationFrom(date),
                                            new double[] {
                                                entry.getX(), entry.getY()
                                            });
            }
            final double[] interpolated = interpolator.value(0);
            if (tidalCorrection != null) {
                final double[] correction = tidalCorrection.value(date);
                interpolated[0] += correction[0];
                interpolated[1] += correction[1];
            }
            return new PoleCorrection(interpolated[0], interpolated[1]);
        } catch (TimeStampedCacheException tce) {
            // this should not happen because of date check above
            throw new OrekitInternalError(tce);
        }
    }

    /** Get the correction to the nutation parameters for equinox-based paradigm.
     * <p>The data provided comes from the IERS files. It is smoothed data.</p>
     * @param date date at which the correction is desired
     * @return nutation correction in longitude ΔΨ and in obliquity Δε
     * (zero if date is outside covered range)
     */
    public double[] getEquinoxNutationCorrection(final AbsoluteDate date) {
        // check if there is data for date
        if (!this.hasDataFor(date)) {
            // no EOP data available for this date, we use a default null correction
            return new double[2];
        }
        //we have EOP data for date -> interpolate correction
        try {
            final HermiteInterpolator interpolator = new HermiteInterpolator();
            for (final EOPEntry entry : getNeighbors(date)) {
                interpolator.addSamplePoint(entry.getDate().durationFrom(date),
                                            new double[] {
                                                entry.getDdPsi(), entry.getDdEps()
                                            });
            }
            return interpolator.value(0);
        } catch (TimeStampedCacheException tce) {
            // this should not happen because of date check above
            throw new OrekitInternalError(tce);
        }
    }

    /** Get the correction to the nutation parameters for Non-Rotating Origin paradigm.
     * <p>The data provided comes from the IERS files. It is smoothed data.</p>
     * @param date date at which the correction is desired
     * @return nutation correction in Celestial Intermediat Pole coordinates
     * δX and δY (zero if date is outside covered range)
     */
    public double[] getNonRotatinOriginNutationCorrection(final AbsoluteDate date) {
        // check if there is data for date
        if (!this.hasDataFor(date)) {
            // no EOP data available for this date, we use a default null correction
            return new double[2];
        }
        //we have EOP data for date -> interpolate correction
        try {
            final HermiteInterpolator interpolator = new HermiteInterpolator();
            for (final EOPEntry entry : getNeighbors(date)) {
                interpolator.addSamplePoint(entry.getDate().durationFrom(date),
                                            new double[] {
                                                entry.getDx(), entry.getDy()
                                            });
            }
            return interpolator.value(0);
        } catch (TimeStampedCacheException tce) {
            // this should not happen because of date check above
            throw new OrekitInternalError(tce);
        }
    }

    /** Check Earth orientation parameters continuity.
     * @param maxGap maximal allowed gap between entries (in seconds)
     * @exception OrekitException if there are holes in the data sequence
     */
    public void checkEOPContinuity(final double maxGap) throws OrekitException {
        TimeStamped preceding = null;
        for (final TimeStamped current : this.cache.getAll()) {

            // compare the dates of preceding and current entries
            if ((preceding != null) && ((current.getDate().durationFrom(preceding.getDate())) > maxGap)) {
                throw new OrekitException(OrekitMessages.MISSING_EARTH_ORIENTATION_PARAMETERS_BETWEEN_DATES,
                                          preceding.getDate(), current.getDate());
            }

            // prepare next iteration
            preceding = current;

        }
    }

    /**
     * Check if the cache has data for the given date using
     * {@link #getStartDate()} and {@link #getEndDate()}.
     *
     * @param date the requested date
     * @return true if the {@link #cache} has data for the requested date, false
     *         otherwise.
     */
    protected boolean hasDataFor(final AbsoluteDate date) {
        /*
         * when there is no EOP data, short circuit getStartDate, which will
         * throw an exception
         */
        return this.hasData && this.getStartDate().compareTo(date) <= 0 &&
               date.compareTo(this.getEndDate()) <= 0;
    }

    /** Get a non-modifiable view of the EOP entries.
     * @return non-modifiable view of the EOP entries
     */
    List<EOPEntry> getEntries() {
        return cache.getAll();
    }

    /** Replace the instance with a data transfer object for serialization.
     * <p>
     * This intermediate class serializes only the frame key.
     * </p>
     * @return data transfer object that will be serialized
     */
    private Object writeReplace() {
        return new DataTransferObject(conventions, getEntries(), tidalCorrection == null);
    }

    /** Internal class used only for serialization. */
    private static class DataTransferObject implements Serializable {

        /** Serializable UID. */
        private static final long serialVersionUID = 20131010L;

        /** IERS conventions. */
        private final IERSConventions conventions;

        /** EOP entries. */
        private final List<EOPEntry> entries;

        /** Indicator for simple interpolation without tidal effects. */
        private final boolean simpleEOP;

        /** Simple constructor.
         * @param conventions IERS conventions to which EOP refers
         * @param entries the EOP data to use
         * @param simpleEOP if true, tidal effects are ignored when interpolating EOP
         */
        DataTransferObject(final IERSConventions conventions,
                                  final List<EOPEntry> entries,
                                  final boolean simpleEOP) {
            this.conventions = conventions;
            this.entries     = entries;
            this.simpleEOP   = simpleEOP;
        }

        /** Replace the deserialized data transfer object with a {@link EOPHistory}.
         * @return replacement {@link EOPHistory}
         */
        private Object readResolve() {
            try {
                // retrieve a managed frame
                return new EOPHistory(conventions, entries, simpleEOP);
            } catch (OrekitException oe) {
                throw new OrekitInternalError(oe);
            }
        }

    }

    /** Internal class for caching tidal correction. */
    private static class TidalCorrectionEntry implements TimeStamped {

        /** Entry date. */
        private final AbsoluteDate date;

        /** Correction. */
        private final double[] correction;

        /** Simple constructor.
         * @param date entry date
         * @param correction correction on the EOP parameters (xp, yp, ut1, lod)
         */
        TidalCorrectionEntry(final AbsoluteDate date, final double[] correction) {
            this.date       = date;
            this.correction = correction;
        }

        /** {@inheritDoc} */
        @Override
        public AbsoluteDate getDate() {
            return date;
        }

    }

    /** Local generator for thread-safe cache. */
    private static class CachedCorrection
        implements TimeFunction<double[]>, TimeStampedGenerator<TidalCorrectionEntry> {

        /** Correction to apply to EOP (may be null). */
        private final TimeFunction<double[]> tidalCorrection;

        /** Step between generated entries. */
        private final double step;

        /** Tidal corrections entries cache. */
        private final TimeStampedCache<TidalCorrectionEntry> cache;

        /** Simple constructor.
         * @param tidalCorrection function computing the tidal correction
         */
        CachedCorrection(final TimeFunction<double[]> tidalCorrection) {
            this.step            = 60 * 60;
            this.tidalCorrection = tidalCorrection;
            this.cache           =
                new GenericTimeStampedCache<TidalCorrectionEntry>(8,
                                                                  OrekitConfiguration.getCacheSlotsNumber(),
                                                                  Constants.JULIAN_DAY * 30,
                                                                  Constants.JULIAN_DAY,
                                                                  this,
                                                                  TidalCorrectionEntry.class);
        }

        /** {@inheritDoc} */
        @Override
        public double[] value(final AbsoluteDate date) {
            try {
                // set up an interpolator
                final HermiteInterpolator interpolator = new HermiteInterpolator();
                for (final TidalCorrectionEntry entry : cache.getNeighbors(date)) {
                    interpolator.addSamplePoint(entry.date.durationFrom(date), entry.correction);
                }

                // interpolate to specified date
                return interpolator.value(0.0);
            } catch (TimeStampedCacheException tsce) {
                // this should never happen
                throw new OrekitInternalError(tsce);
            }
        }

        /** {@inheritDoc} */
        @Override
        public List<TidalCorrectionEntry> generate(final TidalCorrectionEntry existing, final AbsoluteDate date) {

            final List<TidalCorrectionEntry> generated = new ArrayList<TidalCorrectionEntry>();

            if (existing == null) {

                // no prior existing entries, just generate a first set
                for (int i = -cache.getNeighborsSize() / 2; generated.size() < cache.getNeighborsSize(); ++i) {
                    final AbsoluteDate t = date.shiftedBy(i * step);
                    generated.add(new TidalCorrectionEntry(t, tidalCorrection.value(t)));
                }

            } else {

                // some entries have already been generated
                // add the missing ones up to specified date

                AbsoluteDate t = existing.getDate();
                if (date.compareTo(t) > 0) {
                    // forward generation
                    do {
                        t = t.shiftedBy(step);
                        generated.add(new TidalCorrectionEntry(t, tidalCorrection.value(t)));
                    } while (t.compareTo(date) <= 0);
                } else {
                    // backward generation
                    do {
                        t = t.shiftedBy(-step);
                        generated.add(0, new TidalCorrectionEntry(t, tidalCorrection.value(t)));
                    } while (t.compareTo(date) >= 0);
                }
            }

            // return the generated transforms
            return generated;

        }
    }

}
