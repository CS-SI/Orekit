/* Copyright 2002-2020 CS Group
 * Licensed to CS Group (CS) under one or more
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
package org.orekit.gnss.attitude;

import org.hipparchus.analysis.UnivariateFunction;
import org.hipparchus.analysis.differentiation.DSFactory;
import org.hipparchus.analysis.differentiation.DerivativeStructure;
import org.hipparchus.analysis.solvers.BracketingNthOrderBrentSolver;
import org.hipparchus.analysis.solvers.UnivariateSolverUtils;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.FastMath;
import org.hipparchus.util.SinCos;
import org.orekit.frames.Frame;
import org.orekit.frames.LOFType;
import org.orekit.frames.Transform;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.time.TimeStamped;
import org.orekit.utils.ExtendedPVCoordinatesProvider;
import org.orekit.utils.FieldPVCoordinates;
import org.orekit.utils.PVCoordinates;
import org.orekit.utils.PVCoordinatesProvider;
import org.orekit.utils.TimeStampedAngularCoordinates;
import org.orekit.utils.TimeStampedPVCoordinates;

/**
 * Boilerplate computations for GNSS attitude.
 *
 * <p>
 * This class is intended to hold throw-away data pertaining to <em>one</em> call
 * to {@link GNSSAttitudeProvider#getAttitude(org.orekit.utils.PVCoordinatesProvider,
 * org.orekit.time.AbsoluteDate, org.orekit.frames.Frame) getAttitude}.
 * </p>
 *
 * @author Luc Maisonobe
 * @since 9.2
 */
class GNSSAttitudeContext implements TimeStamped {

    /** Derivation order. */
    private static final int ORDER = 2;

    /** Time derivation factory. */
    private static final DSFactory FACTORY = new DSFactory(1, ORDER);

    /** Constant Y axis. */
    private static final PVCoordinates PLUS_Y_PV =
            new PVCoordinates(Vector3D.PLUS_J, Vector3D.ZERO, Vector3D.ZERO);

    /** Constant Z axis. */
    private static final PVCoordinates MINUS_Z_PV =
            new PVCoordinates(Vector3D.MINUS_K, Vector3D.ZERO, Vector3D.ZERO);

    /** Limit value below which we shoud use replace beta by betaIni. */
    private static final double BETA_SIGN_CHANGE_PROTECTION = FastMath.toRadians(0.07);

    /** Current date. */
    private final AbsoluteDate date;

    /** Current date. */
    private final FieldAbsoluteDate<DerivativeStructure> dateDS;

    /** Provider for Sun position. */
    private final ExtendedPVCoordinatesProvider sun;

    /** Provider for spacecraft position. */
    private final PVCoordinatesProvider pvProv;

    /** Inertial frame where velocity are computed. */
    private final Frame inertialFrame;

    /** Cosine of the angle between spacecraft and Sun direction. */
    private final DerivativeStructure svbCos;

    /** Morning/Evening half orbit indicator. */
    private final boolean morning;

    /** Relative orbit angle to turn center. */
    private final DerivativeStructure delta;

    /** Spacecraft angular velocity. */
    private double muRate;

    /** Limit cosine for the midnight turn. */
    private double cNight;

    /** Limit cosine for the noon turn. */
    private double cNoon;

    /** Turn time data. */
    private TurnSpan turnSpan;

    /** Simple constructor.
     * @param date current date
     * @param sun provider for Sun position
     * @param pvProv provider for spacecraft position
     * @param inertialFrame inertial frame where velocity are computed
     * @param turnSpan turn time data, if a turn has already been identified in the date neighborhood,
     * null otherwise
     */
    GNSSAttitudeContext(final AbsoluteDate date,
                        final ExtendedPVCoordinatesProvider sun, final PVCoordinatesProvider pvProv,
                        final Frame inertialFrame, final TurnSpan turnSpan) {

        this.date          = date;
        this.dateDS        = addDerivatives(date);
        this.sun           = sun;
        this.pvProv        = pvProv;
        this.inertialFrame = inertialFrame;
        final FieldPVCoordinates<DerivativeStructure> sunPVDS = sun.getPVCoordinates(dateDS, inertialFrame);

        final TimeStampedPVCoordinates svPV = pvProv.getPVCoordinates(date, inertialFrame);
        final FieldPVCoordinates<DerivativeStructure> svPVDS  = svPV.toDerivativeStructurePV(FACTORY.getCompiler().getOrder());
        this.svbCos  = FieldVector3D.dotProduct(sunPVDS.getPosition(), svPVDS.getPosition()).
                       divide(sunPVDS.getPosition().getNorm().multiply(svPVDS.getPosition().getNorm()));
        this.morning = Vector3D.dotProduct(svPV.getVelocity(), sunPVDS.getPosition().toVector3D()) >= 0.0;

        this.muRate = svPV.getAngularVelocity().getNorm();

        this.turnSpan = turnSpan;

        final DerivativeStructure absDelta;
        if (svbCos.getValue() <= 0) {
            // night side
            absDelta = inOrbitPlaneAbsoluteAngle(svbCos.acos().negate().add(FastMath.PI));
        } else {
            // Sun side
            absDelta = inOrbitPlaneAbsoluteAngle(svbCos.acos());
        }
        delta = FastMath.copySign(absDelta, -absDelta.getPartialDerivative(1));

    }

    /** Convert a date, removing derivatives.
     * @param d date to convert
     * @return date without derivatives
     */
    private AbsoluteDate removeDerivatives(final FieldAbsoluteDate<DerivativeStructure> d) {
        return d.toAbsoluteDate();
    }

    /** Convert a date, adding derivatives.
     * @param d date to convert
     * @return date without derivatives
     */
    private FieldAbsoluteDate<DerivativeStructure> addDerivatives(final AbsoluteDate d) {
        return new FieldAbsoluteDate<>(FACTORY.getDerivativeField(), d).
               shiftedBy(FACTORY.variable(0, 0.0));
    }

    /** Compute nominal yaw steering.
     * @param d computation date
     * @return nominal yaw steering
     */
    public TimeStampedAngularCoordinates nominalYaw(final AbsoluteDate d) {
        final PVCoordinates svPV = pvProv.getPVCoordinates(d, inertialFrame);
        return new TimeStampedAngularCoordinates(d,
                                                 svPV.normalize(),
                                                 PVCoordinates.crossProduct(sun.getPVCoordinates(d, inertialFrame), svPV).normalize(),
                                                 MINUS_Z_PV,
                                                 PLUS_Y_PV,
                                                 1.0e-9);
    }

    /** Compute Sun elevation.
     * @param d computation date
     * @return Sun elevation
     */
    public double beta(final AbsoluteDate d) {
        final TimeStampedPVCoordinates svPV = pvProv.getPVCoordinates(d, inertialFrame);
        return 0.5 * FastMath.PI - Vector3D.angle(sun.getPVCoordinates(d, inertialFrame).getPosition(), svPV.getMomentum());
    }

    /** Compute Sun elevation.
     * @param d computation date
     * @return Sun elevation
     */
    private DerivativeStructure betaDS(final FieldAbsoluteDate<DerivativeStructure> d) {
        final TimeStampedPVCoordinates svPV = pvProv.getPVCoordinates(removeDerivatives(d), inertialFrame);
        final FieldPVCoordinates<DerivativeStructure> svPVDS  = svPV.toDerivativeStructurePV(FACTORY.getCompiler().getOrder());
        return FieldVector3D.angle(sun.getPVCoordinates(d, inertialFrame).getPosition(), svPVDS.getMomentum()).
               negate().
               add(0.5 * FastMath.PI);
    }

    /** Compute Sun elevation.
     * @return Sun elevation
     */
    public DerivativeStructure betaDS() {
        return betaDS(dateDS);
    }

    /** {@inheritDoc} */
    @Override
    public AbsoluteDate getDate() {
        return date;
    }

    /** Get the turn span.
     * @return turn span, may be null if context is outside of turn
     */
    public TurnSpan getTurnSpan() {
        return turnSpan;
    }

    /** Get the cosine of the angle between spacecraft and Sun direction.
     * @return cosine of the angle between spacecraft and Sun direction.
     */
    public double getSVBcos() {
        return svbCos.getValue();
    }

    /** Get a Sun elevation angle that does not change sign within the turn.
     * <p>
     * This method either returns the current beta or replaces it with the
     * value at turn start, so the sign remains constant throughout the
     * turn. As of 9.2, it is used for GPS, Glonass and Galileo.
     * </p>
     * @return secured Sun elevation angle
     * @see #beta(AbsoluteDate)
     * @see #betaDS(FieldAbsoluteDate)
     */
    public double getSecuredBeta() {
        final double beta = beta(getDate());
        return FastMath.abs(beta) < BETA_SIGN_CHANGE_PROTECTION ?
               beta(turnSpan.getTurnStartDate()) :
               beta;
    }

    /** Check if a linear yaw model is still active or if we already reached target yaw.
     * @param linearPhi value of the linear yaw model
     * @param phiDot slope of the linear yaw model
     * @return true if linear model is still active
     */
    public boolean linearModelStillActive(final double linearPhi, final double phiDot) {
        final double dt0 = turnSpan.getTurnEndDate().durationFrom(date);
        final UnivariateFunction yawReached = dt -> {
            final AbsoluteDate  t       = date.shiftedBy(dt);
            final Vector3D      pSun    = sun.getPVCoordinates(t, inertialFrame).getPosition();
            final PVCoordinates pv      = pvProv.getPVCoordinates(t, inertialFrame);
            final Vector3D      pSat    = pv.getPosition();
            final Vector3D      targetX = Vector3D.crossProduct(pSat, Vector3D.crossProduct(pSun, pSat)).normalize();

            final double        phi         = linearPhi + dt * phiDot;
            final SinCos        sc          = FastMath.sinCos(phi);
            final Vector3D      pU          = pv.getPosition().normalize();
            final Vector3D      mU          = pv.getMomentum().normalize();
            final Vector3D      omega       = new Vector3D(-phiDot, pU);
            final Vector3D      currentX    = new Vector3D(-sc.sin(), mU, -sc.cos(), Vector3D.crossProduct(pU, mU));
            final Vector3D      currentXDot = Vector3D.crossProduct(omega, currentX);

            return Vector3D.dotProduct(targetX, currentXDot);
        };
        final double fullTurn = 2 * FastMath.PI / FastMath.abs(phiDot);
        final double dtMin    = FastMath.min(turnSpan.getTurnStartDate().durationFrom(date), dt0 - 60.0);
        final double dtMax    = FastMath.max(dtMin + fullTurn, dt0 + 60.0);
        double[] bracket = UnivariateSolverUtils.bracket(yawReached, dt0,
                                                         dtMin, dtMax, fullTurn / 100, 1.0, 100);
        if (yawReached.value(bracket[0]) <= 0.0) {
            // we have bracketed the wrong crossing
            bracket = UnivariateSolverUtils.bracket(yawReached, 0.5 * (bracket[0] + bracket[1] + fullTurn),
                                                    bracket[1], bracket[1] + fullTurn, fullTurn / 100, 1.0, 100);
        }
        final double dt = new BracketingNthOrderBrentSolver(1.0e-3, 5).
                          solve(100, yawReached, bracket[0], bracket[1]);
        turnSpan.updateEnd(date.shiftedBy(dt), date);

        return dt > 0.0;

    }

    /** Set up the midnight/noon turn region.
     * @param cosNight limit cosine for the midnight turn
     * @param cosNoon limit cosine for the noon turn
     * @return true if spacecraft is in the midnight/noon turn region
     */
    public boolean setUpTurnRegion(final double cosNight, final double cosNoon) {
        this.cNight = cosNight;
        this.cNoon  = cosNoon;

        if (svbCos.getValue() < cNight || svbCos.getValue() > cNoon) {
            // we are within turn triggering zone
            return true;
        } else {
            // we are outside of turn triggering zone,
            // but we may still be trying to recover nominal attitude at the end of a turn
            return inTurnTimeRange();
        }

    }

    /** Get the relative orbit angle to turn center.
     * @return relative orbit angle to turn center
     */
    public DerivativeStructure getDeltaDS() {
        return delta;
    }

    /** Get the orbit angle since solar midnight.
     * @return orbit angle since solar midnight
     */
    public double getOrbitAngleSinceMidnight() {
        final double absAngle = inOrbitPlaneAbsoluteAngle(FastMath.PI - FastMath.acos(svbCos.getValue()));
        return morning ? absAngle : -absAngle;
    }

    /** Check if spacecraft is in the half orbit closest to Sun.
     * @return true if spacecraft is in the half orbit closest to Sun
     */
    public boolean inSunSide() {
        return svbCos.getValue() > 0;
    }

    /** Get yaw at turn start.
     * @param sunBeta Sun elevation above orbital plane
     * (it <em>may</em> be different from {@link #getBeta()} in
     * some special cases)
     * @return yaw at turn start
     */
    public double getYawStart(final double sunBeta) {
        final double halfSpan = 0.5 * turnSpan.getTurnDuration() * muRate;
        return computePhi(sunBeta, FastMath.copySign(halfSpan, svbCos.getValue()));
    }

    /** Get yaw at turn end.
     * @param sunBeta Sun elevation above orbital plane
     * (it <em>may</em> be different from {@link #getBeta()} in
     * some special cases)
     * @return yaw at turn end
     */
    public double getYawEnd(final double sunBeta) {
        final double halfSpan = 0.5 * turnSpan.getTurnDuration() * muRate;
        return computePhi(sunBeta, FastMath.copySign(halfSpan, -svbCos.getValue()));
    }

    /** Compute yaw rate.
     * @param sunBeta Sun elevation above orbital plane
     * (it <em>may</em> be different from {@link #getBeta()} in
     * some special cases)
     * @return yaw rate
     */
    public double yawRate(final double sunBeta) {
        return (getYawEnd(sunBeta) - getYawStart(sunBeta)) / turnSpan.getTurnDuration();
    }

    /** Get the spacecraft angular velocity.
     * @return spacecraft angular velocity
     */
    public double getMuRate() {
        return muRate;
    }

    /** Project a spacecraft/Sun angle into orbital plane.
     * <p>
     * This method is intended to find the limits of the noon and midnight
     * turns in orbital plane. The return angle is signed, depending on the
     * spacecraft being before or after turn middle point.
     * </p>
     * @param angle spacecraft/Sun angle (or spacecraft/opposite-of-Sun)
     * @return angle projected into orbital plane, always positive
     */
    private DerivativeStructure inOrbitPlaneAbsoluteAngle(final DerivativeStructure angle) {
        return FastMath.acos(FastMath.cos(angle).divide(FastMath.cos(beta(getDate()))));
    }

    /** Project a spacecraft/Sun angle into orbital plane.
     * <p>
     * This method is intended to find the limits of the noon and midnight
     * turns in orbital plane. The return angle is always positive. The
     * correct sign to apply depends on the spacecraft being before or
     * after turn middle point.
     * </p>
     * @param angle spacecraft/Sun angle (or spacecraft/opposite-of-Sun)
     * @return angle projected into orbital plane, always positive
     */
    public double inOrbitPlaneAbsoluteAngle(final double angle) {
        return FastMath.acos(FastMath.cos(angle) / FastMath.cos(beta(getDate())));
    }

    /** Compute yaw.
     * @param sunBeta Sun elevation above orbital plane
     * (it <em>may</em> be different from {@link #getBeta()} in
     * some special cases)
     * @param inOrbitPlaneAngle in orbit angle between spacecraft
     * and Sun (or opposite of Sun) projection
     * @return yaw angle
     */
    public double computePhi(final double sunBeta, final double inOrbitPlaneAngle) {
        return FastMath.atan2(-FastMath.tan(sunBeta), FastMath.sin(inOrbitPlaneAngle));
    }

    /** Set turn half span and compute corresponding turn time range.
     * @param halfSpan half span of the turn region, as an angle in orbit plane
     * @param endMargin margin in seconds after turn end
     */
    public void setHalfSpan(final double halfSpan, final double endMargin) {

        final AbsoluteDate start = date.shiftedBy((delta.getValue() - halfSpan) / muRate);
        final AbsoluteDate end   = date.shiftedBy((delta.getValue() + halfSpan) / muRate);
        final AbsoluteDate estimationDate = getDate();

        if (turnSpan == null) {
            turnSpan = new TurnSpan(start, end, estimationDate, endMargin);
        } else {
            turnSpan.updateStart(start, estimationDate);
            turnSpan.updateEnd(end, estimationDate);
        }
    }

    /** Check if context is within turn range.
     * @return true if context is within range extended by end margin
     */
    public boolean inTurnTimeRange() {
        return turnSpan != null && turnSpan.inTurnTimeRange(getDate());
    }

    /** Get elapsed time since turn start.
     * @return elapsed time from turn start to current date
     */
    public double timeSinceTurnStart() {
        return getDate().durationFrom(turnSpan.getTurnStartDate());
    }

    /** Generate an attitude with turn-corrected yaw.
     * @param yaw yaw value to apply
     * @param yawDot yaw first time derivative
     * @return attitude with specified yaw
     */
    public TimeStampedAngularCoordinates turnCorrectedAttitude(final double yaw, final double yawDot) {
        return turnCorrectedAttitude(FACTORY.build(yaw, yawDot, 0.0));
    }

    /** Generate an attitude with turn-corrected yaw.
     * @param yaw yaw value to apply
     * @return attitude with specified yaw
     */
    public TimeStampedAngularCoordinates turnCorrectedAttitude(final DerivativeStructure yaw) {

        // Earth pointing (Z aligned with position) with linear yaw (momentum with known cos/sin in the X/Y plane)
        final PVCoordinates svPV          = pvProv.getPVCoordinates(date, inertialFrame);
        final Vector3D      p             = svPV.getPosition();
        final Vector3D      v             = svPV.getVelocity();
        final Vector3D      a             = svPV.getAcceleration();
        final double        r2            = p.getNormSq();
        final double        r             = FastMath.sqrt(r2);
        final Vector3D      keplerianJerk = new Vector3D(-3 * Vector3D.dotProduct(p, v) / r2, a, -a.getNorm() / r, v);
        final PVCoordinates velocity      = new PVCoordinates(v, a, keplerianJerk);
        final PVCoordinates momentum      = PVCoordinates.crossProduct(svPV, velocity);

        final DerivativeStructure c = FastMath.cos(yaw).negate();
        final DerivativeStructure s = FastMath.sin(yaw).negate();
        final Vector3D m0 = new Vector3D(s.getValue(),              c.getValue(),              0.0);
        final Vector3D m1 = new Vector3D(s.getPartialDerivative(1), c.getPartialDerivative(1), 0.0);
        final Vector3D m2 = new Vector3D(s.getPartialDerivative(2), c.getPartialDerivative(2), 0.0);
        return new TimeStampedAngularCoordinates(date,
                                                 svPV.normalize(), momentum.normalize(),
                                                 MINUS_Z_PV, new PVCoordinates(m0, m1, m2),
                                                 1.0e-9);

    }

    /** Compute Orbit Normal (ON) yaw.
     * @return Orbit Normal yaw, using inertial frame as reference
     */
    public TimeStampedAngularCoordinates orbitNormalYaw() {
        final Transform t = LOFType.VVLH.transformFromInertial(date, pvProv.getPVCoordinates(date, inertialFrame));
        return new TimeStampedAngularCoordinates(date,
                                                 t.getRotation(),
                                                 t.getRotationRate(),
                                                 t.getRotationAcceleration());
    }

}
