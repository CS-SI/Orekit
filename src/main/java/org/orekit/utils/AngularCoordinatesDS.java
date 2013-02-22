/* Copyright 2002-2013 CS Systèmes d'Information
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
package org.orekit.utils;

import java.io.Serializable;
import java.util.Collection;

import org.apache.commons.math3.analysis.differentiation.DerivativeStructure;
import org.apache.commons.math3.analysis.interpolation.HermiteInterpolator;
import org.apache.commons.math3.util.FastMath;
import org.apache.commons.math3.util.Pair;
import org.orekit.errors.OrekitException;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeShiftable;

/** Simple container for RotationDS/RotationDS rate pairs, using {@link DerivativeStructure}.
 * <p>
 * The state can be slightly shifted to close dates. This shift is based on
 * a simple linear model. It is <em>not</em> intended as a replacement for
 * proper attitude propagation but should be sufficient for either small
 * time shifts or coarse accuracy.
 * </p>
 * <p>
 * This class is the angular counterpart to {@link PVCoordinatesDS}.
 * </p>
 * <p>Instances of this class are guaranteed to be immutable.</p>
 * @author Luc Maisonobe
 * @since 6.0
 * @see AngularCoordinates
 */
public class AngularCoordinatesDS implements TimeShiftable<AngularCoordinatesDS>, Serializable {

    /** Serializable UID. */
    private static final long serialVersionUID = 20130222L;

    /** RotationDS. */
    private final RotationDS rotation;

    /** RotationDS rate. */
    private final Vector3DDS rotationRate;

    /** Builds a RotationDS/RotationDS rate pair.
     * @param rotation RotationDS
     * @param rotationRate RotationDS rate (rad/s)
     */
    public AngularCoordinatesDS(final RotationDS rotation, final Vector3DDS rotationRate) {
        this.rotation     = rotation;
        this.rotationRate = rotationRate;
    }

    /** Estimate RotationDS rate between two orientations.
     * <p>Estimation is based on a simple fixed rate RotationDS
     * during the time interval between the two orientations.</p>
     * @param start start orientation
     * @param end end orientation
     * @param dt time elapsed between the dates of the two orientations
     * @return RotationDS rate allowing to go from start to end orientations
     */
    public static Vector3DDS estimateRate(final RotationDS start, final RotationDS end, final double dt) {
        final RotationDS evolution = start.applyTo(end.revert());
        return new Vector3DDS(evolution.getAngle().divide(dt), evolution.getAxis());
    }

    /** Revert a RotationDS/RotationDS rate pair.
     * Build a pair which reverse the effect of another pair.
     * @return a new pair whose effect is the reverse of the effect
     * of the instance
     */
    public AngularCoordinatesDS revert() {
        return new AngularCoordinatesDS(rotation.revert(), rotation.applyInverseTo(rotationRate.negate()));
    }

    /** Get a time-shifted state.
     * <p>
     * The state can be slightly shifted to close dates. This shift is based on
     * a simple linear model. It is <em>not</em> intended as a replacement for
     * proper attitude propagation but should be sufficient for either small
     * time shifts or coarse accuracy.
     * </p>
     * @param dt time shift in seconds
     * @return a new state, shifted with respect to the instance (which is immutable)
     */
    public AngularCoordinatesDS shiftedBy(final double dt) {
        final DerivativeStructure rate = rotationRate.getNorm();
        if (rate.getValue() == 0.0) {
            // special case for fixed RotationDSs
            return this;
        }

        // BEWARE: there is really a minus sign here, because if
        // the target frame rotates in one direction, the vectors in the origin
        // frame seem to rotate in the opposite direction
        final RotationDS evolution = new RotationDS(rotationRate, rate.negate().multiply(dt));

        return new AngularCoordinatesDS(evolution.applyTo(rotation), rotationRate);

    }

    /** Get the RotationDS.
     * @return the RotationDS.
     */
    public RotationDS getRotation() {
        return rotation;
    }

    /** Get the RotationDS rate.
     * @return the RotationDS rate vector (rad/s).
     */
    public Vector3DDS getRotationRate() {
        return rotationRate;
    }

    /** Add an offset from the instance.
     * <p>
     * We consider here that the offset RotationDS is applied first and the
     * instance is applied afterward. Note that angular coordinates do <em>not</em>
     * commute under this operation, i.e. {@code a.addOffset(b)} and {@code
     * b.addOffset(a)} lead to <em>different</em> results in most cases.
     * </p>
     * <p>
     * The two methods {@link #addOffset(AngularCoordinatesDS) addOffset} and
     * {@link #subtractOffset(AngularCoordinatesDS) subtractOffset} are designed
     * so that round trip applications are possible. This means that both {@code
     * ac1.subtractOffset(ac2).addOffset(ac2)} and {@code
     * ac1.addOffset(ac2).subtractOffset(ac2)} return angular coordinates equal to ac1.
     * </p>
     * @param offset offset to subtract
     * @return new instance, with offset subtracted
     * @see #subtractOffset(AngularCoordinatesDS)
     */
    public AngularCoordinatesDS addOffset(final AngularCoordinatesDS offset) {
        return new AngularCoordinatesDS(rotation.applyTo(offset.rotation),
                                        rotationRate.add(rotation.applyTo(offset.rotationRate)));
    }

    /** Subtract an offset from the instance.
     * <p>
     * We consider here that the offset Rotation is applied first and the
     * instance is applied afterward. Note that angular coordinates do <em>not</em>
     * commute under this operation, i.e. {@code a.subtractOffset(b)} and {@code
     * b.subtractOffset(a)} lead to <em>different</em> results in most cases.
     * </p>
     * <p>
     * The two methods {@link #addOffset(AngularCoordinatesDS) addOffset} and
     * {@link #subtractOffset(AngularCoordinatesDS) subtractOffset} are designed
     * so that round trip applications are possible. This means that both {@code
     * ac1.subtractOffset(ac2).addOffset(ac2)} and {@code
     * ac1.addOffset(ac2).subtractOffset(ac2)} return angular coordinates equal to ac1.
     * </p>
     * @param offset offset to subtract
     * @return new instance, with offset subtracted
     * @see #addOffset(AngularCoordinatesDS)
     */
    public AngularCoordinatesDS subtractOffset(final AngularCoordinatesDS offset) {
        return addOffset(offset.revert());
    }

    /** Convert to a constant angular coordinates without derivatives.
     * @return a constant angular coordinates
     */
    public AngularCoordinates toAngularCoordinates() {
        return new AngularCoordinates(rotation.toRotation(), rotationRate.toVector3D());
    }

    /** Interpolate angular coordinates.
     * <p>
     * The interpolated instance is created by polynomial Hermite interpolation
     * on Rodrigues vector ensuring RotationDS rate remains the exact derivative of RotationDS.
     * </p>
     * <p>
     * This method is based on Sergei Tanygin's paper <a
     * href="http://www.agi.com/downloads/resources/white-papers/Attitude-interpolation.pdf">Attitude
     * Interpolation</a>, changing the norm of the vector to match the modified Rodrigues
     * vector as described in Malcolm D. Shuster's paper <a
     * href="http://www.ladispe.polito.it/corsi/Meccatronica/02JHCOR/2011-12/Slides/Shuster_Pub_1993h_J_Repsurv_scan.pdf">A
     * Survey of Attitude Representations</a>. This change avoids the singularity at &pi;.
     * There is still a singularity at 2&pi;, which is handled by slightly offsetting all RotationDSs
     * when this singularity is detected.
     * </p>
     * <p>
     * Note that even if first time derivatives (RotationDS rates)
     * from sample can be ignored, the interpolated instance always includes
     * interpolated derivatives. This feature can be used explicitly to
     * compute these derivatives when it would be too complex to compute them
     * from an analytical formula: just compute a few sample points from the
     * explicit formula and set the derivatives to zero in these sample points,
     * then use interpolation to add derivatives consistent with the RotationDSs.
     * </p>
     * @param date interpolation date
     * @param useRotationDSRates if true, use sample points RotationDS rates,
     * otherwise ignore them and use only RotationDSs
     * @param sample sample points on which interpolation should be done
     * @return a new position-velocity, interpolated at specified date
     */
    public static AngularCoordinatesDS interpolate(final AbsoluteDate date, final boolean useRotationDSRates,
                                                   final Collection<Pair<AbsoluteDate, AngularCoordinatesDS>> sample) {

        // get derivative structure properties
        final DerivativeStructure prototype = sample.iterator().next().getValue().getRotation().getQ0();
        final int parameters = prototype.getFreeParameters();
        final int order      = prototype.getOrder();
        final DerivativeStructure zero = prototype.getField().getZero();
        final DerivativeStructure one  = prototype.getField().getOne();

        // set up safety elements for 2PI singularity avoidance
        final double epsilon   = 2 * FastMath.PI / sample.size();
        final double threshold = FastMath.min(-(1.0 - 1.0e-4), -FastMath.cos(epsilon / 4));

        // set up a linear offset model canceling mean RotationDS rate
        final Vector3DDS meanRate;
        if (useRotationDSRates) {
            Vector3DDS sum = new Vector3DDS(zero, zero, zero);
            for (final Pair<AbsoluteDate, AngularCoordinatesDS> datedAC : sample) {
                sum = sum.add(datedAC.getValue().getRotationRate());
            }
            meanRate = new Vector3DDS(1.0 / sample.size(), sum);
        } else {
            Vector3DDS sum = new Vector3DDS(zero, zero, zero);
            Pair<AbsoluteDate, AngularCoordinatesDS> previous = null;
            for (final Pair<AbsoluteDate, AngularCoordinatesDS> datedAC : sample) {
                if (previous != null) {
                    sum = sum.add(estimateRate(previous.getValue().getRotation(),
                                                         datedAC.getValue().getRotation(),
                                                         datedAC.getKey().durationFrom(previous.getKey().getDate())));
                }
                previous = datedAC;
            }
            meanRate = new Vector3DDS(1.0 / (sample.size() - 1), sum);
        }
        AngularCoordinatesDS offset =
                new AngularCoordinatesDS(new RotationDS(one, zero, zero, zero, false), meanRate);

        boolean restart = true;
        for (int i = 0; restart && i < sample.size() + 2; ++i) {

            // offset adaptation parameters
            restart = false;

            // set up an interpolator taking derivatives into account
            final HermiteInterpolator interpolator = new HermiteInterpolator();

            // add sample points
            if (useRotationDSRates) {
                // populate sample with RotationDS and RotationDS rate data
                for (final Pair<AbsoluteDate, AngularCoordinatesDS> datedAC : sample) {
                    final DerivativeStructure[] rodrigues = getModifiedRodrigues(datedAC.getKey(), datedAC.getValue(),
                                                                                 date, offset, threshold);
                    if (rodrigues == null) {
                        // the sample point is close to a modified Rodrigues vector singularity
                        // we need to change the linear offset model to avoid this
                        restart = true;
                        break;
                    }
                    interpolator.addSamplePoint(datedAC.getKey().getDate().durationFrom(date),
                                                unfold(rodrigues[0], rodrigues[1], rodrigues[2]),
                                                unfold(rodrigues[3], rodrigues[4], rodrigues[5]));
                }
            } else {
                // populate sample with RotationDS data only, ignoring RotationDS rate
                for (final Pair<AbsoluteDate, AngularCoordinatesDS> datedAC : sample) {
                    final DerivativeStructure[] rodrigues = getModifiedRodrigues(datedAC.getKey(), datedAC.getValue(),
                                                                                 date, offset, threshold);
                    if (rodrigues == null) {
                        // the sample point is close to a modified Rodrigues vector singularity
                        // we need to change the linear offset model to avoid this
                        restart = true;
                        break;
                    }
                    interpolator.addSamplePoint(datedAC.getKey().getDate().durationFrom(date),
                                                unfold(rodrigues[0], rodrigues[1], rodrigues[2]));
                }
            }

            if (restart) {
                // interpolation failed, some intermediate RotationDS was too close to 2PI
                // we need to offset all RotationDSs to avoid the singularity
                offset = offset.addOffset(new AngularCoordinatesDS(new RotationDS(new Vector3DDS(one, zero, zero),
                                                                                  new DerivativeStructure(parameters,
                                                                                                          order,
                                                                                                          epsilon)),
                                                                   new Vector3DDS(one, zero, zero)));
            } else {
                // interpolation succeeded with the current offset
                final DerivativeStructure[] p = interpolator.value(new DerivativeStructure(1, 1, 0, 0.0));
                return createFromModifiedRodrigues(fold(parameters, order, p), offset);
            }

        }

        // this should never happen
        throw OrekitException.createInternalError(null);

    }

    /** Unfold coordinates derivatives arrays.
     * @param v vector to unfold
     * @return all derivatives for all coordinates
     */
    private static double[] unfold(final DerivativeStructure vx,
                                   final DerivativeStructure vy,
                                   final DerivativeStructure vz) {
        final double[] ax = vx.getAllDerivatives();
        final double[] ay = vy.getAllDerivatives();
        final double[] az = vz.getAllDerivatives();
        final int size = ax.length;
        final double[] flat = new double[3 * size];
        System.arraycopy(ax, 0, flat, 0, size);
        System.arraycopy(ay, 0, flat, size, size);
        System.arraycopy(az, 0, flat, 2 * size, size);
        return flat;
    }

    /** Fold coordinates derivatives arrays.
     * @param parameters number of free parameters
     * @param order derivation order
     * @param a array to fold
     * @return vector for specified time-derivative
     */
    private static DerivativeStructure[] fold(final int parameters, final int order,
                                              final DerivativeStructure[] a) {
        final int size = a.length / 3;
        final double[] vx0 = new double[size];
        final double[] vy0 = new double[size];
        final double[] vz0 = new double[size];
        final double[] vx1 = new double[size];
        final double[] vy1 = new double[size];
        final double[] vz1 = new double[size];
        for (int i = 0; i < size; ++i) {
            vx0[i] = a[i].getValue();
            vy0[i] = a[i + size].getValue();
            vz0[i] = a[i + 2 * size].getValue();
            vx1[i] = a[i].getPartialDerivative(1);
            vy1[i] = a[i + size].getPartialDerivative(1);
            vz1[i] = a[i + 2 * size].getPartialDerivative(1);
        }
        return new DerivativeStructure[] {
            new DerivativeStructure(parameters, order, vx0),
            new DerivativeStructure(parameters, order, vy0),
            new DerivativeStructure(parameters, order, vz0),
            new DerivativeStructure(parameters, order, vx1),
            new DerivativeStructure(parameters, order, vy1),
            new DerivativeStructure(parameters, order, vz1)
        };
    }

    /** Convert rotation and rate to modified Rodrigues vector and derivative.
     * <p>
     * The modified Rodrigues vector is tan(&theta;/4) u where &theta; and u are the
     * rotation angle and axis respectively.
     * </p>
     * @param date date of the angular coordinates
     * @param ac coordinates to convert
     * @param offsetDate date of the linear offset model to remove
     * @param offset linear offset model to remove
     * @param threshold threshold for rotations too close to 2&pi;
     * @return modified Rodrigues vector and derivative, or null if rotation is too close to 2&pi;
     */
    private static DerivativeStructure[] getModifiedRodrigues(final AbsoluteDate date, final AngularCoordinatesDS ac,
                                                              final AbsoluteDate offsetDate, final AngularCoordinatesDS offset,
                                                              final double threshold) {

        // remove linear offset from the current coordinates
        final double dt = date.durationFrom(offsetDate);
        final AngularCoordinatesDS fixed = ac.subtractOffset(offset.shiftedBy(dt));

        // check modified Rodrigues vector singularity
        DerivativeStructure q0 = fixed.getRotation().getQ0();
        DerivativeStructure q1 = fixed.getRotation().getQ1();
        DerivativeStructure q2 = fixed.getRotation().getQ2();
        DerivativeStructure q3 = fixed.getRotation().getQ3();
        if (q0.getValue() < threshold && FastMath.abs(dt) * fixed.getRotationRate().getNorm().getValue() > 1.0e-3) {
            // this is an intermediate point that happens to be 2PI away from reference
            // we need to change the linear offset model to avoid this point
            return null;
        }

        // make sure all interpolated points will be on the same branch
        if (q0.getValue() < 0) {
            q0 = q0.negate();
            q1 = q1.negate();
            q2 = q2.negate();
            q3 = q3.negate();
        }

        final DerivativeStructure x  = fixed.getRotationRate().getX();
        final DerivativeStructure y  = fixed.getRotationRate().getY();
        final DerivativeStructure z  = fixed.getRotationRate().getZ();

        // derivatives of the quaternion
        final DerivativeStructure q0Dot = Vector3DDS.linearCombination(q1, x, q2, y,  q3, z).multiply(-0.5);
        final DerivativeStructure q1Dot = Vector3DDS.linearCombination(q0, x, q2, z, q3.negate(), y).multiply(0.5);
        final DerivativeStructure q2Dot = Vector3DDS.linearCombination(q0, y, q3, x, q1.negate(), z).multiply(0.5);
        final DerivativeStructure q3Dot = Vector3DDS.linearCombination(q0, z, q1, y, q2.negate(), x).multiply(0.5);

        final DerivativeStructure inv = q0.add(1).reciprocal();
        return new DerivativeStructure[] {
            inv.multiply(q1),
            inv.multiply(q2),
            inv.multiply(q3),
            inv.multiply(q1Dot.subtract(inv.multiply(q1).multiply(q0Dot))),
            inv.multiply(q2Dot.subtract(inv.multiply(q2).multiply(q0Dot))),
            inv.multiply(q3Dot.subtract(inv.multiply(q3).multiply(q0Dot)))
        };

    }

    /** Convert a modified Rodrigues vector and derivative to angular coordinates.
     * @param r modified Rodrigues vector (with first derivatives)
     * @param offset linear offset model to add (its date must be consistent with the modified Rodrigues vector)
     * @return angular coordinates
     */
    private static AngularCoordinatesDS createFromModifiedRodrigues(final DerivativeStructure[] r,
                                                                    final AngularCoordinatesDS offset) {

        // rotation
        final DerivativeStructure rSquared = r[0].multiply(r[0]).add(r[1].multiply(r[1])).add(r[2].multiply(r[2]));
        final DerivativeStructure inv      = rSquared.add(1).reciprocal();
        final DerivativeStructure ratio    = inv.multiply(rSquared.subtract(1).negate());
        final RotationDS rotation          = new RotationDS(ratio,
                                                            inv.multiply(2).multiply(r[0]),
                                                            inv.multiply(2).multiply(r[1]),
                                                            inv.multiply(2).multiply(r[2]),
                                                            false);

        // rotation rate
        final Vector3DDS p    = new Vector3DDS(r[0], r[1], r[2]);
        final Vector3DDS pDot = new Vector3DDS(r[3], r[4], r[5]);
        final Vector3DDS rate = new Vector3DDS(inv.multiply(ratio).multiply(4), pDot,
                                               inv.multiply(inv).multiply(-8), p.crossProduct(pDot),
                                               inv.multiply(inv).multiply(8).multiply(p.dotProduct(pDot)), p);

        return new AngularCoordinatesDS(rotation, rate).addOffset(offset);

    }

}
