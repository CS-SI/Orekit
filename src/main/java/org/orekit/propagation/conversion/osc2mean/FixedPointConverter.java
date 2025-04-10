/* Copyright 2002-2025 CS GROUP
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
package org.orekit.propagation.conversion.osc2mean;

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.Field;
import org.hipparchus.util.FastMath;
import org.hipparchus.util.MathUtils;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.orbits.EquinoctialOrbit;
import org.orekit.orbits.FieldEquinoctialOrbit;
import org.orekit.orbits.FieldOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.PositionAngleType;
import org.orekit.time.FieldAbsoluteDate;

/**
 * Class enabling conversion from osculating to mean orbit
 * for a given theory using a fixed-point algorithm.
 *
 * @author Pascal Parraud
 * @since 13.0
 */
public class FixedPointConverter implements OsculatingToMeanConverter {

    /** Default convergence threshold. */
    public static final double DEFAULT_THRESHOLD   = 1e-12;

    /** Default maximum number of iterations. */
    public static final int DEFAULT_MAX_ITERATIONS = 100;

    /** Default damping ratio. */
    public static final double DEFAULT_DAMPING     = 1.;

    /** Mean theory used. */
    private MeanTheory theory;

    /** Convergence threshold. */
    private double threshold;

    /** Maximum number of iterations. */
    private int maxIterations;

    /** Damping ratio. */
    private double damping;

    /** Number of iterations performed. */
    private int iterationsNb;

    /**
     * Default constructor.
     * <p>
     * The mean theory must be set before converting.
     */
    public FixedPointConverter() {
        this(null, DEFAULT_THRESHOLD, DEFAULT_MAX_ITERATIONS, DEFAULT_DAMPING);
    }

    /**
     * Constructor.
     * @param theory mean theory to be used
     */
    public FixedPointConverter(final MeanTheory theory) {
        this(theory, DEFAULT_THRESHOLD, DEFAULT_MAX_ITERATIONS, DEFAULT_DAMPING);
    }

    /**
     * Constructor.
     * <p>
     * The mean theory must be set before converting.
     *
     * @param threshold tolerance for convergence
     * @param maxIterations maximum number of iterations
     * @param damping damping ratio
     */
    public FixedPointConverter(final double threshold,
                               final int maxIterations,
                               final double damping) {
        this(null, threshold, maxIterations, damping);
    }

    /**
     * Constructor.
     * @param theory mean theory to be used
     * @param threshold tolerance for convergence
     * @param maxIterations maximum number of iterations
     * @param damping damping ratio
     */
    public FixedPointConverter(final MeanTheory theory,
                               final double threshold,
                               final int maxIterations,
                               final double damping) {
        setMeanTheory(theory);
        setThreshold(threshold);
        setMaxIterations(maxIterations);
        setDamping(damping);
    }

    /** {@inheritDoc} */
    @Override
    public MeanTheory getMeanTheory() {
        return theory;
    }

    /** {@inheritDoc} */
    @Override
    public void setMeanTheory(final MeanTheory meanTheory) {
        this.theory = meanTheory;
    }

    /**
     * Gets convergence threshold.
     * @return convergence threshold
     */
    public double getThreshold() {
        return threshold;
    }

    /**
     * Sets convergence threshold.
     * @param threshold convergence threshold
     */
    public void setThreshold(final double threshold) {
        this.threshold = threshold;
    }

    /**
     * Gets maximum number of iterations.
     * @return maximum number of iterations
     */
    public int getMaxIterations() {
        return maxIterations;
    }

    /**
     * Sets maximum number of iterations.
     * @param maxIterations maximum number of iterations
     */
    public void setMaxIterations(final int maxIterations) {
        this.maxIterations = maxIterations;
    }

    /**
     * Gets damping ratio.
     * @return damping ratio
     */
    public double getDamping() {
        return damping;
    }

    /**
     * Sets damping ratio.
     * @param damping damping ratio
     */
    public void setDamping(final double damping) {
        this.damping = damping;
    }

    /**
     * Gets the number of iterations performed by the last conversion.
     * @return number of iterations
     */
    public int getIterationsNb() {
        return iterationsNb;
    }

    /** {@inheritDoc}
     *  Uses a fixed-point algorithm.
     */
    @Override
    public Orbit convertToMean(final Orbit osculating) {

        // sanity check
        if (osculating.getA() < theory.getReferenceRadius()) {
            throw new OrekitException(OrekitMessages.TRAJECTORY_INSIDE_BRILLOUIN_SPHERE,
                                      osculating.getA());
        }

        // Get equinoctial osculating parameters
        final Orbit equinoctial = theory.preprocessing(osculating);
        double sma = equinoctial.getA();
        double ex  = equinoctial.getEquinoctialEx();
        double ey  = equinoctial.getEquinoctialEy();
        double hx  = equinoctial.getHx();
        double hy  = equinoctial.getHy();
        double lv  = equinoctial.getLv();

        // Set threshold for each parameter
        final double thresholdA  = threshold * FastMath.abs(sma);
        final double thresholdE  = threshold * (1 + FastMath.hypot(ex, ey));
        final double thresholdH  = threshold * (1 + FastMath.hypot(hx, hy));
        final double thresholdLv = threshold * FastMath.PI;

        // Rough initialization of the mean parameters
        Orbit mean = theory.initialize(equinoctial);

        int i = 0;
        while (i++ < maxIterations) {

            // Update osculating parameters from current mean parameters
            final Orbit updated = theory.meanToOsculating(mean);

            // Updated parameters residuals
            final double deltaA  = equinoctial.getA() - updated.getA();
            final double deltaEx = equinoctial.getEquinoctialEx() - updated.getEquinoctialEx();
            final double deltaEy = equinoctial.getEquinoctialEy() - updated.getEquinoctialEy();
            final double deltaHx = equinoctial.getHx() - updated.getHx();
            final double deltaHy = equinoctial.getHy() - updated.getHy();
            final double deltaLv = MathUtils.normalizeAngle(equinoctial.getLv() - updated.getLv(), 0.0);

            // Check convergence
            if (FastMath.abs(deltaA)  < thresholdA &&
                FastMath.abs(deltaEx) < thresholdE &&
                FastMath.abs(deltaEy) < thresholdE &&
                FastMath.abs(deltaHx) < thresholdH &&
                FastMath.abs(deltaHy) < thresholdH &&
                FastMath.abs(deltaLv) < thresholdLv) {
                // Records number of iterations performed
                iterationsNb = i;
                // Returns the mean orbit
                return theory.postprocessing(osculating, mean);
            }

            // Update mean parameters
            sma += damping * deltaA;
            ex  += damping * deltaEx;
            ey  += damping * deltaEy;
            hx  += damping * deltaHx;
            hy  += damping * deltaHy;
            lv  += damping * deltaLv;

            // Update mean orbit
            mean = new EquinoctialOrbit(sma, ex, ey, hx, hy, lv,
                                        PositionAngleType.TRUE,
                                        equinoctial.getFrame(),
                                        equinoctial.getDate(),
                                        equinoctial.getMu());
        }
        throw new OrekitException(OrekitMessages.UNABLE_TO_COMPUTE_MEAN_PARAMETERS, theory.getTheoryName(), i);
    }

    /** {@inheritDoc}
     *  Uses a fixed-point algorithm.
     */
    @Override
    public <T extends CalculusFieldElement<T>> FieldOrbit<T> convertToMean(final FieldOrbit<T> osculating) {

        // Sanity check
        if (osculating.getA().getReal() < theory.getReferenceRadius()) {
            throw new OrekitException(OrekitMessages.TRAJECTORY_INSIDE_BRILLOUIN_SPHERE,
                                           osculating.getA());
        }

        // Get field
        final FieldAbsoluteDate<T> date = osculating.getDate();
        final Field<T> field = date.getField();
        final T zero = field.getZero();
        final T pi   = zero.getPi();

        // Get equinoctial parameters
        final FieldOrbit<T> equinoctial = theory.preprocessing(osculating);
        T sma = equinoctial.getA();
        T ex  = equinoctial.getEquinoctialEx();
        T ey  = equinoctial.getEquinoctialEy();
        T hx  = equinoctial.getHx();
        T hy  = equinoctial.getHy();
        T lv  = equinoctial.getLv();

        // Set threshold for each parameter
        final T thresholdA  = sma.abs().multiply(threshold);
        final T thresholdE  = FastMath.hypot(ex, ey).add(1).multiply(threshold);
        final T thresholdH  = FastMath.hypot(hx, hy).add(1).multiply(threshold);
        final T thresholdLv = pi.multiply(threshold);

        // Rough initialization of the mean parameters
        FieldOrbit<T> mean = theory.initialize(equinoctial);

        int i = 0;
        while (i++ < maxIterations) {

            // recompute the osculating parameters from the current mean parameters
            final FieldOrbit<T> updated = theory.meanToOsculating(mean);

            // Updated parameters residuals
            final T deltaA  = equinoctial.getA().subtract(updated.getA());
            final T deltaEx = equinoctial.getEquinoctialEx().subtract(updated.getEquinoctialEx());
            final T deltaEy = equinoctial.getEquinoctialEy().subtract(updated.getEquinoctialEy());
            final T deltaHx = equinoctial.getHx().subtract(updated.getHx());
            final T deltaHy = equinoctial.getHy().subtract(updated.getHy());
            final T deltaLv = MathUtils.normalizeAngle(equinoctial.getLv().subtract(updated.getLv()), zero);

            // Check convergence
            if (FastMath.abs(deltaA.getReal())  < thresholdA.getReal() &&
                FastMath.abs(deltaEx.getReal()) < thresholdE.getReal() &&
                FastMath.abs(deltaEy.getReal()) < thresholdE.getReal() &&
                FastMath.abs(deltaHx.getReal()) < thresholdH.getReal() &&
                FastMath.abs(deltaHy.getReal()) < thresholdH.getReal() &&
                FastMath.abs(deltaLv.getReal()) < thresholdLv.getReal()) {
                // Records number of iterations performed
                iterationsNb = i;
                // Returns the mean orbit
                return theory.postprocessing(osculating, mean);
            }

            // Update mean parameters
            sma = sma.add(deltaA.multiply(damping));
            ex  = ex.add(deltaEx.multiply(damping));
            ey  = ey.add(deltaEy.multiply(damping));
            hx  = hx.add(deltaHx.multiply(damping));
            hy  = hy.add(deltaHy.multiply(damping));
            lv  = lv.add(deltaLv.multiply(damping));

            // Update mean orbit
            mean = new FieldEquinoctialOrbit<>(sma, ex, ey, hx, hy, lv,
                                               PositionAngleType.TRUE,
                                               equinoctial.getFrame(),
                                               equinoctial.getDate(),
                                               equinoctial.getMu());
        }
        throw new OrekitException(OrekitMessages.UNABLE_TO_COMPUTE_MEAN_PARAMETERS, theory.getTheoryName(), i);
    }
}
