/* Copyright 2023 Luc Maisonobe
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

import java.io.Serializable;
import java.util.List;
import java.util.ListIterator;
import java.util.function.ToDoubleFunction;

import org.hipparchus.util.MathUtils;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.Constants;
import org.orekit.utils.SecularAndHarmonic;

/** Earth Orientation Parameters fitter for {@link PredictedEOPHistory EOP prediction}.
 * @see SecularAndHarmonic
 * @since 12.0
 * @author Luc Maisonobe
 */
public class EOPFitter implements Serializable {

    /** Sun pulsation, one year period. */
    public static final double SUN_PULSATION = MathUtils.TWO_PI / Constants.JULIAN_YEAR;

    /** Moon pulsation (one Moon synodic period). */
    public static final double MOON_PULSATION = MathUtils.TWO_PI / (29.530589 * Constants.JULIAN_DAY);

    /** Unidentified 13.533 days pulsation. */
    public static final double SHORT_PULSATION = MathUtils.TWO_PI / (13.533 * Constants.JULIAN_DAY);

    /** Serializable UID. */
    private static final long serialVersionUID = 20230228L;

    /** Duration of the fitting window at the end of the raw history (s). */
    private final double fittingDuration;

    /** Convergence on fitted parameter. */
    private final double convergence;

    /** Degree of the polynomial model. */
    private final int degree;

    /** Pulsations of harmonic part (rad/s). */
    private final double[] pulsations;

    /** Simple constructor.
     * @param fittingDuration duration of the fitting window at the end of the raw history (s)
     * @param convergence convergence on fitted parameter
     * @param degree degree of the polynomial model
     * @param pulsations pulsations of harmonic part (rad/s)
     * @see #createDefaultDut1Fitter()
     * @see #createDefaultPoleFitter()
     * @see #createDefaultNutationFitter()
     * @see SecularAndHarmonic
     */
    public EOPFitter(final double fittingDuration, final double convergence,
                     final int degree, final double... pulsations) {
        this.fittingDuration = fittingDuration;
        this.convergence     = convergence;
        this.degree          = degree;
        this.pulsations      = pulsations.clone();
    }

    /** Perform secular and harmonic fitting.
     * @param rawHistory EOP history to fit
     * @param extractor extractor for Earth Orientation Parameter
     * @return configured fitter
     */
    public SecularAndHarmonic fit(final EOPHistory rawHistory, final ToDoubleFunction<EOPEntry> extractor) {

        final List<EOPEntry> rawEntries = rawHistory.getEntries();
        final EOPEntry       last       = rawEntries.get(rawEntries.size() - 1);

        // create fitter
        final SecularAndHarmonic sh = new SecularAndHarmonic(degree, pulsations);

        // set up convergence
        sh.setConvergenceRMS(convergence);

        // set up reference date and initial guess to a constant value
        final double[] initialGuess = new double[degree + 1 + 2 * pulsations.length];
        initialGuess[0] = extractor.applyAsDouble(last);
        sh.resetFitting(last.getDate(), initialGuess);

        // sample history
        final AbsoluteDate           fitStart         = last.getDate().shiftedBy(-fittingDuration);
        final ListIterator<EOPEntry> backwardIterator = rawEntries.listIterator(rawEntries.size());
        while (backwardIterator.hasPrevious()) {
            final EOPEntry entry = backwardIterator.previous();
            if (entry.getDate().isAfterOrEqualTo(fitStart)) {
                // the entry belongs to the fitting interval
                sh.addPoint(entry.getDate(), extractor.applyAsDouble(entry));
            } else {
                // we have processed all entries from the fitting interval
                break;
            }
        }

        // perform fitting
        sh.fit();

        return sh;

    }

    /** Create fitter with default parameters adapted for fitting orientation parameters dUT1 and LOD.
     * <ul>
     *   <li>fitting duration set to two {@link Constants.JULIAN_YEAR years}</li>
     *   <li>convergence set to 10⁻¹² s</li>
     *   <li>polynomial part set to degree 3</li>
     *   <li>one harmonic term at {@link #SUN_PULSATION}}</li>
     *   <li>one harmonic term at 2 times {@link #SUN_PULSATION}}</li>
     *   <li>one harmonic term at 3 times {@link #SUN_PULSATION}}</li>
     *   <li>one harmonic term at {@link #MOON_PULSATION}}</li>
     *   <li>one harmonic term at 2 times {@link #MOON_PULSATION}}</li>
     *   <li>one harmonic term at 3 times {@link #MOON_PULSATION}}</li>
     *   <li>one harmonic term at {@link #SHORT_PULSATION 13.533 days}}</li>
     * </ul>
     * @return fitter with default configuration for orientation parameters dUT1 and LOD
     */
    public static EOPFitter createDefaultDut1Fitter() {
        return new EOPFitter(2 * Constants.JULIAN_YEAR, 1.0e-12, 3,
                             SUN_PULSATION, 2 * SUN_PULSATION, 3 * SUN_PULSATION,
                             MOON_PULSATION, 2 * MOON_PULSATION, 3 * MOON_PULSATION,
                             SHORT_PULSATION);
    }

    /** Create fitter with default parameters adapted for fitting pole parameters Xp and Yp.
     * <ul>
     *   <li>fitting duration set to one {@link Constants.JULIAN_YEAR year}</li>
     *   <li>convergence set to 10⁻¹² rad</li>
     *   <li>polynomial part set to degree 3</li>
     *   <li>one harmonic term at {@link #SUN_PULSATION}}</li>
     *   <li>one harmonic term at 2 times {@link #SUN_PULSATION}}</li>
     *   <li>one harmonic term at 3 times {@link #SUN_PULSATION}}</li>
     *   <li>one harmonic term at {@link #MOON_PULSATION}}</li>
     *   <li>one harmonic term at 2 times {@link #MOON_PULSATION}}</li>
     *   <li>one harmonic term at 3 times {@link #MOON_PULSATION}}</li>
     * </ul>
     * @return fitter with default configuration for pole parameters Xp and Yp
     */
    public static EOPFitter createDefaultPoleFitter() {
        return new EOPFitter(Constants.JULIAN_YEAR, 1.0e-12, 3,
                             SUN_PULSATION, 2 * SUN_PULSATION, 3 * SUN_PULSATION,
                             MOON_PULSATION, 2 * MOON_PULSATION, 3 * MOON_PULSATION);
    }

    /** Create fitter with default parameters adapted for fitting nutation parameters dx and dy.
     * <ul>
     *   <li>fitting duration set to one {@link Constants.JULIAN_YEAR year}</li>
     *   <li>convergence set to 10⁻¹² s</li>
     *   <li>polynomial part set to degree 3</li>
     *   <li>one harmonic term at {@link #SUN_PULSATION}}</li>
     *   <li>one harmonic term at 2 times {@link #SUN_PULSATION}}</li>
     *   <li>one harmonic term at 3 times {@link #SUN_PULSATION}}</li>
     *   <li>one harmonic term at {@link #MOON_PULSATION}}</li>
     *   <li>one harmonic term at 2 times {@link #MOON_PULSATION}}</li>
     *   <li>one harmonic term at 3 times {@link #MOON_PULSATION}}</li>
     * </ul>
     * @return fitter with default configuration for pole nutation parameters dx and dy
     */
    public static EOPFitter createDefaultNutationFitter() {
        return new EOPFitter(Constants.JULIAN_YEAR, 1.0e-12, 3,
                             SUN_PULSATION, 2 * SUN_PULSATION, 3 * SUN_PULSATION,
                             MOON_PULSATION, 2 * MOON_PULSATION, 3 * MOON_PULSATION);
    }

}
