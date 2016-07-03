/* Copyright 2002-2016 CS Systèmes d'Information
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
package org.orekit.models.earth.tessellation;

import org.hipparchus.geometry.euclidean.threed.Vector3D;

/** Enumerate for neighboring directions in a {@link Mesh}.
 * @author Luc Maisonobe
 */
enum Direction {

    /** Along tile in the plus direction. */
    PLUS_ALONG() {

        /** {@inheritDoc} */
        @Override
        public Direction next() {
            return PLUS_ACROSS;
        }

        /** {@inheritDoc} */
        @Override
        public int neighborAlongIndex(final Mesh.Node base) {
            return base.getAlongIndex() + 1;
        }

        /** {@inheritDoc} */
        @Override
        public Vector3D motion(final Mesh.Node base,
                               final double alongDistance, final double acrossDistance) {
            return new Vector3D(alongDistance, base.getAlong());
        }

    },

    /** Along tile in the minus direction. */
    MINUS_ALONG() {

        /** {@inheritDoc} */
        @Override
        public Direction next() {
            return MINUS_ACROSS;
        }

        /** {@inheritDoc} */
        @Override
        public int neighborAlongIndex(final Mesh.Node base) {
            return base.getAlongIndex() - 1;
        }

        /** {@inheritDoc} */
        @Override
        public Vector3D motion(final Mesh.Node base,
                               final double alongDistance, final double acrossDistance) {
            return new Vector3D(-alongDistance, base.getAlong());
        }

    },

    /** Across tile in the plus direction. */
    PLUS_ACROSS() {

        /** {@inheritDoc} */
        @Override
        public Direction next() {
            return MINUS_ALONG;
        }

        /** {@inheritDoc} */
        @Override
        public int neighborAcrossIndex(final Mesh.Node base) {
            return base.getAcrossIndex() + 1;
        }

        /** {@inheritDoc} */
        @Override
        public Vector3D motion(final Mesh.Node base,
                               final double alongDistance, final double acrossDistance) {
            return new Vector3D(acrossDistance, base.getAcross());
        }

    },

    /** Across tile in the minus direction. */
    MINUS_ACROSS() {

        /** {@inheritDoc} */
        @Override
        public Direction next() {
            return PLUS_ALONG;
        }

        /** {@inheritDoc} */
        @Override
        public int neighborAcrossIndex(final Mesh.Node base) {
            return base.getAcrossIndex() - 1;
        }

        /** {@inheritDoc} */
        @Override
        public Vector3D motion(final Mesh.Node base,
                               final double alongDistance, final double acrossDistance) {
            return new Vector3D(-acrossDistance, base.getAcross());
        }

    };

    /** Get the next direction in counterclockwise order.
     * @return next direction
     */
    public abstract Direction next();

    /** Get the along index of neighbor.
     * @param base base node
     * @return along index of neighbor node
     */
    public int neighborAlongIndex(final Mesh.Node base) {
        return base.getAlongIndex();
    }

    /** Get the across index of neighbor.
     * @param base base node
     * @return across index of neighbor node
     */
    public int neighborAcrossIndex(final Mesh.Node base) {
        return base.getAcrossIndex();
    }

    /** Get the motion towards neighbor.
     * @param base base node
     * @param alongDistance distance for along tile motions
     * @param acrossDistance distance for across tile motions
     * @return motion towards neighbor
     */
    public abstract Vector3D motion(Mesh.Node base, double alongDistance, double acrossDistance);

}
