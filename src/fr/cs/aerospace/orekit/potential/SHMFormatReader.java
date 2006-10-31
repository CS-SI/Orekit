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
 * <p> The proper way to use this class is to call the 
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
      if (line==null) {
        iKnow = true;
      }
      else {
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
        // initialize the arrays
        if("SHM".equals(tab[0])) {
          C = new double[Integer.parseInt(tab[1])+1][];
          S = new double[Integer.parseInt(tab[1])+1][];
          for(int i =0; i< C.length ; i++) {
            C[i]= new double[i+1];
            S[i]= new double[i+1];
          }
        }
        // fill the arrays
        if("GRCOEF".equals(line.substring(0,6))) {
          tab[3] = tab[3].replace('D','E');
          tab[4] = tab[4].replace('D','E');
          C[Integer.parseInt(tab[1])]
            [Integer.parseInt(tab[2])] =Double.parseDouble(tab[3]);
          S[Integer.parseInt(tab[1])]
            [Integer.parseInt(tab[2])] =Double.parseDouble(tab[4]);
        }
        if("GRCOF2".equals(tab[0])) {
          tab[3] = tab[3].replace('D','E');
          tab[4] = tab[4].replace('D','E');
          C[Integer.parseInt(tab[1])]
            [Integer.parseInt(tab[2])] =Double.parseDouble(tab[3]);
          S[Integer.parseInt(tab[1])]
            [Integer.parseInt(tab[2])] =Double.parseDouble(tab[4]);
        }
      }      
    }    
  }
    
  /** Get the fully normalized zonal coefficients.
   * @return J the zonal coefficients array.
   */
  public double[] getNormJ() {
    if (J==null) {
      J = new double[C.length];
      for(int i = 0; i<C.length; i++){
        J[i]=-C[i][0];
      }
    }
    return J;
  }
  
  /** Get the fully normalized tesseral-secorial and zonal coefficients. 
   * @return C the coefficients matrix
   */
  public double[][] getNormC() {
    return C;
  }
  
  /** Get the fully normalized tesseral-secorial coefficients. 
   * @return S the coefficients matrix
   */
  public double[][] getNormS() {
    return S;
  }
  
  /** Get the un-normalized  zonal coefficients.
   * @return J the zonal coefficients array.
   */
  public double[] getUnNormJ() {
    if (UJ==null) {
      getUnNormC();
      UJ = new double[UC.length];
      for(int i = 0; i<UC.length; i++){
        UJ[i]=-UC[i][0];
      }
    }
    return UJ;
  }
  
  /** Get the un-normalized tesseral-secorial and zonal coefficients. 
   * @return C the coefficients matrix
   */
  public double[][] getUnNormC() {
    // calculate only if asked
    if (UC==null) {
      UC = new double[C.length][];
      for(int i =0; i< C.length ; i++) {
        UC[i]= new double[i+1];
      }
      // initialization
      double normCoef = 0.0;
      double factN = 1.0;
      double mfactNMinusM = 1.0;
      double mfactNPlusM = 1.0;
      UC[0][0] = C[0][0];
      
      for (int n=1; n<C.length; n++ ) {
        factN *= n;
        mfactNMinusM = factN;
        mfactNPlusM = factN;
        UC[n][0] = Math.sqrt(2.0*n+1)*C[n][0];
        for (int m=1; m<C[n].length; m++) {
          mfactNPlusM *= (n+m);
          mfactNMinusM /= (n-m+1);
          normCoef = Math.sqrt(2*(2.0*n+1)*mfactNMinusM/mfactNPlusM);
          UC[n][m] = normCoef*C[n][m];
        }
      }
    }
    return UC;
  }
  
  /** Get the un-normalized tesseral-secorial coefficients. 
   * @return S the coefficients matrix
   */
  public double[][] getUnNormS() {
    // calculate only if asked
    if (US==null) {
      US = new double[S.length][];
      for(int i =0; i< S.length ; i++) {
        US[i]= new double[i+1];
      }
      // initialization
      double normCoef = 0.0;
      double factN = 1.0;
      double mfactNMinusM = 1.0;
      double mfactNPlusM = 1.0;
      US[0][0] = S[0][0];
      
      for (int n=1; n<S.length; n++ ) {
        factN *= n;
        mfactNMinusM = factN;
        mfactNPlusM = factN;
        US[n][0] = Math.sqrt(2.0*n+1)*S[n][0];
        for (int m=1; m<S[n].length; m++) {
          mfactNPlusM *= (n+m);
          mfactNMinusM /= (n-m+1);
          normCoef = Math.sqrt(2*(2.0*n+1)*mfactNMinusM/mfactNPlusM);
          US[n][m] = normCoef*S[n][m];
        }
      }
    }
    return US;
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
  
  /** fully normalized zonal coefficients array */
  private double[] J;
  
  /** fully normalized tesseral-secorial coefficients matrix */
  private double[][] C;
  
  /** fully normalized tesseral-secorial coefficients matrix */
  private double[][] S;
  
  /** un-normalized zonal coefficients array */
  private double[] UJ;
  
  /** un-normalized tesseral-secorial coefficients matrix */
  private double[][] UC;
  
  /** un-normalized tesseral-secorial coefficients matrix */
  private double[][] US;

  public double[][] getNormC(int n, int m) throws OrekitException {
    // TODO Auto-generated method stub
    return null;
  }

  public double[] getNormJ(int n, int m) {
    // TODO Auto-generated method stub
    return null;
  }

  public double[][] getNormS(int n, int m) {
    // TODO Auto-generated method stub
    return null;
  }

  public double[][] getUnNormC(int n, int m) {
    // TODO Auto-generated method stub
    return null;
  }

  public double[] getUnNormJ(int n, int m) {
    // TODO Auto-generated method stub
    return null;
  }

  public double[][] getUnNormS(int n, int m) {
    // TODO Auto-generated method stub
    return null;
  }
  
}
