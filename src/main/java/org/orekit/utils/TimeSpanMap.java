/* Copyright 2002-2022 CS GROUP
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
import java.util.NavigableSet;
import java.util.TreeSet;
import java.util.function.Consumer;

import org.orekit.time.AbsoluteDate;
import org.orekit.time.ChronologicalComparator;
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
 * @param <T> Type of the data.
 * @author Luc Maisonobe
 * @since 7.1
 */
public class TimeSpanMap<T> {

    /** Container for the data. */
    private final NavigableSet<Transition<T>> data;

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

        // prepare a single dummy transition
        final Transition<T> dummy = new Transition<>(AbsoluteDate.ARBITRARY_EPOCH);
        final Span<T>       span   = new Span<>(entry);
        dummy.before = span; // don't call dummy.setBefore(span) to preserve span.start == null
        dummy.after  = span; // don't call dummy.setAfter(span) to preserve span.end == null

        data = new TreeSet<>(new ChronologicalComparator());
        data.add(dummy);

    }

    /** Get the number of transitions.
     * @return number of transitions
     * @since 11.1
     */
    public int getTransitionsNumber() {
        return hasSingleDummyTransition() ? 0 : data.size();
    }

    /** Check if the map has a single dummy transition.
     * @return true if the transition has a single dummy transition
     * @since 11.1
     */
    boolean hasSingleDummyTransition() {
        return data.size() == 1 &&
               data.first().getSpanBefore().getData() == data.first().getSpanAfter().getData();
    }

    /** Add an entry valid before a limit date.
     * <p>
     * Calling this method is equivalent to call {@link #addValidAfter(Object,
     * AbsoluteDate, boolean) addValidAfter(entry, latestValidityDate, false)}.
     * </p>
     * @param entry entry to add
     * @param latestValidityDate date before which the entry is valid
     * @deprecated as of 11.1, replaced by {@link #addValidBefore(Object, AbsoluteDate, boolean)}
     */
    @Deprecated
    public void addValidBefore(final T entry, final AbsoluteDate latestValidityDate) {
        addValidBefore(entry, latestValidityDate, false);
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
     * <ul>
     *  <li>if {@code erasesEarlier} is {@code true}, then all earlier transitions
     *      up to and including tₖ are erased, and the {@code entry} will be valid from past infinity
     *      to {@code latestValidityDate}</li>
     *  <li>if {@code erasesEarlier} is {@code false}, then all earlier transitions
     *      are preserved, and the {@code entry} will be valid from tₖ
     *      to {@code latestValidityDate}</li>
     *  </ul>
     *  In both cases, the existing entry eₖ time span will be truncated and will be valid
     *  only from {@code latestValidityDate} to tₖ₊₁.
     * </p>
     * @param entry entry to add
     * @param latestValidityDate date before which the entry is valid
     * @param erasesEarlier if true, the entry erases all existing transitions
     * that are earlier than {@code latestValidityDate}
     * @since 11.1
     */
    public void addValidBefore(final T entry, final AbsoluteDate latestValidityDate, final boolean erasesEarlier) {

        final Span<T> span = new Span<>(entry);

        if (hasSingleDummyTransition()) {
            // change the single dummy transition into a real transition
            final Transition<T> single = data.first();
            single.date = latestValidityDate;
            single.setBefore(span);
            single.setAfter(single.getSpanAfter()); // just for resetting single.after.start to non-null
            return;
        }

        final Transition<T> current = new Transition<>(latestValidityDate);
        current.setBefore(span);

        final Transition<T> previous = data.floor(current);
        if (previous == null) {
            // the new transition will be the first one
            current.setAfter(data.first().getSpanBefore());
            data.add(current);
        } else if (previous.getDate().equals(latestValidityDate)) {
            // we already have a transition at the exact same date
            // we update it, without adding a new transition
            final Transition<T> effectivePrevious = previous.getSpanBefore().getStartTransition();
            if (effectivePrevious != null) {
                effectivePrevious.setAfter(span);
            }
            previous.setBefore(span); // remember here previous is not really previous, it's current
            return;
        } else {
            current.setAfter(previous.getSpanAfter());
            if (erasesEarlier) {
                // remove all transitions up to and including previous
                while (data.pollFirst() != previous) {
                    // empty
                }
            } else {
                // the new transition will be after the previous one
                previous.setAfter(span);
            }
            data.add(current);
        }

    }

    /** Add an entry valid after a limit date.
     * <p>
     * Calling this method is equivalent to call {@link #addValidAfter(Object,
     * AbsoluteDate, boolean) addValidAfter(entry, earliestValidityDate, false)}.
     * </p>
     * @param entry entry to add
     * @param earliestValidityDate date after which the entry is valid
     * @deprecated as of 11.1, replaced by {@link #addValidAfter(Object, AbsoluteDate, boolean)}
     */
    @Deprecated
    public void addValidAfter(final T entry, final AbsoluteDate earliestValidityDate) {
        addValidAfter(entry, earliestValidityDate, false);
    }

    /** Add an entry valid after a limit date.
     * <p>
     * As an entry is valid, it truncates or overrides the validity of the neighboring
     * entries already present in the map.
     * </p>
     * <p>
     * If the map already contains transitions that occur earlier than {@code earliestValidityDate},
     * the {@code erasesEarlier} parameter controls what to do with them. Lets consider
     * the time span [tₖ ; tₖ₊₁[ associated with entry eₖ that would have been valid at time
     * {@code earliestValidityDate} prior to the call to the method (i.e. tₖ &lt;
     * {@code earliestValidityDate} &lt; tₖ₊₁).
     * <ul>
     *  <li>if {@code erasesEarlier} is {@code true}, then all earlier transitions
     *      up to and including tₖ are erased, and the {@code entry} will be valid from past infinity
     *      to {@code earliestValidityDate}</li>
     *  <li>if {@code erasesEarlier} is {@code false}, then all earlier transitions
     *      are preserved, and the {@code entry} will be valid from tₖ
     *      to {@code earliestValidityDate}</li>
     *  </ul>
     *  In both cases, the existing entry eₖ time span will be truncated and will be valid
     *  only from {@code earliestValidityDate} to tₖ₊₁.
     * </p>
     * @param entry entry to add
     * @param earliestValidityDate date after which the entry is valid
     * @param erasesLater if true, the entry erases all existing transitions
     * that are later than {@code earliestValidityDate}
     * @since 11.1
     */
    public void addValidAfter(final T entry, final AbsoluteDate earliestValidityDate, final boolean erasesLater) {

        final Span<T> span = new Span<>(entry);

        if (hasSingleDummyTransition()) {
            // change the single dummy transition into a real transition
            final Transition<T> single = data.first();
            single.date = earliestValidityDate;
            single.setBefore(single.getSpanBefore()); // just for resetting single.before.end to non-null
            single.setAfter(span);
            return;
        }

        final Transition<T> current = new Transition<>(earliestValidityDate);
        current.setAfter(span);

        final Transition<T> next = data.ceiling(current);
        if (next == null) {
            // the new transition will be the last one
            current.setBefore(data.last().getSpanAfter());
            data.add(current);
        } else if (next.getDate().equals(earliestValidityDate)) {
            // we already have a transition at the exact same date
            // we update it, without adding a new transition
            final Transition<T> effectiveNext = next.getSpanAfter().getEndTransition();
            if (effectiveNext != null) {
                effectiveNext.setBefore(span);
            }
            next.setAfter(span); // remember here next is not really next, it's current
            return;
        } else {
            current.setBefore(next.getSpanBefore());
            if (erasesLater) {
                // remove all transitions down to and including next
                while (data.pollLast() != next) {
                    // empty
                }
            } else {
                // the new transition will be before the next one
                next.setBefore(span);
            }
            data.add(current);
        }

    }

    /** Get the entry valid at a specified date.
     * @param date date at which the entry must be valid
     * @return valid entry at specified date
     */
    public T get(final AbsoluteDate date) {
        return getSpan(date).getData();
    }

    /** Get the time span containing a specified date.
     * @param date date belonging to the desired time span
     * @return time span containing the specified date
     * @since 9.3
     */
    public Span<T> getSpan(final AbsoluteDate date) {

        if (hasSingleDummyTransition()) {
            // both spans before and after the dummy transition are the same
            return data.first().getSpanBefore();
        }

        final Transition<T> previous = data.floor(new Transition<>(date));
        if (previous == null) {
            // there are no transition before the specified date
            // return the first valid entry
            return data.first().getSpanBefore();
        } else {
            return previous.getSpanAfter();
        }
    }

    /** Get the first (earliest) transition.
     * @return first (earliest) transition, or null if there are no transitions
     * @since 11.1
     */
    public Transition<T> getFirstTransition() {
        return hasSingleDummyTransition() ? null : data.first();
    }

    /** Get the last (latest) transition.
     * @return last (latest) transition, or null if there are no transitions
     * @since 11.1
     */
    public Transition<T> getLastTransition() {
        return hasSingleDummyTransition() ? null : data.last();
    }

    /** Get the first (earliest) span.
     * @return first (earliest) span
     * @since 11.1
     */
    public Span<T> getFirstSpan() {
        return data.first().getSpanBefore();
    }

    /** Get the last (latest) span.
     * @return last (latest) span
     * @since 11.1
     */
    public Span<T> getLastSpan() {
        return data.last().getSpanAfter();
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
    public TimeSpanMap<T> extractRange(final AbsoluteDate start, final AbsoluteDate end) {

        final NavigableSet<Transition<T>> inRange =
                        data.subSet(new Transition<>(start), true, new Transition<>(end), true);
        if (inRange.isEmpty()) {
            // there are no transitions at all in the range
            // we need to pick up the only valid object
            return new TimeSpanMap<>(get(start));
        }

        final TimeSpanMap<T> range = new TimeSpanMap<>(inRange.first().before.getData());
        for (final Transition<T> transition : inRange) {
            range.addValidAfter(transition.after.getData(), transition.getDate(), false);
        }

        return range;

    }

    /** Get an unmodifiable view of the sorted transitions.
     * @return unmodifiable view of the sorted transitions
     * @deprecated as of 11.1, replaced by {@link #getFirstSpan()}, {@link #getLastSpan()},
     * {@link #getFirstTransition()}, {@link #getLastTransition()}, and {@link #getTransitionsNumber()}
     */
    @Deprecated
    public NavigableSet<Transition<T>> getTransitions() {
        return Collections.unmodifiableNavigableSet(data);
    }

    /**
     * Performs an action for each element of map.
     * <p>
     * The action is performed chronologically.
     * </p>
     * @param action action to perform on the elements
     * @since 10.3
     */
    public void forEach(final Consumer<T> action) {
        boolean first = true;
        for (Transition<T> transition : data) {
            if (first) {
                if (transition.getBefore() != null) {
                    action.accept(transition.getBefore());
                }
                first = false;
            }
            if (transition.getAfter() != null) {
                action.accept(transition.getAfter());
            }
        }
    }

    /** Class holding transition times.
     * <p>
     * This data type is dual to {@link Span}, it is
     * focused one transition date, and gives access to
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
         * if {@link #getStartTransition() returns null)
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
         * if {@link #getEndTransition() returns null)
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
