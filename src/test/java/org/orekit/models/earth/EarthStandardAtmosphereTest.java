package org.orekit.models.earth;

import org.apache.commons.math3.util.FastMath;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;


public class EarthStandardAtmosphereTest {

    private final double epsilon = 1e-15;

    @Before
    public void setUp()
        throws Exception {
    }

    @After
    public void tearDown()
        throws Exception {
    }

    @Test
    public void testEarthStandardAtmosphereRefraction() {
        EarthStandardAtmosphereRefraction model = new EarthStandardAtmosphereRefraction();
        
        Assert.assertEquals(model.getPressure(), EarthStandardAtmosphereRefraction.DEFAULT_PRESSURE, epsilon);
        Assert.assertEquals(model.getTemperature(), EarthStandardAtmosphereRefraction.DEFAULT_TEMPERATURE, epsilon);
    }

    @Test
    public void testEarthStandardAtmosphereRefractionDoubleDouble() {
        final double pressure = 100e3;
        final double temperature = 270;
        EarthStandardAtmosphereRefraction model = new EarthStandardAtmosphereRefraction(pressure, temperature);
        
        Assert.assertEquals(model.getPressure(), pressure, epsilon);
        Assert.assertEquals(model.getTemperature(), temperature, epsilon);
    }

    @Test
    public void testSetGetPressure() {
        double pressure = 100e3;
        double temperature = 270;
        EarthStandardAtmosphereRefraction model = new EarthStandardAtmosphereRefraction(pressure, temperature);
        
        Assert.assertEquals(model.getPressure(), pressure, epsilon);
        
        pressure = 105389.2;
        model.setPressure(pressure);
        
        Assert.assertEquals(model.getPressure(), pressure, epsilon);
    }

    @Test
    public void testSetGetTemperature() {
        double pressure = 100e3;
        double temperature = 270;
        EarthStandardAtmosphereRefraction model = new EarthStandardAtmosphereRefraction(pressure, temperature);
        
        Assert.assertEquals(model.getTemperature(), temperature, epsilon);
        
        temperature = 273;
        model.setTemperature(temperature);
        
        Assert.assertEquals(model.getTemperature(), temperature, epsilon);
        
    }

    // test temporarily ignored, waiting for further investigation
    @Test
    @Ignore
    public void testGetRefraction() {
        double pressure = 101325;
        double temperature = 290;
        EarthStandardAtmosphereRefraction model = new EarthStandardAtmosphereRefraction(pressure, temperature);

        double refractedElevation = model.getRefraction(FastMath.toRadians(1.0));
        
        Assert.assertEquals(0.0065023463, refractedElevation, 1e-9);
    }

}
