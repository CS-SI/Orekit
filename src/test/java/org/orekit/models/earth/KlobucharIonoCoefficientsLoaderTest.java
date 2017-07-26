package org.orekit.models.earth;

import org.junit.Assert;
import org.junit.Test;
import org.orekit.Utils;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.time.DateComponents;

public class KlobucharIonoCoefficientsLoaderTest {

    @Test
    /**
     * Regular test for 1st of January 2017
     * @throws OrekitException if file does not exist or data cannot be read properly
     */
    public void testRegularFile() throws OrekitException {

        Utils.setDataRoot("klobuchar-ionospheric-coefficients");
        
        KlobucharIonoCoefficientsLoader ionoLoader = new KlobucharIonoCoefficientsLoader();
        DateComponents dateComponents = new DateComponents(2017, 1);
        ionoLoader.loadKlobucharIonosphericCoefficients(dateComponents);
        
        final double alpha[] = ionoLoader.getAlpha();
        final double beta[]  = ionoLoader.getBeta();
        
        Assert.assertEquals(1.2821e-08 , alpha[0], 1e-16);
        Assert.assertEquals(-9.6222e-09, alpha[1], 1e-16);
        Assert.assertEquals(-3.5982e-07, alpha[2], 1e-16);
        Assert.assertEquals(-6.0901e-07, alpha[3], 1e-16);
        
        Assert.assertEquals(1.0840e+05 , beta[0], 1e-16);
        Assert.assertEquals(-1.3197e+05, beta[1], 1e-16);
        Assert.assertEquals(-2.6331e+05, beta[2], 1e-16);
        Assert.assertEquals(4.0570e+05 , beta[3], 1e-16);
    }
    
    @Test
    /**
     * Test of a corrupted file without keyword ALPHA
     */
    public void testCorruptedFileBadKeyword() {
        
        Utils.setDataRoot("klobuchar-ionospheric-coefficients");
        final String fileName = "corrupted-bad-keyword-CGIM0020.17N";
        KlobucharIonoCoefficientsLoader ionoLoader = 
                        new KlobucharIonoCoefficientsLoader(fileName);
                        
        try {
            ionoLoader.loadKlobucharIonosphericCoefficients();
            Assert.fail("An exception should have been thrown");
            
        } catch (OrekitException oe) {
            Assert.assertEquals(OrekitMessages.NO_KLOBUCHAR_ALPHA_BETA_IN_FILE, oe.getSpecifier());
            Assert.assertTrue(((String) oe.getParts()[0]).endsWith(fileName));
        }
    }
    
    @Test
    /**
     * Test of a corrupted file with improper data
     */
    public void testCorruptedFileBadData() {
        
        Utils.setDataRoot("klobuchar-ionospheric-coefficients");
        final String fileName = "corrupted-bad-data-CGIM0020.17N";
        KlobucharIonoCoefficientsLoader ionoLoader = 
                        new KlobucharIonoCoefficientsLoader(fileName);
                        
        try {
            ionoLoader.loadKlobucharIonosphericCoefficients();
            Assert.fail("An exception should have been thrown");
            
        } catch (OrekitException oe) {
            Assert.assertEquals(OrekitMessages.UNABLE_TO_PARSE_LINE_IN_FILE, oe.getSpecifier());
        }
    }
    
    @Test
    /**
     * Test for a file that cannot be found
     */
    public void testAbsentFile() throws OrekitException {
        
        Utils.setDataRoot("klobuchar-ionospheric-coefficients");
        KlobucharIonoCoefficientsLoader ionoLoader = new KlobucharIonoCoefficientsLoader();
        DateComponents dateComponents = new DateComponents(2017, 3);
        
        try {
            ionoLoader.loadKlobucharIonosphericCoefficients(dateComponents);
            Assert.fail("An exception should have been thrown");
            
        } catch (OrekitException oe) {
            Assert.assertEquals(OrekitMessages.KLOBUCHAR_ALPHA_BETA_NOT_AVAILABLE_FOR_DATE,
                                oe.getSpecifier());
            Assert.assertEquals(dateComponents.toString(),
                                (String) oe.getParts()[0]);
        }
    }
}
