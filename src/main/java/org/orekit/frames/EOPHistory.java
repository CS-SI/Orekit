/* Copyright 2002-2023 CS GROUP
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
package org.orekit.frames;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.analysis.interpolation.FieldHermiteInterpolator;
import org.hipparchus.analysis.interpolation.HermiteInterpolator;
import org.hipparchus.util.FastMath;
import org.hipparchus.util.MathArrays;
import org.orekit.annotation.DefaultDataContext;
import org.orekit.data.DataContext;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitInternalError;
import org.orekit.errors.OrekitMessages;
import org.orekit.errors.TimeStampedCacheException;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.time.TimeScales;
import org.orekit.time.TimeStamped;
import org.orekit.time.TimeVectorFunction;
import org.orekit.utils.Constants;
import org.orekit.utils.GenericTimeStampedCache;
import org.orekit.utils.IERSConventions;
import org.orekit.utils.ImmutableTimeStampedCache;
import org.orekit.utils.OrekitConfiguration;
import org.orekit.utils.TimeStampedCache;
import org.orekit.utils.TimeStampedGenerator;

/** This class loads any kind of Earth Orientation Parameter data throughout a large time range.
 * @author Pascal Parraud
 * @author Evan Ward
 */
public class EOPHistory implements Serializable {

    /** Default interpolation degree.
     * @since 12.0
     */
    public static final int DEFAULT_INTERPOLATION_DEGREE = 3;

    /** Serializable UID. */
    private static final long serialVersionUID = 20231022L;

    /** Interpolation degree.
     * @since 12.0
     */
    private final int interpolationDegree;

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
    private final transient TimeVectorFunction tidalCorrection;

    /** Time scales to use when computing corrections. */
    private final transient TimeScales timeScales;

    /** Simple constructor.
     *
     * <p>This method uses the {@link DataContext#getDefault() default data context}.
     *
     * @param conventions IERS conventions to which EOP refers
     * @param interpolationDegree interpolation degree (must be of the form 4k-1)
     * @param data the EOP data to use
     * @param simpleEOP if true, tidal effects are ignored when interpolating EOP
     * @see #EOPHistory(IERSConventions, int, Collection, boolean, TimeScales)
     */
    @DefaultDataContext
    protected EOPHistory(final IERSConventions conventions,
                         final int interpolationDegree,
                         final Collection<? extends EOPEntry> data,
                         final boolean simpleEOP) {
        this(conventions, interpolationDegree, data, simpleEOP, DataContext.getDefault().getTimeScales());
    }

    /** Simple constructor.
     * @param conventions IERS conventions to which EOP refers
     * @param interpolationDegree interpolation degree (must be of the form 4k-1)
     * @param data the EOP data to use
     * @param simpleEOP if true, tidal effects are ignored when interpolating EOP
     * @param timeScales to use when computing EOP corrections.
     * @since 10.1
     */
    public EOPHistory(final IERSConventions conventions,
                      final int interpolationDegree,
                      final Collection<? extends EOPEntry> data,
                      final boolean simpleEOP,
                      final TimeScales timeScales) {
        this(conventions, interpolationDegree, data,
             simpleEOP ? null : new CachedCorrection(conventions.getEOPTidalCorrection(timeScales)),
             timeScales);
    }

    /** Simple constructor.
     * @param conventions IERS conventions to which EOP refers
     * @param interpolationDegree interpolation degree (must be of the form 4k-1)
     * @param data the EOP data to use
     * @param tidalCorrection correction to apply to EOP
     * @param timeScales to use when computing EOP corrections
     * @since 12
     */
    private EOPHistory(final IERSConventions conventions,
                       final int interpolationDegree,
                       final Collection<? extends EOPEntry> data,
                       final TimeVectorFunction tidalCorrection,
                       final TimeScales timeScales) {

        // check interpolation degree is 4k-1
        final int k = (interpolationDegree + 1) / 4;
        if (interpolationDegree != 4 * k - 1) {
            throw new OrekitException(OrekitMessages.WRONG_EOP_INTERPOLATION_DEGREE, interpolationDegree);
        }

        this.conventions         = conventions;
        this.interpolationDegree = interpolationDegree;
        this.tidalCorrection     = tidalCorrection;
        this.timeScales          = timeScales;
        if (data.size() >= 1) {
            // enough data to interpolate
            if (missSomeDerivatives(data)) {
                // we need to estimate the missing derivatives
                final ImmutableTimeStampedCache<EOPEntry> rawCache =
                                new ImmutableTimeStampedCache<>(FastMath.min(interpolationDegree + 1, data.size()), data);
                final List<EOPEntry>fixedData = new ArrayList<>();
                for (final EOPEntry entry : rawCache.getAll()) {
                    fixedData.add(fixDerivatives(entry, rawCache));
                }
                cache = new ImmutableTimeStampedCache<>(FastMath.min(interpolationDegree + 1, fixedData.size()), fixedData);
            } else {
                cache = new ImmutableTimeStampedCache<>(FastMath.min(interpolationDegree + 1, data.size()), data);
            }
            hasData = true;
        } else {
            // not enough data to interpolate -> always use null correction
            cache   = ImmutableTimeStampedCache.emptyCache();
            hasData = false;
        }
    }

    /**
     * Determine if this history uses simplified EOP corrections.
     *
     * @return {@code true} if tidal corrections are ignored, {@code false} otherwise.
     */
    public boolean isSimpleEop() {
        return tidalCorrection == null;
    }

    /** Get interpolation degree.
     * @return interpolation degree
     * @since 12.0
     */
    public int getInterpolationDegree() {
        return interpolationDegree;
    }

    /**
     * Get the time scales used in computing EOP corrections.
     *
     * @return set of time scales.
     * @since 10.1
     */
    public TimeScales getTimeScales() {
        return timeScales;
    }

    /** Get version of the instance that does not cache tidal correction.
     * @return version of the instance that does not cache tidal correction
     * @since 12.0
     */
    public EOPHistory getEOPHistoryWithoutCachedTidalCorrection() {
        return new EOPHistory(conventions, interpolationDegree, getEntries(),
                              conventions.getEOPTidalCorrection(timeScales),
                              timeScales);
    }

    /** Check if the instance caches tidal corrections.
     * @return true if the instance caches tidal corrections
     * @since 12.0
     */
    public boolean cachesTidalCorrection() {
        return tidalCorrection instanceof CachedCorrection;
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

        // we have EOP data -> interpolate offset
        try {
            final DUT1Interpolator interpolator = new DUT1Interpolator(date);
            getNeighbors(date, interpolationDegree + 1).forEach(interpolator);
            double interpolated = interpolator.getInterpolated();
            if (tidalCorrection != null) {
                interpolated += tidalCorrection.value(date)[2];
            }
            return interpolated;
        } catch (TimeStampedCacheException tce) {
            //this should not happen because of date check above
            throw new OrekitInternalError(tce);
        }

    }

    /** Get the UT1-UTC value.
     * <p>The data provided comes from the IERS files. It is smoothed data.</p>
     * @param date date at which the value is desired
     * @param <T> type of the field elements
     * @return UT1-UTC in seconds (0 if date is outside covered range)
     * @since 9.0
     */
    public <T extends CalculusFieldElement<T>> T getUT1MinusUTC(final FieldAbsoluteDate<T> date) {

        //check if there is data for date
        final AbsoluteDate absDate = date.toAbsoluteDate();
        if (!this.hasDataFor(absDate)) {
            // no EOP data available for this date, we use a default 0.0 offset
            return (tidalCorrection == null) ? date.getField().getZero() : tidalCorrection.value(date)[2];
        }

        // we have EOP data -> interpolate offset
        try {
            final FieldDUT1Interpolator<T> interpolator = new FieldDUT1Interpolator<>(date, absDate);
            getNeighbors(absDate, interpolationDegree + 1).forEach(interpolator);
            T interpolated = interpolator.getInterpolated();
            if (tidalCorrection != null) {
                interpolated = interpolated.add(tidalCorrection.value(date)[2]);
            }
            return interpolated;
        } catch (TimeStampedCacheException tce) {
            //this should not happen because of date check above
            throw new OrekitInternalError(tce);
        }

    }

    /** Local class for DUT1 interpolation, crossing leaps safely. */
    private static class DUT1Interpolator implements Consumer<EOPEntry> {

        /** DUT at first entry. */
        private double firstDUT;

        /** Indicator for dates just before a leap occurring during the interpolation sample. */
        private boolean beforeLeap;

        /** Interpolator to use. */
        private final HermiteInterpolator interpolator;

        /** Interpolation date. */
        private AbsoluteDate date;

        /** Simple constructor.
         * @param date interpolation date
         */
        DUT1Interpolator(final AbsoluteDate date) {
            this.firstDUT     = Double.NaN;
            this.beforeLeap   = true;
            this.interpolator = new HermiteInterpolator();
            this.date         = date;
        }

        /** {@inheritDoc} */
        @Override
        public void accept(final EOPEntry neighbor) {
            if (Double.isNaN(firstDUT)) {
                firstDUT = neighbor.getUT1MinusUTC();
            }
            final double dut;
            if (neighbor.getUT1MinusUTC() - firstDUT > 0.9) {
                // there was a leap second between the entries
                dut = neighbor.getUT1MinusUTC() - 1.0;
                // UTCScale considers the discontinuity to occur at the start of the leap
                // second so this code must use the same convention. EOP entries are time
                // stamped at midnight UTC so 1 second before is the start of the leap
                // second.
                if (neighbor.getDate().shiftedBy(-1).compareTo(date) <= 0) {
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

        /** Get the interpolated value.
         * @return interpolated value
         */
        public double getInterpolated() {
            final double interpolated = interpolator.value(0)[0];
            return beforeLeap ? interpolated : interpolated + 1.0;
        }

    }

    /** Local class for DUT1 interpolation, crossing leaps safely. */
    private static class FieldDUT1Interpolator<T extends CalculusFieldElement<T>> implements Consumer<EOPEntry> {

        /** DUT at first entry. */
        private double firstDUT;

        /** Indicator for dates just before a leap occurring during the interpolation sample. */
        private boolean beforeLeap;

        /** Interpolator to use. */
        private final FieldHermiteInterpolator<T> interpolator;

        /** Interpolation date. */
        private FieldAbsoluteDate<T> date;

        /** Interpolation date. */
        private AbsoluteDate absDate;

        /** Simple constructor.
         * @param date interpolation date
         * @param absDate interpolation date
         */
        FieldDUT1Interpolator(final FieldAbsoluteDate<T> date, final AbsoluteDate absDate) {
            this.firstDUT     = Double.NaN;
            this.beforeLeap   = true;
            this.interpolator = new FieldHermiteInterpolator<>();
            this.date         = date;
            this.absDate      = absDate;
        }

        /** {@inheritDoc} */
        @Override
        public void accept(final EOPEntry neighbor) {
            if (Double.isNaN(firstDUT)) {
                firstDUT = neighbor.getUT1MinusUTC();
            }
            final double dut;
            if (neighbor.getUT1MinusUTC() - firstDUT > 0.9) {
                // there was a leap second between the entries
                dut = neighbor.getUT1MinusUTC() - 1.0;
                if (neighbor.getDate().compareTo(absDate) <= 0) {
                    beforeLeap = false;
                }
            } else {
                dut = neighbor.getUT1MinusUTC();
            }
            final T[] array = MathArrays.buildArray(date.getField(), 1);
            array[0] = date.getField().getZero().add(dut);
            interpolator.addSamplePoint(date.durationFrom(neighbor.getDate()).negate(),
                                        array);
        }

        /** Get the interpolated value.
         * @return interpolated value
         */
        public T getInterpolated() {
            final T interpolated = interpolator.value(date.getField().getZero())[0];
            return beforeLeap ? interpolated : interpolated.add(1.0);
        }

    }

    /**
     * Get the entries surrounding a central date.
     * <p>
     * See {@link #hasDataFor(AbsoluteDate)} to determine if the cache has data
     * for {@code central} without throwing an exception.
     *
     * @param central central date
     * @param n number of neighbors
     * @return array of cached entries surrounding specified date
     * @since 12.0
     */
    protected Stream<EOPEntry> getNeighbors(final AbsoluteDate central, final int n) {
        return cache.getNeighbors(central, n);
    }

    /** Get the LoD (Length of Day) value.
     * <p>The data provided comes from the IERS files. It is smoothed data.</p>
     * @param date date at which the value is desired
     * @return LoD in seconds (0 if date is outside covered range)
     */
    public double getLOD(final AbsoluteDate date) {

        // check if there is data for date
        if (!this.hasDataFor(date)) {
            // no EOP data available for this date, we use a default null correction
            return (tidalCorrection == null) ? 0.0 : tidalCorrection.value(date)[3];
        }

        // we have EOP data for date -> interpolate correction
        double interpolated = interpolate(date, entry -> entry.getLOD());
        if (tidalCorrection != null) {
            interpolated += tidalCorrection.value(date)[3];
        }
        return interpolated;

    }

    /** Get the LoD (Length of Day) value.
     * <p>The data provided comes from the IERS files. It is smoothed data.</p>
     * @param date date at which the value is desired
     * @param <T> type of the filed elements
     * @return LoD in seconds (0 if date is outside covered range)
     * @since 9.0
     */
    public <T extends CalculusFieldElement<T>> T getLOD(final FieldAbsoluteDate<T> date) {

        final AbsoluteDate aDate = date.toAbsoluteDate();

        // check if there is data for date
        if (!this.hasDataFor(aDate)) {
            // no EOP data available for this date, we use a default null correction
            return (tidalCorrection == null) ? date.getField().getZero() : tidalCorrection.value(date)[3];
        }

        // we have EOP data for date -> interpolate correction
        T interpolated = interpolate(date, aDate, entry -> entry.getLOD());
        if (tidalCorrection != null) {
            interpolated = interpolated.add(tidalCorrection.value(date)[3]);
        }

        return interpolated;

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

        // we have EOP data for date -> interpolate correction
        final double[] interpolated = interpolate(date,
                                                  entry -> entry.getX(),     entry -> entry.getY(),
                                                  entry -> entry.getXRate(), entry -> entry.getYRate());
        if (tidalCorrection != null) {
            final double[] correction = tidalCorrection.value(date);
            interpolated[0] += correction[0];
            interpolated[1] += correction[1];
        }
        return new PoleCorrection(interpolated[0], interpolated[1]);

    }

    /** Get the pole IERS Reference Pole correction.
     * <p>The data provided comes from the IERS files. It is smoothed data.</p>
     * @param date date at which the correction is desired
     * @param <T> type of the field elements
     * @return pole correction ({@link PoleCorrection#NULL_CORRECTION
     * PoleCorrection.NULL_CORRECTION} if date is outside covered range)
     */
    public <T extends CalculusFieldElement<T>> FieldPoleCorrection<T> getPoleCorrection(final FieldAbsoluteDate<T> date) {

        final AbsoluteDate aDate = date.toAbsoluteDate();

        // check if there is data for date
        if (!this.hasDataFor(aDate)) {
            // no EOP data available for this date, we use a default null correction
            if (tidalCorrection == null) {
                return new FieldPoleCorrection<>(date.getField().getZero(), date.getField().getZero());
            } else {
                final T[] correction = tidalCorrection.value(date);
                return new FieldPoleCorrection<>(correction[0], correction[1]);
            }
        }

        // we have EOP data for date -> interpolate correction
        final T[] interpolated = interpolate(date, aDate, entry -> entry.getX(), entry -> entry.getY());
        if (tidalCorrection != null) {
            final T[] correction = tidalCorrection.value(date);
            interpolated[0] = interpolated[0].add(correction[0]);
            interpolated[1] = interpolated[1].add(correction[1]);
        }
        return new FieldPoleCorrection<>(interpolated[0], interpolated[1]);

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

        // we have EOP data for date -> interpolate correction
        return interpolate(date, entry -> entry.getDdPsi(), entry -> entry.getDdEps());

    }

    /** Get the correction to the nutation parameters for equinox-based paradigm.
     * <p>The data provided comes from the IERS files. It is smoothed data.</p>
     * @param date date at which the correction is desired
     * @param <T> type of the field elements
     * @return nutation correction in longitude ΔΨ and in obliquity Δε
     * (zero if date is outside covered range)
     */
    public <T extends CalculusFieldElement<T>> T[] getEquinoxNutationCorrection(final FieldAbsoluteDate<T> date) {

        final AbsoluteDate aDate = date.toAbsoluteDate();

        // check if there is data for date
        if (!this.hasDataFor(aDate)) {
            // no EOP data available for this date, we use a default null correction
            return MathArrays.buildArray(date.getField(), 2);
        }

        // we have EOP data for date -> interpolate correction
        return interpolate(date, aDate, entry -> entry.getDdPsi(), entry -> entry.getDdEps());

    }

    /** Get the correction to the nutation parameters for Non-Rotating Origin paradigm.
     * <p>The data provided comes from the IERS files. It is smoothed data.</p>
     * @param date date at which the correction is desired
     * @return nutation correction in Celestial Intermediate Pole coordinates
     * δX and δY (zero if date is outside covered range)
     */
    public double[] getNonRotatinOriginNutationCorrection(final AbsoluteDate date) {

        // check if there is data for date
        if (!this.hasDataFor(date)) {
            // no EOP data available for this date, we use a default null correction
            return new double[2];
        }

        // we have EOP data for date -> interpolate correction
        return interpolate(date, entry -> entry.getDx(), entry -> entry.getDy());

    }

    /** Get the correction to the nutation parameters for Non-Rotating Origin paradigm.
     * <p>The data provided comes from the IERS files. It is smoothed data.</p>
     * @param date date at which the correction is desired
     * @param <T> type of the filed elements
     * @return nutation correction in Celestial Intermediate Pole coordinates
     * δX and δY (zero if date is outside covered range)
     */
    public <T extends CalculusFieldElement<T>> T[] getNonRotatinOriginNutationCorrection(final FieldAbsoluteDate<T> date) {

        final AbsoluteDate aDate = date.toAbsoluteDate();

        // check if there is data for date
        if (!this.hasDataFor(aDate)) {
            // no EOP data available for this date, we use a default null correction
            return MathArrays.buildArray(date.getField(), 2);
        }

        // we have EOP data for date -> interpolate correction
        return interpolate(date, aDate, entry -> entry.getDx(), entry -> entry.getDy());

    }

    /** Get the ITRF version.
     * @param date date at which the value is desired
     * @return ITRF version of the EOP covering the specified date
     * @since 9.2
     */
    public ITRFVersion getITRFVersion(final AbsoluteDate date) {

        // check if there is data for date
        if (!this.hasDataFor(date)) {
            // no EOP data available for this date, we use a default ITRF 2014
            return ITRFVersion.ITRF_2014;
        }

        try {
            // we have EOP data for date
            final Optional<EOPEntry> first = getNeighbors(date, 1).findFirst();
            return first.isPresent() ? first.get().getITRFType() : ITRFVersion.ITRF_2014;

        } catch (TimeStampedCacheException tce) {
            // this should not happen because of date check performed at start
            throw new OrekitInternalError(tce);
        }

    }

    /** Check Earth orientation parameters continuity.
     * @param maxGap maximal allowed gap between entries (in seconds)
     */
    public void checkEOPContinuity(final double maxGap) {
        TimeStamped preceding = null;
        for (final TimeStamped current : this.cache.getAll()) {

            // compare the dates of preceding and current entries
            if (preceding != null && (current.getDate().durationFrom(preceding.getDate())) > maxGap) {
                throw new OrekitException(OrekitMessages.MISSING_EARTH_ORIENTATION_PARAMETERS_BETWEEN_DATES_GAP,
                                          preceding.getDate(), current.getDate(),
                                          current.getDate().durationFrom(preceding.getDate()));
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
    public List<EOPEntry> getEntries() {
        return cache.getAll();
    }

    /** Interpolate a single EOP component.
     * <p>
     * This method should be called <em>only</em> when {@link #hasDataFor(AbsoluteDate)} returns true.
     * </p>
     * @param date interpolation date
     * @param selector selector for EOP entry component
     * @return interpolated value
     */
    private double interpolate(final AbsoluteDate date, final Function<EOPEntry, Double> selector) {
        try {
            final HermiteInterpolator interpolator = new HermiteInterpolator();
            getNeighbors(date, interpolationDegree + 1).
                forEach(entry -> interpolator.addSamplePoint(entry.getDate().durationFrom(date),
                                                             new double[] {
                                                                 selector.apply(entry)
                                                             }));
            return interpolator.value(0)[0];
        } catch (TimeStampedCacheException tce) {
            // this should not happen because of date check performed by caller
            throw new OrekitInternalError(tce);
        }
    }

    /** Interpolate a single EOP component.
     * <p>
     * This method should be called <em>only</em> when {@link #hasDataFor(AbsoluteDate)} returns true.
     * </p>
     * @param date interpolation date
     * @param aDate interpolation date, as an {@link AbsoluteDate}
     * @param selector selector for EOP entry component
     * @param <T> type of the field elements
     * @return interpolated value
     */
    private <T extends CalculusFieldElement<T>> T interpolate(final FieldAbsoluteDate<T> date,
                                                              final AbsoluteDate aDate,
                                                              final Function<EOPEntry, Double> selector) {
        try {
            final FieldHermiteInterpolator<T> interpolator = new FieldHermiteInterpolator<>();
            final T[] y = MathArrays.buildArray(date.getField(), 1);
            final T zero = date.getField().getZero();
            final FieldAbsoluteDate<T> central = new FieldAbsoluteDate<>(aDate, zero); // here, we attempt to get a constant date,
                                                                                       // for example removing derivatives
                                                                                       // if T was DerivativeStructure
            getNeighbors(aDate, interpolationDegree + 1).
                forEach(entry -> {
                    y[0] = zero.add(selector.apply(entry));
                    interpolator.addSamplePoint(central.durationFrom(entry.getDate()).negate(), y);
                });
            return interpolator.value(date.durationFrom(central))[0]; // here, we introduce derivatives again (in DerivativeStructure case)
        } catch (TimeStampedCacheException tce) {
            // this should not happen because of date check performed by caller
            throw new OrekitInternalError(tce);
        }
    }

    /** Interpolate two EOP components.
     * <p>
     * This method should be called <em>only</em> when {@link #hasDataFor(AbsoluteDate)} returns true.
     * </p>
     * @param date interpolation date
     * @param selector1 selector for first EOP entry component
     * @param selector2 selector for second EOP entry component
     * @return interpolated value
     */
    private double[] interpolate(final AbsoluteDate date,
                                 final Function<EOPEntry, Double> selector1,
                                 final Function<EOPEntry, Double> selector2) {
        try {
            final HermiteInterpolator interpolator = new HermiteInterpolator();
            getNeighbors(date, interpolationDegree + 1).
                forEach(entry -> interpolator.addSamplePoint(entry.getDate().durationFrom(date),
                                                             new double[] {
                                                                 selector1.apply(entry),
                                                                 selector2.apply(entry)
                                                             }));
            return interpolator.value(0);
        } catch (TimeStampedCacheException tce) {
            // this should not happen because of date check performed by caller
            throw new OrekitInternalError(tce);
        }
    }

    /** Interpolate two EOP components.
     * <p>
     * This method should be called <em>only</em> when {@link #hasDataFor(AbsoluteDate)} returns true.
     * </p>
     * @param date interpolation date
     * @param selector1 selector for first EOP entry component
     * @param selector2 selector for second EOP entry component
     * @param selector1Rate selector for first EOP entry component rate
     * @param selector2Rate selector for second EOP entry component rate
     * @return interpolated value
     * @since 12.0
     */
    private double[] interpolate(final AbsoluteDate date,
                                 final Function<EOPEntry, Double> selector1,
                                 final Function<EOPEntry, Double> selector2,
                                 final Function<EOPEntry, Double> selector1Rate,
                                 final Function<EOPEntry, Double> selector2Rate) {
        try {
            final HermiteInterpolator interpolator = new HermiteInterpolator();
            getNeighbors(date, (interpolationDegree + 1) / 2).
                forEach(entry -> interpolator.addSamplePoint(entry.getDate().durationFrom(date),
                                                             new double[] {
                                                                 selector1.apply(entry),
                                                                 selector2.apply(entry)
                                                             },
                                                             new double[] {
                                                                 selector1Rate.apply(entry),
                                                                 selector2Rate.apply(entry)
                                                             }));
            return interpolator.value(0);
        } catch (TimeStampedCacheException tce) {
            // this should not happen because of date check performed by caller
            throw new OrekitInternalError(tce);
        }
    }

    /** Interpolate two EOP components.
     * <p>
     * This method should be called <em>only</em> when {@link #hasDataFor(AbsoluteDate)} returns true.
     * </p>
     * @param date interpolation date
     * @param aDate interpolation date, as an {@link AbsoluteDate}
     * @param selector1 selector for first EOP entry component
     * @param selector2 selector for second EOP entry component
     * @param <T> type of the field elements
     * @return interpolated value
     */
    private <T extends CalculusFieldElement<T>> T[] interpolate(final FieldAbsoluteDate<T> date,
                                                                final AbsoluteDate aDate,
                                                                final Function<EOPEntry, Double> selector1,
                                                                final Function<EOPEntry, Double> selector2) {
        try {
            final FieldHermiteInterpolator<T> interpolator = new FieldHermiteInterpolator<>();
            final T[] y = MathArrays.buildArray(date.getField(), 2);
            final T zero = date.getField().getZero();
            final FieldAbsoluteDate<T> central = new FieldAbsoluteDate<>(aDate, zero); // here, we attempt to get a constant date,
                                                                                       // for example removing derivatives
                                                                                       // if T was DerivativeStructure
            getNeighbors(aDate, interpolationDegree + 1).
                forEach(entry -> {
                    y[0] = zero.add(selector1.apply(entry));
                    y[1] = zero.add(selector2.apply(entry));
                    interpolator.addSamplePoint(central.durationFrom(entry.getDate()).negate(), y);
                });
            return interpolator.value(date.durationFrom(central)); // here, we introduce derivatives again (in DerivativeStructure case)
        } catch (TimeStampedCacheException tce) {
            // this should not happen because of date check performed by caller
            throw new OrekitInternalError(tce);
        }
    }

    /** Check if some derivatives are missing.
     * @param raw raw EOP history
     * @return true if some derivatives are missing
     * @since 12.0
     */
    private boolean missSomeDerivatives(final Collection<? extends EOPEntry> raw) {
        for (final EOPEntry entry : raw) {
            if (Double.isNaN(entry.getLOD() + entry.getXRate() + entry.getYRate())) {
                return true;
            }
        }
        return false;
    }

    /** Fix missing derivatives.
     * @param entry EOP entry to fix
     * @param rawCache raw EOP history cache
     * @return fixed entry
     * @since 12.0
     */
    private EOPEntry fixDerivatives(final EOPEntry entry, final ImmutableTimeStampedCache<EOPEntry> rawCache) {

        // helper function to differentiate some EOP parameters
        final BiFunction<EOPEntry, Function<EOPEntry, Double>, Double> differentiator =
                        (e, selector) -> {
                            final HermiteInterpolator interpolator = new HermiteInterpolator();
                            rawCache.getNeighbors(e.getDate()).
                                forEach(f -> interpolator.addSamplePoint(f.getDate().durationFrom(e.getDate()),
                                                                         new double[] {
                                                                             selector.apply(f)
                                                                         }));
                            return interpolator.derivatives(0.0, 1)[1][0];
                        };

        if (Double.isNaN(entry.getLOD() + entry.getXRate() + entry.getYRate())) {
            final double lod   = Double.isNaN(entry.getLOD()) ?
                                 -differentiator.apply(entry, e -> e.getUT1MinusUTC()) :
                                 entry.getLOD();
            final double xRate = Double.isNaN(entry.getXRate()) ?
                                 differentiator.apply(entry, e -> e.getX()) :
                                 entry.getXRate();
            final double yRate = Double.isNaN(entry.getYRate()) ?
                                 differentiator.apply(entry, e -> e.getY()) :
                                 entry.getYRate();
            return new EOPEntry(entry.getMjd(),
                                entry.getUT1MinusUTC(), lod,
                                entry.getX(), entry.getY(), xRate, yRate,
                                entry.getDdPsi(), entry.getDdEps(),
                                entry.getDx(), entry.getDy(),
                                entry.getITRFType(), entry.getDate());
        } else {
            // the entry already has all derivatives
            return entry;
        }
    }

    /** Replace the instance with a data transfer object for serialization.
     * @return data transfer object that will be serialized
     */
    @DefaultDataContext
    private Object writeReplace() {
        return new DataTransferObject(conventions, interpolationDegree, getEntries(), tidalCorrection == null);
    }

    /** Internal class used only for serialization. */
    @DefaultDataContext
    private static class DataTransferObject implements Serializable {

        /** Serializable UID. */
        private static final long serialVersionUID = 20231122L;

        /** IERS conventions. */
        private final IERSConventions conventions;

        /** Interpolation degree.
         * @since 12.0
         */
        private final int interpolationDegree;

        /** EOP entries. */
        private final List<EOPEntry> entries;

        /** Indicator for simple interpolation without tidal effects. */
        private final boolean simpleEOP;

        /** Simple constructor.
         * @param conventions IERS conventions to which EOP refers
         * @param interpolationDegree interpolation degree (must be of the form 4k-1)
         * @param entries the EOP data to use
         * @param simpleEOP if true, tidal effects are ignored when interpolating EOP
         * @since 12.0
         */
        DataTransferObject(final IERSConventions conventions,
                           final int interpolationDegree,
                           final List<EOPEntry> entries,
                           final boolean simpleEOP) {
            this.conventions         = conventions;
            this.interpolationDegree = interpolationDegree;
            this.entries             = entries;
            this.simpleEOP           = simpleEOP;
        }

        /** Replace the deserialized data transfer object with a {@link EOPHistory}.
         * @return replacement {@link EOPHistory}
         */
        private Object readResolve() {
            try {
                return new EOPHistory(conventions, interpolationDegree, entries, simpleEOP);
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
        implements TimeVectorFunction, TimeStampedGenerator<TidalCorrectionEntry> {

        /** Correction to apply to EOP (may be null). */
        private final TimeVectorFunction tidalCorrection;

        /** Step between generated entries. */
        private final double step;

        /** Tidal corrections entries cache. */
        private final TimeStampedCache<TidalCorrectionEntry> cache;

        /** Simple constructor.
         * @param tidalCorrection function computing the tidal correction
         */
        CachedCorrection(final TimeVectorFunction tidalCorrection) {
            this.step            = 60 * 60;
            this.tidalCorrection = tidalCorrection;
            this.cache           =
                new GenericTimeStampedCache<TidalCorrectionEntry>(8,
                                                                  OrekitConfiguration.getCacheSlotsNumber(),
                                                                  Constants.JULIAN_DAY * 30,
                                                                  Constants.JULIAN_DAY,
                                                                  this);
        }

        /** {@inheritDoc} */
        @Override
        public double[] value(final AbsoluteDate date) {
            try {
                // set up an interpolator
                final HermiteInterpolator interpolator = new HermiteInterpolator();
                cache.getNeighbors(date).forEach(entry -> interpolator.addSamplePoint(entry.date.durationFrom(date), entry.correction));

                // interpolate to specified date
                return interpolator.value(0.0);
            } catch (TimeStampedCacheException tsce) {
                // this should never happen
                throw new OrekitInternalError(tsce);
            }
        }

        /** {@inheritDoc} */
        @Override
        public <T extends CalculusFieldElement<T>> T[] value(final FieldAbsoluteDate<T> date) {
            try {

                final AbsoluteDate aDate = date.toAbsoluteDate();

                final FieldHermiteInterpolator<T> interpolator = new FieldHermiteInterpolator<>();
                final T[] y = MathArrays.buildArray(date.getField(), 4);
                final T zero = date.getField().getZero();
                final FieldAbsoluteDate<T> central = new FieldAbsoluteDate<>(aDate, zero); // here, we attempt to get a constant date,
                                                                                           // for example removing derivatives
                                                                                           // if T was DerivativeStructure
                cache.getNeighbors(aDate).forEach(entry -> {
                    for (int i = 0; i < y.length; ++i) {
                        y[i] = zero.add(entry.correction[i]);
                    }
                    interpolator.addSamplePoint(central.durationFrom(entry.getDate()).negate(), y);
                });

                // interpolate to specified date
                return interpolator.value(date.durationFrom(central)); // here, we introduce derivatives again (in DerivativeStructure case)

            } catch (TimeStampedCacheException tsce) {
                // this should never happen
                throw new OrekitInternalError(tsce);
            }
        }

        /** {@inheritDoc} */
        @Override
        public List<TidalCorrectionEntry> generate(final AbsoluteDate existingDate, final AbsoluteDate date) {

            final List<TidalCorrectionEntry> generated = new ArrayList<TidalCorrectionEntry>();

            if (existingDate == null) {

                // no prior existing entries, just generate a first set
                for (int i = -cache.getMaxNeighborsSize() / 2; generated.size() < cache.getMaxNeighborsSize(); ++i) {
                    final AbsoluteDate t = date.shiftedBy(i * step);
                    generated.add(new TidalCorrectionEntry(t, tidalCorrection.value(t)));
                }

            } else {

                // some entries have already been generated
                // add the missing ones up to specified date

                AbsoluteDate t = existingDate;
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
