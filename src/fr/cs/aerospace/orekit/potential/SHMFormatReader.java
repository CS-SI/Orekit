package fr.cs.aerospace.orekit.potential;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import fr.cs.aerospace.orekit.errors.OrekitException;

/**This reader is adapted to the SHM Format.
 * 
 * <p> This format is used to describe the gravity field of EIGEN models, 
 * edited by the GFZ Postdam.  
 * It is described in <a> href="http://www.gfz-potsdam.de/grace/results/"
 *  </a>.
 * 
 * <p> The proper way to use this interface is to call the 
 *  {@link fr.cs.aerospace.orekit.potential.PotentialReaderFactory}
 *  which will determine which reader to use with the selected potential
 *  coefficients file <p>
 *   
 * @see fr.cs.aerospace.orekit.potential.PotentialReaderFactory
 * @author F. Maussion
 */
public class SHMFormatReader implements PotentialCoefficientsReader {
  
  
  /** Simple constructor (the first method to call then is <code>isFileOK</code>).
   */
  protected SHMFormatReader() {
    fileIsOK = false;
  }
  
  /** Check the file to determine if its format is understood by the reader or not.
   * @param in the input to check
   * @return true if it is readable, false if not.
   * @throws IOException when the {@link InputStream} cannot be buffered.
   */
  public boolean isFileOK(InputStream in) throws IOException {
    
    this.in = in;
    BufferedReader r = new BufferedReader(new InputStreamReader(in));
    // tests variables
    boolean iKnow = false;
    boolean earth = false;
    int lineIndex = 0;
    int c = 1;
    // read the first lines to detect the format
    for (String line = r.readLine(); iKnow!=true; line=r.readLine()) {   
      // check the first line
      if (c==1) {
        if (("FIRST ".equals(line.substring(0,6)))&&"SHM    ".equals(line.substring(49,56))) {
        } else {
          iKnow = true;  
        }
      }
      // check for the earth line
      if ((line.length()>=6)&&earth==false&&c>1&&c<25) {
        if ("EARTH ".equals(line.substring(0,6))){
          earth = true;
        } 
      }
      if (c>=25&&earth==false) {
        iKnow = true;
      }
      // check there is at least two coef line
      if((line.length()>=6)&&earth==true&&c>2&&c<27){
        if ("GRCOEF".equals(line.substring(0,6))) {
          lineIndex++;
        }
        if ("GRCOF2".equals(line.substring(0,6))) {
          lineIndex++;
        }
        
      }
      if (c>=27&&lineIndex<2) {
        iKnow = true;
      }
      // if everything is OK, accept
      if (earth==true&&lineIndex>=2){
        iKnow = true;
        fileIsOK = true;         
      }
      c++;
    }
    return fileIsOK;
  }
  
  /** Computes the coefficients by reading the selected (and tested) file 
   * @throws OrekitException when the file has not been initialized or checked.
   * @throws IOException when the file is corrupted.
   */
  public void read() throws OrekitException, IOException {
    if (in==null) {
      throw new OrekitException("the reader has not been tested " , new String[0]);
    }
    if (fileIsOK==false) {
      throw new OrekitException("the reader is not adapted to the format " , new String[0]);
    }
    BufferedReader r = new BufferedReader(new InputStreamReader(in));
    for (String line = r.readLine();line!=null; line=r.readLine()) { 
      if(line.length()>=6) {
        String[] tab = line.split("\\s+");
        // read the earth values
        if("EARTH".equals(tab[0])) {
          tab[1] = tab[1].replace('D','E');
          tab[2] = tab[2].replace('D','E');
          mu = Double.parseDouble(tab[1]);
          ae = Double.parseDouble(tab[2]);
        }
        if("SHM".equals(tab[0])) {
          C = new double[Integer.parseInt(tab[1])+1]
                         [Integer.parseInt(tab[2])+1];
          S = new double[Integer.parseInt(tab[1])+1]
                         [Integer.parseInt(tab[2])+1];
        }
        if("GRCOEF".equals(line.substring(0,6))) {
          tab[3] = tab[3].replace('D','E');
          tab[4] = tab[4].replace('D','E');
          C[Integer.parseInt(tab[1])]
            [Integer.parseInt(tab[2])] =
              Double.parseDouble(tab[3]);
          S[Integer.parseInt(tab[1])]
            [Integer.parseInt(tab[2])] =
              Double.parseDouble(tab[4]);
        }
        if("GRCOF2".equals(tab[0])) {
          tab[3] = tab[3].replace('D','E');
          tab[4] = tab[4].replace('D','E');
          C[Integer.parseInt(tab[1])]
            [Integer.parseInt(tab[2])] =
              Double.parseDouble(tab[3]);
          S[Integer.parseInt(tab[1])]
            [Integer.parseInt(tab[2])] =
              Double.parseDouble(tab[4]);
        }
      }      
    }    
  }
    
  /** Get the zonal coefficients.
   * @return J the zonal coefficients array.
   */
  public double[] getJ() {
    if (J==null) {
      for(int i = 0; i<C.length; i++){
        J[i]=C[i][0];
      }
    }
    return J;
  }
  
  /** Get the tesseral-secorial and zonal coefficients. 
   * @return C the coefficients matrix
   */
  public double[][] getC() {
    return C;
  }
  
  /** Get the tesseral-secorial coefficients. 
   * @return S the coefficients matrix
   */
  public double[][] getS() {
    return S;
  }
  
  /** Get the value of mu associtated to the other coefficients.
   * @return mu (m³/s²)
   */
  public double getMu() {
    return mu;
  }
  
  /** Get the value of the Earth Equatorial Radius.
   * @return ae (m)
   */
  public double getAe() {
    return ae;
  }
  
  /** is file ok? */
  private boolean fileIsOK;
  
  /** The input to check and read */
  private InputStream in;
  
  /**  Earth Equatorial Radius */
  private double ae;
  
  /** Mu */
  private double mu;
  
  /** zonal coefficients array */
  private double[] J;
  
  /** tesseral-secorial coefficients matrix */
  private double[][] C;
  
  /** tesseral-secorial coefficients matrix */
  private double[][] S;
  
}
