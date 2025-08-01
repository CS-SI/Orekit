/* Copyright 2022-2025 Thales Alenia Space
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
package org.orekit.time;

/**
 * Class that associates a double, its time derivative with a date.
 *
 * @author Luc Maisonobe
 * @since 12.1
 */
public class TimeStampedDoubleAndDerivative extends TimeStampedDouble {

    /** Time derivative. */
    private final double derivative;

    /**
     * Constructor.
     *
     * @param date       date associated to value
     * @param value      value
     * @param derivative time derivative
     */
    public TimeStampedDoubleAndDerivative(final AbsoluteDate date, final double value, final double derivative) {
        super(date, value);
        this.derivative = derivative;
    }

    /**
     * Constructor.
     *
     * @param value value
     * @param derivative time derivative
     * @param date date associated to value
     */
    @Deprecated
    public TimeStampedDoubleAndDerivative(final double value, final double derivative,
                                          final AbsoluteDate date) {
        super(date, value);
        this.derivative = derivative;
    }

    /** Get time derivative.
     * @return time derivative
     */
    public double getDerivative() {
        return derivative;
    }

    @Override
    public String toString() {
        return "{" +
                "date=" + getDate() +
                ", value=" + getValue() +
                ", derivative=" + derivative +
                '}';
    }
}
