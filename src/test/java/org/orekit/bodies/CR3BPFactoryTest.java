package org.orekit.bodies;

import org.junit.Assert;
import org.junit.Test;
import org.orekit.Utils;

public class CR3BPFactoryTest {
    
    @Test
    public void getSunJupiterCR3BP() {
	Utils.setDataRoot("regular-data");

	CR3BPSystem sunJupiterCR3BP = CR3BPFactory.getSunJupiterCR3BP();
	Assert.assertNotNull(sunJupiterCR3BP);
    }
    
    @Test
    public void getEarthMoonCR3BP() {
	Utils.setDataRoot("regular-data");

	CR3BPSystem earthMoonCR3BP = CR3BPFactory.getEarthMoonCR3BP();
	Assert.assertNotNull(earthMoonCR3BP);
    }
    
    @Test
    public void getSunEarthCR3BP() {
	Utils.setDataRoot("regular-data");

	CR3BPSystem sunEarthCR3BP = CR3BPFactory.getSunEarthCR3BP();
	Assert.assertNotNull(sunEarthCR3BP);
    }
    
}
