/* Copyright 2002-2017 CS Systèmes d'Information
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
package org.orekit.gnss.attitude;

import org.hipparchus.RealFieldElement;
import org.hipparchus.analysis.differentiation.DerivativeStructure;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Rotation;
import org.hipparchus.geometry.euclidean.threed.RotationConvention;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.FastMath;
import org.orekit.attitudes.Attitude;
import org.orekit.attitudes.FieldAttitude;
import org.orekit.errors.OrekitException;
import org.orekit.frames.Frame;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.utils.AngularCoordinates;
import org.orekit.utils.FieldPVCoordinates;
import org.orekit.utils.FieldPVCoordinatesProvider;
import org.orekit.utils.PVCoordinates;
import org.orekit.utils.PVCoordinatesProvider;
import org.orekit.utils.TimeStampedAngularCoordinates;
import org.orekit.utils.TimeStampedPVCoordinates;

/**
 * Base class for attitude providers for navigation satellites.
 *
 * @author Luc Maisonobe
 * @since 9.2
 */
public abstract class AbstractGNSSAttitudeProvider implements GNSSAttitudeProvider {

    /** Serializable UID. */
    private static final long serialVersionUID = 20171114L;

    /** Constant X axis. */
    private static final PVCoordinates PLUS_X =
            new PVCoordinates(Vector3D.PLUS_I, Vector3D.ZERO, Vector3D.ZERO);

    /** Constant Y axis. */
    private static final PVCoordinates PLUS_Y =
            new PVCoordinates(Vector3D.PLUS_J, Vector3D.ZERO, Vector3D.ZERO);

    /** Constant Z axis. */
    private static final PVCoordinates MINUS_Z =
            new PVCoordinates(Vector3D.MINUS_K, Vector3D.ZERO, Vector3D.ZERO);

    /** Start of validity for this provider. */
    private final AbsoluteDate validityStart;

    /** End of validity for this provider. */
    private final AbsoluteDate validityEnd;

    /** Provider for Sun position. */
    private final PVCoordinatesProvider sun;

    /** Simple constructor.
     * @param validityStart start of validity for this provider
     * @param validityEnd end of validity for this provider
     * @param sun provider for Sun position
     * @param earthFrame Earth frame to use
     */
    protected AbstractGNSSAttitudeProvider(final AbsoluteDate validityStart,
                                           final AbsoluteDate validityEnd,
                                           final PVCoordinatesProvider sun) {
        this.validityStart = validityStart;
        this.validityEnd   = validityEnd;
        this.sun           = sun;
    }

    /** {@inheritDoc} */
    @Override
    public AbsoluteDate validityStart() {
        return validityStart;
    }

    /** {@inheritDoc} */
    @Override
    public AbsoluteDate validityEnd() {
        return validityEnd;
    }

    /** {@inheritDoc} */
    @Override
    public Attitude getAttitude(final PVCoordinatesProvider pvProv,
                                final AbsoluteDate date,
                                final Frame frame)
        throws OrekitException {

        // Sun/spacecraft geometry
        // computed in inertial frame so orbital plane (which depends on spacecraft velocity) is correct
        final TimeStampedPVCoordinates                sunPV   = sun.getPVCoordinates(date, frame);
        final FieldPVCoordinates<DerivativeStructure> sunPVDS = sunPV.toDerivativeStructurePV(2);
        final TimeStampedPVCoordinates                svPV    = pvProv.getPVCoordinates(date, frame);
        final FieldPVCoordinates<DerivativeStructure> svPVDS  = svPV.toDerivativeStructurePV(2);
        final DerivativeStructure r      = svPVDS.getPosition().getNorm();
        final DerivativeStructure svbCos = FieldVector3D.dotProduct(sunPVDS.getPosition(), svPVDS.getPosition()).
                                           divide(sunPVDS.getPosition().getNorm().multiply(r));
        final DerivativeStructure beta   = FieldVector3D.angle(sunPVDS.getPosition(), svPVDS.getMomentum()).
                                           negate().
                                           add(0.5 * FastMath.PI);

        // nominal yaw steering
        final TimeStampedAngularCoordinates nominalYaw =
                        new TimeStampedAngularCoordinates(date,
                                                          svPV.normalize(),
                                                          PVCoordinates.crossProduct(svPV, sunPV).normalize(),
                                                          MINUS_Z,
                                                          PLUS_Y,
                                                          1.0e-9);

        // compute yaw correction
        final TimeStampedAngularCoordinates corrected = correctYaw(svPV, svPVDS, beta, svbCos, nominalYaw);

        return new Attitude(frame, corrected);

    }

    /** {@inheritDoc} */
    @Override
    public <T extends RealFieldElement<T>> FieldAttitude<T> getAttitude(final FieldPVCoordinatesProvider<T> pvProv,
                                                                        final FieldAbsoluteDate<T> date,
                                                                        final Frame frame)
        throws OrekitException {
        // TODO
        return null;
    }

    /** Compute yaw angle correction.
     * @param pv spacecraft position-velocity in inertial frame
     * @param pvDS spacecraft position-velocity in inertial frame
     * @param beta angle between Sun and orbital plane
     * @param svbCos cosine of the angle between spacecraft and Sun direction
     * @param nominal nominal yaw
     * @return corrected yaw
     * @exception OrekitException if yaw cannot be corrected 
     */
    protected abstract TimeStampedAngularCoordinates correctYaw(TimeStampedPVCoordinates pv,
                                                                FieldPVCoordinates<DerivativeStructure> pvDS,
                                                                DerivativeStructure beta,
                                                                DerivativeStructure svbCos, TimeStampedAngularCoordinates nominal)
        throws OrekitException;

    /** Combine nominal yaw and correction.
     * @param nominalYaw nominal yaw
     * @param yawCorrection yaw correction angle
     * @param yawCorrectionDot yaw correction angle derivative
     * @return corrected yaw
     */
    protected TimeStampedAngularCoordinates applyCorrection(final TimeStampedAngularCoordinates nominalYaw,
                                                            final double yawCorrection,
                                                            final double yawCorrectionDot) {

        // compute a linear correction model
        final AngularCoordinates correction =
                        new AngularCoordinates(new Rotation(Vector3D.PLUS_K, yawCorrection,
                                                            RotationConvention.FRAME_TRANSFORM),
                                               new Vector3D(yawCorrectionDot, Vector3D.PLUS_K));

        // combine the correction with the nominal yaw
        return nominalYaw.addOffset(correction);
        
    }

    /** Compute Orbit Normal (ON) yaw.
     * @param pv spacecraft position-velocity in inertial frame
     * @return Orbit Normal yaw
     * @exception OrekitException if derivation order is too large (never happens with hard-coded order)
     */
    protected TimeStampedAngularCoordinates orbitNormalYaw(final TimeStampedPVCoordinates pv)
        throws OrekitException {
        final Vector3D p             = pv.getPosition();
        final Vector3D v             = pv.getVelocity();
        final Vector3D a             = pv.getAcceleration();
        final double   r2            = p.getNormSq();
        final double   r             = FastMath.sqrt(r2);
        final Vector3D keplerianJerk = new Vector3D(-3 * Vector3D.dotProduct(p, v) / r2, a, -a.getNorm() / r, v);
        final PVCoordinates velocity = new PVCoordinates(v, a, keplerianJerk);
        final PVCoordinates normal   =
                        new PVCoordinates(FieldVector3D.crossProduct(pv.toDerivativeStructureVector(2),
                                                                     velocity.toDerivativeStructureVector(2)).
                                          normalize());

        return new TimeStampedAngularCoordinates(pv.getDate(), MINUS_Z, pv, PLUS_Y, normal, 1.0e-9);
    }

    /** Compute nominal yaw angle.
     * @param pv spacecraft position-velocity in inertial frame
     * @param beta angle between Sun and orbital plane
     * @param nominal nominal yaw
     * @exception OrekitException if the user specified order is too large
     * (never really thrown)
     */
    protected DerivativeStructure yawAngle(final FieldPVCoordinates<DerivativeStructure> pv,
                                           final DerivativeStructure beta,
                                           final TimeStampedAngularCoordinates nominal)
        throws OrekitException {
        final int order = pv.getPosition().getX().getOrder();
        final FieldVector3D<DerivativeStructure> xSat =
                        nominal.revert().applyTo(PLUS_X).toDerivativeStructureVector(order);
        return FieldVector3D.angle(pv.getVelocity(), xSat).copySign(-beta.getReal());
    }

}
