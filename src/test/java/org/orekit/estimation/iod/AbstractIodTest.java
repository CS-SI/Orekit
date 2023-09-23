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

package org.orekit.estimation.iod;

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.FastMath;
import org.junit.jupiter.api.BeforeEach;
import org.orekit.Utils;
import org.orekit.bodies.GeodeticPoint;
import org.orekit.bodies.OneAxisEllipsoid;
import org.orekit.estimation.measurements.AngularRaDec;
import org.orekit.estimation.measurements.GroundStation;
import org.orekit.estimation.measurements.ObservableSatellite;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.frames.TopocentricFrame;
import org.orekit.orbits.Orbit;
import org.orekit.propagation.Propagator;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.IERSConventions;
import org.orekit.utils.Constants;

public abstract class AbstractIodTest {
    /**
     * gcrf frame.
     */
    protected Frame gcrf;

    /**
     * EME2OOO frame.
     */
    protected Frame eme2000;

    /**
     * ground station for the observations.
     */
    protected GroundStation observer;

    /**
     * gravitational constant.
     */
    protected double mu;

    /**
     * itrf frame.
     */
    protected Frame itrf;

    @BeforeEach
    public void setUp() {
        Utils.setDataRoot("regular-data");

        this.mu      = Constants.WGS84_EARTH_MU;
        this.gcrf    = FramesFactory.getGCRF();
        this.eme2000 = FramesFactory.getEME2000();

        this.itrf = FramesFactory.getITRF(IERSConventions.IERS_2010, false);
        // The ground station is set to Austin, Texas, U.S.A
        final OneAxisEllipsoid body = new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
                                                           Constants.WGS84_EARTH_FLATTENING, itrf);
        this.observer = new GroundStation(
                new TopocentricFrame(body, new GeodeticPoint(FastMath.toRadians(40), FastMath.toRadians(-110),
                                                             2000.0), ""));
        this.observer.getPrimeMeridianOffsetDriver().setReferenceDate(AbsoluteDate.J2000_EPOCH);
        this.observer.getPolarOffsetXDriver().setReferenceDate(AbsoluteDate.J2000_EPOCH);
        this.observer.getPolarOffsetYDriver().setReferenceDate(AbsoluteDate.J2000_EPOCH);

    }

    // Computation of LOS angles
    protected Vector3D getLOSAngles(final Propagator prop, final AbsoluteDate date) {
        final AngularRaDec raDec = new AngularRaDec(observer, gcrf, date, new double[] { 0.0, 0.0 },
                                                    new double[] { 1.0, 1.0 },
                                                    new double[] { 1.0, 1.0 }, new ObservableSatellite(0));
        return (raDec.getEstimatedLineOfSight(prop, date, gcrf));
    }

    protected double getRelativeRangeError(final Orbit estimatedGauss, final Orbit orbitRef) {

        return FastMath.abs(estimatedGauss.getPVCoordinates().getPosition().getNorm() -
                                    orbitRef.getPVCoordinates().getPosition().getNorm()) /
                FastMath.abs(orbitRef.getPVCoordinates().getPosition().getNorm());
    }

    // Computation of the relative error in velocity
    protected double getRelativeVelocityError(final Orbit estimatedGauss, final Orbit orbitRef) {

        return FastMath.abs(estimatedGauss.getPVCoordinates().getVelocity().getNorm() -
                                    orbitRef.getPVCoordinates().getVelocity().getNorm()) /
                FastMath.abs(orbitRef.getPVCoordinates().getVelocity().getNorm());
    }
}