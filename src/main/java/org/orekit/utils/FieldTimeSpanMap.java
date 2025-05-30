/* Copyright 2002-2025 CS GROUP
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
package org.orekit.utils;

import java.util.Collections;
import java.util.Comparator;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.function.Consumer;

import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.time.FieldTimeStamped;
import org.hipparchus.Field;
import org.hipparchus.CalculusFieldElement;
import org.orekit.time.AbsoluteDate;

/** Container for objects that apply to spans of time.

 * <p>
 * Time span maps can be seen either as an ordered collection of
 * {@link Span time spans} or as an ordered collection
 * of {@link Transition transitions}. Both views are dual one to
 * each other. A time span extends from one transition to the
 * next one, and a transition separates one time span from the
 * next one. Each time span contains one entry that is valid during
 * the time span; this entry may be null if nothing is valid during
 * this time span.
 * </p>
 * <p>
 * Typical uses of {@link FieldTimeSpanMap} are to hold piecewise data, like for
 * example an orbit count that changes at ascending nodes (in which case the
 * entry would be an {@link Integer}), or a visibility status between several
 * objects (in which case the entry would be a {@link Boolean}), or a drag
 * coefficient that is expected to be estimated daily or three-hourly.
 * </p>
 * <p>
 * Time span maps are built progressively. At first, they contain one
 * {@link Span time span} only whose validity extends from past infinity to
 * future infinity. Then new entries are added one at a time, associated with
 * transition dates, in order to build up the complete map. The transition dates
 * can be either the start of validity (when calling {@link #addValidAfter(Object,
 * FieldAbsoluteDate, boolean)}), or the end of the validity (when calling {@link
 * #addValidBefore(Object, FieldAbsoluteDate, boolean)}). Entries are often added at one
 * end only (and mainly in chronological order), but this is not required. It is
 * possible for example to first set up a map that covers a large range (say one day),
 * and then to insert intermediate dates using for example propagation and event
 * detectors to carve out some parts. This is akin to the way Binary Space Partitioning
 * Trees work.
 * </p>
 * <p>
 * Since 13.1, this class is thread-safe
 * </p>
 * @param <T> Type of the data.
 * @param <F> type of the date field elements

 * @author Luc Maisonobe
 * @since 7.1
 */
public class FieldTimeSpanMap<T, F extends CalculusFieldElement<F>> {

    /** Field.*/
    private final Field<F> field;

    /** Reference to last accessed data.
     * @since 13.1
     */
    private Span<T, F> current;

    /** First span.
     * @since 13.1
     */
    private Span<T, F> firstSpan;

    /** Last span.
     * @since 13.1
     */
    private Span<T, F> lastSpan;

    /** End of early expunged range.
     * @since 13.1
     */
    private FieldAbsoluteDate<F> expungedEarly;

    /** Start of late expunged range.
     * @since 13.1
     */
    private FieldAbsoluteDate<F> expungedLate;

    /** Number of time spans.
     * @since 13.1
     */
    private int nbSpans;

    /** Maximum number of time spans.
     * @since 13.1
     */
    private int maxNbSpans;

    /** Maximum time range between the earliest and the latest transitions.
     * @since 13.1
     */
    private double maxRange;

    /** Expunge policy.
     * @since 13.1
     */
    private ExpungePolicy expungePolicy;

    /** Create a map containing a single object, initially valid throughout the timeline.
     * <p>
     * The real validity of this first entry will be truncated as other
     * entries are either {@link #addValidBefore(Object, FieldAbsoluteDate, boolean)
     * added before} it or {@link #addValidAfter(Object, FieldAbsoluteDate, boolean)
     * added after} it.
     * </p>
     * <p>
     * The initial {@link #configureExpunge(int, double, ExpungePolicy) expunge policy}
     * is to never expunge any entries, it can be changed afterward by calling
     * {@link #configureExpunge(int, double, ExpungePolicy)}
     * </p>
     * @param entry entry (initially valid throughout the timeline)
     * @param field field used by default.
     */
    public FieldTimeSpanMap(final T entry, final Field<F> field) {
        this.field     = field;
        this.current   = new Span<>(field, entry);
        this.firstSpan = current;
        this.lastSpan  = current;
        this.nbSpans   = 1;
        configureExpunge(Integer.MAX_VALUE, Double.POSITIVE_INFINITY, ExpungePolicy.EXPUNGE_FARTHEST);
    }

    /** Configure (or reconfigure) expunge policy for later additions.
     * <p>
     * When an entry is added to the map (using either {@link #addValidBefore(Object, FieldAbsoluteDate, boolean)},
     * {@link #addValidBetween(Object, FieldAbsoluteDate, FieldAbsoluteDate)}, or
     * {@link #addValidAfter(Object, FieldAbsoluteDate, boolean)} that exceeds the allowed capacity in terms
     * of number of time spans or maximum time range between the earliest and the latest transitions,
     * then exceeding data is expunged according to the {@code expungePolicy}.
     * </p>
     * <p>
     * Note that as the policy depends on the date at which new entries are added, the policy will be enforced
     * only for the <em>next</em> calls to {@link #addValidBefore(Object, FieldAbsoluteDate, boolean)},
     * {@link #addValidBetween(Object, FieldAbsoluteDate, FieldAbsoluteDate)}, and {@link #addValidAfter(Object,
     * FieldAbsoluteDate, boolean)}, it is <em>not</em> enforce immediately.
     * </p>
     * @param newMaxNbSpans maximum number of time spans
     * @param newMaxRange maximum time range between the earliest and the latest transitions
     * @param newExpungePolicy expunge policy to apply when capacity is exceeded
     * @since 13.1
     */
    public synchronized void configureExpunge(final int newMaxNbSpans, final double newMaxRange, final ExpungePolicy newExpungePolicy) {
        this.maxNbSpans    = newMaxNbSpans;
        this.maxRange      = newMaxRange;
        this.expungePolicy = newExpungePolicy;
        this.expungedEarly = FieldAbsoluteDate.getPastInfinity(field);
        this.expungedLate  = FieldAbsoluteDate.getFutureInfinity(field);
    }

    /** Get the number of spans.
     * <p>
     * The number of spans is always at least 1. The number of transitions
     * is always 1 lower than the number of spans.
     * </p>
     * @return number of spans
     * @since 13.1
     */
    public synchronized int getSpansNumber() {
        return nbSpans;
    }

    /** Add an entry valid before a limit date.
     * <p>
     * This method just calls {@link #addValidBefore(Object, FieldAbsoluteDate, boolean)
     * addValidBefore(entry, latestValidityDate, false)}.
     * </p>
     * @param entry entry to add
     * @param latestValidityDate date before which the entry is valid
     * @deprecated as of 13.1, replaced by {@link #addValidBefore(Object, FieldAbsoluteDate, boolean)}
     */
    @Deprecated
    public void addValidBefore(final T entry, final FieldAbsoluteDate<F> latestValidityDate) {
        addValidBefore(entry, latestValidityDate, false);
    }

    /** Add an entry valid before a limit date.
     * <p>
     * As an entry is valid, it truncates the validity of the neighboring
     * entries already present in the map.
     * </p>
     * <p>
     * If the map already contains transitions that occur earlier than {@code latestValidityDate},
     * the {@code erasesEarlier} parameter controls what to do with them. Let's consider
     * the time span [tₖ; tₖ₊₁[ associated with entry eₖ that would have been valid at time
     * {@code latestValidityDate} prior to the call to the method (i.e. tₖ &lt;
     * {@code latestValidityDate} &lt; tₖ₊₁).
     * </p>
     * <ul>
     *  <li>if {@code erasesEarlier} is {@code true}, then all earlier transitions
     *      up to and including tₖ are erased, and the {@code entry} will be valid from past infinity
     *      to {@code latestValidityDate}</li>
     *  <li>if {@code erasesEarlier} is {@code false}, then all earlier transitions
     *      are preserved, and the {@code entry} will be valid from tₖ
     *      to {@code latestValidityDate}</li>
     *  </ul>
     * <p>
     * In both cases, the existing entry eₖ time span will be truncated and will be valid
     * only from {@code latestValidityDate} to tₖ₊₁.
     * </p>
     * @param entry entry to add
     * @param latestValidityDate date before which the entry is valid
     * @param erasesEarlier if true, the entry erases all existing transitions
     * that are earlier than {@code latestValidityDate}
     * @return span with added entry
     * @since 13.1
     */
    public synchronized Span<T, F> addValidBefore(final T entry, final FieldAbsoluteDate<F> latestValidityDate, final boolean erasesEarlier) {

        // update current reference to transition date
        locate(latestValidityDate);

        if (erasesEarlier) {

            // drop everything before date
            current.start = null;

            // update count
            nbSpans = 0;
            for (Span<T, F> span = current; span != null; span = span.next()) {
                ++nbSpans;
            }

        }

        final Span<T, F> span = new Span<>(field, entry);

        final Transition<T, F> start = current.getStartTransition();
        if (start != null && start.getDate().equals(latestValidityDate)) {
            // the transition at the start of the current span is at the exact same date
            // we update it, without adding a new transition
            if (start.previous() != null) {
                start.previous().setAfter(span);
            }
            start.setBefore(span);
            updateFirstIfNeeded(span);
        } else {

            if (current.getStartTransition() != null) {
                current.getStartTransition().setAfter(span);
            }

            // we need to add a new transition somewhere inside the current span
            insertTransition(latestValidityDate, span, current);

        }

        // we consider the last added transition as the new current one
        current = span;

        expungeOldData(latestValidityDate);

        return span;

    }

    /** Add an entry valid after a limit date.
     * <p>
     * This method just calls {@link #addValidAfter(Object, FieldAbsoluteDate, boolean)
     * addValidAfter(entry, earliestValidityDate, false)}.
     * </p>
     * @param entry entry to add
     * @param earliestValidityDate date after which the entry is valid
     * @deprecated as of 13.1, replaced by {@link #addValidAfter(Object, FieldAbsoluteDate, boolean)}
     */
    @Deprecated
    public void addValidAfter(final T entry, final FieldAbsoluteDate<F> earliestValidityDate) {
        addValidAfter(entry, earliestValidityDate, false);
    }

    /** Add an entry valid after a limit date.
     * <p>
     * As an entry is valid, it truncates or overrides the validity of the neighboring
     * entries already present in the map.
     * </p>
     * <p>
     * If the map already contains transitions that occur later than {@code earliestValidityDate},
     * the {@code erasesLater} parameter controls what to do with them. Let's consider
     * the time span [tₖ; tₖ₊₁[ associated with entry eₖ that would have been valid at time
     * {@code earliestValidityDate} prior to the call to the method (i.e. tₖ &lt;
     * {@code earliestValidityDate} &lt; tₖ₊₁).
     * </p>
     * <ul>
     *  <li>if {@code erasesLater} is {@code true}, then all later transitions
     *      from and including tₖ₊₁ are erased, and the {@code entry} will be valid from
     *      {@code earliestValidityDate} to future infinity</li>
     *  <li>if {@code erasesLater} is {@code false}, then all later transitions
     *      are preserved, and the {@code entry} will be valid from {@code earliestValidityDate}
     *      to tₖ₊₁</li>
     *  </ul>
     * <p>
     * In both cases, the existing entry eₖ time span will be truncated and will be valid
     * only from tₖ to {@code earliestValidityDate}.
     * </p>
     * @param entry entry to add
     * @param earliestValidityDate date after which the entry is valid
     * @param erasesLater if true, the entry erases all existing transitions
     * that are later than {@code earliestValidityDate}
     * @return span with added entry
     * @since 13.1
     */
    public synchronized Span<T, F> addValidAfter(final T entry, final FieldAbsoluteDate<F> earliestValidityDate, final boolean erasesLater) {

        // update current reference to transition date
        locate(earliestValidityDate);

        if (erasesLater) {

            // drop everything after date
            current.end = null;

            // update count
            nbSpans = 0;
            for (Span<T, F> span = current; span != null; span = span.previous()) {
                ++nbSpans;
            }

        }

        final Span<T, F> span = new Span<>(field, entry);
        if (current.getEndTransition() != null) {
            current.getEndTransition().setBefore(span);
        }

        final Transition<T, F> start = current.getStartTransition();
        if (start != null && start.getDate().equals(earliestValidityDate)) {
            // the transition at the start of the current span is at the exact same date
            // we update it, without adding a new transition
            start.setAfter(span);
            updateLastIfNeeded(span);
        } else {
            // we need to add a new transition somewhere inside the current span
            insertTransition(earliestValidityDate, current, span);
        }

        // we consider the last added transition as the new current one
        current = span;

        // update metadata
        expungeOldData(earliestValidityDate);

        return span;

    }

    /** Add an entry valid between two limit dates.
     * <p>
     * As an entry is valid, it truncates or overrides the validity of the neighboring
     * entries already present in the map.
     * </p>
     * @param entry entry to add
     * @param earliestValidityDate date after which the entry is valid
     * @param latestValidityDate date before which the entry is valid
     * @return span with added entry
     * @since 13.1
     */
    public synchronized Span<T, F> addValidBetween(final T entry,
                                                   final FieldAbsoluteDate<F> earliestValidityDate,
                                                   final FieldAbsoluteDate<F> latestValidityDate) {

        // handle special cases
        if (AbsoluteDate.PAST_INFINITY.equals(earliestValidityDate.toAbsoluteDate())) {
            if (AbsoluteDate.FUTURE_INFINITY.equals(latestValidityDate.toAbsoluteDate())) {
                // we wipe everything in the map
                current   = new Span<>(field, entry);
                firstSpan = current;
                lastSpan  = current;
                return current;
            } else {
                // we wipe from past infinity
                return addValidBefore(entry, latestValidityDate, true);
            }
        } else if (AbsoluteDate.FUTURE_INFINITY.equals(latestValidityDate.toAbsoluteDate())) {
            // we wipe up to future infinity
            return addValidAfter(entry, earliestValidityDate, true);
        } else {

            // locate spans at earliest and latest dates
            locate(earliestValidityDate);
            Span<T, F> latest = current;
            while (latest.getEndTransition() != null && latest.getEnd().isBeforeOrEqualTo(latestValidityDate)) {
                latest = latest.next();
                --nbSpans;
            }
            if (latest == current) {
                // the interval splits one transition in the middle, we need to duplicate the instance
                latest = new Span<>(field, current.data);
                if (current.getEndTransition() != null) {
                    current.getEndTransition().setBefore(latest);
                }
            }

            final Span<T, F> span = new Span<>(field, entry);

            // manage earliest transition
            final Transition<T, F> start = current.getStartTransition();
            if (start != null && start.getDate().equals(earliestValidityDate)) {
                // the transition at the start of the current span is at the exact same date
                // we update it, without adding a new transition
                start.setAfter(span);
                updateLastIfNeeded(span);
            } else {
                // we need to add a new transition somewhere inside the current span
                insertTransition(earliestValidityDate, current, span);
            }

            // manage latest transition
            insertTransition(latestValidityDate, span, latest);

            // we consider the last added transition as the new current one
            current = span;

            // update metadata
            final FieldAbsoluteDate<F> midDate = earliestValidityDate.
                                                 shiftedBy(latestValidityDate.durationFrom(earliestValidityDate).multiply(0.5));
            expungeOldData(midDate);

            return span;

        }

    }

    /** Get the entry valid at a specified date.
     * <p>
     * The expected complexity is O(1) for successive calls with
     * neighboring dates, which is the more frequent use in propagation
     * or orbit determination applications, and O(n) for random calls.
     * </p>
     * @param date date at which the entry must be valid
     * @return valid entry at specified date
     * @see #getSpan(FieldAbsoluteDate)
     */
    public synchronized T get(final FieldAbsoluteDate<F> date) {
        return getSpan(date).getData();
    }

    /** Get the time span containing a specified date.
     * <p>
     * The expected complexity is O(1) for successive calls with
     * neighboring dates, which is the more frequent use in propagation
     * or orbit determination applications, and O(n) for random calls.
     * </p>
     * @param date date belonging to the desired time span
     * @return time span containing the specified date
     * @since 13.1
     */
    public synchronized Span<T, F> getSpan(final FieldAbsoluteDate<F> date) {

        // safety check
        if (date.isBefore(expungedEarly) || date.isAfter(expungedLate)) {
            throw new OrekitException(OrekitMessages.EXPUNGED_SPAN, date);
        }

        locate(date);
        return current;
    }

    /** Locate the time span containing a specified date.
     * <p>
     * The {@code current} field is updated to the located span.
     * After the method returns, {@code current.getStartTransition()} is either
     * null or its date is before or equal to date, and {@code
     * current.getEndTransition()} is either null or its date is after date.
     * </p>
     * @param date date belonging to the desired time span
     * @since 13.1
     */
    private synchronized void locate(final FieldAbsoluteDate<F> date) {

        while (current.getStart().isAfter(date)) {
            // the current span is too late
            current = current.previous();
        }

        while (current.getEnd().isBeforeOrEqualTo(date)) {

            final Span<T, F> next = current.next();
            if (next == null) {
                // this happens when date is FUTURE_INFINITY
                return;
            }

            // the current span is too early
            current = next;

        }

    }

    /** Insert a transition.
     * @param date transition date
     * @param before span before transition
     * @param after span after transition
     * @since 13.1
     */
    private void insertTransition(final FieldAbsoluteDate<F> date, final Span<T, F> before, final Span<T, F> after) {
        final Transition<T, F> transition = new Transition<>(this, date);
        transition.setBefore(before);
        transition.setAfter(after);
        updateFirstIfNeeded(before);
        updateLastIfNeeded(after);
        ++nbSpans;
    }

    /** Get the first (earliest) transition.
     * @return first (earliest) transition, or null if there are no transitions
     * @since 13.1
     */
    public synchronized Transition<T, F> getFirstTransition() {
        return getFirstSpan().getEndTransition();
    }

    /** Get the last (latest) transition.
     * @return last (latest) transition, or null if there are no transitions
     * @since 13.1
     */
    public synchronized Transition<T, F> getLastTransition() {
        return getLastSpan().getStartTransition();
    }

    /** Get the first (earliest) span.
     * @return first (earliest) span
     * @since 13.1
     */
    public synchronized Span<T, F> getFirstSpan() {
        return firstSpan;
    }

    /** Get the first (earliest) span with non-null data.
     * @return first (earliest) span with non-null data
     * @since 13.1
     */
    public synchronized Span<T, F> getFirstNonNullSpan() {
        Span<T, F> span = getFirstSpan();
        while (span.getData() == null) {
            if (span.getEndTransition() == null) {
                throw new OrekitException(OrekitMessages.NO_CACHED_ENTRIES);
            }
            span = span.next();
        }
        return span;
    }

    /** Get the last (latest) span.
     * @return last (latest) span
     * @since 13.1
     */
    public synchronized Span<T, F> getLastSpan() {
        return lastSpan;
    }

    /** Get the last (latest) span with non-null data.
     * @return last (latest) span with non-null data
     * @since 13.1
     */
    public synchronized Span<T, F> getLastNonNullSpan() {
        Span<T, F> span = getLastSpan();
        while (span.getData() == null) {
            if (span.getStartTransition() == null) {
                throw new OrekitException(OrekitMessages.NO_CACHED_ENTRIES);
            }
            span = span.previous();
        }
        return span;
    }

    /** Extract a range of the map.
     * <p>
     * The object returned will be a new independent instance that will contain
     * only the transitions that lie in the specified range.
     * </p>
     * <p>
     * Consider, for example, a map containing objects O₀ valid before t₁, O₁ valid
     * between t₁ and t₂, O₂ valid between t₂ and t₃, O₃ valid between t₃ and t₄,
     * and O₄ valid after t₄. then calling this method with a {@code start}
     * date between t₁ and t₂ and a {@code end} date between t₃ and t₄
     * will result in a new map containing objects O₁ valid before t₂, O₂
     * valid between t₂ and t₃, and O₃ valid after t₃. The validity of O₁
     * is therefore extended in the past, and the validity of O₃ is extended
     * in the future.
     * </p>
     * @param start earliest date at which a transition is included in the range
     * (may be set to {@link AbsoluteDate#PAST_INFINITY} to keep all early transitions)
     * @param end latest date at which a transition is included in the r
     * (may be set to {@link AbsoluteDate#FUTURE_INFINITY} to keep all late transitions)
     * @return a new instance with all transitions restricted to the specified range
     * @since 13.1
     */
    public synchronized FieldTimeSpanMap<T, F> extractRange(final FieldAbsoluteDate<F> start,
                                                            final FieldAbsoluteDate<F> end) {

        Span<T, F>  span  = getSpan(start);
        final FieldTimeSpanMap<T, F> range = new FieldTimeSpanMap<>(span.getData(), field);
        while (span.getEndTransition() != null && span.getEndTransition().getDate().isBeforeOrEqualTo(end)) {
            span = span.next();
            range.addValidAfter(span.getData(), span.getStartTransition().getDate(), false);
        }

        return range;

    }

    /**
     * Performs an action for each non-null element of the map.
     * <p>
     * The action is performed chronologically.
     * </p>
     * @param action action to perform on the non-null elements
     * @since 13.1
     */
    public synchronized void forEach(final Consumer<T> action) {
        for (Span<T, F> span = getFirstSpan(); span != null; span = span.next()) {
            if (span.getData() != null) {
                action.accept(span.getData());
            }
        }
    }

    /**
     * Expunge old data.
     * @param date date of the latest added data
     */
    private synchronized void expungeOldData(final FieldAbsoluteDate<F> date) {

        while (nbSpans > maxNbSpans || lastSpan.getStart().durationFrom(firstSpan.getEnd()).getReal() > maxRange) {
            // capacity exceeded, we need to purge old data
            if (expungePolicy.expungeEarliest(date.toAbsoluteDate(),
                                              firstSpan.getEnd().toAbsoluteDate(),
                                              lastSpan.getStart().toAbsoluteDate())) {
                // we need to purge the earliest data
                if (firstSpan.getEnd().isAfter(expungedEarly)) {
                    expungedEarly  = firstSpan.getEnd();
                }
                firstSpan       = firstSpan.next();
                firstSpan.start = null;
                if (current.start == null) {
                    // the current span was the one we just expunged
                    // we need to update it
                    current = firstSpan;
                }
            } else {
                // we need to purge the latest data
                if (lastSpan.getStart().isBefore(expungedLate)) {
                    expungedLate = lastSpan.getStart();
                }
                lastSpan     = lastSpan.previous();
                lastSpan.end = null;
                if (current.end == null) {
                    // the current span was the one we just expunged
                    // we need to update it
                    current = lastSpan;
                }
            }
            --nbSpans;
        }

    }

    /** Update first span if needed.
     * @param candidate candidate first span
     * @since 13.1
     */
    private void updateFirstIfNeeded(final Span<T, F> candidate) {
        if (candidate.getStartTransition() == null) {
            firstSpan = candidate;
        }
    }

    /** Update last span if needed.
     * @param candidate candidate last span
     * @since 13.1
     */
    private void updateLastIfNeeded(final Span<T, F> candidate) {
        if (candidate.getEndTransition() == null) {
            lastSpan = candidate;
        }
    }

    /** Get an unmodifiable view of the sorted transitions.
     * <p>
     * Note that since 13.1, this method creates a copy of the current data,
     * it therefore does not update when new spans are added
     * </p>
     * @return unmodifiable view of the sorted transitions
     * @deprecated as of 13.1, this method is replaced by {@link #getFirstTransition()}
     * and then following intertwined links between {@link Span Span} and {@link Transition Transition}
     */
    @Deprecated
    public SortedSet<Transition<T, F>> getTransitions() {
        final SortedSet<Transition<T, F>> copy =
                new TreeSet<>(Comparator.comparing(Transition::getDate));
        for (Transition<T, F> transition = getFirstTransition(); transition != null; transition = transition.next()) {
            copy.add(transition);
        }
        return Collections.unmodifiableSortedSet(copy);
    }

    /** Class holding transition times.
     * <p>
     * This data type is dual to {@link Span}, it is
     * focused on one transition date, and gives access to
     * surrounding valid data whereas {@link Span} is focused
     * on one valid data, and gives access to surrounding
     * transition dates.
     * </p>
     * @param <S> Type of the data.
     * @param <F> Type of the field elements
     * @since 13.1
     */
    public static class Transition<S, F extends CalculusFieldElement<F>> implements FieldTimeStamped<F> {

        /** Map this transition belongs to. */
        private final FieldTimeSpanMap<S, F> map;

        /** Transition date. */
        private FieldAbsoluteDate<F> date;

        /** Entry valid before the transition. */
        private Span<S, F> before;

        /** Entry valid after the transition. */
        private Span<S, F> after;

        /** Simple constructor.
         * @param map map this transition belongs to
         * @param date transition date
         */
        private Transition(final FieldTimeSpanMap<S, F> map, final FieldAbsoluteDate<F> date) {
            this.map  = map;
            this.date = date;
        }

        /** Set the span valid before transition.
         * @param before span valid before transition (must be non-null)
         */
        void setBefore(final Span<S, F> before) {
            this.before = before;
            before.end  = this;
        }

        /** Set the span valid after transition.
         * @param after span valid after transition (must be non-null)
         */
        void setAfter(final Span<S, F> after) {
            this.after  = after;
            after.start = this;
        }

        /** Get the transition date.
         * @return transition date
         */
        @Override
        public FieldAbsoluteDate<F> getDate() {
            return date;
        }

        /** Move transition.
         * <p>
         * When moving a transition to past or future infinity, it will be disconnected
         * from the time span it initially belonged to as the next or previous time
         * span validity will be extends to infinity.
         * </p>
         * @param newDate new transition date
         * @param eraseOverridden if true, spans that are entirely between current
         * and new transition dates will be silently removed, if false and such
         * spans exist, an exception will be triggered
         */
        public void resetDate(final FieldAbsoluteDate<F> newDate, final boolean eraseOverridden) {
            if (newDate.isAfter(date)) {
                // we are moving the transition towards future

                // find span after new date
                Span<S, F> newAfter = after;
                while (newAfter.getEndTransition() != null &&
                       newAfter.getEndTransition().getDate().isBeforeOrEqualTo(newDate)) {
                    if (eraseOverridden) {
                        map.nbSpans--;
                    } else {
                        // forbidden collision detected
                        throw new OrekitException(OrekitMessages.TRANSITION_DATES_COLLISION,
                                                  date.toAbsoluteDate(),
                                                  newDate.toAbsoluteDate(),
                                                  newAfter.getEndTransition().getDate().toAbsoluteDate());
                    }
                    newAfter = newAfter.next();
                }

                synchronized (map) {
                    // perform update
                    date = newDate;
                    after = newAfter;
                    after.start = this;
                    map.current = before;

                    if (newDate.toAbsoluteDate().isInfinite()) {
                        // we have just moved the transition to future infinity, it should really disappear
                        map.nbSpans--;
                        map.lastSpan = before;
                        before.end   = null;
                    }
                }

            } else {
                // we are moving transition towards the past

                // find span before new date
                Span<S, F> newBefore = before;
                while (newBefore.getStartTransition() != null &&
                       newBefore.getStartTransition().getDate().isAfterOrEqualTo(newDate)) {
                    if (eraseOverridden) {
                        map.nbSpans--;
                    } else {
                        // forbidden collision detected
                        throw new OrekitException(OrekitMessages.TRANSITION_DATES_COLLISION,
                                                  date.toAbsoluteDate(),
                                                  newDate.toAbsoluteDate(),
                                                  newBefore.getStartTransition().getDate().toAbsoluteDate());
                    }
                    newBefore = newBefore.previous();
                }

                synchronized (map) {
                    // perform update
                    date = newDate;
                    before = newBefore;
                    before.end = this;
                    map.current = after;

                    if (newDate.toAbsoluteDate().isInfinite()) {
                        // we have just moved the transition to past infinity, it should really disappear
                        map.nbSpans--;
                        map.firstSpan = after;
                        after.start   = null;
                    }
                }

            }
        }

        /** Get the previous transition.
         * @return previous transition, or null if this transition was the first one
         */
        public Transition<S, F> previous() {
            return before.getStartTransition();
        }

        /** Get the next transition.
         * @return next transition, or null if this transition was the last one
         */
        public Transition<S, F> next() {
            return after.getEndTransition();
        }

        /** Get the entry valid before transition.
         * @return entry valid before transition
         * @see #getSpanBefore()
         */
        public S getBefore() {
            return before.getData();
        }

        /** Get the {@link Span} valid before transition.
         * @return {@link Span} valid before transition
         */
        public Span<S, F> getSpanBefore() {
            return before;
        }

        /** Get the entry valid after transition.
         * @return entry valid after transition
         * @see #getSpanAfter()
         */
        public S getAfter() {
            return after.getData();
        }

        /** Get the {@link Span} valid after transition.
         * @return {@link Span} valid after transition
         */
        public Span<S, F> getSpanAfter() {
            return after;
        }

    }

    /** Holder for one time span.
     * <p>
     * This data type is dual to {@link Transition}, it
     * is focused on one valid data, and gives access to
     * surrounding transition dates whereas {@link Transition}
     * is focused on one transition date, and gives access to
     * surrounding valid data.
     * </p>
     * @param <S> Type of the data.
     * @param <F> Type of the field elements
     * @since 13.1
     */
    public static class Span<S, F extends CalculusFieldElement<F>> {

        /** Field.*/
        private final Field<F> field;

        /** Valid data. */
        private final S data;

        /** Start of validity for the data (null if span extends to past infinity). */
        private Transition<S, F> start;

        /** End of validity for the data (null if span extends to future infinity). */
        private Transition<S, F> end;

        /** Simple constructor.
         * @param field field to which dates belong
         * @param data valid data
         */
        private Span(final Field<F> field, final S data) {
            this.field = field;
            this.data  = data;
        }

        /** Get the data valid during this time span.
         * @return data valid during this time span
         */
        public S getData() {
            return data;
        }

        /** Get the previous time span.
         * @return previous time span, or null if this time span was the first one
         */
        public Span<S, F> previous() {
            return start == null ? null : start.getSpanBefore();
        }

        /** Get the next time span.
         * @return next time span, or null if this time span was the last one
         */
        public Span<S, F> next() {
            return end == null ? null : end.getSpanAfter();
        }

        /** Get the start of this time span.
         * @return start of this time span (will be {@link FieldAbsoluteDate#getPastInfinity(Field)}
         * if {@link #getStartTransition()} returns null)
         * @see #getStartTransition()
         */
        public FieldAbsoluteDate<F> getStart() {
            return start == null ? FieldAbsoluteDate.getPastInfinity(field) : start.getDate();
        }

        /** Get the transition at the start of this time span.
         * @return transition at the start of this time span (null if span extends to past infinity)
         * @see #getStart()
         */
        public Transition<S, F> getStartTransition() {
            return start;
        }

        /** Get the end of this time span.
         * @return end of this time span (will be {@link FieldAbsoluteDate#getFutureInfinity(Field)}
         * if {@link #getEndTransition()} returns null)
         * @see #getEndTransition()
         */
        public FieldAbsoluteDate<F> getEnd() {
            return end == null ? FieldAbsoluteDate.getFutureInfinity(field) : end.getDate();
        }

        /** Get the transition at the end of this time span.
         * @return transition at the end of this time span (null if span extends to future infinity)
         * @see #getEnd()
         */
        public Transition<S, F> getEndTransition() {
            return end;
        }

    }

}
