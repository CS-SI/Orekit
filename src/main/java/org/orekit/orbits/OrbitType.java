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

import java.util.Arrays;
import java.util.List;

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.FastMath;
import org.orekit.frames.Frame;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.PVCoordinates;

/** Enumerate for {@link Orbit orbital} parameters types.
 */
public enum OrbitType {

    /** Type for propagation in {@link CartesianOrbit Cartesian parameters}. */
    CARTESIAN {

        /** {@inheritDoc} */
        public Orbit convertType(final Orbit orbit) {
            return (orbit.getType() == this) ? orbit : new CartesianOrbit(orbit);
        }

        /** {@inheritDoc} */
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
        public Orbit mapArrayToOrbit(final double[] stateVector, final PositionAngle type,
                                     final AbsoluteDate date, final double mu, final Frame frame) {

            final Vector3D p     = new Vector3D(stateVector[0], stateVector[1], stateVector[2]);
            final double r2      = p.getNormSq();
            final Vector3D v     = new Vector3D(stateVector[3], stateVector[4], stateVector[5]);
            final Vector3D a     = new Vector3D(-mu / (r2 * FastMath.sqrt(r2)), p);
            return new CartesianOrbit(new PVCoordinates(p, v, a), frame, date, mu);

        }

        /** {@inheritDoc} */
        public List<String> parametersNames(final PositionAngle type) {
            return Arrays.asList(POS_X, POS_Y, POS_Z, VEL_X, VEL_Y, VEL_Z);
        }

    },

    /** Type for propagation in {@link CircularOrbit circular parameters}. */
    CIRCULAR {

        /** {@inheritDoc} */
        public Orbit convertType(final Orbit orbit) {
            return (orbit.getType() == this) ? orbit : new CircularOrbit(orbit);
        }

        /** {@inheritDoc} */
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
        public Orbit mapArrayToOrbit(final double[] stateVector, final PositionAngle type,
                                     final AbsoluteDate date, final double mu, final Frame frame) {
            return new CircularOrbit(stateVector[0], stateVector[1], stateVector[2], stateVector[3],
                                     stateVector[4], stateVector[5], type,
                                     frame, date, mu);
        }

        /** {@inheritDoc} */
        public List<String> parametersNames(final PositionAngle type) {
            return Arrays.asList(A, E_X, E_Y, INC, RAAN,
                                 type == PositionAngle.MEAN ?
                                     MEAN_LAT_ARG :
                                     type == PositionAngle.ECCENTRIC ?
                                         ECC_LAT_ARG :
                                         TRUE_LAT_ARG);
        }

    },

    /** Type for propagation in {@link EquinoctialOrbit equinoctial parameters}. */
    EQUINOCTIAL {

        /** {@inheritDoc} */
        public Orbit convertType(final Orbit orbit) {
            return (orbit.getType() == this) ? orbit : new EquinoctialOrbit(orbit);
        }

        /** {@inheritDoc} */
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
        public Orbit mapArrayToOrbit(final double[] stateVector, final PositionAngle type,
                                     final AbsoluteDate date, final double mu, final Frame frame) {
            return new EquinoctialOrbit(stateVector[0], stateVector[1], stateVector[2], stateVector[3],
                                        stateVector[4], stateVector[5], type,
                                        frame, date, mu);
        }

        /** {@inheritDoc} */
        public List<String> parametersNames(final PositionAngle type) {
            return Arrays.asList(A, E_X, E_Y, H_X, H_Y,
                                 type == PositionAngle.MEAN ?
                                     MEAN_LON_ARG :
                                     type == PositionAngle.ECCENTRIC ?
                                         ECC_LON_ARG :
                                         TRUE_LON_ARG);
        }

    },

    /** Type for propagation in {@link KeplerianOrbit Keplerian parameters}. */
    KEPLERIAN {

        /** {@inheritDoc} */
        public Orbit convertType(final Orbit orbit) {
            return (orbit.getType() == this) ? orbit : new KeplerianOrbit(orbit);
        }

        /** {@inheritDoc} */
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
        public Orbit mapArrayToOrbit(final double[] stateVector, final PositionAngle type,
                                     final AbsoluteDate date, final double mu, final Frame frame) {
            return new KeplerianOrbit(stateVector[0], stateVector[1], stateVector[2], stateVector[3],
                                      stateVector[4], stateVector[5], type,
                                      frame, date, mu);
        }

        /** {@inheritDoc} */
        public List<String> parametersNames(final PositionAngle type) {
            return Arrays.asList(A, ECC, INC, PA, RAAN,
                                 type == PositionAngle.MEAN ?
                                     MEAN_ANOM :
                                     type == PositionAngle.ECCENTRIC ?
                                         ECC_ANOM :
                                         TRUE_ANOM);
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
     * @return orbit corresponding to the flat array as a space dynamics object
     */
    public abstract Orbit mapArrayToOrbit(double[] array, PositionAngle type,
                                          AbsoluteDate date, double mu, Frame frame);

    /** Get the names of the parameters.
     * <p>
     * These names are suitable for text displaying, they are short (one or two letters).
     * Beware some names include unicode greek letters, they cannot be displayed in ASCII,
     * UTF-8 is recommended for display.
     * </p>
     * @param type type of the angle
     * @return names of the parameters
     */
    public abstract List<String> parametersNames(final PositionAngle type);

}
