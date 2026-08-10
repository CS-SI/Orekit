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
package org.orekit.models.earth.ionosphere;

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.orekit.bodies.OneAxisEllipsoid;

/**
 * Abstract ionospheric model parent class.
 * <p>
 * Defines an abstract IonosphericModel parent class to hold
 * the earth model, which is used in calculating path delay.
 * </p>
 *
 * @author Brianna Aubin
 * @since 14.0
 */
public abstract class AbstractIonosphericModel implements IonosphericModel {

    /** Stores earth data. */
    private final OneAxisEllipsoid earth;

    /** Simple constructor.
     * @param earth elliptic earth body
     * @since 14.0
     */
    protected AbstractIonosphericModel(final OneAxisEllipsoid earth) {
        this.earth = earth;
    }

    /** Get the earth body shape for earth-frame calculations.
     * @return earth body shape
     * @since 14.0
     */
    public final OneAxisEllipsoid getEarth() {
        return earth;
    }

    /**
     * Checks to see if it is valid to calculate the ionospheric path delay
     * for a position endpoint value w.r.t. the minimum altitude point
     * between two given objects (whether they are space or ground based).
     * @param position     position of object value being considered (one of the two endpoints)
     * @param p1           position of first endpoint in the link
     * @param p2           position of second endpoint in the link
     * @param baseAltitude altitude of the groundpoint that represents the minimum
     *                     altitude value on the path between p1 and p2
     * @return True if the path delay needs to be calculated
     */
    protected final Boolean checkIfPathIsValid(final Vector3D position, final Vector3D p1, final Vector3D p2, final Double baseAltitude) {
        final Double elevation = position.getDelta();
        return endpointCheck(elevation, position) ^ midpointCheck(elevation, p1, p2, baseAltitude);
    }

    /**
     * Checks to see if it is valid to calculate the ionospheric path delay
     * for a position endpoint value w.r.t. the minimum altitude point
     * between two given objects (whether they are space or ground based).
     * @param <T>          type of the elements
     * @param position     position of object value being considered (one of the two endpoints)
     * @param p1           position of first endpoint in the link
     * @param p2           position of second endpoint in the link
     * @param baseAltitude altitude of the groundpoint that represents the minimum
     *                     altitude value on the path between p1 and p2
     * @return True if the path delay needs to be calculated
     */
    protected final <T extends CalculusFieldElement<T>> Boolean checkIfPathIsValid(final FieldVector3D<T> position, final FieldVector3D<T> p1, final FieldVector3D<T> p2, final Double baseAltitude) {
        return checkIfPathIsValid(position.toVector3D(), p1.toVector3D(), p2.toVector3D(), baseAltitude);
    }

    /**
     * These conditions are true for either a ground-space link, or a space-space link where
     * the minimum altitude point of the link is co-located with one of the spacecraft.
     * @param elevation    elevation of object value being considered w.r.t. the
     *                     minimum altitude point between the two link end points
     * @param position     position of object value being considered (one of the two endpoints)
     * @return boolean indiciating whther the path delay needs to be calculated
     */
    private Boolean endpointCheck(final Double elevation, final Vector3D position) {

        // Check that elevation of position value in topocentric frame is > 0 deg
        // This is true for a ground-space link, or a space-space link where one satellite has a lower
        // altitude than the other
        final Boolean elevationCheck = elevation > 1e-7;

        // Check that distance to topocentric frame is > 1 m
        final Boolean distanceCheck = position.getNorm() > 1.0;

        return elevationCheck && distanceCheck;
    }

    /**
     * This is true for a space-space link where the minimum altitude point is not
     * co-located with either spacecraft.
     * @param elevation    elevation of object value being considered w.r.t. the
     *                     minimum altitude point between the two link end points
     * @param p1           position of first endpoint in the link
     * @param p2           position of second endpoint in the link
     * @param baseAltitude altitude of the groundpoint that represents the minimum
     *                     altitude value on the path between p1 and p2
     * @return boolean indiciating whther the path delay needs to be calculated
     */
    private Boolean midpointCheck(final Double elevation, final Vector3D p1, final Vector3D p2, final Double baseAltitude) {

        // Check that elevation of position value in topocentric frame is == 0 deg
        final Boolean elevationCheck = Math.abs(elevation) < 1e-5;

        // Check that both endpoints have zero-altitude w.r.t. the topocentric frame
        final Boolean distanceCheck = Math.abs(p1.getZ()) < 1e-5 && Math.abs(p2.getZ()) < 1e-5;

        // Checks that the minimum altitude point is NOT inside the earth
        final Boolean altitudeCheck = baseAltitude > -1e-6;

        return elevationCheck && distanceCheck && altitudeCheck;
    }

}
