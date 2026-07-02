/* Copyright 2022-2026 Luc Maisonobe
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
package org.orekit.frames;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.hipparchus.util.FastMath;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.Constants;
import org.orekit.utils.IERSConventions.NutationCorrectionConverter;
import org.orekit.utils.SecularAndHarmonic;

/** This class extends an {@link EOPHistory} for some weeks using fitting.
 * <p>
 * The goal of this class is to provide a reasonable prediction of
 * Earth Orientation Parameters past the last date available in
 * regular {@link EOPHistory}, which just generated corrections set
 * to 0 when they have no data.
 * </p>
 * <p>
 * The prediction is based on fitting of last data, with both
 * {@link SecularAndHarmonic secular (polynomial) and harmonic (periodic)}
 * terms. The extended entries are generated at one point per day
 * and are continuous (i.e. no leap seconds are introduced)
 * </p>
 * <p>
 * After construction, the history contains both the initial
 * raw history and an extension part appended after it.
 * </p>
 * @see EOPFitter
 * @see SecularAndHarmonic
 * @since 12.0
 * @author Luc Maisonobe
 */
public class PredictedEOPHistory extends EOPHistory {

    /** Simple constructor.
     * @param rawHistory raw EOP history to extend.
     * @param extensionDuration duration of the extension period (s)
     * @param fittedModel fitted EOP model
     */
    public PredictedEOPHistory(final EOPHistory rawHistory, final double extensionDuration,
                               final EOPFittedModel fittedModel) {
        super(rawHistory.getConventions(), rawHistory.getInterpolationDegree(),
              extendHistory(rawHistory, extensionDuration, fittedModel),
              rawHistory.isSimpleEop(), rawHistory.getTimeScales());
    }

    /** Extends raw history.
     * @param rawHistory raw EOP history to extend.
     * @param extensionDuration duration of the extension period (s)
     * @param fittedModel fitted EOP model
     * @return extended history
     */
    private static Collection<? extends EOPEntry> extendHistory(final EOPHistory rawHistory,
                                                                final double extensionDuration,
                                                                final EOPFittedModel fittedModel) {


        // create a converter for nutation corrections
        final NutationCorrectionConverter converter =
                        rawHistory.getConventions().getNutationCorrectionConverter(rawHistory.getTimeScales());

        // generate extension entries
        final List<EOPEntry> rawEntries = rawHistory.getEntries();
        final EOPEntry       last       = rawEntries.getLast();
        final int n = (int) FastMath.rint(extensionDuration / Constants.JULIAN_DAY);
        final List<EOPEntry> entries = new ArrayList<>(rawEntries.size() + n);
        entries.addAll(rawEntries);
        for (int i = 0; i < n; ++i) {
            final AbsoluteDate date = last.getDate().shiftedBy((i + 1) * Constants.JULIAN_DAY);
            final double dut1   = fittedModel.getDUT1().osculatingValue(date);
            final double lod    = -Constants.JULIAN_DAY * fittedModel.getDUT1().osculatingDerivative(date);
            final double xp     = fittedModel.getXp().osculatingValue(date);
            final double yp     = fittedModel.getYp().osculatingValue(date);
            final double xpRate = fittedModel.getXp().osculatingDerivative(date);
            final double ypRate = fittedModel.getYp().osculatingDerivative(date);
            final double dx     = fittedModel.getDx().osculatingValue(date);
            final double dy     = fittedModel.getDy().osculatingValue(date);
            final double[] equinox = converter.toEquinox(date, dx, dy);
            entries.add(new EOPEntry(last.getMjd() + i + 1, dut1, lod, xp, yp, xpRate, ypRate,
                                     equinox[0], equinox[1], dx, dy,
                                     last.getITRFType(), date, EopDataType.PREDICTED));
        }

        return entries;

    }

}
