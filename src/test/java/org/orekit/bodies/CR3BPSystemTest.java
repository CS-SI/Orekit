package org.orekit.bodies;

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.FastMath;
import org.junit.Assert;
import org.junit.Test;
import org.orekit.Utils;
import org.orekit.frames.Frame;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.Constants;

public class CR3BPSystemTest {

    @Test
    public void testCR3BPSystem() {
	Utils.setDataRoot("regular-data");

	final double lDim = CR3BPFactory.getSunEarthCR3BP().getLdim();
	Assert.assertNotNull(lDim);
	
	final double vDim = CR3BPFactory.getSunEarthCR3BP().getVdim();
	Assert.assertNotNull(vDim);
	
	final double tDim = CR3BPFactory.getSunEarthCR3BP().getTdim();
	Assert.assertNotNull(tDim);
    }
    
    @Test
    public void testgetBarycenter() {
	Utils.setDataRoot("regular-data");

	final double bary = CR3BPFactory.getSunEarthCR3BP().getBarycenter();
	Assert.assertNotNull(bary);
    }
    
    @Test
    public void testgetPrimary() {
	Utils.setDataRoot("regular-data");

	final CelestialBody primaryBody = CR3BPFactory.getSunEarthCR3BP().getPrimary();
	Assert.assertNotNull(primaryBody);
    }
    
    @Test
    public void testgetSecondary() {
	Utils.setDataRoot("regular-data");

	final CelestialBody secondaryBody = CR3BPFactory.getSunEarthCR3BP().getSecondary();
	Assert.assertNotNull(secondaryBody);	
    }
    
    @Test
    public void testgetMu() {
	Utils.setDataRoot("regular-data");

	final double mu = CR3BPFactory.getSunEarthCR3BP().getMu();
	Assert.assertNotNull(mu);
    }
    
    @Test
    public void testgetName() {
	Utils.setDataRoot("regular-data");

	final String name = CR3BPFactory.getSunEarthCR3BP().getName();
	Assert.assertNotNull(name);
    }
    
    @Test
    public void testgetLFrame() {
	Utils.setDataRoot("regular-data");

	
	final Frame l2Frame = CR3BPFactory.getEarthMoonCR3BP().getL2Frame();
	Assert.assertNotNull(l2Frame);
	
	final Frame l3Frame = CR3BPFactory.getSunEarthCR3BP().getL3Frame();
	Assert.assertNotNull(l3Frame);	
    }
    
    @Test
    public void testLOrientation() {

        final AbsoluteDate date0 = new AbsoluteDate(2000, 01, 1, 11, 58, 20.000,
                                                   TimeScalesFactory.getUTC());
        final CelestialBody sun     = CelestialBodyFactory.getSun();
        final CelestialBody earth   = CelestialBodyFactory.getEarth();
        final Frame l1Frame = CR3BPFactory.getSunEarthCR3BP().getL1Frame();
        for (double dt = -Constants.JULIAN_DAY; dt <= Constants.JULIAN_DAY; dt += 3600.0) {
            final AbsoluteDate date              = date0.shiftedBy(dt);
            final Vector3D     sunPositionInL1   = sun.getPVCoordinates(date, l1Frame).getPosition();
            final Vector3D     earthPositionInL1 = earth.getPVCoordinates(date, l1Frame).getPosition();
            Assert.assertEquals(FastMath.PI, Vector3D.angle(sunPositionInL1,   Vector3D.MINUS_I), 3.0e-14);
            Assert.assertEquals(FastMath.PI, Vector3D.angle(earthPositionInL1, Vector3D.MINUS_I), 3.0e-14);
        }
    }
    
    @Test
    public void testgetLPos() {
	Utils.setDataRoot("regular-data");

	final Vector3D l4Position = CR3BPFactory.getSunEarthCR3BP().getL4Position();
	Assert.assertNotNull(l4Position);
	
	final Vector3D l5Position = CR3BPFactory.getSunEarthCR3BP().getL5Position();
	Assert.assertNotNull(l5Position);
    }
}
