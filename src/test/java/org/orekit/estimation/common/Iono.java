/* Copyright 2002-2024 CS GROUP
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

package org.orekit.estimation.common;

import org.orekit.estimation.measurements.modifiers.RangeIonosphericDelayModifier;
import org.orekit.estimation.measurements.modifiers.RangeRateIonosphericDelayModifier;
import org.orekit.gnss.RadioWave;
import org.orekit.models.earth.ionosphere.IonosphericModel;
import org.orekit.models.earth.ionosphere.KlobucharIonoCoefficientsLoader;
import org.orekit.models.earth.ionosphere.KlobucharIonoModel;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.DateComponents;
import org.orekit.time.TimeScalesFactory;

import java.util.HashMap;
import java.util.Map;

/** Ionospheric modifiers.
 * @author Bryan Cazabonne
 */
class Iono {

    /** Flag for two-way range-rate. */
    private final boolean twoWay;

    /** Map for range modifiers. */
    private final Map<RadioWave, Map<DateComponents, RangeIonosphericDelayModifier>> rangeModifiers;

    /** Map for range-rate modifiers. */
    private final Map<RadioWave, Map<DateComponents, RangeRateIonosphericDelayModifier>> rangeRateModifiers;

    /** Simple constructor.
     * @param twoWay flag for two-way range-rate
     */
    Iono(final boolean twoWay) {
        this.twoWay             = twoWay;
        this.rangeModifiers     = new HashMap<>();
        this.rangeRateModifiers = new HashMap<>();
    }

    /** Get range modifier for a measurement.
     * @param radioWave radio wave of the signal
     * @param date measurement date
     * @return range modifier
     */
    public RangeIonosphericDelayModifier getRangeModifier(final RadioWave radioWave,
                                                          final AbsoluteDate date) {
        final DateComponents dc = date.getComponents(TimeScalesFactory.getUTC()).getDate();
        ensureFrequencyAndDateSupported(radioWave, dc);
        return rangeModifiers.get(radioWave).get(dc);
    }

    /**
     * Get range-rate modifier for a measurement.
     *
     * @param radioWave radio wave of the signal
     * @param date      measurement date
     * @return range-rate modifier
     */
    public RangeRateIonosphericDelayModifier getRangeRateModifier(final RadioWave radioWave,
                                                                  final AbsoluteDate date) {
        final DateComponents dc = date.getComponents(TimeScalesFactory.getUTC()).getDate();
        ensureFrequencyAndDateSupported(radioWave, dc);
        return rangeRateModifiers.get(radioWave).get(dc);
    }

    /** Create modifiers for a frequency and date if needed.
     * @param radioWave radio wave of the signal
     * @param dc date for which modifiers are required
     */
    private void ensureFrequencyAndDateSupported(final RadioWave radioWave, final DateComponents dc) {

        if (!rangeModifiers.containsKey(radioWave)) {
            rangeModifiers.put(radioWave, new HashMap<>());
            rangeRateModifiers.put(radioWave, new HashMap<>());
        }

        if (!rangeModifiers.get(radioWave).containsKey(dc)) {

            // load Klobuchar model for the L1 frequency
            final KlobucharIonoCoefficientsLoader loader = new KlobucharIonoCoefficientsLoader();
            loader.loadKlobucharIonosphericCoefficients(dc);
            final IonosphericModel model = new KlobucharIonoModel(loader.getAlpha(), loader.getBeta());

            // scale for current frequency
            final double f = radioWave.getFrequency();

            // create modifiers
            rangeModifiers.get(radioWave).put(dc, new RangeIonosphericDelayModifier(model, f));
            rangeRateModifiers.get(radioWave).put(dc, new RangeRateIonosphericDelayModifier(model, f, twoWay));

        }

    }

}
