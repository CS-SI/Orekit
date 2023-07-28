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
package org.orekit.estimation.measurements;

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.FastMath;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.orekit.Utils;
import org.orekit.bodies.GeodeticPoint;
import org.orekit.bodies.OneAxisEllipsoid;
import org.orekit.frames.FramesFactory;
import org.orekit.frames.TopocentricFrame;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.Constants;
import org.orekit.utils.IERSConventions;

/** This test class is only used to test the <i>compareTo</i> default method of interface <i>ComparableMeasurement</i>.<br>
 * This is done to test the resolution of  <a href="https://gitlab.orekit.org/orekit/orekit/issues/538"> issue #538</a> on Orekit forge.
 */
public class ComparableMeasurementTest {

    /** Test default method compareTo, see <a href="https://gitlab.orekit.org/orekit/orekit/issues/538"> issue #538</a> on Orekit forge. */
    @Test
    public void testDefaultCompareToIssue538() {

        // Print on console ?
        boolean print = false;

        // Setup data
        Utils.setDataRoot("regular-data");
        OneAxisEllipsoid earth = new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS, Constants.WGS84_EARTH_FLATTENING,
                                                      FramesFactory.getITRF(IERSConventions.IERS_2010, false));
        TopocentricFrame stationFrame = new TopocentricFrame(earth, new GeodeticPoint(FastMath.toRadians(45.0), FastMath.toRadians(0.0), 0.0), "station");
        GroundStation station = new GroundStation(stationFrame);
        AbsoluteDate date = AbsoluteDate.J2000_EPOCH;

        // Create a Range and Azel at same date,
        // A 2 identical PV before date
        // And 1 PV measurement before date with one value bigger
        ObservableSatellite satellite = new ObservableSatellite(0);
        Range range = new Range(station, false, date, 400e3, 1.0, 1.0, satellite);
        AngularAzEl azel = new AngularAzEl(station, date, new double[] {1.0, 0.5}, new double[] {1.0e-2, 1.0e-2}, new double[] {1.0, 1.0}, satellite);
        PV pv = new PV(date.shiftedBy(-1.), new Vector3D(7e3,7e3,7e3), new Vector3D(7,7,7), 1., 1e-3, 1., satellite);
        PV pv2 = new PV(date.shiftedBy(-1.), new Vector3D(7e3,7e3,7e3), new Vector3D(7,7,7), 1., 1e-3, 1., satellite);
        PV pv3 = new PV(date.shiftedBy(-1.), new Vector3D(7e3,7e3,7e3), new Vector3D(7,8,7), 1., 1e-3, 1., satellite);

        // Print out results
        if (print) {
            System.out.println("azel.compareTo(azel)  = " + azel.compareTo(azel));
            System.out.println("pv.compareTo(azel)    = " + pv.compareTo(azel));
            System.out.println("range.compareTo(pv2)  = " + range.compareTo(pv2));
            System.out.println("range.compareTo(azel) = " + range.compareTo(azel));
            System.out.println("azel.compareTo(range) = " + azel.compareTo(range));
            System.out.println("pv.compareTo(pv3)     = " + pv.compareTo(pv3));
            System.out.println("pv3.compareTo(pv)     = " + pv3.compareTo(pv));
            System.out.println("pv.compareTo(pv2)     = " + pv.compareTo(pv2));
            System.out.println("pv2.compareTo(pv)     = " + pv.compareTo(pv2));
        }

        // Same object, only case when compareTo returns 0
        Assertions.assertEquals(azel.compareTo(azel), 0);

        // Sorted by date by default
        Assertions.assertEquals(pv.compareTo(azel), -1);
        Assertions.assertEquals(range.compareTo(pv2), +1);

        // Same date but different measurement - "bigger" measurement after "smaller" one
        Assertions.assertEquals(range.compareTo(azel), -1);
        Assertions.assertEquals(azel.compareTo(range), +1);

        // Same date, same size, but different values, "bigger" measurement after "smaller" one
        Assertions.assertEquals(pv.compareTo(pv3), -1);
        Assertions.assertEquals(pv3.compareTo(pv), +1);

        // Same date, same size, same values - Arbitrary order, always return -1
        Assertions.assertEquals(pv.compareTo(pv2), -1);
        Assertions.assertEquals(pv2.compareTo(pv), -1);

    }
}
