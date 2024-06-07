/* Copyright 2002-2024 Luc Maisonobe
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
package org.orekit.estimation.measurements.gnss;

import org.hipparchus.util.FastMath;
import org.orekit.gnss.GnssSignal;
import org.orekit.utils.Constants;
import org.orekit.utils.ParameterDriver;

import java.util.Locale;

/** Specialized {@link ParameterDriver} for ambiguity.
 * @author Luc Maisonobe
 * @since 12.1
 */
public class AmbiguityDriver extends ParameterDriver {

    /** Prefix for parameter drivers names. */
    public static final String PREFIX = "ambiguity";

    /** Ambiguity scale factor.
     * <p>
     * We use a power of 2 to avoid numeric noise introduction
     * in the multiplications/divisions sequences.
     * </p>
     */
    private static final double AMBIGUITY_SCALE = FastMath.scalb(1.0, 26);

    /** Emitter id. */
    private final String emitter;

    /** Receiver id. */
    private final String receiver;

    /** Wavelength. */
    private final double wavelength;

    /** Simple constructor.
     * @param emitter emitter id
     * @param receiver receiver id
     * @param wavelength signal wavelength
     */
    public AmbiguityDriver(final String emitter, final String receiver, final double wavelength) {
        // the name is built from emitter, receiver and the multiplier
        // with respect to common frequency F0 (10.23 MHz)
        super(String.format(Locale.US, "%s-%s-%s-%.2f",
                            PREFIX, emitter, receiver,
                            Constants.SPEED_OF_LIGHT / (wavelength * 1.0e6 * GnssSignal.F0)),
              0.0, AMBIGUITY_SCALE, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY);
        this.emitter    = emitter;
        this.receiver   = receiver;
        this.wavelength = wavelength;
    }

    /** Get emitter id.
     * @return emitter id
     */
    public String getEmitter() {
        return emitter;
    }

    /** Get receiver id.
     * @return receiver id
     */
    public String getReceiver() {
        return receiver;
    }

    /** Get signal wavelength.
     * @return signal wavelength
     */
    public double getWavelength() {
        return wavelength;
    }

}
