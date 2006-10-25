package fr.cs.aerospace.orekit.potential;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import fr.cs.aerospace.orekit.errors.OrekitException;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

public class SHMFormatReaderTest extends TestCase {
  
  public void testRead() throws OrekitException, IOException {
    
    InputStream in = new FileInputStream("/home/fab/workspace/orekit/" +
    "tests-src/fr/cs/aerospace/orekit/data/potential/shm-format/g003_eigen-cg01c_coef");
    PotentialCoefficientsReader reader = PotentialReaderFactory.getPotentialReader(in);
    reader.read();
    double[][] C = reader.getC();
    double[][] S = reader.getS();
    
    assertEquals(0.957187536534E-06,C[3][0],  0);
    assertEquals(-.600855921000E-12,C[360][360],  0);
    assertEquals(0, S[4][0],  0);
    assertEquals(-.781165450789E-09 ,S[89][89],  0);
    assertEquals(0.3986004415E+15 ,reader.getMu(),  0);
    assertEquals(0.6378136460E+07 ,reader.getAe(),  0);
    
     in = new FileInputStream("/home/fab/workspace/orekit/" +
    "tests-src/fr/cs/aerospace/orekit/data/potential/shm-format/eigen_cg03c_coef");
     reader = PotentialReaderFactory.getPotentialReader(in);
    reader.read();
    C = reader.getC();
    S = reader.getS();
    
    assertEquals(0.957201462136E-06,C[3][0],  0);
    assertEquals(-.600855921000E-12,C[360][360],  0);
    assertEquals(0, S[4][0],  0);
    assertEquals(-.719392021047E-09 ,S[89][89],  0);
    assertEquals(0.3986004415E+15 ,reader.getMu(),  0);
    assertEquals(0.6378136460E+07 ,reader.getAe(),  0);
        
  }
  
  public void testReadCompressed() throws OrekitException, IOException {
    
    InputStream in = new FileInputStream("/home/fab/workspace/orekit/" +
    "tests-src/fr/cs/aerospace/orekit/data/potential/shm-format-compressed/eigen-cg01c_coef.gz");
    PotentialCoefficientsReader reader = PotentialReaderFactory.getPotentialReader(in);
    reader.read();
    double[][] C = reader.getC();
    double[][] S = reader.getS();
    
    assertEquals(0.957187536534E-06,C[3][0],  0);
    assertEquals(-.600855921000E-12,C[360][360],  0);
    assertEquals(0, S[4][0],  0);
    assertEquals(-.781165450789E-09 ,S[89][89],  0);
    assertEquals(0.3986004415E+15 ,reader.getMu(),  0);
    assertEquals(0.6378136460E+07 ,reader.getAe(),  0);
    
     in = new FileInputStream("/home/fab/workspace/orekit/" +
    "tests-src/fr/cs/aerospace/orekit/data/potential/shm-format-compressed/eigen_cg03c_coef.gz");
     reader = PotentialReaderFactory.getPotentialReader(in);
    reader.read();
    C = reader.getC();
    S = reader.getS();
    
    assertEquals(0.957201462136E-06,C[3][0],  0);
    assertEquals(-.600855921000E-12,C[360][360],  0);
    assertEquals(0, S[4][0],  0);
    assertEquals(-.719392021047E-09 ,S[89][89],  0);
    assertEquals(0.3986004415E+15 ,reader.getMu(),  0);
    assertEquals(0.6378136460E+07 ,reader.getAe(),  0);
        
  }
  
public void testExeption() throws FileNotFoundException {
    
    InputStream in = new FileInputStream("/home/fab/workspace/orekit/" +
    "tests-src/fr/cs/aerospace/orekit/data/potential/shm-format-corrupted/fakeeigen1");
    
    PotentialCoefficientsReader reader;
    try {
      reader = PotentialReaderFactory.getPotentialReader(in);
    } catch (OrekitException e) {
      // expected behaviour
    } catch (IOException e) {
      e.printStackTrace();
    }

        
  }

  public static Test suite() {
    return new TestSuite(SHMFormatReaderTest.class);
  }
}
