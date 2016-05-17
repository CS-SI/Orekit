/* Contributed in the public domain.
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
package org.orekit;

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.FastMath;
import org.hipparchus.util.Precision;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.SelfDescribing;
import org.hamcrest.TypeSafeDiagnosingMatcher;
import org.hamcrest.TypeSafeMatcher;
import org.orekit.bodies.GeodeticPoint;
import org.orekit.utils.Constants;
import org.orekit.utils.PVCoordinates;

import java.util.Arrays;

import static org.hamcrest.CoreMatchers.either;
import static org.hamcrest.CoreMatchers.is;

/**
 * A set of matchers specific to the Orekit value classes.
 *
 * @author Evan Ward
 */
public class OrekitMatchers {

    /**
     * Match a geodetic point
     *
     * @param lat latitude matcher, in radians
     * @param lon longitude matcher, in radians
     * @param alt altitude matcher, in meters
     * @return a {@link GeodeticPoint} matcher
     */
    public static Matcher<GeodeticPoint> geodeticPoint(
            final Matcher<Double> lat,
            final Matcher<Double> lon,
            final Matcher<Double> alt) {
        return new TypeSafeDiagnosingMatcher<GeodeticPoint>() {
            @Override
            public void describeTo(Description description) {
                description.appendList("GeodeticPoint[", ",", "]",
                        Arrays.<SelfDescribing>asList(lat, lon, alt));
            }

            @Override
            protected boolean matchesSafely(GeodeticPoint item,
                                            Description mismatchDescription) {
                if (!lat.matches(item.getLatitude())) {
                    mismatchDescription.appendText("the latitude ");
                    lat.describeMismatch(item.getLatitude(),
                            mismatchDescription);
                    return false;
                }
                if (!lon.matches(item.getLongitude())) {
                    mismatchDescription.appendText("the longitude ");
                    lon.describeMismatch(item.getLongitude(),
                            mismatchDescription);
                    return false;
                }
                if (!alt.matches(item.getAltitude())) {
                    mismatchDescription.appendText("the altitude ");
                    alt.describeMismatch(item.getAltitude(),
                            mismatchDescription);
                    return false;
                }
                return true;
            }
        };
    }

    /**
     * Match a geodetic point
     *
     * @param lat latitude, in radians
     * @param lon longitude, in radians
     * @param alt altitude, in meters
     * @return matcher of a {@link GeodeticPoint}
     */
    public static Matcher<GeodeticPoint> geodeticPoint(double lat,
                                                       double lon,
                                                       double alt) {
        return geodeticPoint(is(lat), is(lon), is(alt));
    }

    /**
     * Match a geodetic point by comparing it with another one.
     *
     * @param expected the expected value
     * @param absTol   the absolute tolerance on the comparison, in meters.
     *                 Differences less than this value will be ignored.
     * @return a {@link GeodeticPoint} matcher
     */
    public static Matcher<GeodeticPoint> geodeticPointCloseTo(
            GeodeticPoint expected, double absTol) {
        double angularAbsTol = absTol / Constants.WGS84_EARTH_EQUATORIAL_RADIUS;
        return geodeticPoint(closeTo(expected.getLatitude(), angularAbsTol),
                closeTo(expected.getLongitude(), angularAbsTol),
                closeTo(expected.getAltitude(), absTol));
    }

    /**
     * Match a geodetic point by comparing it with another one.
     *
     * @param expected the expected value
     * @param ulps     the ulps difference allowed
     * @return a {@link GeodeticPoint} matcher
     * @see #relativelyCloseTo
     */
    public static Matcher<GeodeticPoint> geodeticPointCloseTo(
            GeodeticPoint expected, int ulps) {
        return geodeticPoint(relativelyCloseTo(expected.getLatitude(), ulps),
                relativelyCloseTo(expected.getLongitude(), ulps),
                relativelyCloseTo(expected.getAltitude(), ulps));
    }

    /**
     * Matches a {@link Vector3D} based on its three coordinates.
     *
     * @param x matcher for the x coordinate
     * @param y matcher for the y coordinate
     * @param z matcher for the z coordinate
     * @return a vector matcher
     */
    public static Matcher<Vector3D> vector(final Matcher<Double> x,
                                           final Matcher<Double> y,
                                           final Matcher<Double> z) {
        return new TypeSafeDiagnosingMatcher<Vector3D>() {
            @Override
            public void describeTo(Description description) {
                description.appendList("Vector3D[", ",", "]",
                        Arrays.<SelfDescribing>asList(x, y, z));
            }

            @Override
            protected boolean matchesSafely(Vector3D item,
                                            Description mismatchDescription) {
                if (!x.matches(item.getX())) {
                    mismatchDescription.appendText("the x coordinate ");
                    x.describeMismatch(item.getX(), mismatchDescription);
                    return false;
                }
                if (!y.matches(item.getY())) {
                    mismatchDescription.appendText("the y coordinate ");
                    y.describeMismatch(item.getY(), mismatchDescription);
                    return false;
                }
                if (!z.matches(item.getZ())) {
                    mismatchDescription.appendText("the z coordinate ");
                    z.describeMismatch(item.getZ(), mismatchDescription);
                    return false;
                }
                return true;
            }
        };
    }

    /**
     * Matches a {@link Vector3D} close to another one.
     *
     * @param vector the reference vector
     * @param absTol absolute tolerance of comparison, in each dimension
     * @return a vector matcher.
     */
    public static Matcher<Vector3D> vectorCloseTo(Vector3D vector, double absTol) {
        return vector(closeTo(vector.getX(), absTol),
                closeTo(vector.getY(), absTol), closeTo(vector.getZ(), absTol));
    }

    /**
     * Matches a {@link Vector3D} close to another one.
     *
     * @param vector the reference vector
     * @param ulps   the relative tolerance, in units in last place, of the
     *               Comparison of each dimension.
     * @return a vector matcher.
     */
    public static Matcher<Vector3D> vectorCloseTo(Vector3D vector, int ulps) {
        return vector(relativelyCloseTo(vector.getX(), ulps),
                relativelyCloseTo(vector.getY(), ulps),
                relativelyCloseTo(vector.getZ(), ulps));
    }

    /**
     * Alias for {@link #vectorCloseTo(Vector3D, int)}
     *
     * @param x    the x component
     * @param y    the y component
     * @param z    the z component
     * @param ulps the relative tolerance, in ulps
     * @return a vector matcher
     */
    public static Matcher<Vector3D> vectorCloseTo(double x, double y, double z, int ulps) {
        return vectorCloseTo(new Vector3D(x, y, z), ulps);
    }

    /**
     * Matches a {@link Vector3D} to another one.
     *
     * @param vector the reference vector
     * @param absTol the absolute tolerance of comparison, in each dimension.
     * @param ulps   the relative tolerance of comparison in each dimension, in
     *               units in last place.
     * @return a matcher that matches if either the absolute or relative
     * comparison matches in each dimension.
     */
    public static Matcher<Vector3D> vectorCloseTo(Vector3D vector,
                                                  double absTol, int ulps) {
        return vector(numberCloseTo(vector.getX(), absTol, ulps),
                numberCloseTo(vector.getY(), absTol, ulps),
                numberCloseTo(vector.getZ(), absTol, ulps));
    }

    /**
     * Match a {@link PVCoordinates}
     *
     * @param position matcher for the position
     * @param velocity matcher for the velocity
     * @return a matcher of {@link PVCoordinates}
     */
    public static Matcher<PVCoordinates> pvIs(
            final Matcher<? super Vector3D> position,
            final Matcher<? super Vector3D> velocity) {
        return new TypeSafeDiagnosingMatcher<PVCoordinates>() {

            @Override
            public void describeTo(Description description) {
                description.appendText("position ");
                description.appendDescriptionOf(position);
                description.appendText(" and velocity ");
                description.appendDescriptionOf(velocity);
            }

            @Override
            protected boolean matchesSafely(PVCoordinates item,
                    Description mismatchDescription) {
                if (!position.matches(item.getPosition())) {
                    // position doesn't match
                    mismatchDescription.appendText("position ");
                    position.describeMismatch(item.getPosition(), mismatchDescription);
                    return false;
                } else if (!velocity.matches(item.getVelocity())) {
                    // velocity doesn't match
                    mismatchDescription.appendText("velocity ");
                    velocity.describeMismatch(item.getVelocity(), mismatchDescription);
                    return false;
                } else {
                    // both p and v matched
                    return true;
                }
            }
        };
    }

    /**
     * Check that a {@link PVCoordinates} is the same as another one.
     *
     * @param pv the reference {@link PVCoordinates}
     * @return a {@link PVCoordinates} {@link Matcher}
     */
    public static Matcher<PVCoordinates> pvIs(PVCoordinates pv) {
        return pvCloseTo(pv, 0);
    }

    /**
     * Match a {@link PVCoordinates} close to another one.
     *
     * @param pv     the reference {@link PVCoordinates}
     * @param absTol distance a matched {@link PVCoordinates} can be from the reference in
     *               any one coordinate.
     * @return a matcher of {@link PVCoordinates}.
     */
    public static Matcher<PVCoordinates> pvCloseTo(PVCoordinates pv,
                                                   double absTol) {
        return pvIs(vectorCloseTo(pv.getPosition(), absTol),
                vectorCloseTo(pv.getVelocity(), absTol));
    }

    /**
     * Match a {@link PVCoordinates} close to another one.
     *
     * @param pv   the reference {@link PVCoordinates}
     * @param ulps the units in last place any coordinate can be off by.
     * @return a matcher of {@link PVCoordinates}.
     */
    public static Matcher<PVCoordinates> pvCloseTo(PVCoordinates pv, int ulps) {
        return pvIs(vectorCloseTo(pv.getPosition(), ulps),
                vectorCloseTo(pv.getVelocity(), ulps));
    }

    /**
     * Checks if two numbers are relatively close to each other. For absolute
     * comparisons, see {@link #closeTo(double, double)}.
     *
     * @param expected the expected value in the relative comparison
     * @param ulps     the units in last place of {@code expected} the two
     *                 numbers can be off by.
     * @return a matcher of numbers
     */
    public static Matcher<Double> relativelyCloseTo(final double expected,
                                                    final int ulps) {
        return new TypeSafeDiagnosingMatcher<Double>() {

            @Override
            public void describeTo(Description description) {
                description.appendText("a numeric value within ")
                        .appendValue(ulps).appendText(" ulps of ")
                        .appendValue(expected);
            }

            @Override
            protected boolean matchesSafely(Double item,
                                            Description mismatchDescription) {
                if (!Precision.equals(item, expected, ulps)) {
                    mismatchDescription
                            .appendValue(item)
                            .appendText(" was off by ")
                            .appendValue(
                                    Double.doubleToLongBits(item)
                                            - Double.doubleToLongBits(expected))
                            .appendText(" ulps");
                    return false;
                }
                return true;
            }
        };
    }

    /**
     * Check a number is close to another number using a relative
     * <strong>or</strong> absolute comparison.
     *
     * @param number the expected value
     * @param absTol absolute tolerance of comparison
     * @param ulps   units in last place tolerance for relative comparison
     * @return a matcher that matches if the differences is less than or equal
     * to absTol <strong>or</strong> the two numbers differ by less or equal
     * ulps.
     */
    public static Matcher<Double> numberCloseTo(double number, double absTol,
                                                int ulps) {
        return either(closeTo(number, absTol)).or(
                relativelyCloseTo(number, ulps));
    }

    /* Copid from Hamcrest's IsCloseTo under the new BSD license.
     * Copyright (c) 2000-2006 hamcrest.org
     */

    /**
     * Creates a matcher of {@link Double}s that matches when an examined double
     * is equal to the specified <code>operand</code>, within a range of +/-
     * <code>error</code>. <p/> For example:
     * <pre>assertThat(1.03, is(closeTo(1.0, 0.03)))</pre>
     *
     * @param value the expected value of matching doubles
     * @param delta the delta (+/-) within which matches will be allowed
     * @return a double matcher.
     */
    public static Matcher<Double> closeTo(final double value,
                                          final double delta) {

        return new TypeSafeMatcher<Double>() {
            @Override
            public boolean matchesSafely(Double item) {
                return actualDelta(item) <= 0.0;
            }

            @Override
            public void describeMismatchSafely(Double item, Description mismatchDescription) {
                mismatchDescription.appendValue(item)
                        .appendText(" differed by ")
                        .appendValue(actualDelta(item));
            }

            @Override
            public void describeTo(Description description) {
                description.appendText("a numeric value within ")
                        .appendValue(delta)
                        .appendText(" of ")
                        .appendValue(value);
            }

            private double actualDelta(Double item) {
                return (FastMath.abs((item - value)) - delta);
            }
        };

    }

}
