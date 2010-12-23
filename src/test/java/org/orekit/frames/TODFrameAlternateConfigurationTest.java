/* Copyright 2002-2010 CS Communication & Systèmes
 * Licensed to CS Communication & Systèmes (CS) under one or more
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


import java.io.FileNotFoundException;

import org.apache.commons.math.geometry.Vector3D;
import org.apache.commons.math.util.FastMath;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.orekit.Utils;
import org.orekit.errors.OrekitException;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.DateComponents;
import org.orekit.time.TimeComponents;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.PVCoordinates;


public class TODFrameAlternateConfigurationTest {

    @Test
    public void testAASReferenceLEO() throws OrekitException {

        // this reference test has been extracted from the following paper:
        // Implementation Issues Surrounding the New IAU Reference Systems for Astrodynamics
        // David A. Vallado, John H. Seago, P. Kenneth Seidelmann
        // http://www.centerforspace.com/downloads/files/pubs/AAS-06-134.pdf
        AbsoluteDate t0 = new AbsoluteDate(new DateComponents(2004, 04, 06),
                                           new TimeComponents(07, 51, 28.386009),
                                           TimeScalesFactory.getUTC());

        Transform tt = FramesFactory.getMOD(true).getTransformTo(FramesFactory.getTOD(true), t0);
        Transform tf = FramesFactory.getMOD(false).getTransformTo(FramesFactory.getTOD(false), t0);

        //TOD iau76
        PVCoordinates pvTODiau76 =
            new PVCoordinates(new Vector3D(5094514.7804, 6127366.4612, 6380344.5328),
                              new Vector3D(-4746.088567, 786.077222, 5531.931288));
        //MOD iau76
        PVCoordinates pvMODiau76 =
            new PVCoordinates(new Vector3D(5094029.0167, 6127870.9363, 6380247.8885),
                              new Vector3D(-4746.262495, 786.014149, 5531.791025));
        //MOD iau76 w corr
        PVCoordinates pvMODiau76Wcorr =
            new PVCoordinates(new Vector3D(5094028.3745, 6127870.8164, 6380248.5164),
                              new Vector3D(-4746.263052, 786.014045, 5531.790562));

        checkPV(pvTODiau76, tt.transformPVCoordinates(pvMODiau76Wcorr), 1.8, 1.6e-3);
        checkPV(pvTODiau76, tt.transformPVCoordinates(pvMODiau76), 2.5, 1.5e-3);
        checkPV(pvTODiau76, tf.transformPVCoordinates(pvMODiau76), 1.1e-3, 6.0e-7);
        checkPV(pvTODiau76, tf.transformPVCoordinates(pvMODiau76Wcorr), 0.90615, 7.4e-4);

    }

    @Test
    public void testAASReferenceGEO() throws OrekitException {

        // this reference test has been extracted from the following paper:
        // Implementation Issues Surrounding the New IAU Reference Systems for Astrodynamics
        // David A. Vallado, John H. Seago, P. Kenneth Seidelmann
        // http://www.centerforspace.com/downloads/files/pubs/AAS-06-134.pdf

        AbsoluteDate t0 = new AbsoluteDate(new DateComponents(2004, 06, 01),
                                           TimeComponents.H00,
                                           TimeScalesFactory.getUTC());

        Transform tt = FramesFactory.getMOD(true).getTransformTo(FramesFactory.getTOD(true), t0);
        Transform tf = FramesFactory.getMOD(false).getTransformTo(FramesFactory.getTOD(false), t0);

        //TOD iau76
        PVCoordinates pvTODiau76 =
            new PVCoordinates(new Vector3D(-40577427.7501, -11500096.1306, 10293.2583),
                              new Vector3D(837.552338, -2957.524176, -0.928772));
        //MOD iau76
        PVCoordinates pvMODiau76 =
            new PVCoordinates(new Vector3D(-40576822.6385, -11502231.5013, 9738.2304),
                              new Vector3D(837.708020, -2957.480118, -0.814275));
        //MOD iau76 w corr
        PVCoordinates pvMODiau76Wcorr =
            new PVCoordinates(new Vector3D(-40576822.6395, -11502231.5015, 9733.7842),
                              new Vector3D(837.708020, -2957.480117, -0.814253));

        checkPV(pvTODiau76, tt.transformPVCoordinates(pvMODiau76Wcorr), 1.4, 7.9e-4);
        checkPV(pvTODiau76, tf.transformPVCoordinates(pvMODiau76Wcorr), 4.5, 2.3e-5);

        checkPV(pvTODiau76, tf.transformPVCoordinates(pvMODiau76), 5.2e-4, 9.1e-7);
        checkPV(pvTODiau76, tt.transformPVCoordinates(pvMODiau76), 3.5, 7.7e-4);

    }

    @Test
    public void testInterpolationAccuracy() throws OrekitException, FileNotFoundException {

        final boolean withNutationCorrection = true;
        
        TODFrame interpolatingFrame =
            new TODFrame(withNutationCorrection, Predefined.TOD_WITH_EOP_CORRECTIONS);
        NonInterpolatingTODFrame nonInterpolatingFrame =
            new NonInterpolatingTODFrame(withNutationCorrection, Predefined.TOD_WITH_EOP_CORRECTIONS);

        // the following time range is located around the maximal observed error
        AbsoluteDate start = new AbsoluteDate(2002, 11, 11, 0, 0, 0.0, TimeScalesFactory.getTAI());
        AbsoluteDate end   = new AbsoluteDate(2002, 11, 15, 6, 0, 0.0, TimeScalesFactory.getTAI());
        double maxError = 0.0;
        for (AbsoluteDate date = start; date.compareTo(end) < 0; date = date.shiftedBy(60)) {
            final Transform transform =
                interpolatingFrame.getTransformTo(nonInterpolatingFrame, date);
            final double error = transform.getRotation().getAngle() * 648000 / FastMath.PI;
            maxError = FastMath.max(maxError, error);
        }

        Assert.assertTrue(maxError < 7.2e-11);

    }

    @Before
    public void setUp() {
        Utils.setDataRoot("testpef-data");
    }

    private class NonInterpolatingTODFrame extends TODFrame {
        private static final long serialVersionUID = -7116622345154042273L;
        public NonInterpolatingTODFrame(final boolean ignoreNutationCorrection,
                                         final Predefined factoryKey)
            throws OrekitException {
            super(ignoreNutationCorrection, factoryKey);
        }
        protected void setInterpolatedNutationElements(final double t) {
            computeNutationElements(t);
        }
    }

    private void checkPV(PVCoordinates reference,
                         PVCoordinates result, double positionThreshold,
                         double velocityThreshold) {

        Vector3D dP = result.getPosition().subtract(reference.getPosition());
        Vector3D dV = result.getVelocity().subtract(reference.getVelocity());
        Assert.assertEquals(0, dP.getNorm(), positionThreshold);
        Assert.assertEquals(0, dV.getNorm(), velocityThreshold);
    }

}
