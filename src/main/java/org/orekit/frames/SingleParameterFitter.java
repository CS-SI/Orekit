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

import org.hipparchus.util.FastMath;
import org.hipparchus.util.MathUtils;
import org.orekit.utils.Constants;
import org.orekit.utils.SecularAndHarmonic;

/** Fitter for one Earth Orientation Parameter.
 * @see PredictedEOPHistory
 * @see EOPFitter
 * @see SecularAndHarmonic
 * @since 12.0
 * @author Luc Maisonobe
 */
public class SingleParameterFitter implements Serializable {

    /** Sun pulsation, one year period. */
    public static final double SUN_PULSATION = MathUtils.TWO_PI / Constants.JULIAN_YEAR;

    /** Moon pulsation (one Moon draconic period). */
    public static final double MOON_DRACONIC_PULSATION = MathUtils.TWO_PI / (27.212221 * Constants.JULIAN_DAY);

    /** Serializable UID. */
    private static final long serialVersionUID = 20230309L;

    /** Time constant of the exponential decay weight. */
    private final double timeConstant;

    /** Convergence on fitted parameter. */
    private final double convergence;

    /** Degree of the polynomial model. */
    private final int degree;

    /** Pulsations of harmonic part (rad/s). */
    private final double[] pulsations;

    /** Simple constructor.
     * @param fittingDuration ignored parameter since 12.0
     * @param timeConstant time constant \(\tau\) of the exponential decay weight, point weight is \(e^{\frac{t-t_0}{\tau}}\),
     * i.e. points far in the past before \(t_0\) have smaller weights
     * @param convergence convergence on fitted parameter
     * @param degree degree of the polynomial model
     * @param pulsations pulsations of harmonic part (rad/s)
     * @see #createDefaultDut1FitterShortTermPrediction()
     * @see #createDefaultDut1FitterLongTermPrediction()
     * @see #createDefaultPoleFitterShortTermPrediction()
     * @see #createDefaultPoleFitterLongTermPrediction()
     * @see #createDefaultNutationFitterShortTermPrediction()
     * @see #createDefaultNutationFitterLongTermPrediction()
     * @see SecularAndHarmonic
     * @deprecated replaced by {@link #SingleParameterFitter(double, double, int, double...)}
     */
    @Deprecated
    public SingleParameterFitter(final double fittingDuration, final double timeConstant, final double convergence,
                                 final int degree, final double... pulsations) {
        this(timeConstant, convergence, degree, pulsations);
    }

    /** Simple constructor.
     * @param timeConstant time constant \(\tau\) of the exponential decay weight, point weight is \(e^{\frac{t-t_0}{\tau}}\),
     * i.e. points far in the past before \(t_0\) have smaller weights
     * @param convergence convergence on fitted parameter
     * @param degree degree of the polynomial model
     * @param pulsations pulsations of harmonic part (rad/s)
     * @see #createDefaultDut1FitterShortTermPrediction()
     * @see #createDefaultDut1FitterLongTermPrediction()
     * @see #createDefaultPoleFitterShortTermPrediction()
     * @see #createDefaultPoleFitterLongTermPrediction()
     * @see #createDefaultNutationFitterShortTermPrediction()
     * @see #createDefaultNutationFitterLongTermPrediction()
     * @see SecularAndHarmonic
     * @since 12.0.1
     */
    public SingleParameterFitter(final double timeConstant, final double convergence,
                                 final int degree, final double... pulsations) {
        this.timeConstant    = timeConstant;
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
        final ListIterator<EOPEntry> backwardIterator = rawEntries.listIterator(rawEntries.size());
        while (backwardIterator.hasPrevious()) {
            final EOPEntry entry = backwardIterator.previous();
            sh.addWeightedPoint(entry.getDate(), extractor.applyAsDouble(entry),
                                FastMath.exp(entry.getDate().durationFrom(last.getDate()) / timeConstant));
        }

        // perform fitting
        sh.fit();

        return sh;

    }

    /** Create fitter with default parameters adapted for fitting orientation parameters dUT1 and LOD
     * for short term prediction.
     * <p>
     * The main difference between these settings and {@link #createDefaultDut1FitterLongTermPrediction()
     * the settings for long prediction} is the much smaller \(\tau\). This means more
     * weight is set to the points at the end of the history, hence forcing the fitted prediction
     * model to be closer to these points, hence the prediction error to be smaller just after
     * raw history end. On the other hand, this implies that the model will diverge on long term.
     * These settings are intended when prediction is used for at most 5 days after raw EOP end.
     * </p>
     * <ul>
     *   <li>time constant \(\tau\) of the exponential decay set to 6 {@link Constants#JULIAN_DAY days}</li>
     *   <li>convergence set to 10⁻¹² s</li>
     *   <li>polynomial part set to degree 3</li>
     *   <li>one harmonic term at {@link #SUN_PULSATION}}</li>
     *   <li>one harmonic term at 2 times {@link #SUN_PULSATION}}</li>
     *   <li>one harmonic term at 3 times {@link #SUN_PULSATION}}</li>
     *   <li>one harmonic term at {@link #MOON_DRACONIC_PULSATION}}</li>
     *   <li>one harmonic term at 2 times {@link #MOON_DRACONIC_PULSATION}}</li>
     *   <li>one harmonic term at 3 times {@link #MOON_DRACONIC_PULSATION}}</li>
     * </ul>
     * @return fitter with default configuration for orientation parameters dUT1 and LOD
     * @see #createDefaultDut1FitterShortTermPrediction()
     */
    public static SingleParameterFitter createDefaultDut1FitterShortTermPrediction() {
        return new SingleParameterFitter(6 * Constants.JULIAN_DAY, 1.0e-12, 3,
                                         SUN_PULSATION, 2 * SUN_PULSATION, 3 * SUN_PULSATION,
                                         MOON_DRACONIC_PULSATION, 2 * MOON_DRACONIC_PULSATION, 3 * MOON_DRACONIC_PULSATION);
    }

    /** Create fitter with default parameters adapted for fitting orientation parameters dUT1 and LOD
     * for long term prediction.
     * <p>
     * The main difference between these settings and {@link #createDefaultDut1FitterShortTermPrediction()
     * the settings for short prediction} is the much larger \(\tau\). This means weight
     * is spread throughout history, hence forcing the fitted prediction model to be remain very stable
     * on the long term. On the other hand, this implies that the model will start with already a much
     * larger error just after raw history end.
     * These settings are intended when prediction is used for 5 days after raw EOP end or more.
     * </p>
     * <ul>
     *   <li>time constant \(\tau\) of the exponential decay set to 60 {@link Constants#JULIAN_DAY days}</li>
     *   <li>convergence set to 10⁻¹² s</li>
     *   <li>polynomial part set to degree 3</li>
     *   <li>one harmonic term at {@link #SUN_PULSATION}}</li>
     *   <li>one harmonic term at 2 times {@link #SUN_PULSATION}}</li>
     *   <li>one harmonic term at 3 times {@link #SUN_PULSATION}}</li>
     *   <li>one harmonic term at {@link #MOON_DRACONIC_PULSATION}}</li>
     *   <li>one harmonic term at 2 times {@link #MOON_DRACONIC_PULSATION}}</li>
     *   <li>one harmonic term at 3 times {@link #MOON_DRACONIC_PULSATION}}</li>
     * </ul>
     * @return fitter with default configuration for orientation parameters dUT1 and LOD
     * @see #createDefaultDut1FitterShortTermPrediction()
     */
    public static SingleParameterFitter createDefaultDut1FitterLongTermPrediction() {
        return new SingleParameterFitter(60 * Constants.JULIAN_DAY, 1.0e-12, 3,
                                         SUN_PULSATION, 2 * SUN_PULSATION, 3 * SUN_PULSATION,
                                         MOON_DRACONIC_PULSATION, 2 * MOON_DRACONIC_PULSATION, 3 * MOON_DRACONIC_PULSATION);
    }

    /** Create fitter with default parameters adapted for fitting pole parameters Xp and Yp
     * for long term prediction.
     * <p>
     * The main difference between these settings and {@link #createDefaultPoleFitterLongTermPrediction()
     * the settings for long prediction} is the much smaller \(\tau\). This means more
     * weight is set to the points at the end of the history, hence forcing the fitted prediction
     * model to be closer to these points, hence the prediction error to be smaller just after
     * raw history end. On the other hand, this implies that the model will diverge on long term.
     * These settings are intended when prediction is used for at most 5 days after raw EOP end.
     * </p>
     * <ul>
     *   <li>time constant \(\tau\) of the exponential decay set to 12 {@link Constants#JULIAN_DAY days}</li>
     *   <li>convergence set to 10⁻¹² rad</li>
     *   <li>polynomial part set to degree 3</li>
     *   <li>one harmonic term at {@link #SUN_PULSATION}}</li>
     *   <li>one harmonic term at 2 times {@link #SUN_PULSATION}}</li>
     *   <li>one harmonic term at 3 times {@link #SUN_PULSATION}}</li>
     *   <li>one harmonic term at {@link #MOON_DRACONIC_PULSATION}}</li>
     *   <li>one harmonic term at 2 times {@link #MOON_DRACONIC_PULSATION}}</li>
     *   <li>one harmonic term at 3 times {@link #MOON_DRACONIC_PULSATION}}</li>
     * </ul>
     * @return fitter with default configuration for pole parameters Xp and Yp
     */
    public static SingleParameterFitter createDefaultPoleFitterShortTermPrediction() {
        return new SingleParameterFitter(12 * Constants.JULIAN_DAY, 1.0e-12, 3,
                                         SUN_PULSATION, 2 * SUN_PULSATION, 3 * SUN_PULSATION,
                                         MOON_DRACONIC_PULSATION, 2 * MOON_DRACONIC_PULSATION, 3 * MOON_DRACONIC_PULSATION);
    }

    /** Create fitter with default parameters adapted for fitting pole parameters Xp and Yp
     * for long term prediction.
     * <p>
     * The main difference between these settings and {@link #createDefaultPoleFitterShortTermPrediction()
     * the settings for short prediction} is the much larger \(\tau\). This means weight
     * is spread throughout history, hence forcing the fitted prediction model to be remain very stable
     * on the long term. On the other hand, this implies that the model will start with already a much
     * larger error just after raw history end.
     * These settings are intended when prediction is used for 5 days after raw EOP end or more.
     * </p>
     * <ul>
     *   <li>time constant \(\tau\) of the exponential decay set to 60 {@link Constants#JULIAN_DAY days}</li>
     *   <li>convergence set to 10⁻¹² rad</li>
     *   <li>polynomial part set to degree 3</li>
     *   <li>one harmonic term at {@link #SUN_PULSATION}}</li>
     *   <li>one harmonic term at 2 times {@link #SUN_PULSATION}}</li>
     *   <li>one harmonic term at 3 times {@link #SUN_PULSATION}}</li>
     *   <li>one harmonic term at {@link #MOON_DRACONIC_PULSATION}}</li>
     *   <li>one harmonic term at 2 times {@link #MOON_DRACONIC_PULSATION}}</li>
     *   <li>one harmonic term at 3 times {@link #MOON_DRACONIC_PULSATION}}</li>
     * </ul>
     * @return fitter with default configuration for pole parameters Xp and Yp
     */
    public static SingleParameterFitter createDefaultPoleFitterLongTermPrediction() {
        return new SingleParameterFitter(60 * Constants.JULIAN_DAY, 1.0e-12, 3,
                                         SUN_PULSATION, 2 * SUN_PULSATION, 3 * SUN_PULSATION,
                                         MOON_DRACONIC_PULSATION, 2 * MOON_DRACONIC_PULSATION, 3 * MOON_DRACONIC_PULSATION);
    }

    /** Create fitter with default parameters adapted for fitting nutation parameters dx and dy
     * for long term prediction.
     * <p>
     * The main difference between these settings and {@link #createDefaultNutationFitterLongTermPrediction()
     * the settings for long prediction} is the much smaller \(\tau\). This means more
     * weight is set to the points at the end of the history, hence forcing the fitted prediction
     * model to be closer to these points, hence the prediction error to be smaller just after
     * raw history end. On the other hand, this implies that the model will diverge on long term.
     * These settings are intended when prediction is used for at most 5 days after raw EOP end.
     * </p>
     * <ul>
     *   <li>time constant \(\tau\) of the exponential decay set to 12 {@link Constants#JULIAN_DAY days}</li>
     *   <li>convergence set to 10⁻¹² s</li>
     *   <li>polynomial part set to degree 3</li>
     *   <li>one harmonic term at {@link #SUN_PULSATION}}</li>
     *   <li>one harmonic term at 2 times {@link #SUN_PULSATION}}</li>
     *   <li>one harmonic term at 3 times {@link #SUN_PULSATION}}</li>
     *   <li>one harmonic term at {@link #MOON_DRACONIC_PULSATION}}</li>
     *   <li>one harmonic term at 2 times {@link #MOON_DRACONIC_PULSATION}}</li>
     *   <li>one harmonic term at 3 times {@link #MOON_DRACONIC_PULSATION}}</li>
     * </ul>
     * @return fitter with default configuration for pole nutation parameters dx and dy
     */
    public static SingleParameterFitter createDefaultNutationFitterShortTermPrediction() {
        return new SingleParameterFitter(12 * Constants.JULIAN_DAY, 1.0e-12, 3,
                                         SUN_PULSATION, 2 * SUN_PULSATION, 3 * SUN_PULSATION,
                                         MOON_DRACONIC_PULSATION, 2 * MOON_DRACONIC_PULSATION, 3 * MOON_DRACONIC_PULSATION);
    }

    /** Create fitter with default parameters adapted for fitting nutation parameters dx and dy
     * for long term prediction.
     * <p>
     * The main difference between these settings and {@link #createDefaultNutationFitterShortTermPrediction()
     * the settings for short prediction} is the much larger \(\tau\). This means weight
     * is spread throughout history, hence forcing the fitted prediction model to be remain very stable
     * on the long term. On the other hand, this implies that the model will start with already a much
     * larger error just after raw history end.
     * These settings are intended when prediction is used for 5 days after raw EOP end or more.
     * </p>
     * <ul>
     *   <li>time constant \(\tau\) of the exponential decay set to 60 {@link Constants#JULIAN_DAY days}</li>
     *   <li>convergence set to 10⁻¹² s</li>
     *   <li>polynomial part set to degree 3</li>
     *   <li>one harmonic term at {@link #SUN_PULSATION}}</li>
     *   <li>one harmonic term at 2 times {@link #SUN_PULSATION}}</li>
     *   <li>one harmonic term at 3 times {@link #SUN_PULSATION}}</li>
     *   <li>one harmonic term at {@link #MOON_DRACONIC_PULSATION}}</li>
     *   <li>one harmonic term at 2 times {@link #MOON_DRACONIC_PULSATION}}</li>
     *   <li>one harmonic term at 3 times {@link #MOON_DRACONIC_PULSATION}}</li>
     * </ul>
     * @return fitter with default configuration for pole nutation parameters dx and dy
     */
    public static SingleParameterFitter createDefaultNutationFitterLongTermPrediction() {
        return new SingleParameterFitter(60 * Constants.JULIAN_DAY, 1.0e-12, 3,
                                         SUN_PULSATION, 2 * SUN_PULSATION, 3 * SUN_PULSATION,
                                         MOON_DRACONIC_PULSATION, 2 * MOON_DRACONIC_PULSATION, 3 * MOON_DRACONIC_PULSATION);
    }

}
