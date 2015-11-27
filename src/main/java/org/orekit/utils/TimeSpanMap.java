/* Copyright 2002-2015 CS Systèmes d'Information
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
     * entries are {@link #add(Object, AbsoluteDate, boolean) added}.
     * </p>
     * @param entry entry (initially valid throughout the timeline)
     */
    public TimeSpanMap(final T entry) {
        data = new TreeSet<Transition<T>>(new ChronologicalComparator());
        data.add(new Transition<T>(AbsoluteDate.J2000_EPOCH, entry, entry));
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
     * (sould be different from <em>all</em> dates already used for transitions)
     */
    public void addValidBefore(final T entry, final AbsoluteDate latestValidityDate) {

        if (data.size() == 1) {
            final Transition<T> single = data.first();
            if (single.before == single.after) {
                // the single entry was a dummy one, without a real transition
                // we replace it entirely
                data.clear();
                data.add(new Transition<T>(latestValidityDate, entry, single.after));
                return;
            }
        }

        final Transition<T> previous =
                data.floor(new Transition<T>(latestValidityDate, entry, null));
        if (previous == null) {
            // the new transition will be the first one
            data.add(new Transition<T>(latestValidityDate, entry, data.first().before));
        } else {
            // the new transition will be after the previous one
            data.remove(previous);
            data.add(new Transition<T>(previous.date,      previous.before, entry));
            data.add(new Transition<T>(latestValidityDate, entry,           previous.after));
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
     * (sould be different from <em>all</em> dates already used for transitions)
     */
    public void addValidAfter(final T entry, final AbsoluteDate earliestValidityDate) {

        if (data.size() == 1) {
            final Transition<T> single = data.first();
            if (single.before == single.after) {
                // the single entry was a dummy one, without a real transition
                // we replace it entirely
                data.clear();
                data.add(new Transition<T>(earliestValidityDate, single.before, entry));
                return;
            }
        }

        final Transition<T> next =
                data.ceiling(new Transition<T>(earliestValidityDate, entry, null));
        if (next == null) {
            // the new transition will be the last one
            data.add(new Transition<T>(earliestValidityDate, data.last().after, entry));
        } else {
            // the new transition will be before the next one
            data.remove(next);
            data.add(new Transition<T>(earliestValidityDate, next.before, entry));
            data.add(new Transition<T>(next.date,            entry,       next.after));
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
            return data.first().before;
        } else {
            return previous.after;
        }
    }

    /** Local class holding transition times. */
    private static class Transition<S> implements TimeStamped {

        /** Transition date. */
        private final AbsoluteDate date;

        /** Entry valid before the transition. */
        private final S before;

        /** Entry valid after the transition. */
        private final S after;

        /** Simple constructor.
         * @param date ttransition date
         * @param before entry valid before the transition
         * @param after entry valid after the transition
         */
        private Transition(final AbsoluteDate date, final S before, final S after) {
            this.date   = date;
            this.before = before;
            this.after  = after;
        }

        /** {@inheritDoc} */
        @Override
        public AbsoluteDate getDate() {
            return date;
        }

    }

}
