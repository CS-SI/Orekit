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
package org.orekit.models.earth.atmosphere.data;

import org.orekit.data.DataLoader;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScale;
import org.orekit.time.TimeStamped;

import java.io.Serializable;
import java.util.SortedSet;

/**
 * Abstract class for solar activity data loader.
 *
 * @author Vincent Cucchietti
 * @since 12.0
 */
public abstract class AbstractSolarActivityDataLoader<L extends AbstractSolarActivityDataLoader.LineParameters>
        implements DataLoader {

    /** UTC time scale. */
    private final TimeScale utc;

    /** First available date. */
    private AbsoluteDate firstDate;

    /** Last available date. */
    private AbsoluteDate lastDate;

    /**
     * Constructor.
     *
     * @param utc UTC time scale
     */
    protected AbstractSolarActivityDataLoader(final TimeScale utc) {
        this.utc = utc;
    }

    /** {@inheritDoc} */
    @Override
    public boolean stillAcceptsData() {
        return true;
    }

    /**
     * Get the data set.
     *
     * @return the data set
     */
    public abstract SortedSet<L> getDataSet();

    /**
     * Get the UTC timescale.
     *
     * @return the UTC timescale
     */
    public TimeScale getUTC() {
        return utc;
    }

    /**
     * Gets the available data range minimum date.
     *
     * @return the minimum date.
     */
    public AbsoluteDate getMinDate() {
        return firstDate;
    }

    /**
     * Gets the available data range maximum date.
     *
     * @return the maximum date.
     */
    public AbsoluteDate getMaxDate() {
        return lastDate;
    }

    /** Container class for Solar activity indexes. */
    public abstract static class LineParameters implements TimeStamped, Comparable<LineParameters>, Serializable {

        /** Serializable UID. */
        private static final long serialVersionUID = 6607862001953526475L;

        /** Entry date. */
        private final AbsoluteDate date;

        /**
         * Constructor.
         *
         * @param date entry date
         */
        protected LineParameters(final AbsoluteDate date) {
            this.date = date;
        }

        /** {@inheritDoc} */
        @Override
        public abstract int compareTo(LineParameters lineParameters);

        /** Check if the instance represents the same parameters as given line parameters.
         * @param lineParameters other line parameters
         * @return true if the instance and the other line parameter contain the same parameters
         */
        @Override
        public abstract boolean equals(Object lineParameters);

        /** Get a hashcode for this date.
         * @return hashcode
         */
        @Override
        public abstract int hashCode();

        /** @return entry date */
        @Override
        public AbsoluteDate getDate() {
            return date;
        }
    }

    /**
     * Set the available data range minimum date.
     *
     * @param date available data range minimum date
     */
    public void setMinDate(final AbsoluteDate date) {
        this.firstDate = date;
    }

    /**
     * Set the available data range maximum date.
     *
     * @param date available data range maximum date
     */
    public void setMaxDate(final AbsoluteDate date) {
        this.lastDate = date;
    }
}
