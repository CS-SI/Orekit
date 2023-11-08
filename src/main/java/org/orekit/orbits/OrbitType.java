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
package org.orekit.orbits;

import java.util.Arrays;

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.Field;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.FastMath;
import org.hipparchus.util.MathUtils;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.frames.Frame;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.utils.FieldPVCoordinates;
import org.orekit.utils.PVCoordinates;
import org.orekit.utils.ParameterDriver;
import org.orekit.utils.ParameterDriversList;
import org.orekit.utils.TimeStampedFieldPVCoordinates;

/** Enumerate for {@link Orbit} and {@link FieldOrbit} parameters types.
 */
public enum OrbitType {

    /** Type for orbital representation in {@link CartesianOrbit} and {@link FieldCartesianOrbit} parameters. */
    CARTESIAN {

        /** {@inheritDoc} */
        @Override
        public CartesianOrbit convertType(final Orbit orbit) {
            return (orbit.getType() == this) ? (CartesianOrbit) orbit : new CartesianOrbit(orbit);
        }

        /** {@inheritDoc} */
        @Override
        public void mapOrbitToArray(final Orbit orbit, final PositionAngleType type,
                                    final double[] stateVector, final double[] stateVectorDot) {

            final PVCoordinates pv = orbit.getPVCoordinates();
            final Vector3D      p  = pv.getPosition();
            final Vector3D      v  = pv.getVelocity();

            stateVector[0] = p.getX();
            stateVector[1] = p.getY();
            stateVector[2] = p.getZ();
            stateVector[3] = v.getX();
            stateVector[4] = v.getY();
            stateVector[5] = v.getZ();

            if (stateVectorDot != null) {
                final Vector3D a  = pv.getAcceleration();
                stateVectorDot[0] = v.getX();
                stateVectorDot[1] = v.getY();
                stateVectorDot[2] = v.getZ();
                stateVectorDot[3] = a.getX();
                stateVectorDot[4] = a.getY();
                stateVectorDot[5] = a.getZ();
            }

        }

        /** {@inheritDoc} */
        @Override
        public CartesianOrbit mapArrayToOrbit(final double[] stateVector, final double[] stateVectorDot, final PositionAngleType type,
                                              final AbsoluteDate date, final double mu, final Frame frame) {

            final Vector3D p = new Vector3D(stateVector[0], stateVector[1], stateVector[2]);
            final Vector3D v = new Vector3D(stateVector[3], stateVector[4], stateVector[5]);
            final Vector3D a;
            if (stateVectorDot == null) {
                // we don't have data about acceleration
                return new CartesianOrbit(new PVCoordinates(p, v), frame, date, mu);
            } else {
                // we do have an acceleration
                a = new Vector3D(stateVectorDot[3], stateVectorDot[4], stateVectorDot[5]);
                return new CartesianOrbit(new PVCoordinates(p, v, a), frame, date, mu);
            }

        }

        /** {@inheritDoc} */
        @Override
        public <T extends CalculusFieldElement<T>> FieldCartesianOrbit<T> convertType(final FieldOrbit<T> orbit) {
            return (orbit.getType() == this) ? (FieldCartesianOrbit<T>) orbit : new FieldCartesianOrbit<>(orbit);
        }

        /** {@inheritDoc} */
        @Override
        public <T extends CalculusFieldElement<T>> void mapOrbitToArray(final FieldOrbit<T> orbit,
                                                                        final PositionAngleType type,
                                                                        final T[] stateVector,
                                                                        final T[] stateVectorDot) {

            final TimeStampedFieldPVCoordinates<T> pv = orbit.getPVCoordinates();
            final FieldVector3D<T>                 p  = pv.getPosition();
            final FieldVector3D<T>                 v  = pv.getVelocity();

            stateVector[0] = p.getX();
            stateVector[1] = p.getY();
            stateVector[2] = p.getZ();
            stateVector[3] = v.getX();
            stateVector[4] = v.getY();
            stateVector[5] = v.getZ();

            if (stateVectorDot != null) {
                final FieldVector3D<T> a = pv.getAcceleration();
                stateVectorDot[0] = v.getX();
                stateVectorDot[1] = v.getY();
                stateVectorDot[2] = v.getZ();
                stateVectorDot[3] = a.getX();
                stateVectorDot[4] = a.getY();
                stateVectorDot[5] = a.getZ();
            }

        }

        /** {@inheritDoc} */
        @Override
        public <T extends CalculusFieldElement<T>> FieldCartesianOrbit<T> mapArrayToOrbit(final T[] stateVector,
                                                                                          final T[] stateVectorDot,
                                                                                          final PositionAngleType type,
                                                                                          final FieldAbsoluteDate<T> date,
                                                                                          final T mu, final Frame frame) {
            final FieldVector3D<T> p = new FieldVector3D<>(stateVector[0], stateVector[1], stateVector[2]);
            final FieldVector3D<T> v = new FieldVector3D<>(stateVector[3], stateVector[4], stateVector[5]);
            final FieldVector3D<T> a;
            if (stateVectorDot == null) {
                // we don't have data about acceleration
                return new FieldCartesianOrbit<>(new FieldPVCoordinates<>(p, v), frame, date, mu);
            } else {
                // we do have an acceleration
                a = new FieldVector3D<>(stateVectorDot[3], stateVectorDot[4], stateVectorDot[5]);
                return new FieldCartesianOrbit<>(new FieldPVCoordinates<>(p, v, a), frame, date, mu);
            }

        }

        /** {@inheritDoc} */
        @Override
        public <T extends CalculusFieldElement<T>> FieldCartesianOrbit<T> convertToFieldOrbit(final Field<T> field,
                                                                                              final Orbit orbit) {
            return new FieldCartesianOrbit<>(field, CARTESIAN.convertType(orbit));
        }

        /** {@inheritDoc} */
        @Override
        public ParameterDriversList getDrivers(final double dP, final Orbit orbit, final PositionAngleType type) {
            final ParameterDriversList drivers = new ParameterDriversList();
            final double[] array = new double[6];
            mapOrbitToArray(orbit, type, array, null);
            final double[] scale = scale(dP, orbit);
            drivers.add(new ParameterDriver(POS_X, array[0], scale[0], Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY));
            drivers.add(new ParameterDriver(POS_Y, array[1], scale[1], Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY));
            drivers.add(new ParameterDriver(POS_Z, array[2], scale[2], Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY));
            drivers.add(new ParameterDriver(VEL_X, array[3], scale[3], Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY));
            drivers.add(new ParameterDriver(VEL_Y, array[4], scale[4], Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY));
            drivers.add(new ParameterDriver(VEL_Z, array[5], scale[5], Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY));
            return drivers;
        }

        /** {@inheritDoc} */
        @Override
        public CartesianOrbit normalize(final Orbit orbit, final Orbit reference) {
            // no angular parameters need normalization
            return convertType(orbit);
        }

        /** {@inheritDoc} */
        @Override
        public <T extends CalculusFieldElement<T>> FieldCartesianOrbit<T> normalize(final FieldOrbit<T> orbit, final FieldOrbit<T> reference) {
            // no angular parameters need normalization
            return convertType(orbit);
        }

        /** {@inheritDoc} */
        @Override
        public boolean isPositionAngleBased() {
            return false;
        }

    },

    /** Type for orbital representation in {@link CircularOrbit} and {@link FieldCircularOrbit} parameters. */
    CIRCULAR {

        /** {@inheritDoc} */
        @Override
        public CircularOrbit convertType(final Orbit orbit) {
            return (orbit.getType() == this) ? (CircularOrbit) orbit : new CircularOrbit(orbit);
        }

        /** {@inheritDoc} */
        @Override
        public void mapOrbitToArray(final Orbit orbit, final PositionAngleType type,
                                    final double[] stateVector, final double[] stateVectorDot) {

            final CircularOrbit circularOrbit = (CircularOrbit) OrbitType.CIRCULAR.convertType(orbit);

            stateVector[0] = circularOrbit.getA();
            stateVector[1] = circularOrbit.getCircularEx();
            stateVector[2] = circularOrbit.getCircularEy();
            stateVector[3] = circularOrbit.getI();
            stateVector[4] = circularOrbit.getRightAscensionOfAscendingNode();
            stateVector[5] = circularOrbit.getAlpha(type);

            if (stateVectorDot != null) {
                if (orbit.hasDerivatives()) {
                    stateVectorDot[0] = circularOrbit.getADot();
                    stateVectorDot[1] = circularOrbit.getCircularExDot();
                    stateVectorDot[2] = circularOrbit.getCircularEyDot();
                    stateVectorDot[3] = circularOrbit.getIDot();
                    stateVectorDot[4] = circularOrbit.getRightAscensionOfAscendingNodeDot();
                    stateVectorDot[5] = circularOrbit.getAlphaDot(type);
                } else {
                    Arrays.fill(stateVectorDot, 0, 6, Double.NaN);
                }
            }

        }

        /** {@inheritDoc} */
        @Override
        public CircularOrbit mapArrayToOrbit(final double[] stateVector, final double[] stateVectorDot, final PositionAngleType type,
                                             final AbsoluteDate date, final double mu, final Frame frame) {
            if (stateVectorDot == null) {
                // we don't have orbit derivatives
                return new CircularOrbit(stateVector[0], stateVector[1], stateVector[2],
                                         stateVector[3], stateVector[4], stateVector[5],
                                         type, frame, date, mu);
            } else {
                // we have orbit derivatives
                return new CircularOrbit(stateVector[0],    stateVector[1],    stateVector[2],
                                         stateVector[3],    stateVector[4],    stateVector[5],
                                         stateVectorDot[0], stateVectorDot[1], stateVectorDot[2],
                                         stateVectorDot[3], stateVectorDot[4], stateVectorDot[5],
                                         type, frame, date, mu);
            }
        }

        /** {@inheritDoc} */
        @Override
        public <T extends CalculusFieldElement<T>> FieldCircularOrbit<T> convertType(final FieldOrbit<T> orbit) {
            return (orbit.getType() == this) ? (FieldCircularOrbit<T>) orbit : new FieldCircularOrbit<>(orbit);
        }

        /** {@inheritDoc} */
        @Override
        public <T extends CalculusFieldElement<T>> void mapOrbitToArray(final FieldOrbit<T> orbit,
                                                                        final PositionAngleType type,
                                                                        final T[] stateVector,
                                                                        final T[] stateVectorDot) {

            final FieldCircularOrbit<T> circularOrbit = (FieldCircularOrbit<T>) OrbitType.CIRCULAR.convertType(orbit);

            stateVector[0] = circularOrbit.getA();
            stateVector[1] = circularOrbit.getCircularEx();
            stateVector[2] = circularOrbit.getCircularEy();
            stateVector[3] = circularOrbit.getI();
            stateVector[4] = circularOrbit.getRightAscensionOfAscendingNode();
            stateVector[5] = circularOrbit.getAlpha(type);

            if (stateVectorDot != null) {
                if (orbit.hasDerivatives()) {
                    stateVectorDot[0] = circularOrbit.getADot();
                    stateVectorDot[1] = circularOrbit.getCircularExDot();
                    stateVectorDot[2] = circularOrbit.getCircularEyDot();
                    stateVectorDot[3] = circularOrbit.getIDot();
                    stateVectorDot[4] = circularOrbit.getRightAscensionOfAscendingNodeDot();
                    stateVectorDot[5] = circularOrbit.getAlphaDot(type);
                } else {
                    Arrays.fill(stateVectorDot, 0, 6, orbit.getZero().add(Double.NaN));
                }
            }

        }

        /** {@inheritDoc} */
        @Override
        public <T extends CalculusFieldElement<T>> FieldCircularOrbit<T> mapArrayToOrbit(final T[] stateVector,
                                                                                         final T[] stateVectorDot, final PositionAngleType type,
                                                                                         final FieldAbsoluteDate<T> date,
                                                                                         final T mu, final Frame frame) {
            if (stateVectorDot == null) {
                // we don't have orbit derivatives
                return new FieldCircularOrbit<>(stateVector[0], stateVector[1], stateVector[2],
                                                stateVector[3], stateVector[4], stateVector[5],
                                                type, frame, date, mu);
            } else {
                // we have orbit derivatives
                return new FieldCircularOrbit<>(stateVector[0],    stateVector[1],    stateVector[2],
                                                stateVector[3],    stateVector[4],    stateVector[5],
                                                stateVectorDot[0], stateVectorDot[1], stateVectorDot[2],
                                                stateVectorDot[3], stateVectorDot[4], stateVectorDot[5],
                                                type, frame, date, mu);
            }
        }

        /** {@inheritDoc} */
        @Override
        public <T extends CalculusFieldElement<T>> FieldCircularOrbit<T> convertToFieldOrbit(final Field<T> field,
                                                                                             final Orbit orbit) {
            return new FieldCircularOrbit<>(field, CIRCULAR.convertType(orbit));
        }

        /** {@inheritDoc} */
        @Override
        public ParameterDriversList getDrivers(final double dP, final Orbit orbit, final PositionAngleType type) {
            final ParameterDriversList drivers = new ParameterDriversList();
            final double[] array = new double[6];
            mapOrbitToArray(orbit, type, array, null);
            final double[] scale = scale(dP, orbit);
            final String name = type == PositionAngleType.MEAN ?
                                    MEAN_LAT_ARG :
                                    type == PositionAngleType.ECCENTRIC ? ECC_LAT_ARG : TRUE_LAT_ARG;
            drivers.add(new ParameterDriver(A,    array[0], scale[0],  0.0, Double.POSITIVE_INFINITY));
            drivers.add(new ParameterDriver(E_X,  array[1], scale[1], -1.0, 1.0));
            drivers.add(new ParameterDriver(E_Y,  array[2], scale[2], -1.0, 1.0));
            drivers.add(new ParameterDriver(INC,  array[3], scale[3],  0.0, FastMath.PI));
            drivers.add(new ParameterDriver(RAAN, array[4], scale[4], Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY));
            drivers.add(new ParameterDriver(name, array[5], scale[5], Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY));
            return drivers;
        }

        /** {@inheritDoc} */
        @Override
        public CircularOrbit normalize(final Orbit orbit, final Orbit reference) {

            // convert input to proper type
            final CircularOrbit cO = convertType(orbit);
            final CircularOrbit cR = convertType(reference);

            // perform normalization
            if (cO.hasDerivatives()) {
                return new CircularOrbit(cO.getA(),
                                         cO.getCircularEx(),
                                         cO.getCircularEy(),
                                         cO.getI(),
                                         MathUtils.normalizeAngle(cO.getRightAscensionOfAscendingNode(), cR.getRightAscensionOfAscendingNode()),
                                         MathUtils.normalizeAngle(cO.getAlphaV(), cR.getAlphaV()),
                                         cO.getADot(),
                                         cO.getCircularExDot(),
                                         cO.getCircularEyDot(),
                                         cO.getIDot(),
                                         cO.getRightAscensionOfAscendingNodeDot(),
                                         cO.getAlphaVDot(),
                                         PositionAngleType.TRUE,
                                         cO.getFrame(),
                                         cO.getDate(),
                                         cO.getMu());
            } else {
                return new CircularOrbit(cO.getA(),
                                         cO.getCircularEx(),
                                         cO.getCircularEy(),
                                         cO.getI(),
                                         MathUtils.normalizeAngle(cO.getRightAscensionOfAscendingNode(), cR.getRightAscensionOfAscendingNode()),
                                         MathUtils.normalizeAngle(cO.getAlphaV(), cR.getAlphaV()),
                                         PositionAngleType.TRUE,
                                         cO.getFrame(),
                                         cO.getDate(),
                                         cO.getMu());
            }

        }

        /** {@inheritDoc} */
        @Override
        public <T extends CalculusFieldElement<T>> FieldCircularOrbit<T> normalize(final FieldOrbit<T> orbit, final FieldOrbit<T> reference) {

            // convert input to proper type
            final FieldCircularOrbit<T> cO = convertType(orbit);
            final FieldCircularOrbit<T> cR = convertType(reference);

            // perform normalization
            if (cO.hasDerivatives()) {
                return new FieldCircularOrbit<>(cO.getA(),
                                                cO.getCircularEx(),
                                                cO.getCircularEy(),
                                                cO.getI(),
                                                MathUtils.normalizeAngle(cO.getRightAscensionOfAscendingNode(), cR.getRightAscensionOfAscendingNode()),
                                                MathUtils.normalizeAngle(cO.getAlphaV(), cR.getAlphaV()),
                                                cO.getADot(),
                                                cO.getCircularExDot(),
                                                cO.getCircularEyDot(),
                                                cO.getIDot(),
                                                cO.getRightAscensionOfAscendingNodeDot(),
                                                cO.getAlphaVDot(),
                                                PositionAngleType.TRUE,
                                                cO.getFrame(),
                                                cO.getDate(),
                                                cO.getMu());
            } else {
                return new FieldCircularOrbit<>(cO.getA(),
                                                cO.getCircularEx(),
                                                cO.getCircularEy(),
                                                cO.getI(),
                                                MathUtils.normalizeAngle(cO.getRightAscensionOfAscendingNode(), cR.getRightAscensionOfAscendingNode()),
                                                MathUtils.normalizeAngle(cO.getAlphaV(), cR.getAlphaV()),
                                                PositionAngleType.TRUE,
                                                cO.getFrame(),
                                                cO.getDate(),
                                                cO.getMu());
            }

        }

        /** {@inheritDoc} */
        @Override
        public boolean isPositionAngleBased() {
            return true;
        }

    },

    /** Type for orbital representation in {@link EquinoctialOrbit} and {@link FieldEquinoctialOrbit} parameters. */
    EQUINOCTIAL {

        /** {@inheritDoc} */
        @Override
        public EquinoctialOrbit convertType(final Orbit orbit) {
            return (orbit.getType() == this) ? (EquinoctialOrbit) orbit : new EquinoctialOrbit(orbit);
        }

        /** {@inheritDoc} */
        @Override
        public void mapOrbitToArray(final Orbit orbit, final PositionAngleType type,
                                    final double[] stateVector, final double[] stateVectorDot) {

            final EquinoctialOrbit equinoctialOrbit =
                (EquinoctialOrbit) OrbitType.EQUINOCTIAL.convertType(orbit);

            stateVector[0] = equinoctialOrbit.getA();
            stateVector[1] = equinoctialOrbit.getEquinoctialEx();
            stateVector[2] = equinoctialOrbit.getEquinoctialEy();
            stateVector[3] = equinoctialOrbit.getHx();
            stateVector[4] = equinoctialOrbit.getHy();
            stateVector[5] = equinoctialOrbit.getL(type);

            if (stateVectorDot != null) {
                if (orbit.hasDerivatives()) {
                    stateVectorDot[0] = equinoctialOrbit.getADot();
                    stateVectorDot[1] = equinoctialOrbit.getEquinoctialExDot();
                    stateVectorDot[2] = equinoctialOrbit.getEquinoctialEyDot();
                    stateVectorDot[3] = equinoctialOrbit.getHxDot();
                    stateVectorDot[4] = equinoctialOrbit.getHyDot();
                    stateVectorDot[5] = equinoctialOrbit.getLDot(type);
                } else {
                    Arrays.fill(stateVectorDot, 0, 6, Double.NaN);
                }
            }

        }

        /** {@inheritDoc} */
        @Override
        public EquinoctialOrbit mapArrayToOrbit(final double[] stateVector, final double[] stateVectorDot, final PositionAngleType type,
                                                final AbsoluteDate date, final double mu, final Frame frame) {
            if (stateVectorDot == null) {
                // we don't have orbit derivatives
                return new EquinoctialOrbit(stateVector[0], stateVector[1], stateVector[2],
                                            stateVector[3], stateVector[4], stateVector[5],
                                            type, frame, date, mu);
            } else {
                // we have orbit derivatives
                return new EquinoctialOrbit(stateVector[0],    stateVector[1],    stateVector[2],
                                            stateVector[3],    stateVector[4],    stateVector[5],
                                            stateVectorDot[0], stateVectorDot[1], stateVectorDot[2],
                                            stateVectorDot[3], stateVectorDot[4], stateVectorDot[5],
                                            type, frame, date, mu);
            }
        }

        /** {@inheritDoc} */
        @Override
        public <T extends CalculusFieldElement<T>> FieldEquinoctialOrbit<T> convertType(final FieldOrbit<T> orbit) {
            return (orbit.getType() == this) ? (FieldEquinoctialOrbit<T>) orbit : new FieldEquinoctialOrbit<>(orbit);
        }

        /** {@inheritDoc} */
        @Override
        public <T extends CalculusFieldElement<T>> void mapOrbitToArray(final FieldOrbit<T> orbit,
                                                                        final PositionAngleType type,
                                                                        final T[] stateVector,
                                                                        final T[] stateVectorDot) {

            final FieldEquinoctialOrbit<T> equinoctialOrbit =
                (FieldEquinoctialOrbit<T>) OrbitType.EQUINOCTIAL.convertType(orbit);

            stateVector[0] = equinoctialOrbit.getA();
            stateVector[1] = equinoctialOrbit.getEquinoctialEx();
            stateVector[2] = equinoctialOrbit.getEquinoctialEy();
            stateVector[3] = equinoctialOrbit.getHx();
            stateVector[4] = equinoctialOrbit.getHy();
            stateVector[5] = equinoctialOrbit.getL(type);

            if (stateVectorDot != null) {
                if (orbit.hasDerivatives()) {
                    stateVectorDot[0] = equinoctialOrbit.getADot();
                    stateVectorDot[1] = equinoctialOrbit.getEquinoctialExDot();
                    stateVectorDot[2] = equinoctialOrbit.getEquinoctialEyDot();
                    stateVectorDot[3] = equinoctialOrbit.getHxDot();
                    stateVectorDot[4] = equinoctialOrbit.getHyDot();
                    stateVectorDot[5] = equinoctialOrbit.getLDot(type);
                } else {
                    Arrays.fill(stateVectorDot, 0, 6, orbit.getZero().add(Double.NaN));
                }
            }

        }

        /** {@inheritDoc} */
        @Override
        public <T extends CalculusFieldElement<T>> FieldEquinoctialOrbit<T> mapArrayToOrbit(final T[] stateVector,
                                                                                            final T[] stateVectorDot,
                                                                                            final PositionAngleType type,
                                                                                            final FieldAbsoluteDate<T> date,
                                                                                            final T mu, final Frame frame) {
            if (stateVectorDot == null) {
                // we don't have orbit derivatives
                return new FieldEquinoctialOrbit<>(stateVector[0], stateVector[1], stateVector[2],
                                                   stateVector[3], stateVector[4], stateVector[5],
                                                   type, frame, date, mu);
            } else {
                // we have orbit derivatives
                return new FieldEquinoctialOrbit<>(stateVector[0],    stateVector[1],    stateVector[2],
                                                   stateVector[3],    stateVector[4],    stateVector[5],
                                                   stateVectorDot[0], stateVectorDot[1], stateVectorDot[2],
                                                   stateVectorDot[3], stateVectorDot[4], stateVectorDot[5],
                                                   type, frame, date, mu);
            }
        }

        /** {@inheritDoc} */
        @Override
        public <T extends CalculusFieldElement<T>> FieldEquinoctialOrbit<T> convertToFieldOrbit(final Field<T> field,
                                                                                                final Orbit orbit) {
            return new FieldEquinoctialOrbit<>(field, EQUINOCTIAL.convertType(orbit));
        }

        /** {@inheritDoc} */
        @Override
        public ParameterDriversList getDrivers(final double dP, final Orbit orbit, final PositionAngleType type) {
            final ParameterDriversList drivers = new ParameterDriversList();
            final double[] array = new double[6];
            mapOrbitToArray(orbit, type, array, null);
            final double[] scale = scale(dP, orbit);
            final String name = type == PositionAngleType.MEAN ?
                                    MEAN_LON_ARG :
                                    type == PositionAngleType.ECCENTRIC ? ECC_LON_ARG : TRUE_LON_ARG;
            drivers.add(new ParameterDriver(A,    array[0], scale[0],  0.0, Double.POSITIVE_INFINITY));
            drivers.add(new ParameterDriver(E_X,  array[1], scale[1], -1.0, 1.0));
            drivers.add(new ParameterDriver(E_Y,  array[2], scale[2], -1.0, 1.0));
            drivers.add(new ParameterDriver(H_X,  array[3], scale[3], Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY));
            drivers.add(new ParameterDriver(H_Y,  array[4], scale[4], Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY));
            drivers.add(new ParameterDriver(name, array[5], scale[5], Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY));
            return drivers;
        }

        /** {@inheritDoc} */
        @Override
        public EquinoctialOrbit normalize(final Orbit orbit, final Orbit reference) {

            // convert input to proper type
            final EquinoctialOrbit eO = convertType(orbit);
            final EquinoctialOrbit eR = convertType(reference);

            // perform normalization
            if (eO.hasDerivatives()) {
                return new EquinoctialOrbit(eO.getA(),
                                            eO.getEquinoctialEx(),
                                            eO.getEquinoctialEy(),
                                            eO.getHx(),
                                            eO.getHy(),
                                            MathUtils.normalizeAngle(eO.getLv(), eR.getLv()),
                                            eO.getADot(),
                                            eO.getEquinoctialExDot(),
                                            eO.getEquinoctialEyDot(),
                                            eO.getHxDot(),
                                            eO.getHyDot(),
                                            eO.getLvDot(),
                                            PositionAngleType.TRUE,
                                            eO.getFrame(),
                                            eO.getDate(),
                                            eO.getMu());
            } else {
                return new EquinoctialOrbit(eO.getA(),
                                            eO.getEquinoctialEx(),
                                            eO.getEquinoctialEy(),
                                            eO.getHx(),
                                            eO.getHy(),
                                            MathUtils.normalizeAngle(eO.getLv(), eR.getLv()),
                                            PositionAngleType.TRUE,
                                            eO.getFrame(),
                                            eO.getDate(),
                                            eO.getMu());
            }

        }

        /** {@inheritDoc} */
        @Override
        public <T extends CalculusFieldElement<T>> FieldEquinoctialOrbit<T> normalize(final FieldOrbit<T> orbit, final FieldOrbit<T> reference) {

            // convert input to proper type
            final FieldEquinoctialOrbit<T> eO = convertType(orbit);
            final FieldEquinoctialOrbit<T> eR = convertType(reference);

            // perform normalization
            if (eO.hasDerivatives()) {
                return new FieldEquinoctialOrbit<>(eO.getA(),
                                                   eO.getEquinoctialEx(),
                                                   eO.getEquinoctialEy(),
                                                   eO.getHx(),
                                                   eO.getHy(),
                                                   MathUtils.normalizeAngle(eO.getLv(), eR.getLv()),
                                                   eO.getADot(),
                                                   eO.getEquinoctialExDot(),
                                                   eO.getEquinoctialEyDot(),
                                                   eO.getHxDot(),
                                                   eO.getHyDot(),
                                                   eO.getLvDot(),
                                                   PositionAngleType.TRUE,
                                                   eO.getFrame(),
                                                   eO.getDate(),
                                                   eO.getMu());
            } else {
                return new FieldEquinoctialOrbit<>(eO.getA(),
                                                   eO.getEquinoctialEx(),
                                                   eO.getEquinoctialEy(),
                                                   eO.getHx(),
                                                   eO.getHy(),
                                                   MathUtils.normalizeAngle(eO.getLv(), eR.getLv()),
                                                   PositionAngleType.TRUE,
                                                   eO.getFrame(),
                                                   eO.getDate(),
                                                   eO.getMu());
            }

        }

        /** {@inheritDoc} */
        @Override
        public boolean isPositionAngleBased() {
            return true;
        }

    },

    /** Type for orbital representation in {@link KeplerianOrbit} and {@link FieldKeplerianOrbit} parameters. */
    KEPLERIAN {

        /** {@inheritDoc} */
        @Override
        public KeplerianOrbit convertType(final Orbit orbit) {
            return (orbit.getType() == this) ? (KeplerianOrbit) orbit : new KeplerianOrbit(orbit);
        }

        /** {@inheritDoc} */
        @Override
        public void mapOrbitToArray(final Orbit orbit, final PositionAngleType type,
                                    final double[] stateVector, final double[] stateVectorDot) {

            final KeplerianOrbit keplerianOrbit =
                (KeplerianOrbit) OrbitType.KEPLERIAN.convertType(orbit);

            stateVector[0] = keplerianOrbit.getA();
            stateVector[1] = keplerianOrbit.getE();
            stateVector[2] = keplerianOrbit.getI();
            stateVector[3] = keplerianOrbit.getPerigeeArgument();
            stateVector[4] = keplerianOrbit.getRightAscensionOfAscendingNode();
            stateVector[5] = keplerianOrbit.getAnomaly(type);

            if (stateVectorDot != null) {
                if (orbit.hasDerivatives()) {
                    stateVectorDot[0] = keplerianOrbit.getADot();
                    stateVectorDot[1] = keplerianOrbit.getEDot();
                    stateVectorDot[2] = keplerianOrbit.getIDot();
                    stateVectorDot[3] = keplerianOrbit.getPerigeeArgumentDot();
                    stateVectorDot[4] = keplerianOrbit.getRightAscensionOfAscendingNodeDot();
                    stateVectorDot[5] = keplerianOrbit.getAnomalyDot(type);
                } else {
                    Arrays.fill(stateVectorDot, 0, 6, Double.NaN);
                }
            }

        }

        /** {@inheritDoc} */
        @Override
        public KeplerianOrbit mapArrayToOrbit(final double[] stateVector, final double[] stateVectorDot, final PositionAngleType type,
                                              final AbsoluteDate date, final double mu, final Frame frame) {
            if (stateVectorDot == null) {
                // we don't have orbit derivatives
                return new KeplerianOrbit(stateVector[0], stateVector[1], stateVector[2],
                                          stateVector[3], stateVector[4], stateVector[5],
                                          type, frame, date, mu);
            } else {
                // we have orbit derivatives
                return new KeplerianOrbit(stateVector[0],    stateVector[1],    stateVector[2],
                                          stateVector[3],    stateVector[4],    stateVector[5],
                                          stateVectorDot[0], stateVectorDot[1], stateVectorDot[2],
                                          stateVectorDot[3], stateVectorDot[4], stateVectorDot[5],
                                          type, frame, date, mu);
            }
        }

        /** {@inheritDoc} */
        @Override
        public <T extends CalculusFieldElement<T>> FieldKeplerianOrbit<T> convertType(final FieldOrbit<T> orbit) {
            return (orbit.getType() == this) ? (FieldKeplerianOrbit<T>) orbit : new FieldKeplerianOrbit<>(orbit);
        }

        /** {@inheritDoc} */
        @Override
        public <T extends CalculusFieldElement<T>> void mapOrbitToArray(final FieldOrbit<T> orbit,
                                                                        final PositionAngleType type,
                                                                        final T[] stateVector,
                                                                        final T[] stateVectorDot) {
            final FieldKeplerianOrbit<T> keplerianOrbit =
                            (FieldKeplerianOrbit<T>) OrbitType.KEPLERIAN.convertType(orbit);

            stateVector[0] = keplerianOrbit.getA();
            stateVector[1] = keplerianOrbit.getE();
            stateVector[2] = keplerianOrbit.getI();
            stateVector[3] = keplerianOrbit.getPerigeeArgument();
            stateVector[4] = keplerianOrbit.getRightAscensionOfAscendingNode();
            stateVector[5] = keplerianOrbit.getAnomaly(type);

            if (stateVectorDot != null) {
                if (orbit.hasDerivatives()) {
                    stateVectorDot[0] = keplerianOrbit.getADot();
                    stateVectorDot[1] = keplerianOrbit.getEDot();
                    stateVectorDot[2] = keplerianOrbit.getIDot();
                    stateVectorDot[3] = keplerianOrbit.getPerigeeArgumentDot();
                    stateVectorDot[4] = keplerianOrbit.getRightAscensionOfAscendingNodeDot();
                    stateVectorDot[5] = keplerianOrbit.getAnomalyDot(type);
                } else {
                    Arrays.fill(stateVectorDot, 0, 6, orbit.getZero().add(Double.NaN));
                }
            }

        }

        /** {@inheritDoc} */
        @Override
        public <T extends CalculusFieldElement<T>> FieldKeplerianOrbit<T> mapArrayToOrbit(final T[] stateVector,
                                                                                          final T[] stateVectorDot,
                                                                                          final PositionAngleType type,
                                                                                          final FieldAbsoluteDate<T> date,
                                                                                          final T mu, final Frame frame) {
            if (stateVectorDot == null) {
                // we don't have orbit derivatives
                return new FieldKeplerianOrbit<>(stateVector[0], stateVector[1], stateVector[2],
                                                 stateVector[3], stateVector[4], stateVector[5],
                                                 type, frame, date, mu);
            } else {
                // we have orbit derivatives
                return new FieldKeplerianOrbit<>(stateVector[0],    stateVector[1],    stateVector[2],
                                                 stateVector[3],    stateVector[4],    stateVector[5],
                                                 stateVectorDot[0], stateVectorDot[1], stateVectorDot[2],
                                                 stateVectorDot[3], stateVectorDot[4], stateVectorDot[5],
                                                 type, frame, date, mu);
            }
        }

        /** {@inheritDoc} */
        @Override
        public <T extends CalculusFieldElement<T>> FieldKeplerianOrbit<T> convertToFieldOrbit(final Field<T> field,
                                                                                              final Orbit orbit) {
            return new FieldKeplerianOrbit<>(field, KEPLERIAN.convertType(orbit));
        }

        /** {@inheritDoc} */
        @Override
        public ParameterDriversList getDrivers(final double dP, final Orbit orbit, final PositionAngleType type) {
            final ParameterDriversList drivers = new ParameterDriversList();
            final double[] array = new double[6];
            mapOrbitToArray(orbit, type, array, null);
            final double[] scale = scale(dP, orbit);
            final String name = type == PositionAngleType.MEAN ?
                                    MEAN_ANOM :
                                    type == PositionAngleType.ECCENTRIC ? ECC_ANOM : TRUE_ANOM;
            drivers.add(new ParameterDriver(A,    array[0], scale[0],  0.0, Double.POSITIVE_INFINITY));
            drivers.add(new ParameterDriver(ECC,  array[1], scale[1],  0.0, 1.0));
            drivers.add(new ParameterDriver(INC,  array[2], scale[2],  0.0, FastMath.PI));
            drivers.add(new ParameterDriver(PA,   array[3], scale[3], Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY));
            drivers.add(new ParameterDriver(RAAN, array[4], scale[4], Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY));
            drivers.add(new ParameterDriver(name, array[5], scale[5], Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY));
            return drivers;
        }

        /** {@inheritDoc} */
        @Override
        public KeplerianOrbit normalize(final Orbit orbit, final Orbit reference) {

            // convert input to proper type
            final KeplerianOrbit kO = convertType(orbit);
            final KeplerianOrbit kR = convertType(reference);

            // perform normalization
            if (kO.hasDerivatives()) {
                return new KeplerianOrbit(kO.getA(),
                                          kO.getE(),
                                          kO.getI(),
                                          MathUtils.normalizeAngle(kO.getPerigeeArgument(), kR.getPerigeeArgument()),
                                          MathUtils.normalizeAngle(kO.getRightAscensionOfAscendingNode(), kR.getRightAscensionOfAscendingNode()),
                                          MathUtils.normalizeAngle(kO.getTrueAnomaly(), kR.getTrueAnomaly()),
                                          kO.getADot(),
                                          kO.getEDot(),
                                          kO.getIDot(),
                                          kO.getPerigeeArgumentDot(),
                                          kO.getRightAscensionOfAscendingNodeDot(),
                                          kO.getTrueAnomalyDot(),
                                          PositionAngleType.TRUE,
                                          kO.getFrame(),
                                          kO.getDate(),
                                          kO.getMu());
            } else {
                return new KeplerianOrbit(kO.getA(),
                                          kO.getE(),
                                          kO.getI(),
                                          MathUtils.normalizeAngle(kO.getPerigeeArgument(), kR.getPerigeeArgument()),
                                          MathUtils.normalizeAngle(kO.getRightAscensionOfAscendingNode(), kR.getRightAscensionOfAscendingNode()),
                                          MathUtils.normalizeAngle(kO.getTrueAnomaly(), kR.getTrueAnomaly()),
                                          PositionAngleType.TRUE,
                                          kO.getFrame(),
                                          kO.getDate(),
                                          kO.getMu());
            }

        }

        /** {@inheritDoc} */
        @Override
        public <T extends CalculusFieldElement<T>> FieldKeplerianOrbit<T> normalize(final FieldOrbit<T> orbit, final FieldOrbit<T> reference) {

            // convert input to proper type
            final FieldKeplerianOrbit<T> kO = convertType(orbit);
            final FieldKeplerianOrbit<T> kR = convertType(reference);

            // perform normalization
            if (kO.hasDerivatives()) {
                return new FieldKeplerianOrbit<>(kO.getA(),
                                                 kO.getE(),
                                                 kO.getI(),
                                                 MathUtils.normalizeAngle(kO.getPerigeeArgument(), kR.getPerigeeArgument()),
                                                 MathUtils.normalizeAngle(kO.getRightAscensionOfAscendingNode(), kR.getRightAscensionOfAscendingNode()),
                                                 MathUtils.normalizeAngle(kO.getTrueAnomaly(), kR.getTrueAnomaly()),
                                                 kO.getADot(),
                                                 kO.getEDot(),
                                                 kO.getIDot(),
                                                 kO.getPerigeeArgumentDot(),
                                                 kO.getRightAscensionOfAscendingNodeDot(),
                                                 kO.getTrueAnomalyDot(),
                                                 PositionAngleType.TRUE,
                                                 kO.getFrame(),
                                                 kO.getDate(),
                                                 kO.getMu());
            } else {
                return new FieldKeplerianOrbit<>(kO.getA(),
                                                 kO.getE(),
                                                 kO.getI(),
                                                 MathUtils.normalizeAngle(kO.getPerigeeArgument(), kR.getPerigeeArgument()),
                                                 MathUtils.normalizeAngle(kO.getRightAscensionOfAscendingNode(), kR.getRightAscensionOfAscendingNode()),
                                                 MathUtils.normalizeAngle(kO.getTrueAnomaly(), kR.getTrueAnomaly()),
                                                 PositionAngleType.TRUE,
                                                 kO.getFrame(),
                                                 kO.getDate(),
                                                 kO.getMu());
            }

        }

        /** {@inheritDoc} */
        @Override
        public boolean isPositionAngleBased() {
            return true;
        }

    };

    /** Name for position along X. */
    public static final String POS_X = "Px";

    /** Name for position along Y. */
    public static final String POS_Y = "Py";

    /** Name for position along Z. */
    public static final String POS_Z = "Pz";

    /** Name for velocity along X. */
    public static final String VEL_X = "Vx";

    /** Name for velocity along Y. */
    public static final String VEL_Y = "Vy";

    /** Name for velocity along Z. */
    public static final String VEL_Z = "Vz";

    /** Name for semi major axis. */
    public static final String A     = "a";

    /** Name for eccentricity. */
    public static final String ECC   = "e";

    /** Name for eccentricity vector first component. */
    public static final String E_X   = "ex";

    /** Name for eccentricity vector second component. */
    public static final String E_Y   = "ey";

    /** Name for inclination. */
    public static final String INC   = "i";

    /** Name for inclination vector first component. */
    public static final String H_X   = "hx";

    /** Name for inclination vector second component . */
    public static final String H_Y   = "hy";

    /** Name for perigee argument. */
    public static final String PA    = "ω";

    /** Name for right ascension of ascending node. */
    public static final String RAAN    = "Ω";

    /** Name for mean anomaly. */
    public static final String MEAN_ANOM = "M";

    /** Name for eccentric anomaly. */
    public static final String ECC_ANOM  = "E";

    /** Name for mean anomaly. */
    public static final String TRUE_ANOM = "v";

    /** Name for mean argument of latitude. */
    public static final String MEAN_LAT_ARG = "αM";

    /** Name for eccentric argument of latitude. */
    public static final String ECC_LAT_ARG  = "αE";

    /** Name for mean argument of latitude. */
    public static final String TRUE_LAT_ARG = "αv";

    /** Name for mean argument of longitude. */
    public static final String MEAN_LON_ARG = "λM";

    /** Name for eccentric argument of longitude. */
    public static final String ECC_LON_ARG  = "λE";

    /** Name for mean argument of longitude. */
    public static final String TRUE_LON_ARG = "λv";

    /** Convert an orbit to the instance type.
     * <p>
     * The returned orbit is the specified instance itself if its type already matches,
     * otherwise, a new orbit of the proper type created
     * </p>
     * @param orbit orbit to convert
     * @return converted orbit with type guaranteed to match (so it can be cast safely)
     */
    public abstract Orbit convertType(Orbit orbit);

    /** Convert orbit to state array.
     * <p>
     * Note that all implementations of this method <em>must</em> be consistent with the
     * implementation of the {@link org.orekit.orbits.Orbit#getJacobianWrtCartesian(
     * PositionAngleType, double[][]) Orbit.getJacobianWrtCartesian}
     * method for the corresponding orbit type in terms of parameters order and meaning.
     * </p>
     * @param orbit orbit to map
     * @param type type of the angle
     * @param stateVector flat array into which the state vector should be mapped
     * (it can have more than 6 elements, extra elements are untouched)
     * @param stateVectorDot flat array into which the state vector derivative should be mapped
     * (it can be null if derivatives are not desired, and it can have more than 6 elements, extra elements are untouched)
     */
    public abstract void mapOrbitToArray(Orbit orbit, PositionAngleType type, double[] stateVector, double[] stateVectorDot);

     /** Convert state array to orbital parameters.
     * <p>
     * Note that all implementations of this method <em>must</em> be consistent with the
     * implementation of the {@link org.orekit.orbits.Orbit#getJacobianWrtCartesian(
     * PositionAngleType, double[][]) Orbit.getJacobianWrtCartesian}
     * method for the corresponding orbit type in terms of parameters order and meaning.
     * </p>
     * @param array state as a flat array
     * (it can have more than 6 elements, extra elements are ignored)
     * @param arrayDot state derivative as a flat array
     * (it can be null, in which case Keplerian motion is assumed,
     * and it can have more than 6 elements, extra elements are ignored)
     * @param type type of the angle
     * @param date integration date
     * @param mu central attraction coefficient used for propagation (m³/s²)
     * @param frame frame in which integration is performed
     * @return orbit corresponding to the flat array as a space dynamics object
     */
    public abstract Orbit mapArrayToOrbit(double[] array, double[] arrayDot, PositionAngleType type,
                                          AbsoluteDate date, double mu, Frame frame);

    /** Convert an orbit to the instance type.
     * <p>
     * The returned orbit is the specified instance itself if its type already matches,
     * otherwise, a new orbit of the proper type created
     * </p>
     * @param <T> CalculusFieldElement used
     * @param orbit orbit to convert
     * @return converted orbit with type guaranteed to match (so it can be cast safely)
     */
    public abstract <T extends CalculusFieldElement<T>> FieldOrbit<T> convertType(FieldOrbit<T> orbit);

    /** Convert orbit to state array.
     * <p>
     * Note that all implementations of this method <em>must</em> be consistent with the
     * implementation of the {@link org.orekit.orbits.Orbit#getJacobianWrtCartesian(
     * PositionAngleType, double[][]) Orbit.getJacobianWrtCartesian}
     * method for the corresponding orbit type in terms of parameters order and meaning.
     * </p>
     * @param <T> CalculusFieldElement used
     * @param orbit orbit to map
     * @param type type of the angle
     * @param stateVector flat array into which the state vector should be mapped
     * (it can have more than 6 elements, extra elements are untouched)
     * @param stateVectorDot flat array into which the state vector derivative should be mapped
     * (it can be null if derivatives are not desired, and it can have more than 6 elements, extra elements are untouched)
     */
    public abstract <T extends CalculusFieldElement<T>>void mapOrbitToArray(FieldOrbit<T> orbit, PositionAngleType type,
                                                                            T[] stateVector, T[] stateVectorDot);


    /** Convert state array to orbital parameters.
     * <p>
     * Note that all implementations of this method <em>must</em> be consistent with the
     * implementation of the {@link org.orekit.orbits.Orbit#getJacobianWrtCartesian(
     * PositionAngleType, double[][]) Orbit.getJacobianWrtCartesian}
     * method for the corresponding orbit type in terms of parameters order and meaning.
     * </p>
     * @param <T> CalculusFieldElement used
     * @param array state as a flat array
     * (it can have more than 6 elements, extra elements are ignored)
     * @param arrayDot state derivative as a flat array
     * (it can be null, in which case Keplerian motion is assumed,
     * @param type type of the angle
     * @param date integration date
     * @param mu central attraction coefficient used for propagation (m³/s²)
     * @param frame frame in which integration is performed
     * @return orbit corresponding to the flat array as a space dynamics object
     */
    public abstract <T extends CalculusFieldElement<T>> FieldOrbit<T> mapArrayToOrbit(T[] array,
                                                                                      T[] arrayDot,
                                                                                      PositionAngleType type,
                                                                                      FieldAbsoluteDate<T> date,
                                                                                      T mu, Frame frame);

    /** Convert an orbit to the "Fielded" instance type.
     * @param <T> CalculusFieldElement used
     * @param field CalculusField
     * @param orbit base orbit
     * @return converted FieldOrbit with type guaranteed to match (so it can be cast safely)
     * @since 12.0
     */
    public abstract <T extends CalculusFieldElement<T>> FieldOrbit<T> convertToFieldOrbit(Field<T> field,
                                                                                           Orbit orbit);

    /** Get parameters drivers initialized from a reference orbit.
     * @param dP user specified position error
     * @param orbit reference orbit
     * @param type type of the angle
     * @return parameters drivers initialized from reference orbit
     */
    public abstract ParameterDriversList getDrivers(double dP, Orbit orbit,
                                                    PositionAngleType type);

    /** Normalize one orbit with respect to a reference one.
     * <p>
     * Given a, angular component ζ of an orbit and the corresponding
     * angular component ζᵣ in the reference orbit, the angular component
     * ζₙ of the normalized orbit will be ζₙ = ζ + 2kπ
     * where k is chosen such that ζᵣ - π ≤ ζₙ ≤ ζᵣ + π. This is intended
     * to avoid too large discontinuities and is particularly useful
     * for normalizing the orbit after an impulsive maneuver with respect
     * to the reference picked up before the maneuver.
     * </p>
     * @param <T> CalculusFieldElement used
     * @param orbit orbit to normalize
     * @param reference reference orbit
     * @return normalized orbit (the type is guaranteed to match {@link OrbitType})
     * @since 11.1
     */
    public abstract <T extends CalculusFieldElement<T>> FieldOrbit<T> normalize(FieldOrbit<T> orbit,
                                                                                FieldOrbit<T> reference);

    /** Normalize one orbit with respect to a reference one.
     * <p>
     * Given a, angular component ζ of an orbit and the corresponding
     * angular component ζᵣ in the reference orbit, the angular component
     * ζₙ of the normalized orbit will be ζₙ = ζ + 2kπ
     * where k is chosen such that ζᵣ - π ≤ ζₙ ≤ ζᵣ + π. This is intended
     * to avoid too large discontinuities and is particularly useful
     * for normalizing the orbit after an impulsive maneuver with respect
     * to the reference picked up before the maneuver.
     * </p>
     * @param orbit orbit to normalize
     * @param reference reference orbit
     * @return normalized orbit (the type is guaranteed to match {@link OrbitType})
     * @since 11.1
     */
    public abstract Orbit normalize(Orbit orbit, Orbit reference);

    /** Tells if the orbit type is based on position angles or not.
     * @return true if based on {@link PositionAngleType}
     * @since 12.0
     */
    public abstract boolean isPositionAngleBased();

    /** Compute scaling factor for parameters drivers.
     * <p>
     * The scales are estimated from partial derivatives properties of orbits,
     * starting from a scalar position error specified by the user.
     * Considering the energy conservation equation V = sqrt(mu (2/r - 1/a)),
     * we get at constant energy (i.e. on a Keplerian trajectory):
     * <pre>
     * V r² |dV| = mu |dr|
     * </pre>
     * <p> So we deduce a scalar velocity error consistent with the position error.
     * From here, we apply orbits Jacobians matrices to get consistent scales
     * on orbital parameters.
     *
     * @param dP user specified position error
     * @param orbit reference orbit
     * @return scaling factor array
     */
    protected double[] scale(final double dP, final Orbit orbit) {

        // estimate the scalar velocity error
        final PVCoordinates pv = orbit.getPVCoordinates();
        final double r2 = pv.getPosition().getNormSq();
        final double v  = pv.getVelocity().getNorm();
        final double dV = orbit.getMu() * dP / (v * r2);

        final double[] scale = new double[6];

        // convert the orbit to the desired type
        final double[][] jacobian = new double[6][6];
        final Orbit converted = convertType(orbit);
        converted.getJacobianWrtCartesian(PositionAngleType.TRUE, jacobian);

        for (int i = 0; i < 6; ++i) {
            final double[] row = jacobian[i];
            scale[i] = FastMath.abs(row[0]) * dP +
                       FastMath.abs(row[1]) * dP +
                       FastMath.abs(row[2]) * dP +
                       FastMath.abs(row[3]) * dV +
                       FastMath.abs(row[4]) * dV +
                       FastMath.abs(row[5]) * dV;
            if (Double.isNaN(scale[i])) {
                throw new OrekitException(OrekitMessages.SINGULAR_JACOBIAN_FOR_ORBIT_TYPE, this);
            }
        }

        return scale;

    }

}
