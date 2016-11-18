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
package org.orekit.frames;


import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import org.hipparchus.geometry.euclidean.threed.Rotation;
import org.hipparchus.geometry.euclidean.threed.RotationConvention;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.FastMath;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.orekit.Utils;
import org.orekit.errors.OrekitException;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.DateComponents;
import org.orekit.time.TimeComponents;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.IERSConventions;
import org.orekit.utils.PVCoordinates;


public class GTODProviderTest {

    @Test
    public void testAASReferenceLEO() throws OrekitException {

        // this reference test has been extracted from the following paper:
        // Implementation Issues Surrounding the New IAU Reference Systems for Astrodynamics
        // David A. Vallado, John H. Seago, P. Kenneth Seidelmann
        // http://www.centerforspace.com/downloads/files/pubs/AAS-06-134.pdf
        Utils.setLoaders(IERSConventions.IERS_1996,
                         Utils.buildEOPList(IERSConventions.IERS_1996, new double[][] {
                             { 53098, -0.4399619, 0.0015563, -0.140682, 0.333309, -0.052195, -0.003875, Double.NaN, Double.NaN },
                             { 53099, -0.4399619, 0.0015563, -0.140682, 0.333309, -0.052195, -0.003875, Double.NaN, Double.NaN },
                             { 53100, -0.4399619, 0.0015563, -0.140682, 0.333309, -0.052195, -0.003875, Double.NaN, Double.NaN },
                             { 53101, -0.4399619, 0.0015563, -0.140682, 0.333309, -0.052195, -0.003875, Double.NaN, Double.NaN },
                             { 53102, -0.4399619, 0.0015563, -0.140682, 0.333309, -0.052195, -0.003875, Double.NaN, Double.NaN },
                             { 53103, -0.4399619, 0.0015563, -0.140682, 0.333309, -0.052195, -0.003875, Double.NaN, Double.NaN },
                             { 53104, -0.4399619, 0.0015563, -0.140682, 0.333309, -0.052195, -0.003875, Double.NaN, Double.NaN },
                             { 53105, -0.4399619, 0.0015563, -0.140682, 0.333309, -0.052195, -0.003875, Double.NaN, Double.NaN }
                         }));
        AbsoluteDate t0 = new AbsoluteDate(new DateComponents(2004, 04, 06),
                                           new TimeComponents(07, 51, 28.386009),
                                           TimeScalesFactory.getUTC());

        // PEF iau76
        PVCoordinates pvPEF =
           new PVCoordinates(new Vector3D(-1033475.0313, 7901305.5856, 6380344.5328),
                             new Vector3D(-3225.632747, -2872.442511, 5531.931288));

        // it seems the induced effect of pole nutation correction δΔψ on the equation of the equinoxes
        // was not taken into account in the reference paper, so we fix it here for the test
        final double dDeltaPsi =
                FramesFactory.getEOPHistory(IERSConventions.IERS_1996, true).getEquinoxNutationCorrection(t0)[0];
        final double epsilonA = IERSConventions.IERS_1996.getMeanObliquityFunction().value(t0);
        final Transform fix =
                new Transform(t0, new Rotation(Vector3D.PLUS_K,
                                               dDeltaPsi * FastMath.cos(epsilonA),
                                               RotationConvention.FRAME_TRANSFORM));

        // TOD iau76
        PVCoordinates pvTOD =
            new PVCoordinates(new Vector3D(5094514.7804, 6127366.4612, 6380344.5328),
                              new Vector3D(-4746.088567, 786.077222, 5531.931288));

        Transform t = FramesFactory.getTOD(IERSConventions.IERS_1996, true).
                getTransformTo(FramesFactory.getGTOD(IERSConventions.IERS_1996, true), t0);
        checkPV(fix.transformPVCoordinates(pvPEF), t.transformPVCoordinates(pvTOD), 0.00942, 3.12e-5);

        // if we forget to apply nutation corrections, results are much worse, which is expected
        t = FramesFactory.getTOD(false).getTransformTo(FramesFactory.getGTOD(false), t0);
        checkPV(fix.transformPVCoordinates(pvPEF), t.transformPVCoordinates(pvTOD), 257.49, 0.13955);

    }

    @Test
    public void testAASReferenceGEO() throws OrekitException {

        // this reference test has been extracted from the following paper:
        // Implementation Issues Surrounding the New IAU Reference Systems for Astrodynamics
        // David A. Vallado, John H. Seago, P. Kenneth Seidelmann
        // http://www.centerforspace.com/downloads/files/pubs/AAS-06-134.pdf
        Utils.setLoaders(IERSConventions.IERS_1996,
                         Utils.buildEOPList(IERSConventions.IERS_1996, new double[][] {
                             { 53153, -0.4709050,  0.0000000, -0.083853,  0.467217, -0.053614, -0.004494, Double.NaN, Double.NaN },
                             { 53154, -0.4709050,  0.0000000, -0.083853,  0.467217, -0.053614, -0.004494, Double.NaN, Double.NaN },
                             { 53155, -0.4709050,  0.0000000, -0.083853,  0.467217, -0.053614, -0.004494, Double.NaN, Double.NaN },
                             { 53156, -0.4709050,  0.0000000, -0.083853,  0.467217, -0.053614, -0.004494, Double.NaN, Double.NaN },
                             { 53157, -0.4709050,  0.0000000, -0.083853,  0.467217, -0.053614, -0.004494, Double.NaN, Double.NaN },
                             { 53158, -0.4709050,  0.0000000, -0.083853,  0.467217, -0.053614, -0.004494, Double.NaN, Double.NaN },
                             { 53159, -0.4709050,  0.0000000, -0.083853,  0.467217, -0.053614, -0.004494, Double.NaN, Double.NaN },
                             { 53160, -0.4709050,  0.0000000, -0.083853,  0.467217, -0.053614, -0.004494, Double.NaN, Double.NaN }
                         }));
        AbsoluteDate t0 = new AbsoluteDate(new DateComponents(2004, 06, 01),
                                           TimeComponents.H00,
                                           TimeScalesFactory.getUTC());

        Transform t = FramesFactory.getTOD(IERSConventions.IERS_1996, true).
                getTransformTo(FramesFactory.getGTOD(IERSConventions.IERS_1996, true), t0);
        // TOD iau76
        PVCoordinates pvTOD =
            new PVCoordinates(new Vector3D(-40577427.7501, -11500096.1306, 10293.2583),
                              new Vector3D(837.552338, -2957.524176, -0.928772));

        // PEF iau76
        PVCoordinates pvPEF =
            new PVCoordinates(new Vector3D(24796919.2956, -34115870.9001, 10293.2583),
                              new Vector3D(-0.979178, -1.476540, -0.928772));

        // it seems the induced effect of pole nutation correction δΔψ on the equation of the equinoxes
        // was not taken into account in the reference paper, so we fix it here for the test
        final double dDeltaPsi =
                FramesFactory.getEOPHistory(IERSConventions.IERS_1996, true).getEquinoxNutationCorrection(t0)[0];
        final double epsilonA = IERSConventions.IERS_1996.getMeanObliquityFunction().value(t0);
        final Transform fix =
                new Transform(t0, new Rotation(Vector3D.PLUS_K,
                                               dDeltaPsi * FastMath.cos(epsilonA),
                                               RotationConvention.FRAME_TRANSFORM));

        checkPV(fix.transformPVCoordinates(pvPEF), t.transformPVCoordinates(pvTOD), 0.0503, 3.59e-4);

        // if we forget to apply nutation corrections, results are much worse, which is expected
        t = FramesFactory.getTOD(false).getTransformTo(FramesFactory.getGTOD(false), t0);
        checkPV(fix.transformPVCoordinates(pvPEF), t.transformPVCoordinates(pvTOD), 1458.27, 3.847e-4);

    }

    @Test
    public void testSerialization() throws OrekitException, IOException, ClassNotFoundException {
        GTODProvider provider = new GTODProvider(IERSConventions.IERS_2010,
                                                 FramesFactory.getEOPHistory(IERSConventions.IERS_2010, true));

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutputStream    oos = new ObjectOutputStream(bos);
        oos.writeObject(provider);

        Assert.assertTrue(bos.size() > 280000);
        Assert.assertTrue(bos.size() < 285000);

        ByteArrayInputStream  bis = new ByteArrayInputStream(bos.toByteArray());
        ObjectInputStream     ois = new ObjectInputStream(bis);
        GTODProvider deserialized  = (GTODProvider) ois.readObject();
        for (int i = 0; i < FastMath.min(100, provider.getEOPHistory().getEntries().size()); ++i) {
            AbsoluteDate date = provider.getEOPHistory().getEntries().get(i).getDate();
            Transform expectedIdentity = new Transform(date,
                                                       provider.getTransform(date).getInverse(),
                                                       deserialized.getTransform(date));
            Assert.assertEquals(0.0, expectedIdentity.getTranslation().getNorm(), 1.0e-15);
            Assert.assertEquals(0.0, expectedIdentity.getRotation().getAngle(),   1.0e-15);
        }

    }

    @Before
    public void setUp() {
        Utils.setDataRoot("compressed-data");
    }

    private void checkPV(PVCoordinates reference, PVCoordinates result,
                         double expectedPositionError, double expectedVelocityError) {

        Vector3D dP = result.getPosition().subtract(reference.getPosition());
        Vector3D dV = result.getVelocity().subtract(reference.getVelocity());
        Assert.assertEquals(expectedPositionError, dP.getNorm(), 0.01 * expectedPositionError);
        Assert.assertEquals(expectedVelocityError, dV.getNorm(), 0.01 * expectedVelocityError);
    }

}
