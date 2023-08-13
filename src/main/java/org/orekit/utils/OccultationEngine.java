/* Copyright 2002-2023 Luc Maisonobe
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

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.FastMath;
import org.orekit.bodies.OneAxisEllipsoid;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.SpacecraftState;

/** Computation engine for occultation events.
 * @author Luc Maisonobe
 * @since 12.0
 */
public class OccultationEngine {

    /** Occulting body. */
    private final OneAxisEllipsoid occulting;

    /** Occulted body. */
    private final ExtendedPVCoordinatesProvider occulted;

    /** Occulted body radius (m). */
    private final double occultedRadius;

    /** Build a new occultation engine.
     * @param occulted the body to be occulted
     * @param occultedRadius the radius of the body to be occulted (m)
     * @param occulting the occulting body
     */
    public OccultationEngine(final ExtendedPVCoordinatesProvider occulted,  final double occultedRadius,
                             final OneAxisEllipsoid occulting) {
        this.occulted       = occulted;
        this.occultedRadius = FastMath.abs(occultedRadius);
        this.occulting      = occulting;
    }

    /** Getter for the occulting body.
     * @return the occulting body
     */
    public OneAxisEllipsoid getOcculting() {
        return occulting;
    }

    /** Getter for the occulted body.
     * @return the occulted body
     */
    public ExtendedPVCoordinatesProvider getOcculted() {
        return occulted;
    }

    /** Getter for the occultedRadius.
     * @return the occultedRadius
     */
    public double getOccultedRadius() {
        return occultedRadius;
    }

    /** Compute the occultation angles as seen from a spacecraft.
     * @param state the current state information: date, kinematics, attitude
     * @return occultation angles
     */
    public OccultationAngles angles(final SpacecraftState state) {

        final Vector3D psat  = state.getPosition(occulting.getBodyFrame());
        final Vector3D pted  = occulted.getPosition(state.getDate(), occulting.getBodyFrame());
        final Vector3D plimb = occulting.pointOnLimb(psat, pted);
        final Vector3D ps    = psat.subtract(pted);
        final Vector3D pi    = psat.subtract(plimb);
        final double angle   = Vector3D.angle(ps, psat);
        final double rs      = FastMath.asin(occultedRadius / ps.getNorm());
        final double ro      = Vector3D.angle(pi, psat);
        if (Double.isNaN(rs)) {
            // we are inside the occulted body…
            // set up dummy values consistent with full lighting (assuming occulted is the Sun)
            return new OccultationAngles(FastMath.PI, 0.0, 0.0);
        } else {
            // regular case, we can compute limit angles as seen from spacecraft
            return new OccultationAngles(angle, ro, rs);
        }

    }

    /** Compute the occultation angles as seen from a spacecraft.
     * @param state the current state information: date, kinematics, attitude
     * @param <T> the type of the field elements
     * @return occultation angles
     */
    public <T extends CalculusFieldElement<T>> FieldOccultationAngles<T> angles(final FieldSpacecraftState<T> state) {

        final FieldVector3D<T> psat  = state.getPosition(occulting.getBodyFrame());
        final FieldVector3D<T> pted  = occulted.getPosition(state.getDate(), occulting.getBodyFrame());
        final FieldVector3D<T> plimb = occulting.pointOnLimb(psat, pted);
        final FieldVector3D<T> ps    = psat.subtract(pted);
        final FieldVector3D<T> pi    = psat.subtract(plimb);
        final T                angle = FieldVector3D.angle(ps, psat);
        final T                rs    = FastMath.asin(ps.getNorm().reciprocal().multiply(occultedRadius));
        final T                ro    = FieldVector3D.angle(pi, psat);
        if (rs.isNaN()) {
            // we are inside the occulted body…
            // set up dummy values consistent with full lighting (assuming occulted is the Sun)
            final T zero = rs.getField().getZero();
            return new FieldOccultationAngles<>(zero.newInstance(FastMath.PI), zero, zero);
        } else {
            // regular case, we can compute limit angles as seen from spacecraft
            return new FieldOccultationAngles<>(angle, ro, rs);
        }

    }

    /** Container for occultation angles.
     * @since 12.0
     */
    public static class OccultationAngles {

        /** Apparent separation between occulting and occulted directions. */
        private final double separation;

        /** Limb radius in occulting/occulted plane. */
        private final double limbRadius;

        /** Apparent radius of occulted body. */
        private final double occultedApparentRadius;

        /** Simple constructor.
         * @param separation apparent separation between occulting and occulted directions (rad)
         * @param limbRadius limb radius in occulting/occulted plane (rad)
         * @param occultedApparentRadius apparent radius of occulted body (rad)
         */
        OccultationAngles(final double separation, final double limbRadius, final double occultedApparentRadius) {
            this.separation             = separation;
            this.limbRadius             = limbRadius;
            this.occultedApparentRadius = occultedApparentRadius;
        }

        /** Get apparent separation between occulting and occulted directions.
         * @return apparent separation between occulting and occulted directions (rad)
         */
        public double getSeparation() {
            return separation;
        }

        /** Get limb radius in occulting/occulted plane.
         * @return limb radius in occulting/occulted plane (rad)
         */
        public double getLimbRadius() {
            return limbRadius;
        }

        /** Get apparent radius of occulted body.
         * @return apparent radius of occulted body (rad)
         */
        public double getOccultedApparentRadius() {
            return occultedApparentRadius;
        }

    }

    /** Container for occultation angles.
     * @param <T> the type of the field elements
     * @since 12.0
     */
    public static class FieldOccultationAngles<T extends CalculusFieldElement<T>> {

        /** Apparent separation between occulting and occulted directions. */
        private final T separation;

        /** Limb radius in occulting/occulted plane. */
        private final T limbRadius;

        /** Apparent radius of occulted body. */
        private final T occultedApparentRadius;

        /** Simple constructor.
         * @param separation apparent separation between occulting and occulted directions (rad)
         * @param limbRadius limb radius in occulting/occulted plane (rad)
         * @param occultedApparentRadius apparent radius of occulted body (rad)
         */
        FieldOccultationAngles(final T separation, final T limbRadius, final T occultedApparentRadius) {
            this.separation             = separation;
            this.limbRadius             = limbRadius;
            this.occultedApparentRadius = occultedApparentRadius;
        }

        /** Get apparent separation between occulting and occulted directions.
         * @return apparent separation between occulting and occulted directions (rad)
         */
        public T getSeparation() {
            return separation;
        }

        /** Get limb radius in occulting/occulted plane.
         * @return limb radius in occulting/occulted plane (rad)
         */
        public T getLimbRadius() {
            return limbRadius;
        }

        /** Get apparent radius of occulted body.
         * @return apparent radius of occulted body (rad)
         */
        public T getOccultedApparentRadius() {
            return occultedApparentRadius;
        }

    }

}
