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
package org.orekit.utils;

import java.util.Collections;
import java.util.NavigableSet;
import java.util.SortedSet;
import java.util.TreeSet;

import org.orekit.time.FieldAbsoluteDate;
import org.orekit.time.TimeStamped;
import org.hipparchus.Field;
import org.hipparchus.RealFieldElement;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.ChronologicalComparator;

/** Container for objects that apply to spans of time.

 * @param <T> Type of the data.

 * @author Luc Maisonobe
 * @since 7.1
 */
public class FieldTimeSpanMap<T, D extends RealFieldElement<D>> {

    /** Container for the data. */
    private final NavigableSet<Transition<T, D>> data;

    /**Field.*/
    private final Field<D> field;

    /** Create a map containing a single object, initially valid throughout the timeline.
     * <p>
     * The real validity of this first entry will be truncated as other
     * entries are either {@link #addValidBefore(Object, FieldAbsoluteDate)
     * added before} it or {@link #addValidAfter(Object, FieldAbsoluteDate)
     * added after} it.
     * </p>
     * @param entry entry (initially valid throughout the timeline)
     * @param field_n field used by default.
     */
    public FieldTimeSpanMap(final T entry, final Field<D> field_n) {
        data = new TreeSet<Transition<T, D>>(new ChronologicalComparator());
        field = field_n;
        data.add(new Transition<T, D>(new FieldAbsoluteDate<D>(field), entry, entry));
    }

    /** Add an entry valid before a limit date.
     * <p>
     * As an entry is valid, it truncates the validity of the neighboring
     * entries already present in the map.
     * </p>
     * <p>
     * The transition dates should be entered only once, either
     * by a call to this method or by a call to {@link #addValidAfter(Object,
     * FieldAbsoluteDate)}. Repeating a transition date will lead to unexpected
     * result and is not supported.
     * </p>
     * @param entry entry to add
     * @param latestValidityDate date before which the entry is valid
     * (sould be different from <em>all</em> dates already used for transitions)
     */
    public void addValidBefore(final T entry, final FieldAbsoluteDate<D> latestValidityDate) {

        if (data.size() == 1) {
            final Transition<T, D> single = data.first();
            if (single.getBefore() == single.getAfter()) {
                // the single entry was a dummy one, without a real transition
                // we replace it entirely
                data.clear();
                data.add(new Transition<T, D>(latestValidityDate, entry, single.getAfter()));
                return;
            }
        }

        final Transition<T, D> previous =
                data.floor(new Transition<T, D>(latestValidityDate, entry, null));
        if (previous == null) {
            // the new transition will be the first one
            data.add(new Transition<T, D>(latestValidityDate, entry, data.first().getBefore()));
        } else {
            // the new transition will be after the previous one
            data.remove(previous);
            data.add(new Transition<T, D>(previous.date,      previous.getBefore(), entry));
            data.add(new Transition<T, D>(latestValidityDate, entry,                previous.getAfter()));
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
     * FieldAbsoluteDate)}. Repeating a transition date will lead to unexpected
     * result and is not supported.
     * </p>
     * @param entry entry to add
     * @param earliestValidityDate date after which the entry is valid
     * (sould be different from <em>all</em> dates already used for transitions)
     */
    public void addValidAfter(final T entry, final FieldAbsoluteDate<D> earliestValidityDate) {

        if (data.size() == 1) {
            final Transition<T, D> single = data.first();
            if (single.getBefore() == single.getAfter()) {
                // the single entry was a dummy one, without a real transition
                // we replace it entirely
                data.clear();
                data.add(new Transition<T, D>(earliestValidityDate, single.getBefore(), entry));
                return;
            }
        }

        final Transition<T, D> next =
                data.ceiling(new Transition<T, D>(earliestValidityDate, entry, null));
        if (next == null) {
            // the new transition will be the last one
            data.add(new Transition<T, D>(earliestValidityDate, data.last().getAfter(), entry));
        } else {
            // the new transition will be before the next one
            data.remove(next);
            data.add(new Transition<T, D>(earliestValidityDate, next.getBefore(), entry));
            data.add(new Transition<T, D>(next.date,            entry,            next.getAfter()));
        }

    }

    /** Get the entry valid at a specified date.
     * @param date date at which the entry must be valid
     * @return valid entry at specified date
     */
    public T get(final FieldAbsoluteDate<D> date) {
        final Transition<T, D> previous = data.floor(new Transition<T, D>(date, null, null));
        if (previous == null) {
            // there are no transition before the specified date
            // return the first valid entry
            return data.first().getBefore();
        } else {
            return previous.getAfter();
        }
    }

    /** Get an unmodifiable view of the sorted transitions.
     * @return unmodifiable view of the sorted transitions
     */
    public SortedSet<Transition<T, D>> getTransitions() {
        return Collections.unmodifiableSortedSet(data);
    }

    /** Local class holding transition times. */
    public static class Transition<S, D extends RealFieldElement<D>> implements TimeStamped {

        /** Transition date. */
        private final FieldAbsoluteDate<D> date;

        /** Entry valid before the transition. */
        private final S before;

        /** Entry valid after the transition. */
        private final S after;

        /** Simple constructor.
         * @param date transition date
         * @param before entry valid before the transition
         * @param after entry valid after the transition
         */
        private Transition(final FieldAbsoluteDate<D> date, final S before, final S after) {
            this.date   = date;
            this.before = before;
            this.after  = after;
        }

        /** Get the transition field absolute date.
         * @return transition date
         */
        public FieldAbsoluteDate<D> getAbsoluteDate() {
            return date;
        }
        /** Get the transition absolute date.
         * @return transition date
         */

        public AbsoluteDate getDate() {
            return date.toAbsoluteDate();
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



}
