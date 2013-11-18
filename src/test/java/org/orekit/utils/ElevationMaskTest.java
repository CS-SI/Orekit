package org.orekit.utils;

import org.apache.commons.math3.util.FastMath;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.orekit.errors.OrekitException;


public class ElevationMaskTest {

    @Before
    public void setUp()
        throws Exception {
    }

    @After
    public void tearDown()
        throws Exception {
    }

    @Test
    public void testElevationMask() {
        double [][] masqueData = {{FastMath.toRadians(  0),FastMath.toRadians(5)},
                                  {FastMath.toRadians(180),FastMath.toRadians(3)},
                                  {FastMath.toRadians(-90),FastMath.toRadians(4)}};

        ElevationMask mask = new ElevationMask(masqueData);
        
        Assert.assertNotNull(mask);
    }

    @Test(expected=IllegalArgumentException.class)
    public void testMaskException() throws OrekitException {
        double [][] masque = {{FastMath.toRadians(   0),FastMath.toRadians(5)},
                              {FastMath.toRadians( 360),FastMath.toRadians(4)}};
        
        new ElevationMask(masque);
    }

    @Test
    public void testGetElevation() throws OrekitException {
        double [][] masqueData = {{FastMath.toRadians(  0),FastMath.toRadians(5)},
                              {FastMath.toRadians(180),FastMath.toRadians(3)},
                              {FastMath.toRadians(-90),FastMath.toRadians(4)}};
        ElevationMask mask = new ElevationMask(masqueData);

        double azimuth = FastMath.toRadians(90);
        double elevation = mask.getElevation(azimuth);
        Assert.assertEquals(FastMath.toRadians(4), elevation, 1.0e-15);
    }

}
