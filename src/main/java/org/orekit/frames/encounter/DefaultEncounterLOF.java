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
package org.orekit.frames.encounter;

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.Field;
import org.hipparchus.geometry.euclidean.threed.FieldRotation;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Rotation;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.orekit.utils.FieldPVCoordinates;
import org.orekit.utils.PVCoordinates;

/**
 * Default encounter local orbital frame.
 * <p>
 * Note that <b>it is up to the user</b> to choose which object should be at the origin.
 * <p>
 * It is defined as follows :
 * <ul>
 * <li>z axis : Normalized relative velocity vector.</li>
 * <li>y axis : Normalized cross product between z axis and other relative to origin position.</li>
 * <li>x axis : Completes the right handed coordinate system.</li>
 * </ul>
 *
 * @author Vincent Cucchietti
 * @since 12.0
 */
public class DefaultEncounterLOF extends AbstractEncounterLOF {

    /**
     * Constructor.
     *
     * @param other other object to create the encounter frame with (not the origin of the frame !)
     */
    public DefaultEncounterLOF(final PVCoordinates other) {
        super(other);
    }

    /**
     * Field constructor.
     *
     * @param other other object to create the encounter frame with (not the origin of the frame !)
     * @param <T> type of the field elements
     */
    public <T extends CalculusFieldElement<T>> DefaultEncounterLOF(final FieldPVCoordinates<T> other) {
        super(other);
    }

    /** {@inheritDoc} */
    @Override
    public Rotation rotationFromInertial(final PVCoordinates origin, final PVCoordinates other) {
        final Vector3D zAxis = other.getVelocity().subtract(origin.getVelocity()).normalize();
        final Vector3D yAxis = zAxis.crossProduct(other.getPosition().subtract(origin.getPosition()));

        return new Rotation(yAxis, zAxis, Vector3D.PLUS_J, Vector3D.PLUS_K);
    }

    /**
     * {@inheritDoc}
     * <p>
     * In this case, return (0,0,1);
     */
    @Override
    public <T extends CalculusFieldElement<T>> FieldVector3D<T> getAxisNormalToCollisionPlane(final Field<T> field) {
        return FieldVector3D.getPlusK(field);
    }

    /** {@inheritDoc} */
    @Override
    public <T extends CalculusFieldElement<T>> FieldRotation<T> rotationFromInertial(final Field<T> field,
                                                                                     final FieldPVCoordinates<T> origin,
                                                                                     final FieldPVCoordinates<T> other) {
        final FieldVector3D<T> otherVelocity = other.getVelocity();
        final FieldVector3D<T> otherPosition = other.getPosition();

        final FieldVector3D<T> zAxis = otherVelocity.subtract(origin.getVelocity()).normalize();
        final FieldVector3D<T> yAxis = zAxis.crossProduct(otherPosition.subtract(origin.getPosition()));

        return new FieldRotation<>(yAxis, zAxis, FieldVector3D.getPlusJ(field), FieldVector3D.getPlusK(field));
    }

    /**
     * {@inheritDoc}
     * <p>
     * In this case, return (0,0,1);
     */
    @Override
    public Vector3D getAxisNormalToCollisionPlane() {
        return Vector3D.PLUS_K;
    }

    /** {@inheritDoc} */
    @Override
    public String getName() {
        return "DEFAULT_ENCOUNTER_LOF";
    }
}
