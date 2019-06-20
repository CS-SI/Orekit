package org.orekit.bodies;

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.junit.Assert;
import org.junit.Test;
import org.orekit.Utils;
import org.orekit.frames.Frame;
import org.orekit.utils.LagrangianPoints;

public class CR3BPSystemTest {

    @Test
    public void testCR3BPSystem() {
	Utils.setDataRoot("regular-data");
	final CR3BPSystem syst = CR3BPFactory.getSunEarthCR3BP();
	final double lDim = syst.getLdim();
	Assert.assertNotNull(lDim);
	
	final double vDim = syst.getVdim();
	Assert.assertNotNull(vDim);
	
	final double tDim = syst.getTdim();
	Assert.assertNotNull(tDim);
    }
    
   
    @Test
    public void testgetRotatingFrame() {
    Utils.setDataRoot("regular-data");

    final Frame baryFrame = CR3BPFactory.getSunEarthCR3BP().getRotatingFrame();
    Assert.assertNotNull(baryFrame);
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

	final double mu = CR3BPFactory.getSunJupiterCR3BP().getMassRatio();
	Assert.assertNotNull(mu);
    }
    
    @Test
    public void testgetName() {
	Utils.setDataRoot("regular-data");

	final String name = CR3BPFactory.getSunEarthCR3BP().getName();
	Assert.assertNotNull(name);
    }
    
    @Test
    public void testgetLPos() {
	Utils.setDataRoot("regular-data");
	
	final CR3BPSystem syst = CR3BPFactory.getEarthMoonCR3BP();
	
	final Vector3D l1Position = syst.getLPosition(LagrangianPoints.L1);
    Assert.assertEquals(3.23E8, l1Position.getX() * syst.getLdim(),3E6);
    Assert.assertEquals(0.0, l1Position.getY() * syst.getLdim(),1E3);
    Assert.assertEquals(0.0, l1Position.getZ() * syst.getLdim(),1E3);
    
    final Vector3D l2Position = syst.getLPosition(LagrangianPoints.L2);
    Assert.assertEquals(4.45E8, l2Position.getX() * syst.getLdim(),3E6);
    Assert.assertEquals(0.0, l2Position.getY() * syst.getLdim(),1E3);
    Assert.assertEquals(0.0, l2Position.getZ() * syst.getLdim(),1E3);
    
    final Vector3D l3Position = syst.getLPosition(LagrangianPoints.L3);
    Assert.assertEquals(-3.86E8, l3Position.getX() * syst.getLdim(),3E6);
    Assert.assertEquals(0.0, l3Position.getY() * syst.getLdim(),1E3);
    Assert.assertEquals(0.0, l3Position.getZ() * syst.getLdim(),1E3);

	final Vector3D l4Position = syst.getLPosition(LagrangianPoints.L4);
    Assert.assertEquals(1.87E8, l4Position.getX() * syst.getLdim(),3E6);
    Assert.assertEquals(3.32E8, l4Position.getY() * syst.getLdim(),3E6);
    Assert.assertEquals(0.0, l4Position.getZ() * syst.getLdim(),1E3);
	
	final Vector3D l5Position = syst.getLPosition(LagrangianPoints.L5);
    Assert.assertEquals(1.87E8, l5Position.getX() * syst.getLdim(),3E6);
    Assert.assertEquals(-3.32E8, l5Position.getY() * syst.getLdim(),3E6);
    Assert.assertEquals(0.0, l5Position.getZ() * syst.getLdim(),1E3);
    }
    
    @Test
    public void testgetGamma() {
    Utils.setDataRoot("regular-data");

    final CR3BPSystem syst = CR3BPFactory.getSunEarthCR3BP();
    
    final double l1Gamma = syst.getGamma(LagrangianPoints.L1);
    Assert.assertEquals(1.491E9, l1Gamma * syst.getLdim(),1E6);

    
    final double l2Gamma = syst.getGamma(LagrangianPoints.L2);
    Assert.assertEquals(1.501E9, l2Gamma * syst.getLdim(),1E6);

    
    final double l3Gamma = syst.getGamma(LagrangianPoints.L3);
    Assert.assertEquals(1.495E11, l3Gamma * syst.getLdim(),1E8);

    }
}
