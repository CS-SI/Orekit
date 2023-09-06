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
 * Valsecchi encounter local orbital frame based on Valsecchi formulation from : "Valsecchi, G. B., Milani, A., Gronchi, G.
 * F. &amp; Ches- ley, S. R. Resonant returns to close approaches: Analytical theory. Astronomy &amp; Astrophysics 408,
 * 1179–1196 (2003).".
 * <p>
 * Note that <b>it is up to the user</b> to choose which object should be at the origin.
 *
 * @author Vincent Cucchietti
 * @author Quentin Parpaite
 * @since 12.0
 */
public class ValsecchiEncounterFrame extends AbstractEncounterLOF {

    /**
     * Constructor.
     *
     * @param other other object to create the encounter frame with (not the origin of the frame !)
     */
    public ValsecchiEncounterFrame(final PVCoordinates other) {
        super(other);
    }

    /**
     * Constructor.
     *
     * @param other other object to create the encounter frame with (not the origin of the frame !)
     * @param <T> type of the field elements
     */
    public <T extends CalculusFieldElement<T>> ValsecchiEncounterFrame(final FieldPVCoordinates<T> other) {
        super(other);
    }

    /** {@inheritDoc} */
    @Override
    public <T extends CalculusFieldElement<T>> FieldRotation<T> rotationFromInertial(final Field<T> field,
                                                                                     final FieldPVCoordinates<T> origin,
                                                                                     final FieldPVCoordinates<T> other) {
        final FieldVector3D<T> otherVelocity = other.getVelocity();

        final FieldVector3D<T> xAxis = origin.getVelocity().crossProduct(otherVelocity).normalize();
        final FieldVector3D<T> yAxis = otherVelocity.subtract(origin.getVelocity()).normalize();

        return new FieldRotation<>(xAxis, yAxis, FieldVector3D.getPlusI(field), FieldVector3D.getPlusJ(field));
    }

    /** {@inheritDoc} */
    @Override
    public Rotation rotationFromInertial(final PVCoordinates origin, final PVCoordinates other) {
        final Vector3D xAxis = origin.getVelocity().crossProduct(other.getVelocity()).normalize();
        final Vector3D yAxis = other.getVelocity().subtract(origin.getVelocity()).normalize();

        return new Rotation(xAxis, yAxis, Vector3D.PLUS_I, Vector3D.PLUS_J);
    }

    /**
     * {@inheritDoc}
     * <p>
     * In this case, return (0,1,0);
     */
    @Override
    public <T extends CalculusFieldElement<T>> FieldVector3D<T> getAxisNormalToCollisionPlane(final Field<T> field) {
        return FieldVector3D.getPlusJ(field);
    }

    /**
     * {@inheritDoc}
     * <p>
     * In this case, return (0,1,0);
     */
    @Override
    public Vector3D getAxisNormalToCollisionPlane() {
        return Vector3D.PLUS_J;
    }

    /** {@inheritDoc} */
    @Override
    public String getName() {
        return "VALSECCHI_ENCOUNTER_LOF";
    }
}
