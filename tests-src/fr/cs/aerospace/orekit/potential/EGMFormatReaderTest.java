package fr.cs.aerospace.orekit.potential;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import fr.cs.aerospace.orekit.FindFile;
import fr.cs.aerospace.orekit.errors.OrekitException;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

public class EGMFormatReaderTest extends TestCase {
  
  public void testRead() throws OrekitException, IOException {
    
    File rootDir = FindFile.find("/tests-src/fr/cs/aerospace/orekit/data" +
                                 "/potential/egm-format/egm96_to360.ascii.gz", "/");
    InputStream in = new FileInputStream(rootDir.getAbsolutePath());
    
    PotentialCoefficientsReader reader = PotentialReaderFactory.getPotentialReader(in);
    reader.read();
    double[][] C = reader.getNormC();
    double[][] S = reader.getNormS();    
    assertEquals(0.957254173792E-06 ,C[3][0],  0);
    assertEquals(-0.447516389678E-24,C[360][360],  0);
    assertEquals(0, S[4][0],  0);
    assertEquals(-0.524580548778E-09,S[89][89],  0);
   
    double[][] UC = reader.getUnNormC();
    double a = (-0.267133325490e-06);
    double b = 11*10*9*8*7*6*5*4*3*2;
    double c = 2*13/b;
    double result = a*Math.sqrt(c);
    
    assertEquals(result,UC[6][5],  0);
    
    a = -0.188560802735E-06;
    b = 8*7*6*5*4*3*2;
    c=2*9/b;
    result = a*Math.sqrt(c);
    assertEquals(result,UC[4][4],  0);
    
    assertEquals(1.0826266835531513e-3, reader.getUnNormJ()[2],0);
    
  }
    
  public void testExeption() throws FileNotFoundException {
    
    PotentialCoefficientsReader reader;
    int c = 0;
    try {
      File rootDir = FindFile.find("/tests-src/fr/cs/aerospace/orekit/data" +
                                   "/potential/egm-format-corrupted/fakegm1", "/");
      InputStream in = new FileInputStream(rootDir.getAbsolutePath());
      reader = PotentialReaderFactory.getPotentialReader(in);
    } catch (OrekitException e) {
      c++;
      // expected behaviour
    } catch (IOException e) {
      e.printStackTrace();
    }
    try {
      File rootDir = FindFile.find("/tests-src/fr/cs/aerospace/orekit/data" +
                                   "/potential/egm-format-corrupted/fakegm2", "/");
      InputStream in = new FileInputStream(rootDir.getAbsolutePath());
      reader = PotentialReaderFactory.getPotentialReader(in);
    } catch (OrekitException e) {
      c++;
      // expected behaviour
    } catch (IOException e) {
      e.printStackTrace();
    }
    
    try {
      reader = new SHMFormatReader();
      reader.read();
    } catch (OrekitException e) {
      c++;
      // expected behaviour
    } catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    
    assertEquals(3 , c);   
    
  }
  
  public static Test suite() {
    return new TestSuite(EGMFormatReaderTest.class);
  }
}
