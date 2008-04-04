package fr.cs.orekit.potential;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import fr.cs.orekit.errors.OrekitException;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

public class EGMFormatReaderTest extends TestCase {

    public void testRead() throws OrekitException, IOException {

        InputStream in =
            EGMFormatReaderTest.class.getResourceAsStream("/potential/egm-format/egm96_to5.ascii.gz");

        PotentialReaderFactory factory = new PotentialReaderFactory();
        PotentialCoefficientsReader reader = factory.getPotentialReader(in);

        reader.read();
        double[][] C = reader.getC(5, 5, true);
        double[][] S = reader.getS(5, 5, true);
        assertEquals(0.957254173792E-06 ,C[3][0],  0);
        assertEquals(0.174971983203E-06,C[5][5],  0);
        assertEquals(0, S[4][0],  0);
        assertEquals(0.308853169333E-06,S[4][4],  0);

        double[][] UC = reader.getC(5, 5, false);
        double a = (-0.295301647654E-06);
        double b = 9*8*7*6*5*4*3*2;
        double c = 2*11/b;
        double result = a*Math.sqrt(c);

        assertEquals(result,UC[5][4],  0);

        a = -0.188560802735E-06;
        b = 8*7*6*5*4*3*2;
        c=2*9/b;
        result = a*Math.sqrt(c);
        assertEquals(result,UC[4][4],  0);

        assertEquals(1.0826266835531513e-3, reader.getJ(false, 2)[2],0);

    }

    public void testException() throws FileNotFoundException, IOException {

        PotentialReaderFactory factory = new PotentialReaderFactory();
        int c = 0;
        try {
            InputStream in =
                EGMFormatReaderTest.class.getResourceAsStream("/potential/egm-format-corrupted/fakegm1");
            factory.getPotentialReader(in);
        } catch (OrekitException e) {
            c++;
            // expected behaviour
        }
        try {
            InputStream in =
                EGMFormatReaderTest.class.getResourceAsStream("/potential/egm-format-corrupted/fakegm2");
            factory.getPotentialReader(in);
        } catch (OrekitException e) {
            c++;
            // expected behaviour
        }

        try {
            PotentialCoefficientsReader reader = new SHMFormatReader();
            reader.read();
        } catch (OrekitException e) {
            c++;
            // expected behaviour
        }

        assertEquals(3 , c);

    }

    public static Test suite() {
        return new TestSuite(EGMFormatReaderTest.class);
    }
}
