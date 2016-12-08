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

import org.hipparchus.RealFieldElement;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.FastMath;
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

/** Enumerate for {@link Orbit orbital} parameters types.
 */
public enum OrbitType {

    /** Type for propagation in {@link CartesianOrbit Cartesian parameters}. */
    CARTESIAN {

        /** {@inheritDoc} */
        @Override
        public Orbit convertType(final Orbit orbit) {
            return (orbit.getType() == this) ? orbit : new CartesianOrbit(orbit);
        }

        /** {@inheritDoc} */
        @Override
        public void mapOrbitToArray(final Orbit orbit, final PositionAngle type,
                                    final double[] stateVector) {

            final PVCoordinates pv = orbit.getPVCoordinates();
            final Vector3D      p  = pv.getPosition();
            final Vector3D      v  = pv.getVelocity();

            stateVector[0] = p.getX();
            stateVector[1] = p.getY();
            stateVector[2] = p.getZ();
            stateVector[3] = v.getX();
            stateVector[4] = v.getY();
            stateVector[5] = v.getZ();

        }

        /** {@inheritDoc} */
        @Override
        public Orbit mapArrayToOrbit(final double[] stateVector, final PositionAngle type,
                                     final AbsoluteDate date, final double mu, final Frame frame) {

            final Vector3D p     = new Vector3D(stateVector[0], stateVector[1], stateVector[2]);
            final double r2      = p.getNormSq();
            final Vector3D v     = new Vector3D(stateVector[3], stateVector[4], stateVector[5]);
            final Vector3D a     = new Vector3D(-mu / (r2 * FastMath.sqrt(r2)), p);
            return new CartesianOrbit(new PVCoordinates(p, v, a), frame, date, mu);

        }

        /** {@inheritDoc} */
        @Override
        public <T extends RealFieldElement<T>> FieldOrbit<T> convertType(final FieldOrbit<T> orbit) {
            return (orbit.getType() == this) ? orbit : new FieldCartesianOrbit<T>(orbit);
        }

        /** {@inheritDoc} */
        @Override
        public <T extends RealFieldElement<T>> void mapOrbitToArray(final FieldOrbit<T> orbit,
                                                                    final PositionAngle type,
                                                                    final T[] stateVector) {

            final TimeStampedFieldPVCoordinates<T> pv = orbit.getPVCoordinates();
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
        @Override
        public <T extends RealFieldElement<T>> FieldOrbit<T> mapArrayToOrbit(final T[] stateVector, final PositionAngle type,
                                                                             final FieldAbsoluteDate<T> date,
                                                                             final double mu, final Frame frame) {
            final T zero = stateVector[0].getField().getZero();
            final FieldVector3D<T> p     = new FieldVector3D<T>(stateVector[0], stateVector[1], stateVector[2]);
            final T r2      = p.getNormSq();
            final FieldVector3D<T> v     = new FieldVector3D<T>(stateVector[3], stateVector[4], stateVector[5]);
            final FieldVector3D<T> a     = new FieldVector3D<T>(zero.add(-mu).divide(r2.sqrt().multiply(r2)), p);
            return new FieldCartesianOrbit<T>(new FieldPVCoordinates<T>(p, v, a), frame, date, mu);

        }

        /** {@inheritDoc} */
        @Override
        public ParameterDriversList getDrivers(final double dP, final Orbit orbit, final PositionAngle type)
            throws OrekitException {
            final ParameterDriversList drivers = new ParameterDriversList();
            final double[] array = new double[6];
            mapOrbitToArray(orbit, type, array);
            final double[] scale = scale(dP, orbit);
            drivers.add(new ParameterDriver(POS_X, array[0], scale[0], Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY));
            drivers.add(new ParameterDriver(POS_Y, array[1], scale[1], Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY));
            drivers.add(new ParameterDriver(POS_Z, array[2], scale[2], Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY));
            drivers.add(new ParameterDriver(VEL_X, array[3], scale[3], Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY));
            drivers.add(new ParameterDriver(VEL_Y, array[4], scale[4], Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY));
            drivers.add(new ParameterDriver(VEL_Z, array[5], scale[5], Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY));
            return drivers;
        }

    },

    /** Type for propagation in {@link CircularOrbit circular parameters}. */
    CIRCULAR {

        /** {@inheritDoc} */
        @Override
        public Orbit convertType(final Orbit orbit) {
            return (orbit.getType() == this) ? orbit : new CircularOrbit(orbit);
        }

        /** {@inheritDoc} */
        @Override
        public void mapOrbitToArray(final Orbit orbit, final PositionAngle type,
                                    final double[] stateVector) {

            final CircularOrbit circularOrbit = (CircularOrbit) OrbitType.CIRCULAR.convertType(orbit);

            stateVector[0] = circularOrbit.getA();
            stateVector[1] = circularOrbit.getCircularEx();
            stateVector[2] = circularOrbit.getCircularEy();
            stateVector[3] = circularOrbit.getI();
            stateVector[4] = circularOrbit.getRightAscensionOfAscendingNode();
            stateVector[5] = circularOrbit.getAlpha(type);

        }

        /** {@inheritDoc} */
        @Override
        public Orbit mapArrayToOrbit(final double[] stateVector, final PositionAngle type,
                                     final AbsoluteDate date, final double mu, final Frame frame) {
            return new CircularOrbit(stateVector[0], stateVector[1], stateVector[2], stateVector[3],
                                     stateVector[4], stateVector[5], type,
                                     frame, date, mu);
        }

        /** {@inheritDoc} */
        @Override
        public <T extends RealFieldElement<T>> FieldOrbit<T> convertType(final FieldOrbit<T> orbit) {
            return (orbit.getType() == this) ? orbit : new FieldCircularOrbit<T>(orbit);
        }

        /** {@inheritDoc} */
        @Override
        public <T extends RealFieldElement<T>> void mapOrbitToArray(final FieldOrbit<T> orbit,
                                                                    final PositionAngle type,
                                                                    final T[] stateVector) {

            final FieldCircularOrbit<T> circularOrbit = (FieldCircularOrbit<T>) OrbitType.CIRCULAR.convertType(orbit);

            stateVector[0] = circularOrbit.getA();
            stateVector[1] = circularOrbit.getCircularEx();
            stateVector[2] = circularOrbit.getCircularEy();
            stateVector[3] = circularOrbit.getI();
            stateVector[4] = circularOrbit.getRightAscensionOfAscendingNode();
            stateVector[5] = circularOrbit.getAlpha(type);

        }

        /** {@inheritDoc} */
        @Override
        public <T extends RealFieldElement<T>> FieldOrbit<T> mapArrayToOrbit(final T[] stateVector, final PositionAngle type,
                                                                             final FieldAbsoluteDate<T> date,
                                                                             final double mu, final Frame frame) {
            return new FieldCircularOrbit<T>(stateVector[0], stateVector[1], stateVector[2], stateVector[3],
                                     stateVector[4], stateVector[5], type,
                                     frame, date, mu);
        }

        /** {@inheritDoc} */
        @Override
        public ParameterDriversList getDrivers(final double dP, final Orbit orbit, final PositionAngle type)
            throws OrekitException {
            final ParameterDriversList drivers = new ParameterDriversList();
            final double[] array = new double[6];
            mapOrbitToArray(orbit, type, array);
            final double[] scale = scale(dP, orbit);
            final String name = type == PositionAngle.MEAN ?
                                    MEAN_LAT_ARG :
                                    type == PositionAngle.ECCENTRIC ? ECC_LAT_ARG : TRUE_LAT_ARG;
            drivers.add(new ParameterDriver(A,    array[0], scale[0],  0.0, Double.POSITIVE_INFINITY));
            drivers.add(new ParameterDriver(E_X,  array[1], scale[1], -1.0, 1.0));
            drivers.add(new ParameterDriver(E_Y,  array[2], scale[2], -1.0, 1.0));
            drivers.add(new ParameterDriver(INC,  array[3], scale[3],  0.0, FastMath.PI));
            drivers.add(new ParameterDriver(RAAN, array[4], scale[4], Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY));
            drivers.add(new ParameterDriver(name, array[5], scale[5], Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY));
            return drivers;
        }

    },

    /** Type for propagation in {@link EquinoctialOrbit equinoctial parameters}. */
    EQUINOCTIAL {

        /** {@inheritDoc} */
        @Override
        public Orbit convertType(final Orbit orbit) {
            return (orbit.getType() == this) ? orbit : new EquinoctialOrbit(orbit);
        }

        /** {@inheritDoc} */
        @Override
       public void mapOrbitToArray(final Orbit orbit, final PositionAngle type,
                                    final double[] stateVector) {

            final EquinoctialOrbit equinoctialOrbit =
                (EquinoctialOrbit) OrbitType.EQUINOCTIAL.convertType(orbit);

            stateVector[0] = equinoctialOrbit.getA();
            stateVector[1] = equinoctialOrbit.getEquinoctialEx();
            stateVector[2] = equinoctialOrbit.getEquinoctialEy();
            stateVector[3] = equinoctialOrbit.getHx();
            stateVector[4] = equinoctialOrbit.getHy();
            stateVector[5] = equinoctialOrbit.getL(type);

        }

        /** {@inheritDoc} */
        @Override
        public Orbit mapArrayToOrbit(final double[] stateVector, final PositionAngle type,
                                     final AbsoluteDate date, final double mu, final Frame frame) {
            return new EquinoctialOrbit(stateVector[0], stateVector[1], stateVector[2], stateVector[3],
                                        stateVector[4], stateVector[5], type,
                                        frame, date, mu);
        }

        /** {@inheritDoc} */
        @Override
        public <T extends RealFieldElement<T>> FieldOrbit<T> convertType(final FieldOrbit<T> orbit) {
            return (orbit.getType() == this) ? orbit : new FieldEquinoctialOrbit<T>(orbit);
        }

        /** {@inheritDoc} */
        @Override
        public <T extends RealFieldElement<T>> void mapOrbitToArray(final FieldOrbit<T> orbit,
                                                                    final PositionAngle type,
                                                                    final T[] stateVector) {

            final FieldEquinoctialOrbit<T> equinoctialOrbit =
                (FieldEquinoctialOrbit<T>) OrbitType.EQUINOCTIAL.convertType(orbit);

            stateVector[0] = equinoctialOrbit.getA();
            stateVector[1] = equinoctialOrbit.getEquinoctialEx();
            stateVector[2] = equinoctialOrbit.getEquinoctialEy();
            stateVector[3] = equinoctialOrbit.getHx();
            stateVector[4] = equinoctialOrbit.getHy();
            stateVector[5] = equinoctialOrbit.getL(type);

        }

        /** {@inheritDoc} */
        @Override
        public <T extends RealFieldElement<T>> FieldOrbit<T> mapArrayToOrbit(final T[] stateVector, final PositionAngle type,
                                                                             final FieldAbsoluteDate<T> date,
                                                                             final double mu, final Frame frame) {
            return new FieldEquinoctialOrbit<T>(stateVector[0], stateVector[1], stateVector[2], stateVector[3],
                                                stateVector[4], stateVector[5], type,
                                                frame, date, mu);
        }

        /** {@inheritDoc} */
        @Override
        public ParameterDriversList getDrivers(final double dP, final Orbit orbit, final PositionAngle type)
            throws OrekitException {
            final ParameterDriversList drivers = new ParameterDriversList();
            final double[] array = new double[6];
            mapOrbitToArray(orbit, type, array);
            final double[] scale = scale(dP, orbit);
            final String name = type == PositionAngle.MEAN ?
                                    MEAN_LON_ARG :
                                    type == PositionAngle.ECCENTRIC ? ECC_LON_ARG : TRUE_LON_ARG;
            drivers.add(new ParameterDriver(A,    array[0], scale[0],  0.0, Double.POSITIVE_INFINITY));
            drivers.add(new ParameterDriver(E_X,  array[1], scale[1], -1.0, 1.0));
            drivers.add(new ParameterDriver(E_Y,  array[2], scale[2], -1.0, 1.0));
            drivers.add(new ParameterDriver(H_X,  array[3], scale[3], Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY));
            drivers.add(new ParameterDriver(H_Y,  array[4], scale[4], Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY));
            drivers.add(new ParameterDriver(name, array[5], scale[5], Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY));
            return drivers;
        }


    },

    /** Type for propagation in {@link KeplerianOrbit Keplerian parameters}. */
    KEPLERIAN {

        /** {@inheritDoc} */
        @Override
        public Orbit convertType(final Orbit orbit) {
            return (orbit.getType() == this) ? orbit : new KeplerianOrbit(orbit);
        }

        /** {@inheritDoc} */
        @Override
        public void mapOrbitToArray(final Orbit orbit, final PositionAngle type,
                                    final double[] stateVector) {

            final KeplerianOrbit keplerianOrbit =
                (KeplerianOrbit) OrbitType.KEPLERIAN.convertType(orbit);

            stateVector[0] = keplerianOrbit.getA();
            stateVector[1] = keplerianOrbit.getE();
            stateVector[2] = keplerianOrbit.getI();
            stateVector[3] = keplerianOrbit.getPerigeeArgument();
            stateVector[4] = keplerianOrbit.getRightAscensionOfAscendingNode();
            stateVector[5] = keplerianOrbit.getAnomaly(type);

        }

        /** {@inheritDoc} */
        @Override
        public Orbit mapArrayToOrbit(final double[] stateVector, final PositionAngle type,
                                     final AbsoluteDate date, final double mu, final Frame frame) {
            return new KeplerianOrbit(stateVector[0], stateVector[1], stateVector[2], stateVector[3],
                                      stateVector[4], stateVector[5], type,
                                      frame, date, mu);
        }

        /** {@inheritDoc} */
        @Override
        public <T extends RealFieldElement<T>> FieldOrbit<T> convertType(final FieldOrbit<T> orbit) {
            return (orbit.getType() == this) ? orbit : new FieldKeplerianOrbit<T>(orbit);
        }

        /** {@inheritDoc} */
        @Override
        public <T extends RealFieldElement<T>> void mapOrbitToArray(final FieldOrbit<T> orbit,
                                                                    final PositionAngle type,
                                                                    final T[] stateVector) {
            final FieldKeplerianOrbit<T> keplerianOrbit =
                            (FieldKeplerianOrbit<T>) OrbitType.KEPLERIAN.convertType(orbit);

            stateVector[0] = keplerianOrbit.getA();
            stateVector[1] = keplerianOrbit.getE();
            stateVector[2] = keplerianOrbit.getI();
            stateVector[3] = keplerianOrbit.getPerigeeArgument();
            stateVector[4] = keplerianOrbit.getRightAscensionOfAscendingNode();
            stateVector[5] = keplerianOrbit.getAnomaly(type);
        }

        /** {@inheritDoc} */
        @Override
        public <T extends RealFieldElement<T>> FieldOrbit<T> mapArrayToOrbit(final T[] stateVector, final PositionAngle type,
                                                                             final FieldAbsoluteDate<T> date,
                                                                             final double mu, final Frame frame) {
            return new FieldKeplerianOrbit<T>(stateVector[0], stateVector[1], stateVector[2], stateVector[3],
                                              stateVector[4], stateVector[5], type,
                                              frame, date, mu);
        }

        /** {@inheritDoc} */
        @Override
        public ParameterDriversList getDrivers(final double dP, final Orbit orbit, final PositionAngle type)
            throws OrekitException {
            final ParameterDriversList drivers = new ParameterDriversList();
            final double[] array = new double[6];
            mapOrbitToArray(orbit, type, array);
            final double[] scale = scale(dP, orbit);
            final String name = type == PositionAngle.MEAN ?
                                    MEAN_ANOM :
                                    type == PositionAngle.ECCENTRIC ? ECC_ANOM : TRUE_ANOM;
            drivers.add(new ParameterDriver(A,    array[0], scale[0],  0.0, Double.POSITIVE_INFINITY));
            drivers.add(new ParameterDriver(ECC,  array[1], scale[1],  0.0, 1.0));
            drivers.add(new ParameterDriver(INC,  array[2], scale[2],  0.0, FastMath.PI));
            drivers.add(new ParameterDriver(PA,   array[3], scale[3], Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY));
            drivers.add(new ParameterDriver(RAAN, array[4], scale[4], Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY));
            drivers.add(new ParameterDriver(name, array[5], scale[5], Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY));
            return drivers;
        }

    };

    /** Name for position along X. */
    private static final String POS_X = "Px";

    /** Name for position along Y. */
    private static final String POS_Y = "Py";

    /** Name for position along Z. */
    private static final String POS_Z = "Pz";

    /** Name for velocity along X. */
    private static final String VEL_X = "Vx";

    /** Name for velocity along Y. */
    private static final String VEL_Y = "Vy";

    /** Name for velocity along Z. */
    private static final String VEL_Z = "Vz";

    /** Name for semi major axis. */
    private static final String A     = "a";

    /** Name for eccentricity. */
    private static final String ECC   = "e";

    /** Name for eccentricity vector first component. */
    private static final String E_X   = "ex";

    /** Name for eccentricity vector second component. */
    private static final String E_Y   = "ey";

    /** Name for inclination. */
    private static final String INC   = "i";

    /** Name for inclination vector first component. */
    private static final String H_X   = "hx";

    /** Name for inclination vector second component . */
    private static final String H_Y   = "hy";

    /** Name for perigee argument. */
    private static final String PA    = "ω";

    /** Name for right ascension of ascending node. */
    private static final String RAAN    = "Ω";

    /** Name for mean anomaly. */
    private static final String MEAN_ANOM = "M";

    /** Name for eccentric anomaly. */
    private static final String ECC_ANOM  = "E";

    /** Name for mean anomaly. */
    private static final String TRUE_ANOM = "v";

    /** Name for mean argument of latitude. */
    private static final String MEAN_LAT_ARG = "αM";

    /** Name for eccentric argument of latitude. */
    private static final String ECC_LAT_ARG  = "αE";

    /** Name for mean argument of latitude. */
    private static final String TRUE_LAT_ARG = "αv";

    /** Name for mean argument of longitude. */
    private static final String MEAN_LON_ARG = "λM";

    /** Name for eccentric argument of longitude. */
    private static final String ECC_LON_ARG  = "λE";

    /** Name for mean argument of longitude. */
    private static final String TRUE_LON_ARG = "λv";

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
     * @return orbit corresponding to the flat array as a space dynamics object
     */
    public abstract Orbit mapArrayToOrbit(double[] array, PositionAngle type,
                                          AbsoluteDate date, double mu, Frame frame);

    /** Convert an orbit to the instance type.
     * <p>
     * The returned orbit is the specified instance itself if its type already matches,
     * otherwise, a new orbit of the proper type created
     * </p>
     * @param <T> RealFieldElement used
     * @param orbit orbit to convert
     * @return converted orbit with type guaranteed to match (so it can be cast safely)
     */
    public abstract <T extends RealFieldElement<T>> FieldOrbit<T> convertType(FieldOrbit<T> orbit);

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
                                                                                  FieldAbsoluteDate<T> date,
                                                                                  double mu, Frame frame);

    /** Get parameters drivers initialized from a reference orbit.
     * @param dP user specified position error
     * @param orbit reference orbit
     * @param type type of the angle
     * @return parameters drivers initialized from reference orbit
     * @exception OrekitException if Jacobian is singular
     */
    public abstract ParameterDriversList getDrivers(double dP, Orbit orbit,
                                                    PositionAngle type)
        throws OrekitException;

    /** Compute scaling factor for parameters drivers.
     * <p>
     * The scales are estimated from partial derivatives properties of orbits,
     * starting from a scalar position error specified by the user.
     * Considering the energy conservation equation V = sqrt(mu (2/r - 1/a)),
     * we get at constant energy (i.e. on a Keplerian trajectory):
     * <pre>
     * V² r |dV| = mu |dr|
     * </pre>
     * <p> So we deduce a scalar velocity error consistent with the position error.
     * From here, we apply orbits Jacobians matrices to get consistent scales
     * on orbital parameters.
     *
     * @param dP user specified position error
     * @param orbit reference orbit
     * @return scaling factor array
     * @exception OrekitException if Jacobian is singular
     */
    protected double[] scale(final double dP, final Orbit orbit)
        throws OrekitException {

        // estimate the scalar velocity error
        final PVCoordinates pv = orbit.getPVCoordinates();
        final double r2 = pv.getPosition().getNormSq();
        final double v  = pv.getVelocity().getNorm();
        final double dV = orbit.getMu() * dP / (v * r2);

        final double[] scale = new double[6];

        // convert the orbit to the desired type
        final double[][] jacobian = new double[6][6];
        final Orbit converted = convertType(orbit);
        converted.getJacobianWrtCartesian(PositionAngle.TRUE, jacobian);

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
