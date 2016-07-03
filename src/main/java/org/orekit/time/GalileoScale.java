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

/** Galileo system time scale.
 * <p>By convention, TGST = UTC + 13s at Galileo epoch (1999-08-22T00:00:00Z).</p>
 * <p>This is intended to be accessed thanks to the {@link TimeScalesFactory} class,
 * so there is no public constructor.</p>
 * <p>
 * Galileo System Time and GPS time are very close scales. Without any errors, they
 * should be identical. The offset between these two scales is the GGTO, it depends
 * on the clocks used to realize the time scales. It is of the order of a few
 * tens nanoseconds. This class does not implement this offset, so it is virtually
 * identical to the {@link GPSScale GPS scale}.
 * </p>
 * @author Luc Maisonobe
 * @see AbsoluteDate
 */
public class GalileoScale implements TimeScale {

    /** Serializable UID. */
    private static final long serialVersionUID = 20131209L;

    /** Package private constructor for the factory.
     */
    GalileoScale() {
    }

    /** {@inheritDoc} */
    @Override
    public double offsetFromTAI(final AbsoluteDate date) {
        return -19;
    }

    /** {@inheritDoc} */
    @Override
    public double offsetToTAI(final DateComponents date, final TimeComponents time) {
        return 19;
    }

    /** {@inheritDoc} */
    public String getName() {
        return "GST";
    }

    /** {@inheritDoc} */
    public String toString() {
        return getName();
    }

}
