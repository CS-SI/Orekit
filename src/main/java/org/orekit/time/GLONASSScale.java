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
package org.orekit.time;

/** GLONASS time scale.
 * <p>By convention, TGLONASS = UTC + 3 hours.</p>
 * <p>The time scale is defined in <a
 * href="http://www.spacecorp.ru/upload/iblock/1c4/cgs-aaixymyt%205.1%20ENG%20v%202014.02.18w.pdf">
 * Global Navigation Sattelite System GLONASS - Interface Control document</a>, version 5.1 2008
 * (the typo in the title is in the original document title).
 * </p>
 * <p>This is intended to be accessed thanks to the {@link TimeScalesFactory} class,
 * so there is no public constructor.</p>
 * @author Luc Maisonobe
 * @see AbsoluteDate
 */
public class GLONASSScale implements TimeScale {

    /** Serializable UID. */
    private static final long serialVersionUID = 20160331L;

    /** Constant offset with respect to UTC (3 hours). */
    private static final double OFFSET = 10800;

    /** UTC time scale. */
    private final UTCScale utc;

    /** Package private constructor for the factory.
     * @param utc underlying UTC scale
     */
    GLONASSScale(final UTCScale utc) {
        this.utc = utc;
    }

    /** {@inheritDoc} */
    @Override
    public double offsetFromTAI(final AbsoluteDate date) {
        return OFFSET + utc.offsetFromTAI(date);
    }

    /** {@inheritDoc} */
    @Override
    public double offsetToTAI(final DateComponents date, final TimeComponents time) {
        final DateTimeComponents utcComponents =
                        new DateTimeComponents(new DateTimeComponents(date, time), -OFFSET);
        return utc.offsetToTAI(utcComponents.getDate(), utcComponents.getTime()) - OFFSET;
    }

    /** {@inheritDoc} */
    @Override
    public boolean insideLeap(final AbsoluteDate date) {
        return utc.insideLeap(date);
    }

    /** {@inheritDoc} */
    @Override
    public int minuteDuration(final AbsoluteDate date) {
        return utc.minuteDuration(date);
    }

    /** {@inheritDoc} */
    @Override
    public double getLeap(final AbsoluteDate date) {
        return utc.getLeap(date);
    }

    /** {@inheritDoc} */
    public String getName() {
        return "GLONASS";
    }

    /** {@inheritDoc} */
    public String toString() {
        return getName();
    }

}
