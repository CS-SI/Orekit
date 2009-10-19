/* Copyright 2009 CS Communication & Systèmes
 * Licensed to CS Communication & Systèmes (CS) under one or more
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
package org.orekit.forces;

import org.apache.commons.math.geometry.Vector3D;
import org.apache.commons.math.util.MathUtils;
import org.orekit.errors.OrekitException;
import org.orekit.forces.drag.DragSensitive;
import org.orekit.forces.radiation.RadiationSensitive;
import org.orekit.frames.Frame;
import org.orekit.propagation.SpacecraftState;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.PVCoordinatesProvider;

/** This class represents the features of a classical satellite
 * with a parallelepipedic body shape and rotating flat solar arrays.
 * <p>
 * The solar array rotation with respect to satellite body can be either
 * the best lightning orientation (i.e. Sun exactly in solar array meridian
 * plane defined by solar array rotation axis and solar array normal vector)
 * or a rotation evolving linearly according to a start position and an
 * angular rate (which can be set to 0 for non-rotating panels, which may
 * occur in special modes or during contingencies).
 * </p>
 * <p>
 * This model does not take cast shadow between body and solar array into account.
 * </p>
 * <p>
 * Instances of this class are guaranteed to be immutable.
 * </p>
 *
 * @see SphericalSpacecraft
 * @author Luc Maisonobe
 * @version $Revision$ $Date$
 */
public class BoxAndSolarArraySpacecraft implements RadiationSensitive, DragSensitive {

    /** Serializable UID. */
    private static final long serialVersionUID = 5583800166273334973L;

    /** Body cross section normal to X direction (m<sup>2</sup>). */
    private final double bodyXCrossSection;

    /** Body cross section normal to Y direction (m<sup>2</sup>). */
    private final double bodyYCrossSection;

    /** Body cross section normal to Z direction (m<sup>2</sup>). */
    private final double bodyZCrossSection;

    /** Solar array area (m<sup>2</sup>). */
    private final double solarArrayArea;

    /** Reference date for linear rotation angle (may be null). */
    private final AbsoluteDate referenceDate;

    /** Rotation rate for linear rotation angle. */
    private final double rotationRate;

    /** Solar array reference axis in spacecraft frame (may be null). */
    private final Vector3D saX;

    /** Solar array third axis in spacecraft frame (may be null). */
    private final Vector3D saY;

    /** Solar array rotation axis in spacecraft frame. */
    private final Vector3D saZ;

    /** Drag coefficient. */
    private final double dragCoeff;

    /** Absorption coefficient. */
    private final double absorptionCoeff;

    /** Specular reflection coefficient. */
    private final double specularReflectionCoeff;

    /** Sun model. */
    private final PVCoordinatesProvider sun;

    /** Cached state. */
    private transient SpacecraftState cachedState;

    /** Cached solar array normal. */
    private transient Vector3D cachedNormal;

    /** Build a spacecraft model with best lightning of solar array.
     * <p>
     * Solar arrays orientation will be such that at each time the Sun direction
     * will always be in the solar array meridian plane defined by solar array
     * rotation axis and solar array normal vector.
     * </p>
     * @param xLength length of the body along its X axis (m)
     * @param yLength length of the body along its Y axis (m)
     * @param zLength length of the body along its Z axis (m)
     * @param sun sun model
     * @param solarArrayArea area of the solar array (m<sup>2</sup>)
     * @param solarArrayAxis solar array rotation axis in satellite frame
     * @param dragCoeff drag coefficient
     * @param absorptionCoeff absorption coefficient
     * @param specularReflectionCoeff specular reflection coefficient
     */
    public BoxAndSolarArraySpacecraft(final double xLength, final double yLength,
                                      final double zLength,
                                      final PVCoordinatesProvider sun, final double solarArrayArea,
                                      final Vector3D solarArrayAxis,
                                      final double dragCoeff,
                                      final double absorptionCoeff,
                                      final double specularReflectionCoeff) {

        this.bodyXCrossSection = yLength * zLength;
        this.bodyYCrossSection = xLength * zLength;
        this.bodyZCrossSection = xLength * yLength;

        this.sun            = sun;
        this.solarArrayArea = solarArrayArea;
        this.referenceDate  = null;
        this.rotationRate   = 0;

        this.saZ = solarArrayAxis.normalize();
        this.saY = null;
        this.saX = null;

        this.dragCoeff               = dragCoeff;
        this.absorptionCoeff         = absorptionCoeff;
        this.specularReflectionCoeff = specularReflectionCoeff;

        this.cachedState  = null;
        this.cachedNormal = null;

    }

    /** Build a spacecraft model with linear rotation of solar array.
     * <p>
     * Solar arrays orientation will be a regular rotation from the
     * reference orientation at reference date and using a constant
     * rotation rate.
     * </p>
     * @param xLength length of the body along its X axis (m)
     * @param yLength length of the body along its Y axis (m)
     * @param zLength length of the body along its Z axis (m)
     * @param sun sun model
     * @param solarArrayArea area of the solar array (m<sup>2</sup>)
     * @param solarArrayAxis solar array rotation axis in satellite frame
     * @param referenceDate reference date for the solar array rotation
     * @param referenceNormal direction of the solar array normal at reference date
     * in spacecraft frame
     * @param rotationRate rotation rate of the solar array, may be 0 (rad/s)
     * @param dragCoeff drag coefficient
     * @param absorptionCoeff absorption coefficient
     * @param specularReflectionCoeff specular reflection coefficient
     */
    public BoxAndSolarArraySpacecraft(final double xLength, final double yLength,
                                      final double zLength,
                                      final PVCoordinatesProvider sun, final double solarArrayArea,
                                      final Vector3D solarArrayAxis,
                                      final AbsoluteDate referenceDate,
                                      final Vector3D referenceNormal,
                                      final double rotationRate,
                                      final double dragCoeff,
                                      final double absorptionCoeff,
                                      final double specularReflectionCoeff) {

        this.bodyXCrossSection = yLength * zLength;
        this.bodyYCrossSection = xLength * zLength;
        this.bodyZCrossSection = xLength * yLength;

        this.sun            = sun;
        this.solarArrayArea = solarArrayArea;
        this.referenceDate  = referenceDate;
        this.rotationRate   = rotationRate;

        this.saZ = solarArrayAxis.normalize();
        this.saY = Vector3D.crossProduct(saZ, referenceNormal).normalize();
        this.saX = Vector3D.crossProduct(saY, saZ);

        this.dragCoeff               = dragCoeff;
        this.absorptionCoeff         = absorptionCoeff;
        this.specularReflectionCoeff = specularReflectionCoeff;

        this.cachedState  = null;
        this.cachedNormal = null;

    }

    /** Get solar array normal in spacecraft frame.
     * @param state current state information: date, kinematics, attitude
     * @return solar array normal in spacecraft frame
     * @exception OrekitException if sun direction cannot be computed in best lightning
     * configuration
     */
    public synchronized Vector3D getNormal(final SpacecraftState state)
        throws OrekitException {

        if (state != cachedState) {
            // we need to recompute normal
            cachedState = state;

            if (referenceDate == null) {

                // compute orientation for best lightning
                final Frame inertialFrame = state.getFrame();
                final Vector3D sunInert =
                    sun.getPVCoordinates(cachedState.getDate(), inertialFrame).getPosition();
                final Vector3D sunSpacecraft =
                    cachedState.getAttitude().getRotation().applyTo(sunInert);
                final double d = Vector3D.dotProduct(sunSpacecraft, saZ);
                final double f = 1 - d * d;
                if (f < MathUtils.EPSILON) {
                    // extremely rare case: the sun is along solar array rotation axis
                    // (there will not be much output power ...)
                    // we set up an arbitrary normal
                    cachedNormal = saZ.orthogonal();
                } else {
                    cachedNormal = new Vector3D(1.0 / Math.sqrt(f),
                                                sunSpacecraft.subtract(new Vector3D(d, saZ)));
                }

            } else {

                // compute linear angle rotation
                final double dt    = cachedState.getDate().durationFrom(referenceDate);
                final double alpha = rotationRate * dt;
                cachedNormal = new Vector3D(Math.cos(alpha), saX, Math.sin(alpha), saY);

            }
        }

        return cachedNormal;

    }

    /** {@inheritDoc} */
    public double getDragCrossSection(final SpacecraftState state, final Vector3D direction)
        throws OrekitException {
        return Math.abs(direction.getX() * bodyXCrossSection) +
               Math.abs(direction.getY() * bodyYCrossSection) +
               Math.abs(direction.getZ() * bodyZCrossSection) +
               Vector3D.dotProduct(direction, getNormal(state)) * solarArrayArea;
    }

    /** {@inheritDoc} */
    public Vector3D getDragCoef(final SpacecraftState state, final Vector3D direction) {
        return new Vector3D(dragCoeff, direction);
    }

    /** {@inheritDoc} */
    public double getRadiationCrossSection(final SpacecraftState state, final Vector3D direction)
        throws OrekitException {
        return Math.abs(direction.getX() * bodyXCrossSection) +
               Math.abs(direction.getY() * bodyYCrossSection) +
               Math.abs(direction.getZ() * bodyZCrossSection) +
               Vector3D.dotProduct(direction, getNormal(state)) * solarArrayArea;
    }

    /** {@inheritDoc} */
    public Vector3D getAbsorptionCoef(final SpacecraftState state, final Vector3D direction) {
        return new Vector3D(absorptionCoeff, direction);
    }

    /** {@inheritDoc} */
    public Vector3D getReflectionCoef(final SpacecraftState state, final Vector3D direction) {
        return new Vector3D(specularReflectionCoeff, direction);
    }

}
