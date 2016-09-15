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
package org.orekit.forces.drag;


import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.orekit.Utils;
import org.orekit.bodies.OneAxisEllipsoid;
import org.orekit.errors.OrekitException;
import org.orekit.forces.drag.atmosphere.SimpleExponentialAtmosphere;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.frames.Transform;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.IERSConventions;


public class SimpleExponentialAtmosphereTest {

    @Test
    public void testExpAtmosphere() throws OrekitException {
        Vector3D posInEME2000 = new Vector3D(10000,Vector3D.PLUS_I);
        AbsoluteDate date = AbsoluteDate.J2000_EPOCH;
        Frame itrf = FramesFactory.getITRF(IERSConventions.IERS_2010, true);
        SimpleExponentialAtmosphere atm =
            new SimpleExponentialAtmosphere(new OneAxisEllipsoid(Utils.ae, 1.0 / 298.257222101, itrf),
                                            0.0004, 42000.0, 7500.0);
        Vector3D vel = atm.getVelocity(date, posInEME2000, FramesFactory.getEME2000());

        Transform toBody = FramesFactory.getEME2000().getTransformTo(itrf, date);
        Vector3D test = Vector3D.crossProduct(toBody.getRotationRate(),posInEME2000);
        test = test.subtract(vel);
        Assert.assertEquals(0, test.getNorm(), 2.9e-5);

    }

    @Before
    public void setUp() {
        Utils.setDataRoot("regular-data");
    }

}
