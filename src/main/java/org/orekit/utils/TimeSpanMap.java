/* Copyright 2002-2020 CS Group
 * Licensed to CS Group (CS) under one or more
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

import org.orekit.time.AbsoluteDate;
import org.orekit.time.ChronologicalComparator;
import org.orekit.time.TimeStamped;

/** Container for objects that apply to spans of time.

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
     * entries are either {@link #addValidBefore(Object, AbsoluteDate)
     * added before} it or {@link #addValidAfter(Object, AbsoluteDate)
     * added after} it.
     * </p>
     * @param entry entry (initially valid throughout the timeline)
     */
    public TimeSpanMap(final T entry) {
        data = new TreeSet<Transition<T>>(new ChronologicalComparator());
        data.add(new Transition<T>(AbsoluteDate.ARBITRARY_EPOCH, entry, entry));
    }

    /** Add an entry valid before a limit date.
     * <p>
     * As an entry is valid, it truncates the validity of the neighboring
     * entries already present in the map.
     * </p>
     * <p>
     * The transition dates should be entered only once, either
     * by a call to this method or by a call to {@link #addValidAfter(Object,
     * AbsoluteDate)}. Repeating a transition date will lead to unexpected
     * result and is not supported.
     * </p>
     * @param entry entry to add
     * @param latestValidityDate date before which the entry is valid
     * (must be different from <em>all</em> dates already used for transitions)
     */
    public void addValidBefore(final T entry, final AbsoluteDate latestValidityDate) {

        if (data.size() == 1) {
            final Transition<T> single = data.first();
            if (single.getBefore() == single.getAfter()) {
                // the single entry was a dummy one, without a real transition
                // we replace it entirely
                data.clear();
                data.add(new Transition<T>(latestValidityDate, entry, single.getAfter()));
                return;
            }
        }

        final Transition<T> previous =
                data.floor(new Transition<T>(latestValidityDate, entry, null));
        if (previous == null) {
            // the new transition will be the first one
            data.add(new Transition<T>(latestValidityDate, entry, data.first().getBefore()));
        } else {
            // the new transition will be after the previous one
            data.remove(previous);
            data.add(new Transition<T>(previous.date,      previous.getBefore(), entry));
            data.add(new Transition<T>(latestValidityDate, entry,                previous.getAfter()));
        }

    }

    /** Add an entry valid after a limit date.
     * <p>
     * As an entry is valid, it truncates the validity of the neighboring
     * entries already present in the map.
     * </p>
     * <p>
     * The transition dates should be entered only once, either
     * by a call to this method or by a call to {@link #addValidBefore(Object,
     * AbsoluteDate)}. Repeating a transition date will lead to unexpected
     * result and is not supported.
     * </p>
     * @param entry entry to add
     * @param earliestValidityDate date after which the entry is valid
     * (must be different from <em>all</em> dates already used for transitions)
     */
    public void addValidAfter(final T entry, final AbsoluteDate earliestValidityDate) {

        if (data.size() == 1) {
            final Transition<T> single = data.first();
            if (single.getBefore() == single.getAfter()) {
                // the single entry was a dummy one, without a real transition
                // we replace it entirely
                data.clear();
                data.add(new Transition<T>(earliestValidityDate, single.getBefore(), entry));
                return;
            }
        }

        final Transition<T> next =
                data.ceiling(new Transition<T>(earliestValidityDate, entry, null));
        if (next == null) {
            // the new transition will be the last one
            data.add(new Transition<T>(earliestValidityDate, data.last().getAfter(), entry));
        } else {
            // the new transition will be before the next one
            data.remove(next);
            data.add(new Transition<T>(earliestValidityDate, next.getBefore(), entry));
            data.add(new Transition<T>(next.date,            entry,            next.getAfter()));
        }

    }

    /** Get the entry valid at a specified date.
     * @param date date at which the entry must be valid
     * @return valid entry at specified date
     */
    public T get(final AbsoluteDate date) {
        final Transition<T> previous = data.floor(new Transition<T>(date, null, null));
        if (previous == null) {
            // there are no transition before the specified date
            // return the first valid entry
            return data.first().getBefore();
        } else {
            return previous.getAfter();
        }
    }

    /** Get the time span containing a specified date.
     * @param date date belonging to the desired time span
     * @return time span containing the specified date
     * @since 9.3
     */
    public Span<T> getSpan(final AbsoluteDate date) {
        final Transition<T> previous = data.floor(new Transition<T>(date, null, null));
        if (previous == null) {
            // there are no transition before the specified date
            // return the first valid entry
            return new Span<>(data.first().getBefore(),
                              AbsoluteDate.PAST_INFINITY,
                              data.first().getDate());
        } else {
            final Transition<T> next = data.higher(previous);
            return new Span<>(previous.getAfter(),
                              previous.getDate(),
                              next == null ? AbsoluteDate.FUTURE_INFINITY : next.getDate());
        }
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

        final NavigableSet<Transition<T>> inRange = data.subSet(new Transition<T>(start, null, null), true,
                                                                new Transition<T>(end,   null, null), true);
        if (inRange.isEmpty()) {
            // there are no transitions at all in the range
            // we need to pick up the only valid object
            return new TimeSpanMap<>(get(start));
        }

        final TimeSpanMap<T> range = new TimeSpanMap<>(inRange.first().before);
        for (final Transition<T> transition : inRange) {
            range.addValidAfter(transition.after, transition.getDate());
        }

        return range;

    }

    /** Get an unmodifiable view of the sorted transitions.
     * @return unmodifiable view of the sorted transitions
     */
    public NavigableSet<Transition<T>> getTransitions() {
        return Collections.unmodifiableNavigableSet(data);
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
        private final AbsoluteDate date;

        /** Entry valid before the transition. */
        private final S before;

        /** Entry valid after the transition. */
        private final S after;

        /** Simple constructor.
         * @param date transition date
         * @param before entry valid before the transition
         * @param after entry valid after the transition
         */
        private Transition(final AbsoluteDate date, final S before, final S after) {
            this.date   = date;
            this.before = before;
            this.after  = after;
        }

        /** Get the transition date.
         * @return transition date
         */
        @Override
        public AbsoluteDate getDate() {
            return date;
        }

        /** Get the entry valid before transition.
         * @return entry valid before transition
         */
        public S getBefore() {
            return before;
        }

        /** Get the entry valid after transition.
         * @return entry valid after transition
         */
        public S getAfter() {
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

        /** Start of validity for the data. */
        private final AbsoluteDate start;

        /** End of validity for the data. */
        private final AbsoluteDate end;

        /** Simple constructor.
         * @param data valid data
         * @param start start of validity for the data
         * @param end end of validity for the data
         */
        private Span(final S data, final AbsoluteDate start, final AbsoluteDate end) {
            this.data  = data;
            this.start = start;
            this.end   = end;
        }

        /** Get the data valid during this time span.
         * @return data valid during this time span
         */
        public S getData() {
            return data;
        }

        /** Get the start of this time span.
         * @return start of this time span
         */
        public AbsoluteDate getStart() {
            return start;
        }

        /** Get the end of this time span.
         * @return end of this time span
         */
        public AbsoluteDate getEnd() {
            return end;
        }

    }

}
