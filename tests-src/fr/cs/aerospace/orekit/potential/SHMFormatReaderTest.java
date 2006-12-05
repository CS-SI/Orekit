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

public class SHMFormatReaderTest extends TestCase {
  
  public void testRead() throws OrekitException, IOException {
    
    File rootDir = FindFile.find("/tests-src/fr/cs/aerospace/orekit/data" +
                                 "/potential/shm-format/g003_eigen-cg01c_coef", "/");
    InputStream in = new FileInputStream(rootDir.getAbsolutePath());
    
    PotentialCoefficientsReader reader = PotentialReaderFactory.getPotentialReader(in);
    reader.read();
    double[][] C = reader.getC(360 , 360, true);
    double[][] S = reader.getS(360 , 360, true);
    
    assertEquals(0.957187536534E-06,C[3][0],  0);
    assertEquals(-.600855921000E-12,C[360][360],  0);
    assertEquals(0, S[4][0],  0);
    assertEquals(-.781165450789E-09 ,S[89][89],  0);
    assertEquals(0.3986004415E+15 ,reader.getMu(),  0);
    assertEquals(0.6378136460E+07 ,reader.getAe(),  0);
    
    rootDir = FindFile.find("/tests-src/fr/cs/aerospace/orekit/data" +
                            "/potential/shm-format/eigen_cg03c_coef", "/");
    in = new FileInputStream(rootDir.getAbsolutePath());
    reader = PotentialReaderFactory.getPotentialReader(in);
    reader.read();
    C = reader.getC(360 , 360, true);;
    S = reader.getS(360 , 360, true);
    
    assertEquals(0.957201462136E-06,C[3][0],  0);
    assertEquals(-.600855921000E-12,C[360][360],  0);
    assertEquals(0, S[4][0],  0);
    assertEquals(-.719392021047E-09 ,S[89][89],  0);
    assertEquals(0.3986004415E+15 ,reader.getMu(),  0);
    assertEquals(0.6378136460E+07 ,reader.getAe(),  0);
    
  }
  
  public void testReadCompressed() throws OrekitException, IOException {
    File rootDir = FindFile.find("/tests-src/fr/cs/aerospace/orekit/data" +
                                 "/potential/shm-format-compressed/eigen-cg01c_coef.gz", "/");
    InputStream in = new FileInputStream(rootDir.getAbsolutePath());
    PotentialCoefficientsReader reader = PotentialReaderFactory.getPotentialReader(in);
    reader.read();
    double[][] C = reader.getC(360 , 360, true);;
    double[][] S = reader.getS(360 , 360, true);;
    
    assertEquals(0.957187536534E-06,C[3][0],  0);
    assertEquals(-.600855921000E-12,C[360][360],  0);
    assertEquals(0, S[4][0],  0);
    assertEquals(-.781165450789E-09 ,S[89][89],  0);
    assertEquals(0.3986004415E+15 ,reader.getMu(),  0);
    assertEquals(0.6378136460E+07 ,reader.getAe(),  0);
    
    rootDir = FindFile.find("/tests-src/fr/cs/aerospace/orekit/data" +
                            "/potential/shm-format-compressed/eigen_cg03c_coef.gz", "/");
    in = new FileInputStream(rootDir.getAbsolutePath());
    reader = PotentialReaderFactory.getPotentialReader(in);
    reader.read();
    C = reader.getC(360 , 360, true);;
    S = reader.getS(360 , 360, true);;
    
    assertEquals(0.957201462136E-06,C[3][0],  0);
    assertEquals(-.600855921000E-12,C[360][360],  0);
    assertEquals(0, S[4][0],  0);
    assertEquals(-.719392021047E-09 ,S[89][89],  0);
    assertEquals(0.3986004415E+15 ,reader.getMu(),  0);
    assertEquals(0.6378136460E+07 ,reader.getAe(),  0);
    
  }
  
  public void testException() throws FileNotFoundException, IOException {
    
    PotentialCoefficientsReader reader;
    int c = 0;
    try {
      File rootDir = FindFile.find("/tests-src/fr/cs/aerospace/orekit/data" +
                                   "/potential/shm-format-corrupted/fakeeigen1", "/");
      InputStream in = new FileInputStream(rootDir.getAbsolutePath());
      reader = PotentialReaderFactory.getPotentialReader(in);
    } catch (OrekitException e) {
      c++;
      // expected behaviour
    }
    try {
      File rootDir = FindFile.find("/tests-src/fr/cs/aerospace/orekit/data" +
                                   "/potential/shm-format-corrupted/fakeeigen2", "/");
      InputStream in = new FileInputStream(rootDir.getAbsolutePath());
      reader = PotentialReaderFactory.getPotentialReader(in);
    } catch (OrekitException e) {
      c++;
      // expected behaviour
    }
    try {
      File rootDir = FindFile.find("/tests-src/fr/cs/aerospace/orekit/data" +
                                   "/potential/shm-format-corrupted/fakeeigen3", "/");
      InputStream in = new FileInputStream(rootDir.getAbsolutePath());
      reader = PotentialReaderFactory.getPotentialReader(in);
    } catch (OrekitException e) {
      c++;
      // expected behaviour
    }
    
    try {
      reader = new SHMFormatReader();
      reader.read();
    } catch (OrekitException e) {
      c++;
      // expected behaviour
    }
    
    assertEquals(4 , c);   
    
  }
  
  public static Test suite() {
    return new TestSuite(SHMFormatReaderTest.class);
  }
}
