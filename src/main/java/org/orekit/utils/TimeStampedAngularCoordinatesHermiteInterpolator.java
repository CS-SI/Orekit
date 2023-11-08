/* Copyright 2002-2023 CS GROUP
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
package org.orekit.utils;

import org.hipparchus.analysis.interpolation.HermiteInterpolator;
import org.hipparchus.geometry.euclidean.threed.Rotation;
import org.hipparchus.geometry.euclidean.threed.RotationConvention;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.FastMath;
import org.hipparchus.util.MathArrays;
import org.orekit.errors.OrekitInternalError;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.AbstractTimeInterpolator;

import java.util.List;

/**
 * Class using Hermite interpolator to interpolate time stamped angular coordinates.
 * <p>
 * As this implementation of interpolation is polynomial, it should be used only with small number of interpolation points
 * (about 10-20 points) in order to avoid <a href="http://en.wikipedia.org/wiki/Runge%27s_phenomenon">Runge's phenomenon</a>
 * and numerical problems (including NaN appearing).
 *
 * @author Vincent Cucchietti
 * @author Luc Maisonobe
 * @see HermiteInterpolator
 * @see TimeStampedAngularCoordinates
 */
public class TimeStampedAngularCoordinatesHermiteInterpolator
        extends AbstractTimeInterpolator<TimeStampedAngularCoordinates> {

    /** Filter for derivatives from the sample to use in interpolation. */
    private final AngularDerivativesFilter filter;

    /**
     * Constructor with :
     * <ul>
     *     <li>Default number of interpolation points of {@code DEFAULT_INTERPOLATION_POINTS}</li>
     *     <li>Default extrapolation threshold value ({@code DEFAULT_EXTRAPOLATION_THRESHOLD_SEC} s)</li>
     *     <li>Use of angular and first time derivative for attitude interpolation</li>
     * </ul>
     * As this implementation of interpolation is polynomial, it should be used only with small number of interpolation
     * points (about 10-20 points) in order to avoid <a href="http://en.wikipedia.org/wiki/Runge%27s_phenomenon">Runge's
     * phenomenon</a> and numerical problems (including NaN appearing).
     */
    public TimeStampedAngularCoordinatesHermiteInterpolator() {
        this(DEFAULT_INTERPOLATION_POINTS);
    }

    /**
     * /** Constructor with :
     * <ul>
     *     <li>Default extrapolation threshold value ({@code DEFAULT_EXTRAPOLATION_THRESHOLD_SEC} s)</li>
     *     <li>Use of angular and first time derivative for attitude interpolation</li>
     * </ul>
     * As this implementation of interpolation is polynomial, it should be used only with small number of interpolation
     * points (about 10-20 points) in order to avoid <a href="http://en.wikipedia.org/wiki/Runge%27s_phenomenon">Runge's
     * phenomenon</a> and numerical problems (including NaN appearing).
     *
     * @param interpolationPoints number of interpolation points
     */
    public TimeStampedAngularCoordinatesHermiteInterpolator(final int interpolationPoints) {
        this(interpolationPoints, AngularDerivativesFilter.USE_RR);
    }

    /**
     * Constructor with :
     * <ul>
     *     <li>Default extrapolation threshold value ({@code DEFAULT_EXTRAPOLATION_THRESHOLD_SEC} s)</li>
     * </ul>
     * As this implementation of interpolation is polynomial, it should be used only with small number of interpolation
     * points (about 10-20 points) in order to avoid <a href="http://en.wikipedia.org/wiki/Runge%27s_phenomenon">Runge's
     * phenomenon</a> and numerical problems (including NaN appearing).
     *
     * @param interpolationPoints number of interpolation points
     * @param filter filter for derivatives from the sample to use in interpolation
     */
    public TimeStampedAngularCoordinatesHermiteInterpolator(final int interpolationPoints,
                                                            final AngularDerivativesFilter filter) {
        this(interpolationPoints, DEFAULT_EXTRAPOLATION_THRESHOLD_SEC, filter);
    }

    /**
     * Constructor.
     * <p>
     * As this implementation of interpolation is polynomial, it should be used only with small number of interpolation
     * points (about 10-20 points) in order to avoid <a href="http://en.wikipedia.org/wiki/Runge%27s_phenomenon">Runge's
     * phenomenon</a> and numerical problems (including NaN appearing).
     *
     * @param interpolationPoints number of interpolation points
     * @param extrapolationThreshold extrapolation threshold beyond which the propagation will fail
     * @param filter filter for derivatives from the sample to use in interpolation
     */
    public TimeStampedAngularCoordinatesHermiteInterpolator(final int interpolationPoints,
                                                            final double extrapolationThreshold,
                                                            final AngularDerivativesFilter filter) {
        super(interpolationPoints, extrapolationThreshold);
        this.filter = filter;
    }

    /** Get filter for derivatives from the sample to use in interpolation.
     * @return filter for derivatives from the sample to use in interpolation
     */
    public AngularDerivativesFilter getFilter() {
        return filter;
    }

    /**
     * {@inheritDoc}
     * <p>
     * The interpolated instance is created by polynomial Hermite interpolation on Rodrigues vector ensuring rotation rate
     * remains the exact derivative of rotation.
     * <p>
     * This method is based on Sergei Tanygin's paper <a
     * href="http://www.agi.com/resources/white-papers/attitude-interpolation">Attitude Interpolation</a>, changing the norm
     * of the vector to match the modified Rodrigues vector as described in Malcolm D. Shuster's paper <a
     * href="http://www.ladispe.polito.it/corsi/Meccatronica/02JHCOR/2011-12/Slides/Shuster_Pub_1993h_J_Repsurv_scan.pdf">A
     * Survey of Attitude Representations</a>. This change avoids the singularity at π. There is still a singularity at 2π,
     * which is handled by slightly offsetting all rotations when this singularity is detected. Another change is that the
     * mean linear motion is first removed before interpolation and added back after interpolation. This allows to use
     * interpolation even when the sample covers much more than one turn and even when sample points are separated by more
     * than one turn.
     * </p>
     * <p>
     * Note that even if first and second time derivatives (rotation rates and acceleration) from sample can be ignored, the
     * interpolated instance always includes interpolated derivatives. This feature can be used explicitly to compute these
     * derivatives when it would be too complex to compute them from an analytical formula: just compute a few sample points
     * from the explicit formula and set the derivatives to zero in these sample points, then use interpolation to add
     * derivatives consistent with the rotations.
     */
    @Override
    protected TimeStampedAngularCoordinates interpolate(final InterpolationData interpolationData) {

        // Get date
        final AbsoluteDate date = interpolationData.getInterpolationDate();

        // Get sample
        final List<TimeStampedAngularCoordinates> sample = interpolationData.getNeighborList();

        // set up safety elements for 2π singularity avoidance
        final double epsilon   = 2 * FastMath.PI / sample.size();
        final double threshold = FastMath.min(-(1.0 - 1.0e-4), -FastMath.cos(epsilon / 4));

        // set up a linear model canceling mean rotation rate
        final Vector3D meanRate;
        Vector3D       sum = Vector3D.ZERO;
        if (filter != AngularDerivativesFilter.USE_R) {
            for (final TimeStampedAngularCoordinates datedAC : sample) {
                sum = sum.add(datedAC.getRotationRate());
            }
            meanRate = new Vector3D(1.0 / sample.size(), sum);
        }
        else {
            TimeStampedAngularCoordinates previous = null;
            for (final TimeStampedAngularCoordinates datedAC : sample) {
                if (previous != null) {
                    sum = sum.add(TimeStampedAngularCoordinates.estimateRate(previous.getRotation(), datedAC.getRotation(),
                                                                             datedAC.getDate()
                                                                                    .durationFrom(previous.getDate())));
                }
                previous = datedAC;
            }
            meanRate = new Vector3D(1.0 / (sample.size() - 1), sum);
        }
        TimeStampedAngularCoordinates offset =
                new TimeStampedAngularCoordinates(date, Rotation.IDENTITY, meanRate, Vector3D.ZERO);

        boolean restart = true;
        for (int i = 0; restart && i < sample.size() + 2; ++i) {

            // offset adaptation parameters
            restart = false;

            // set up an interpolator taking derivatives into account
            final HermiteInterpolator interpolator = new HermiteInterpolator();

            // add sample points
            double   sign     = 1.0;
            Rotation previous = Rotation.IDENTITY;

            for (final TimeStampedAngularCoordinates ac : sample) {

                // remove linear offset from the current coordinates
                final double                        dt    = ac.getDate().durationFrom(date);
                final TimeStampedAngularCoordinates fixed = ac.subtractOffset(offset.shiftedBy(dt));

                // make sure all interpolated points will be on the same branch
                final double dot = MathArrays.linearCombination(fixed.getRotation().getQ0(), previous.getQ0(),
                                                                fixed.getRotation().getQ1(), previous.getQ1(),
                                                                fixed.getRotation().getQ2(), previous.getQ2(),
                                                                fixed.getRotation().getQ3(), previous.getQ3());
                sign     = FastMath.copySign(1.0, dot * sign);
                previous = fixed.getRotation();

                // check modified Rodrigues vector singularity
                if (fixed.getRotation().getQ0() * sign < threshold) {
                    // the sample point is close to a modified Rodrigues vector singularity
                    // we need to change the linear offset model to avoid this
                    restart = true;
                    break;
                }

                final double[][] rodrigues = fixed.getModifiedRodrigues(sign);
                switch (filter) {
                    case USE_RRA:
                        // populate sample with rotation, rotation rate and acceleration data
                        interpolator.addSamplePoint(dt, rodrigues[0], rodrigues[1], rodrigues[2]);
                        break;
                    case USE_RR:
                        // populate sample with rotation and rotation rate data
                        interpolator.addSamplePoint(dt, rodrigues[0], rodrigues[1]);
                        break;
                    case USE_R:
                        // populate sample with rotation data only
                        interpolator.addSamplePoint(dt, rodrigues[0]);
                        break;
                    default:
                        // this should never happen
                        throw new OrekitInternalError(null);
                }
            }

            if (restart) {
                // interpolation failed, some intermediate rotation was too close to 2π
                // we need to offset all rotations to avoid the singularity
                offset = offset.addOffset(new AngularCoordinates(new Rotation(Vector3D.PLUS_I,
                                                                              epsilon,
                                                                              RotationConvention.VECTOR_OPERATOR),
                                                                 Vector3D.ZERO, Vector3D.ZERO));
            }
            else {
                // interpolation succeeded with the current offset
                final double[][]         p  = interpolator.derivatives(0.0, 2);
                final AngularCoordinates ac = AngularCoordinates.createFromModifiedRodrigues(p);
                return new TimeStampedAngularCoordinates(offset.getDate(),
                                                         ac.getRotation(),
                                                         ac.getRotationRate(),
                                                         ac.getRotationAcceleration()).addOffset(offset);
            }

        }

        // this should never happen
        throw new OrekitInternalError(null);
    }
}
