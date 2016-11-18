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
package org.orekit.orbits;

import org.hipparchus.Field;
import org.hipparchus.RealFieldElement;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.orekit.frames.Frame;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.utils.FieldPVCoordinates;
import org.orekit.utils.TimeStampedFieldPVCoordinates;

/** Enumerate for {@link Orbit orbital} parameters types.
 */
public enum FieldOrbitType {

    /** Type for propagation in {@link CartesianOrbit Cartesian parameters}. */
    CARTESIAN {

        /** {@inheritDoc} */
        public <T extends RealFieldElement<T>> FieldOrbit<T> convertType(final FieldOrbit<T> orbit) {
            return (orbit.getType() == this) ? orbit : new FieldCartesianOrbit<T>(orbit);
        }

        /** {@inheritDoc} */
        public <T extends RealFieldElement<T>> void mapOrbitToArray(final FieldOrbit<T> orbit, final PositionAngle type,
                                    final T[] stateVector) {

            final TimeStampedFieldPVCoordinates<T> pv = orbit.getFieldPVCoordinates();
            final FieldVector3D<T>      p  = pv.getPosition();
            final FieldVector3D<T>      v  = pv.getVelocity();

            stateVector[0] = p.getX();
            stateVector[1] = p.getY();
            stateVector[2] = p.getZ();
            stateVector[3] = v.getX();
            stateVector[4] = v.getY();
            stateVector[5] = v.getZ();

        }

        /** {@inheritDoc} */
        public <T extends RealFieldElement<T>> FieldOrbit<T> mapArrayToOrbit(final T[] stateVector, final PositionAngle type,
                                     final FieldAbsoluteDate<T> date, final double mu, final Frame frame) {
            final T zero = stateVector[0].getField().getZero();
            final FieldVector3D<T> p     = new FieldVector3D<T>(stateVector[0], stateVector[1], stateVector[2]);
            final T r2      = p.getNormSq();
            final FieldVector3D<T> v     = new FieldVector3D<T>(stateVector[3], stateVector[4], stateVector[5]);
            final FieldVector3D<T> a     = new FieldVector3D<T>(zero.add(-mu).divide(r2.sqrt().multiply(r2)), p);
            return new FieldCartesianOrbit<T>(new FieldPVCoordinates<T>(p, v, a), frame, date, mu);

        }

        @Override
        public Orbit convertType(final Orbit orbit) {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public void mapOrbitToArray(final Orbit orbit, final PositionAngle type,
                                    final double[] stateVector) {
            // TODO Auto-generated method stub
        }

        @Override
        public <T extends RealFieldElement<T>> FieldOrbit<T>
            mapArrayToOrbit(final double[] stateVector, final PositionAngle type,
                            final FieldAbsoluteDate<T> date, final double mu, final Frame frame,
                            final Field<T> field) {
            final T zero = field.getZero();
            final FieldVector3D<T> p     = new FieldVector3D<T>(zero.add(stateVector[0]), zero.add(stateVector[1]), zero.add(stateVector[2]));
            final T r2      = p.getNormSq();
            final FieldVector3D<T> v     = new FieldVector3D<T>(zero.add(stateVector[3]), zero.add(stateVector[4]), zero.add(stateVector[5]));
            final FieldVector3D<T> a     = new FieldVector3D<T>(zero.add(-mu).divide(r2.sqrt().multiply(r2)), p);
            return new FieldCartesianOrbit<T>(new FieldPVCoordinates<T>(p, v, a), frame, date, mu);
        }

        @Override
        protected OrbitType toOrbitType() {
            return OrbitType.CARTESIAN;
        }

    },

    /** Type for propagation in {@link CircularOrbit circular parameters}. */
    CIRCULAR {

        /** {@inheritDoc} */
        public <T extends RealFieldElement<T>> FieldOrbit<T> convertType(final FieldOrbit<T> orbit) {
            return (orbit.getType() == this) ? orbit : new FieldCircularOrbit<T>(orbit);
        }

        /** {@inheritDoc} */
        public <T extends RealFieldElement<T>> void mapOrbitToArray(final FieldOrbit<T> orbit, final PositionAngle type,
                                    final T[] stateVector) {

            final FieldCircularOrbit<T> circularOrbit = (FieldCircularOrbit<T>) FieldOrbitType.CIRCULAR.convertType(orbit);

            stateVector[0] = circularOrbit.getA();
            stateVector[1] = circularOrbit.getCircularEx();
            stateVector[2] = circularOrbit.getCircularEy();
            stateVector[3] = circularOrbit.getI();
            stateVector[4] = circularOrbit.getRightAscensionOfAscendingNode();
            stateVector[5] = circularOrbit.getAlpha(type);

        }

        /** {@inheritDoc} */
        public <T extends RealFieldElement<T>> FieldOrbit<T> mapArrayToOrbit(final T[] stateVector, final PositionAngle type,
                                     final FieldAbsoluteDate<T> date, final double mu, final Frame frame) {
            return new FieldCircularOrbit<T>(stateVector[0], stateVector[1], stateVector[2], stateVector[3],
                                     stateVector[4], stateVector[5], type,
                                     frame, date, mu);
        }

        /** {@inheritDoc} */
        public <T extends RealFieldElement<T>> FieldOrbit<T> mapArrayToOrbit(final double[] stateVector, final PositionAngle type,
                                     final FieldAbsoluteDate<T> date, final double mu, final Frame frame, final Field<T> field) {
            final T zero = field.getZero();
            return new FieldCircularOrbit<T>(zero.add(stateVector[0]),
                                             zero.add(stateVector[1]),
                                             zero.add(stateVector[2]),
                                             zero.add(stateVector[3]),
                                             zero.add(stateVector[4]),
                                             zero.add(stateVector[5]),
                                             type, frame, date, mu);
        }

        @Override
        protected OrbitType toOrbitType() {
            return OrbitType.CIRCULAR;
        }

        @Override
        public Orbit convertType(final Orbit orbit) {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public void mapOrbitToArray(final Orbit orbit, final PositionAngle type,
                                    final double[] stateVector) {
            // TODO Auto-generated method stub
        }

    },
//
    /** Type for propagation in {@link EquinoctialOrbit equinoctial parameters}. */
    EQUINOCTIAL {

        /** {@inheritDoc} */
        public <T extends RealFieldElement<T>> FieldOrbit<T> convertType(final FieldOrbit<T> orbit) {
            return (orbit.getType() == this) ? orbit : new FieldEquinoctialOrbit<T>(orbit);
        }

        /** {@inheritDoc} */
        public <T extends RealFieldElement<T>> void mapOrbitToArray(final FieldOrbit<T> orbit, final PositionAngle type,
                                    final T[] stateVector) {

            final FieldEquinoctialOrbit<T> equinoctialOrbit =
                (FieldEquinoctialOrbit<T>) FieldOrbitType.EQUINOCTIAL.convertType(orbit);

            stateVector[0] = equinoctialOrbit.getA();
            stateVector[1] = equinoctialOrbit.getEquinoctialEx();
            stateVector[2] = equinoctialOrbit.getEquinoctialEy();
            stateVector[3] = equinoctialOrbit.getHx();
            stateVector[4] = equinoctialOrbit.getHy();
            stateVector[5] = equinoctialOrbit.getL(type);

        }

        /** {@inheritDoc} */
        public <T extends RealFieldElement<T>> FieldOrbit<T> mapArrayToOrbit(final T[] stateVector, final PositionAngle type,
                                     final FieldAbsoluteDate<T> date, final double mu, final Frame frame) {
            return new FieldEquinoctialOrbit<T>(stateVector[0], stateVector[1], stateVector[2], stateVector[3],
                                        stateVector[4], stateVector[5], type,
                                        frame, date, mu);
        }

        @Override
        public Orbit convertType(final Orbit orbit) {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public void mapOrbitToArray(final Orbit orbit, final PositionAngle type,
                                    final double[] stateVector) {
            // TODO Auto-generated method stub
        }

        @Override
        public <T extends RealFieldElement<T>> FieldOrbit<T>
            mapArrayToOrbit(final double[] stateVector, final PositionAngle type,
                            final FieldAbsoluteDate<T> date, final double mu, final Frame frame,
                            final Field<T> field) {
            final T zero = field.getZero();
            return new FieldEquinoctialOrbit<T>(zero.add(stateVector[0]), zero.add(stateVector[1]), zero.add(stateVector[2]), zero.add(stateVector[3]),
                            zero.add(stateVector[4]), zero.add(stateVector[5]), type,
                            frame, date, mu);
        }

        @Override
        protected OrbitType toOrbitType() {
            return OrbitType.CIRCULAR;
        }
    },

    /** Type for propagation in {@link KeplerianOrbit Keplerian parameters}. */
    KEPLERIAN {

        /** {@inheritDoc} */
        public Orbit convertType(final Orbit orbit) {
            return (orbit.getType() == this.toOrbitType()) ? orbit : new KeplerianOrbit(orbit);
        }

        /** {@inheritDoc} */
        public void mapOrbitToArray(final Orbit orbit, final PositionAngle type,
                                    final double[] stateVector) {

            final KeplerianOrbit keplerianOrbit =
                (KeplerianOrbit) FieldOrbitType.KEPLERIAN.convertType(orbit);

            stateVector[0] = keplerianOrbit.getA();
            stateVector[1] = keplerianOrbit.getE();
            stateVector[2] = keplerianOrbit.getI();
            stateVector[3] = keplerianOrbit.getPerigeeArgument();
            stateVector[4] = keplerianOrbit.getRightAscensionOfAscendingNode();
            stateVector[5] = keplerianOrbit.getAnomaly(type);

        }

        /** {@inheritDoc} */
        public <T extends RealFieldElement<T>> FieldOrbit<T> mapArrayToOrbit(final double[] stateVector, final PositionAngle type,
                                     final FieldAbsoluteDate<T> date, final double mu, final Frame frame, final Field<T> field) {
            final T zero = field.getZero();
            return new FieldKeplerianOrbit<T>(zero.add(stateVector[0]),
                                              zero.add(stateVector[1]),
                                              zero.add(stateVector[2]),
                                              zero.add(stateVector[3]),
                                              zero.add(stateVector[4]),
                                              zero.add(stateVector[5]),
                                              type, frame, date, mu);
        }

        @Override
        public <T extends RealFieldElement<T>> FieldOrbit<T>
            convertType(final FieldOrbit<T> orbit) {
            return (orbit.getType() == this) ? orbit : new FieldKeplerianOrbit<T>(orbit);
        }

        @Override
        public <T extends RealFieldElement<T>> void
            mapOrbitToArray(final FieldOrbit<T> orbit, final PositionAngle type,
                            final T[] stateVector) {
            final FieldKeplerianOrbit<T> keplerianOrbit =
                            (FieldKeplerianOrbit<T>) FieldOrbitType.KEPLERIAN.convertType(orbit);

            stateVector[0] = keplerianOrbit.getA();
            stateVector[1] = keplerianOrbit.getE();
            stateVector[2] = keplerianOrbit.getI();
            stateVector[3] = keplerianOrbit.getPerigeeArgument();
            stateVector[4] = keplerianOrbit.getRightAscensionOfAscendingNode();
            stateVector[5] = keplerianOrbit.getAnomaly(type);
        }

        @Override
        public <T extends RealFieldElement<T>> FieldOrbit<T>
            mapArrayToOrbit(final T[] stateVector, final PositionAngle type,
                            final FieldAbsoluteDate<T> date, final double mu, final Frame frame) {
            return new FieldKeplerianOrbit<T>(stateVector[0], stateVector[1], stateVector[2], stateVector[3],
                                      stateVector[4], stateVector[5], type,
                                      frame, date, mu);
        }

        @Override
        public OrbitType toOrbitType() {
            return OrbitType.KEPLERIAN;
        }

    };

    /** Convert an orbit to the instance type.
     * <p>
     * The returned orbit is the specified instance itself if its type already matches,
     * otherwise, a new orbit of the proper type created
     * </p>
     * @param orbit orbit to convert
     * @return converted orbit with type guaranteed to match (so it can be cast safely)
     */
    public abstract Orbit convertType(final Orbit orbit);

    /** Convert orbit to state array.
     * <p>
     * Note that all implementations of this method <em>must</em> be consistent with the
     * implementation of the {@link org.orekit.orbits.Orbit#getJacobianWrtCartesian(
     * org.orekit.orbits.PositionAngle, double[][]) Orbit.getJacobianWrtCartesian}
     * method for the corresponding orbit type in terms of parameters order and meaning.
     * </p>
     * @param orbit orbit to map
     * @param type type of the angle
     * @param stateVector flat array into which the state vector should be mapped
     * (it can have more than 6 elements, extra elements are untouched)
     */
    public abstract void mapOrbitToArray(Orbit orbit, PositionAngle type, double[] stateVector);

     /** Convert state array to orbital parameters.
     * <p>
     * Note that all implementations of this method <em>must</em> be consistent with the
     * implementation of the {@link org.orekit.orbits.Orbit#getJacobianWrtCartesian(
     * org.orekit.orbits.PositionAngle, double[][]) Orbit.getJacobianWrtCartesian}
     * method for the corresponding orbit type in terms of parameters order and meaning.
     * </p>
     * @param array state as a flat array
     * (it can have more than 6 elements, extra elements are ignored)
     * @param type type of the angle
     * @param date integration date
     * @param mu central attraction coefficient used for propagation (m³/s²)
     * @param frame frame in which integration is performed
     * @param field field used by default
     * @param <T> extends RealFieldElement
     * @return orbit corresponding to the flat array as a space dynamics object
     */
    public abstract  <T extends RealFieldElement<T>> FieldOrbit<T> mapArrayToOrbit(double[] array, PositionAngle type,
                                          FieldAbsoluteDate<T> date, double mu, Frame frame, Field<T> field);

    /** Convert an orbit to the instance type.
     * <p>
     * The returned orbit is the specified instance itself if its type already matches,
     * otherwise, a new orbit of the proper type created
     * </p>
     * @param <T> RealFieldElement used
     * @param orbit orbit to convert
     * @return converted orbit with type guaranteed to match (so it can be cast safely)
     */
    public abstract <T extends RealFieldElement<T>> FieldOrbit<T> convertType(final FieldOrbit<T> orbit);

    /** Convert orbit to state array.
     * <p>
     * Note that all implementations of this method <em>must</em> be consistent with the
     * implementation of the {@link org.orekit.orbits.Orbit#getJacobianWrtCartesian(
     * org.orekit.orbits.PositionAngle, double[][]) Orbit.getJacobianWrtCartesian}
     * method for the corresponding orbit type in terms of parameters order and meaning.
     * </p>
     * @param <T> RealFieldElement used
     * @param orbit orbit to map
     * @param type type of the angle
     * @param stateVector flat array into which the state vector should be mapped
     * (it can have more than 6 elements, extra elements are untouched)
     */
    public abstract <T extends RealFieldElement<T>>void mapOrbitToArray(FieldOrbit<T> orbit, PositionAngle type, T[] stateVector);


    /** Convert state array to orbital parameters.
     * <p>
     * Note that all implementations of this method <em>must</em> be consistent with the
     * implementation of the {@link org.orekit.orbits.Orbit#getJacobianWrtCartesian(
     * org.orekit.orbits.PositionAngle, double[][]) Orbit.getJacobianWrtCartesian}
     * method for the corresponding orbit type in terms of parameters order and meaning.
     * </p>
     * @param <T> RealFieldElement used
     * @param array state as a flat array
     * (it can have more than 6 elements, extra elements are ignored)
     * @param type type of the angle
     * @param date integration date
     * @param mu central attraction coefficient used for propagation (m³/s²)
     * @param frame frame in which integration is performed
     * @return orbit corresponding to the flat array as a space dynamics object
     */
    public abstract <T extends RealFieldElement<T>> FieldOrbit<T> mapArrayToOrbit(T[] array, PositionAngle type,
                                          FieldAbsoluteDate<T> date, double mu, Frame frame);

    /**Converts FieldOrbitType to OrbitType.
     * <p>
     * Protected because if the user instanciates from a FieldOrbit the OrbitType it wouldn't be able to get
     * the correct methods for the OrbitType.
     * </p>
     * @return instance of OrbitType.
     */

    protected abstract OrbitType toOrbitType();
}
