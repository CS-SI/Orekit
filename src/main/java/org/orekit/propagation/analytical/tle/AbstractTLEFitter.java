/* Copyright 2002-2012 CS Systèmes d'Information
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
package org.orekit.propagation.analytical.tle;

import java.util.List;

import org.apache.commons.math3.analysis.DifferentiableMultivariateVectorFunction;
import org.apache.commons.math3.analysis.MultivariateMatrixFunction;
import org.apache.commons.math3.exception.MaxCountExceededException;
import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import org.apache.commons.math3.util.FastMath;
import org.apache.commons.math3.util.MathUtils;
import org.apache.commons.math3.util.Precision;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitExceptionWrapper;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.orbits.KeplerianOrbit;
import org.orekit.propagation.SpacecraftState;
import org.orekit.utils.PVCoordinates;

/** Abstract class for TLE/Orbit fitting.
 * <p>
 * Two-Line Elements are tightly linked to the SGP4/SDP4 propagation models. They
 * cannot be used with other models and do not represent osculating orbits. When
 * conversion is needed, the model must be considered and conversion must be done
 * by some fitting method on a sufficient time range.
 * </p>
 * <p>
 * This base class factor the common code for such conversions.
 * Different implementations correspond to different fitting algorithms.
 * </p>
 * @author Rocca
 * @since 6.0
 */
public abstract class AbstractTLEFitter {

    /** Earth gravity coefficient in m<sup>3</sup>/s<sup>2</sup>. */
    private static final double MU =
        TLEConstants.XKE * TLEConstants.XKE *
        TLEConstants.EARTH_RADIUS * TLEConstants.EARTH_RADIUS * TLEConstants.EARTH_RADIUS *
        (1000 * 1000 * 1000) / (60 * 60);

    /** Satellite number. */
    private final int satelliteNumber;

    /** Classification (U for unclassified). */
    private final char classification;

    /** Launch year (all digits). */
    private final int launchYear;

    /** Launch number. */
    private final int launchNumber;

    /** Launch piece. */
    private final String launchPiece;

    /** Element number. */
    private final int elementNumber;

    /** Revolution number at epoch. */
    private final int revolutionNumberAtEpoch;

    /** Auxiliary outputData: RMS of solution. */
    private double rms;

    /** Spacecraft states samples. */
    private List<SpacecraftState> sample;

    /** TEME frame. */
    private Frame teme;

    /** Desired position tolerance. */
    private double tolerance;

    /** Position use indicator. */
    private boolean onlyPosition;

    /** Function computing residuals. */
    private final ResidualsFunction pvFunction;

    /** Target position and velocities at sample points. */
    private double[] target;

    /** Weight for residuals. */
    private double[] weight;

    /** Fitted Two-Lines Elements. */
    private TLE tle;

    /** Simple constructor.
     * @param satelliteNumber satellite number
     * @param classification classification (U for unclassified)
     * @param launchYear launch year (all digits)
     * @param launchNumber launch number
     * @param launchPiece launch piece
     * @param elementNumber element number
     * @param revolutionNumberAtEpoch revolution number at epoch
     */
    protected AbstractTLEFitter(final int satelliteNumber, final char classification,
                                     final int launchYear, final int launchNumber, final String launchPiece,
                                     final int elementNumber, final int revolutionNumberAtEpoch) {
        this.satelliteNumber         = satelliteNumber;
        this.classification          = classification;
        this.launchYear              = launchYear;
        this.launchNumber            = launchNumber;
        this.launchPiece             = launchPiece;
        this.elementNumber           = elementNumber;
        this.revolutionNumberAtEpoch = revolutionNumberAtEpoch;
        this.pvFunction                = new ResidualsFunction();
    }

    /** Find the TLE elements that minimize the mean square error for a sample of {@link SpacecraftState states}.
     * @param states spacecraft states sample to fit
     * @param positionTolerance desired position tolerance
     * @param positionOnly if true, consider only position data otherwise both position and
     * velocity are used
     * @param withBStar if true, the B* coefficient must be evaluated too, otherwise
     * it will be forced to 0
     * @return fitted TLE
     * @exception OrekitException if TLE cannot be computed
     * @exception MaxCountExceededException if maximal number of iterations is exceeded
     * @see #getTLE()
     * @see #getRMS()
     */
    public TLE toTLE(final List<SpacecraftState> states, final double positionTolerance,
                     final boolean positionOnly, final boolean withBStar)
        throws OrekitException, MaxCountExceededException {

        teme = FramesFactory.getTEME();
        setSample(states);
        this.tolerance    = positionTolerance;
        this.onlyPosition = positionOnly;

        // very rough first guess using osculating parameters of first sample point
        final double[] initial = new double[withBStar ? 7 : 6];
        final PVCoordinates pv = states.get(0).getPVCoordinates(FramesFactory.getTEME());
        initial[0] = pv.getPosition().getX();
        initial[1] = pv.getPosition().getY();
        initial[2] = pv.getPosition().getZ();
        initial[3] = pv.getVelocity().getX();
        initial[4] = pv.getVelocity().getY();
        initial[5] = pv.getVelocity().getZ();

        // warm-up iterations, using only a few points
        setSample(states.subList(0, onlyPosition ? 2 : 1));
        final double[] intermediate = fit(initial);

        // final search using all points
        setSample(states);
        final double[] result = fit(intermediate);

        rms = getRMS(result);
        tle = getTLE(result);
        return tle;

    }

    /** Get the fitted Two-Lines Elements.
     * @return fitted Two-Lines Elements
     * @see #toTLE(List, double, boolean, boolean)
     */
    public TLE getTLE() {
        return tle;
    }

    /** Get Root Mean Square of the fitting.
     * @return rms
     * @see #toTLE(List, double, boolean, boolean)
     */
    public double getRMS() {
        return rms;
    }

    /** Find the TLE elements that minimize the mean square error for a sample of {@link SpacecraftState states}.
     * @param initial initial estimation parameters (position, velocity and B* if estimated)
     * @return fitted parameters
     * @exception OrekitException if TLE cannot be computed
     * @exception MaxCountExceededException if maximal number of iterations is exceeded
     */
    protected abstract double[] fit(double[] initial) throws OrekitException, MaxCountExceededException;

    /** Get the TLE for a given position/velocity/B* parameters set.
     * @param parameters position/velocity/B* parameters set
     * @return TLE
     */
    protected TLE getTLE(final double[] parameters) {
        final KeplerianOrbit orb =
                new KeplerianOrbit(new PVCoordinates(new Vector3D(parameters[0], parameters[1], parameters[2]),
                                                     new Vector3D(parameters[3], parameters[4], parameters[5])),
                                   teme, sample.get(0).getDate(), MU);
        return new TLE(satelliteNumber, classification, launchYear, launchNumber, launchPiece,
                       TLE.DEFAULT, elementNumber, sample.get(0).getDate(),
                       orb.getKeplerianMeanMotion(), 0.0, 0.0,
                       orb.getE(), MathUtils.normalizeAngle(orb.getI(), FastMath.PI),
                       MathUtils.normalizeAngle(orb.getPerigeeArgument(), FastMath.PI),
                       MathUtils.normalizeAngle(orb.getRightAscensionOfAscendingNode(), FastMath.PI),
                       MathUtils.normalizeAngle(orb.getMeanAnomaly(), FastMath.PI),
                       revolutionNumberAtEpoch, (parameters.length == 7) ? parameters[6] / 10000.0 : 0.0);

    }

    /** Get the position/velocity target at sample points.
     * @return position/velocity target at sample points
     */
    protected double[] getTarget() {
        return target.clone();
    }

    /** Get the weights for residuals.
     * @return weights for residuals
     */
    protected double[] getWeight() {
        return weight.clone();
    }

    /** Get the residuals for a given position/velocity/B* parameters set.
     * @param parameters position/velocity/B* parameters set
     * @return residuals
     * @see #getRMS(double[])
     * @exception OrekitException if position/velocity cannot be computed at some date
     */
    protected double[] getResiduals(final double[] parameters) throws OrekitException {
        try {
            final double[] residuals = pvFunction.value(parameters);
            for (int i = 0; i < residuals.length; ++i) {
                residuals[i] = target[i] - residuals[i];
            }
            return residuals;
        } catch (OrekitExceptionWrapper oew) {
            throw oew.getException();
        }
    }

    /** Get the RMS for a given position/velocity/B* parameters set.
     * @param parameters position/velocity/B* parameters set
     * @return RMS
     * @see #getResiduals(double[])
     * @exception OrekitException if position/velocity cannot be computed at some date
     */
    protected double getRMS(final double[] parameters) throws OrekitException {

        final double[] residuals = getResiduals(parameters);
        double sum2 = 0;
        for (final double residual : residuals) {
            sum2 += residual * residual;
        }

        return FastMath.sqrt(sum2 / residuals.length);

    }

    /** Get the function computing position/velocity at sample points.
     * @return function computing position/velocity at sample points
     */
    protected DifferentiableMultivariateVectorFunction getPVFunction() {
        return pvFunction;
    }

    /** Set the states sample.
     * @param sample spacecraft states sample
     * @exception OrekitException if position/velocity cannot be extracted from sample
     */
    private void setSample(final List<SpacecraftState> sample) throws OrekitException {

        // velocity weight relative to position
        final PVCoordinates pv0 = sample.get(0).getPVCoordinates(teme);
        final double r2         = pv0.getPosition().getNormSq();
        final double v          = pv0.getVelocity().getNorm();
        final double vWeight    = v * r2 / MU;

        this.sample = sample;

        if (onlyPosition) {
            target = new double[sample.size() * 3];
            weight = new double[sample.size() * 3];
        } else {
            target = new double[sample.size() * 6];
            weight = new double[sample.size() * 6];
        }

        int k = 0;
        for (int i = 0; i < sample.size(); i++) {

            final PVCoordinates pv = sample.get(i).getPVCoordinates(FramesFactory.getTEME());

            // position
            target[k]   = pv.getPosition().getX();
            weight[k++] = 1;
            target[k]   = pv.getPosition().getY();
            weight[k++] = 1;
            target[k]   = pv.getPosition().getZ();
            weight[k++] = 1;

            // velocity
            if (!onlyPosition) {
                target[k]   = pv.getVelocity().getX();
                weight[k++] = vWeight;
                target[k]   = pv.getVelocity().getY();
                weight[k++] = vWeight;
                target[k]   = pv.getVelocity().getZ();
                weight[k++] = vWeight;
            }

        }

    }

    /** Get the desired position tolerance.
     * @return position tolerance
     */
    protected double getPositionTolerance() {
        return tolerance;
    }

    /** Internal class for computing position/velocity at sample points. */
    private class ResidualsFunction implements DifferentiableMultivariateVectorFunction {

        /** {@inheritDoc} */
        public double[] value(final double[] arg)
            throws IllegalArgumentException, OrekitExceptionWrapper {
            try {

                final TLEPropagator propagator = TLEPropagator.selectExtrapolator(getTLE(arg));
                final double[] eval = new double[target.length];
                int k = 0;
                for (int j = 0; j < sample.size(); j++) {
                    final PVCoordinates pv = propagator.getPVCoordinates(sample.get(j).getDate());
                    eval[k++] = pv.getPosition().getX();
                    eval[k++] = pv.getPosition().getY();
                    eval[k++] = pv.getPosition().getZ();
                    if (!onlyPosition) {
                        eval[k++] = pv.getVelocity().getX();
                        eval[k++] = pv.getVelocity().getY();
                        eval[k++] = pv.getVelocity().getZ();
                    }
                }

                return eval;

            } catch (OrekitException ex) {
                throw new OrekitExceptionWrapper(ex);
            }
        }

        /** {@inheritDoc} */
        public MultivariateMatrixFunction jacobian() {
            return new MultivariateMatrixFunction() {

                /** {@inheritDoc} */
                public double[][] value(final double[] arg)
                    throws IllegalArgumentException, OrekitExceptionWrapper {
                    final double[][] jacob = new double[target.length][arg.length];
                    final double[] eval = ResidualsFunction.this.value(arg);
                    final double[] arg1 = new double[arg.length];
                    double increment = 0;
                    for (int kappa = 0; kappa < arg.length; kappa++) {
                        System.arraycopy(arg, 0, arg1, 0, arg.length);
                        increment = FastMath.sqrt(Precision.EPSILON) * FastMath.abs(arg[kappa]);
                        if (increment <= Precision.SAFE_MIN) {
                            increment = FastMath.sqrt(Precision.EPSILON);
                        }
                        arg1[kappa] += increment;
                        final double[] eval1 = ResidualsFunction.this.value(arg1);
                        for (int t = 0; t < eval.length; t++) {
                            jacob[t][kappa] = (eval1[t] - eval[t]) / increment;
                        }
                    }

                    return jacob;
                }

            };
        }

    }

}
