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
package org.orekit.utils;

import java.util.function.Consumer;

import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeStamped;

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
 * Typical uses of {@link TimeSpanMap} are to hold piecewise data, like for
 * example an orbit count that changes at ascending nodes (in which case the
 * entry would be an {@link Integer}), or a visibility status between several
 * objects (in which case the entry would be a {@link Boolean}) or a drag
 * coefficient that is expected to be estimated daily or three-hourly (this is
 * how {@link org.orekit.forces.drag.TimeSpanDragForce TimeSpanDragForce} is
 * implemented).
 * </p>
 * <p>
 * Time span maps are built progressively. At first, they contain one
 * {@link Span time span} only whose validity extends from past infinity to
 * future infinity. Then new entries are added one at a time, associated with
 * transition dates, in order to build up the complete map. The transition dates
 * can be either the start of validity (when calling {@link #addValidAfter(Object,
 * AbsoluteDate, boolean)}), or the end of the validity (when calling {@link
 * #addValidBefore(Object, AbsoluteDate, boolean)}). Entries are often added at one
 * end only (and mainly in chronological order), but this is not required. It is
 * possible for example to first set up a map that cover a large range (say one day),
 * and then to insert intermediate dates using for example propagation and event
 * detectors to carve out some parts. This is akin to the way Binary Space Partitioning
 * Trees work.
 * </p>
 * <p>
 * Since 11.1, this class is thread-safe
 * </p>
 * @param <T> Type of the data.
 * @author Luc Maisonobe
 * @since 7.1
 */
public class TimeSpanMap<T> {

    /** Reference to last accessed data. */
    private Span<T> current;

    /** Number of time spans. */
    private int nbSpans;

    /** Create a map containing a single object, initially valid throughout the timeline.
     * <p>
     * The real validity of this first entry will be truncated as other
     * entries are either {@link #addValidBefore(Object, AbsoluteDate, boolean)
     * added before} it or {@link #addValidAfter(Object, AbsoluteDate, boolean)
     * added after} it.
     * </p>
     * @param entry entry (initially valid throughout the timeline)
     */
    public TimeSpanMap(final T entry) {
        current = new Span<>(entry);
        nbSpans = 1;
    }

    /** Get the number of spans.
     * <p>
     * The number of spans is always at least 1. The number of transitions
     * is always 1 less than the number of spans.
     * </p>
     * @return number of spans
     * @since 11.1
     */
    public synchronized int getSpansNumber() {
        return nbSpans;
    }

    /** Add an entry valid before a limit date.
     * <p>
     * As an entry is valid, it truncates or overrides the validity of the neighboring
     * entries already present in the map.
     * </p>
     * <p>
     * If the map already contains transitions that occur earlier than {@code latestValidityDate},
     * the {@code erasesEarlier} parameter controls what to do with them. Lets consider
     * the time span [tₖ ; tₖ₊₁[ associated with entry eₖ that would have been valid at time
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
     * @since 11.1
     */
    public synchronized Span<T> addValidBefore(final T entry, final AbsoluteDate latestValidityDate, final boolean erasesEarlier) {

        // update current reference to transition date
        locate(latestValidityDate);

        if (erasesEarlier) {

            // drop everything before date
            current.start = null;

            // update count
            nbSpans = 0;
            for (Span<T> span = current; span != null; span = span.next()) {
                ++nbSpans;
            }

        }

        final Span<T> span = new Span<>(entry);

        final Transition<T> start = current.getStartTransition();
        if (start != null && start.getDate().equals(latestValidityDate)) {
            // the transition at start of the current span is at the exact same date
            // we update it, without adding a new transition
            if (start.previous() != null) {
                start.previous().setAfter(span);
            }
            start.setBefore(span);
        } else {

            if (current.getStartTransition() != null) {
                current.getStartTransition().setAfter(span);
            }

            // we need to add a new transition somewhere inside the current span
            insertTransition(latestValidityDate, span, current);

        }

        // we consider the last added transition as the new current one
        current = span;

        return span;

    }

    /** Add an entry valid after a limit date.
     * <p>
     * As an entry is valid, it truncates or overrides the validity of the neighboring
     * entries already present in the map.
     * </p>
     * <p>
     * If the map already contains transitions that occur later than {@code earliestValidityDate},
     * the {@code erasesLater} parameter controls what to do with them. Lets consider
     * the time span [tₖ ; tₖ₊₁[ associated with entry eₖ that would have been valid at time
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
     * @since 11.1
     */
    public synchronized Span<T> addValidAfter(final T entry, final AbsoluteDate earliestValidityDate, final boolean erasesLater) {

        // update current reference to transition date
        locate(earliestValidityDate);

        if (erasesLater) {

            // drop everything after date
            current.end = null;

            // update count
            nbSpans = 0;
            for (Span<T> span = current; span != null; span = span.previous()) {
                ++nbSpans;
            }

        }

        final Span<T> span = new Span<>(entry);
        if (current.getEndTransition() != null) {
            current.getEndTransition().setBefore(span);
        }

        final Transition<T> start = current.getStartTransition();
        if (start != null && start.getDate().equals(earliestValidityDate)) {
            // the transition at start of the current span is at the exact same date
            // we update it, without adding a new transition
            start.setAfter(span);
        } else {
            // we need to add a new transition somewhere inside the current span
            insertTransition(earliestValidityDate, current, span);
        }

        // we consider the last added transition as the new current one
        current = span;

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
     * @since 11.1
     */
    public synchronized Span<T> addValidBetween(final T entry, final AbsoluteDate earliestValidityDate, final AbsoluteDate latestValidityDate) {

        // handle special cases
        if (AbsoluteDate.PAST_INFINITY.equals(earliestValidityDate)) {
            if (AbsoluteDate.FUTURE_INFINITY.equals(latestValidityDate)) {
                // we wipe everything in the map
                current = new Span<>(entry);
                return current;
            } else {
                // we wipe from past infinity
                return addValidBefore(entry, latestValidityDate, true);
            }
        } else if (AbsoluteDate.FUTURE_INFINITY.equals(latestValidityDate)) {
            // we wipe up to future infinity
            return addValidAfter(entry, earliestValidityDate, true);
        } else {

            // locate spans at earliest and latest dates
            locate(earliestValidityDate);
            Span<T> latest = current;
            while (latest.getEndTransition() != null && latest.getEnd().isBeforeOrEqualTo(latestValidityDate)) {
                latest = latest.next();
                --nbSpans;
            }
            if (latest == current) {
                // the interval splits one transition in the middle, we need to duplicate the instance
                latest = new Span<>(current.data);
                if (current.getEndTransition() != null) {
                    current.getEndTransition().setBefore(latest);
                }
            }

            final Span<T> span = new Span<>(entry);

            // manage earliest transition
            final Transition<T> start = current.getStartTransition();
            if (start != null && start.getDate().equals(earliestValidityDate)) {
                // the transition at start of the current span is at the exact same date
                // we update it, without adding a new transition
                start.setAfter(span);
            } else {
                // we need to add a new transition somewhere inside the current span
                insertTransition(earliestValidityDate, current, span);
            }

            // manage latest transition
            insertTransition(latestValidityDate, span, latest);

            // we consider the last added transition as the new current one
            current = span;

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
     * @see #getSpan(AbsoluteDate)
     */
    public synchronized T get(final AbsoluteDate date) {
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
     * @since 9.3
     */
    public synchronized Span<T> getSpan(final AbsoluteDate date) {
        locate(date);
        return current;
    }

    /** Locate the time span containing a specified date.
     * <p>
     * The {@link current} field is updated to the located span.
     * After the method returns, {@code current.getStartTransition()} is either
     * null or its date is before or equal to date, and {@code
     * current.getEndTransition()} is either null or its date is after date.
     * </p>
     * @param date date belonging to the desired time span
     */
    private synchronized void locate(final AbsoluteDate date) {

        while (current.getStart().isAfter(date)) {
            // current span is too late
            current = current.previous();
        }

        while (current.getEnd().isBeforeOrEqualTo(date)) {

            final Span<T> next = current.next();
            if (next == null) {
                // this happens when date is FUTURE_INFINITY
                return;
            }

            // current span is too early
            current = next;

        }

    }

    /** Insert a transition.
     * @param date transition date
     * @param before span before transition
     * @param after span after transition
     * @since 11.1
     */
    private void insertTransition(final AbsoluteDate date, final Span<T> before, final Span<T> after) {
        final Transition<T> transition = new Transition<>(date);
        transition.setBefore(before);
        transition.setAfter(after);
        ++nbSpans;
    }

    /** Get the first (earliest) transition.
     * @return first (earliest) transition, or null if there are no transitions
     * @since 11.1
     */
    public synchronized Transition<T> getFirstTransition() {
        return getFirstSpan().getEndTransition();
    }

    /** Get the last (latest) transition.
     * @return last (latest) transition, or null if there are no transitions
     * @since 11.1
     */
    public synchronized Transition<T> getLastTransition() {
        return getLastSpan().getStartTransition();
    }

    /** Get the first (earliest) span.
     * @return first (earliest) span
     * @since 11.1
     */
    public synchronized Span<T> getFirstSpan() {
        Span<T> span = current;
        while (span.getStartTransition() != null) {
            span = span.previous();
        }
        return span;
    }

    /** Get the last (latest) span.
     * @return last (latest) span
     * @since 11.1
     */
    public synchronized Span<T> getLastSpan() {
        Span<T> span = current;
        while (span.getEndTransition() != null) {
            span = span.next();
        }
        return span;
    }

    /** Extract a range of the map.
     * <p>
     * The object returned will be a new independent instance that will contain
     * only the transitions that lie in the specified range.
     * </p>
     * <p>
     * Consider for example a map containing objects O₀ valid before t₁, O₁ valid
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
     * @since 9.2
     */
    public synchronized TimeSpanMap<T> extractRange(final AbsoluteDate start, final AbsoluteDate end) {

        Span<T> span = getSpan(start);
        final TimeSpanMap<T> range = new TimeSpanMap<>(span.getData());
        while (span.getEndTransition() != null && span.getEndTransition().getDate().isBeforeOrEqualTo(end)) {
            span = span.next();
            range.addValidAfter(span.getData(), span.getStartTransition().getDate(), false);
        }

        return range;

    }

    /**
     * Performs an action for each non-null element of map.
     * <p>
     * The action is performed chronologically.
     * </p>
     * @param action action to perform on the non-null elements
     * @since 10.3
     */
    public synchronized void forEach(final Consumer<T> action) {
        for (Span<T> span = getFirstSpan(); span != null; span = span.next()) {
            if (span.getData() != null) {
                action.accept(span.getData());
            }
        }
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
     */
    public static class Transition<S> implements TimeStamped {

        /** Transition date. */
        private AbsoluteDate date;

        /** Entry valid before the transition. */
        private Span<S> before;

        /** Entry valid after the transition. */
        private Span<S> after;

        /** Simple constructor.
         * @param date transition date
         */
        private Transition(final AbsoluteDate date) {
            this.date = date;
        }

        /** Set the span valid before transition.
         * @param before span valid before transition (must be non-null)
         */
        void setBefore(final Span<S> before) {
            this.before = before;
            before.end  = this;
        }

        /** Set the span valid after transition.
         * @param after span valid after transition (must be non-null)
         */
        void setAfter(final Span<S> after) {
            this.after  = after;
            after.start = this;
        }

        /** Get the transition date.
         * @return transition date
         */
        @Override
        public AbsoluteDate getDate() {
            return date;
        }

        /** Get the previous transition.
         * @return previous transition, or null if this transition was the first one
         * @since 11.1
         */
        public Transition<S> previous() {
            return before.getStartTransition();
        }

        /** Get the next transition.
         * @return next transition, or null if this transition was the last one
         * @since 11.1
         */
        public Transition<S> next() {
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
         * @since 11.1
         */
        public Span<S> getSpanBefore() {
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
         * @since 11.1
         */
        public Span<S> getSpanAfter() {
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
     * @since 9.3
     */
    public static class Span<S> {

        /** Valid data. */
        private final S data;

        /** Start of validity for the data (null if span extends to past infinity). */
        private Transition<S> start;

        /** End of validity for the data (null if span extends to future infinity). */
        private Transition<S> end;

        /** Simple constructor.
         * @param data valid data
         */
        private Span(final S data) {
            this.data = data;
        }

        /** Get the data valid during this time span.
         * @return data valid during this time span
         */
        public S getData() {
            return data;
        }

        /** Get the previous time span.
         * @return previous time span, or null if this time span was the first one
         * @since 11.1
         */
        public Span<S> previous() {
            return start == null ? null : start.getSpanBefore();
        }

        /** Get the next time span.
         * @return next time span, or null if this time span was the last one
         * @since 11.1
         */
        public Span<S> next() {
            return end == null ? null : end.getSpanAfter();
        }

        /** Get the start of this time span.
         * @return start of this time span (will be {@link AbsoluteDate#PAST_INFINITY}
         * if {@link #getStartTransition()} returns null)
         * @see #getStartTransition()
         */
        public AbsoluteDate getStart() {
            return start == null ? AbsoluteDate.PAST_INFINITY : start.getDate();
        }

        /** Get the transition at start of this time span.
         * @return transition at start of this time span (null if span extends to past infinity)
         * @see #getStart()
         * @since 11.1
         */
        public Transition<S> getStartTransition() {
            return start;
        }

        /** Get the end of this time span.
         * @return end of this time span (will be {@link AbsoluteDate#FUTURE_INFINITY}
         * if {@link #getEndTransition()} returns null)
         * @see #getEndTransition()
         */
        public AbsoluteDate getEnd() {
            return end == null ? AbsoluteDate.FUTURE_INFINITY : end.getDate();
        }

        /** Get the transition at end of this time span.
         * @return transition at end of this time span (null if span extends to future infinity)
         * @see #getEnd()
         * @since 11.1
         */
        public Transition<S> getEndTransition() {
            return end;
        }

    }

}
