package fr.cs.aerospace.orekit.perturbations;

import fr.cs.aerospace.orekit.RDate;
import fr.cs.aerospace.orekit.Attitude;
import fr.cs.aerospace.orekit.OrbitDerivativesAdder;
import fr.cs.aerospace.orekit.Constants;
import fr.cs.aerospace.orekit.OrekitException;
import fr.cs.aerospace.orekit.bodies.RotatingBody;

import org.spaceroots.mantissa.geometry.Vector3D;

/**
 * This class represents the gravitational field of a celestial body.
 
 * <p>The gravitational field of a central body is split in two parts.
 * The first one is the central attraction which is a single coefficient.
 * The second one is the perturbing acceleration which is expressed using
 * spherical harmonics.</p>
 
 * @version $Id$
 * @author L. Maisonobe
 * @author E. Delente
 */

public class CunninghamAttractionModel implements ForceModel {
    
    /** Create a new instance of CentralBodyPotential.
     * @param mu central body attraction coefficient
     * @param body rotating body
     * @param equatorialRadius equatorial radius used for spherical harmonics
     * modelization
     * @param c normalized coefficients array (cosine part)
     * @param s normalized coefficients array (sine part)
     * @param degree degree of potential
     */
    public CunninghamAttractionModel(double mu,  RotatingBody body,
                                    double equatorialRadius,
                                    double[][] C, double[][] S) {
      maxpot = C.length;
      V = new double[maxpot + 3 + 1][maxpot + 3 + 1];
      reald = new double[3];
      imd = new double[3];
      somf = new double[3];
      ndd = Math.max(ndeg+1,4);
      ndd = Math.min(ndd,maxpot+2);
      nod = Math.max(nord+1,4);
      nod = Math.min(nod,maxpot+2);
    }
    
    /** Compute the contribution of the central body potential to the
    * perturbing acceleration, using the Cunningham algorithm.
    * The central part of the acceleration (mu/r^2 term) is not computed here,
    * only the <em>perturbing</em> acceleration is considered, not the main
    * part.
    * @param t current date
    * @param position current position(m)
    * @param velocity current velocity (m/s)
    * @param Attitude current attitude
    * @param adder object where the contribution should be added
    */    

    public void addContribution(RDate t, Vector3D position, Vector3D velocity,
                                Attitude Attitude, OrbitDerivativesAdder adder)
    throws OrekitException{
  
        // Construction of the potential array V(n,m)
        CunninghamPotentialBuilder(ndd, nod, maxpot, position, V);
        
        for (int nn = 1; nn <= (ndeg+1); nn++) {
            int n = ndeg + 1 - nn;
            int npu = n + 1;
            for(int j = 0; j <= 2; j++) {
                somf[j] = 0.0;
            }
            int mpu = Math.min(npu, (nord + 1));
            for (int mm = 1; mm <= mpu; mm++) {
                int m = mm - 1;
                // Calculation of real and imaginary parts of the first 
                // derivative of the Vnm function
                CunninghamPotentialDerivativesBuilder(n, m, maxpot, V, reald, imd);
                for (int j = 0; j <= 2; j++) {
                    somf[j] = somf[j] + reald[j] * C[n][m] + imd[j] * S[n][m];
                }
            }
            
            // Acceleration calculation
            if (equatorialRadius < Constants.Epsilon) {throw new OrekitException("Equatorialradius is equal to 0");}
            fpot[0] = fpot[0] + somf[0] * mu / equatorialRadius;
            fpot[1] = fpot[1] + somf[1] * mu / equatorialRadius;
            fpot[2] = fpot[2] + somf[2] * mu / equatorialRadius;


        }
 
        // Additition of calculated accelration to adder
        adder.addXYZAcceleration(fpot[0], fpot[1], fpot[2]);
    }
    
    /** Potential builder.
     * @param ndeg maximum degree of potential to take into account
     * @param nord maximum order of the potential to take into account
     * @param maxpot maximum degree of potential
     * @param position current position (m)
     * @param V matrix containing all the elementary potentials
     */
    private void CunninghamPotentialBuilder(int ndeg, int nord, int maxpot, 
                                           Vector3D position,double[][] V)
      throws OrekitException {

        // Retrieval of cartesian coordinates
        double x = position.getX();
        double y = position.getY();
        double z = position.getZ();
        
        // Definition of intermediate variables
        double r = Math.sqrt(x * x + y * y + z * z);
        if (r < Constants.Epsilon) {throw new OrekitException("Radius is equal to 0");}
        double cosphicosl = 0.0;
        double cosphisinl = 0.0;
        double sinphi = 0.0; 
        double aeOnr = 0.0;

        cosphicosl = x / r;
        cosphisinl = y / r;
        sinphi = z / r; 
        aeOnr = equatorialRadius / r;

        double aeOnr2 = aeOnr * aeOnr;
        
        int noo = Math.min(maxpot + 3, Math.max(2, nord + 2));
        
        // Construction of the two first columns of matrix V
        V[1][1] = 1.0 / r;

        for (int i = 2; i <= noo; i++) {
            V[i][1] = V[i-1][1] * (double)(2 * i - 3) * aeOnr;
            V[i][2] = V[i][1] * sinphi;
        }
        
        if (ndeg > 1) {
            double mm = Math.min((nord + 1), (ndeg - 1));
            for (int j = 1; j <= mm; j++) {
                for (int i = (j + 2); i <= (ndeg + 1); i++) {
                    V[i][i-j+1] = (V[i-1][i-j] * sinphi * aeOnr * 
                                  (double)(2 * i - 3) - V[i-2][i-j-1] * aeOnr2 *
                                  (double)(i + j - 3)) / (double)(i - j);
                }
            }
        }
        
        if(nord != 0) {
            double sinml[] = new double[maxpot+3];
            double cosml[] = new double [maxpot+3];
            sinml[1] = 0.0;
            cosml[1] = 1.0;
            for (int m = 1; m <= nord; m++) {
                sinml[m+1] = sinml[m] * cosphicosl + cosml[m] * cosphisinl;
                cosml[m+1] = cosml[m] * cosphicosl - sinml[m] * cosphisinl;
                for (int n = (m + 1); n <= (ndeg + 1); n++) {
                    V[n-m][n] = V[n][n-m];
                    V[n][n-m] = V[n][n-m] * sinml[m+1];
                    V[n-m][n] = V[n-m][n] * cosml[m+1];
                }
            }
        }
    }
    
    /** Derivatives builder.
     * @param n degree of the concerened elementary potential
     * @param m order of the concerened elementary potential
     * @param maxpot maximum degree of potential (i.e. size of V)
     * @param V matrix containing all the elementary potentials
     * @param reald real part of first derivative
     * @param imd imaginary part of first derivative
     */    
    private void CunninghamPotentialDerivativesBuilder(int n, int m, int maxpot, 
                                                       double[][] V, double[] reald, double[] imd) {        
        
        // Zonal terms
        if ( m == 0) {
            // Imaginary part
            // For zonal terms, no imaginary part
            imd[0] = 0.0;
            imd[1] = 0.0;
            imd[2] = 0.0;
            // Real part
            reald[0] = - V[n+1][n+2];
            reald[1] = - V[n+2][n+1];
            reald[2] = - V[n+2][n+2] * (double)(n + 1);
        }
        
        // Sectorial terms
        if ( m == 1) {
            // Imaginary part
            double en = (double)n;
            double fn = (double)(n * (n + 1));
            double vn = fn * V[n+2][n+2];
            imd[0] = -V[n+2][n] / 2.0;
            imd[1] = (V[n][n+2] + vn) / 2.0;
            imd[2] = -en * V[n+2][n+1];
            
            // Real part
            reald[0] = (- V[n][n+2] + vn) / 2.0;
            reald[1] = imd[0];
            reald[2] = -en * V[n+1][n+2];
        }
        
        // Tesseral terms
        if ( m >= 2) {
            double enm = (double)(n - m + 1);
            double fnm = (double)(n - m + 1) * (double)(n - m + 2);
            
            // Imaginary part
            imd[0] = (-V[n+2][n-m+1] + fnm * V[n+2][n-m+3]) / 2.0;
            imd[1] = (V[n-m+1][n+2] + fnm * V[n-m+3][n+2]) / 2.0;
            imd[2] = - enm * V[n+2][n-m+2];
            
            // Real part
            reald[0] = (-V[n-m+1][n+2] + fnm * V[n-m+3][n+2]) / 2.0;
            reald[1] = (-V[n+2][n-m+1] - fnm * V[n+2][n-m+3]) / 2.0;
            reald[2] = - enm * V[n-m+2][n+2];
        }
    }

    public SWF[] getSwitchingFunctions() {
      return null;
    }
    
    private int maxpot;

    /** Initialisation of potential array. */
    private double[][] V;
        
    /** Definition of real part and imaginary part of potential derivatives. */
    private double[] reald;
    private double[] imd;
      
    /** Intermediate variables. */
    private double[] somf;
        
    /** Initialization of the acceleration. */
    private int ndd;
    private int nod; 

    /** Central body attraction coefficient. */
    private double mu;
    
    /** Equatorial radius of the Central Body. */
    private double equatorialRadius;
    
    /** First normalized potential tesseral coefficients array. */    
    private double[][] C;
    
    /** Second normalized potential tesseral coefficients array. */    
    private double[][] S;
    
    /** Definition of degree, order and maximum potential size. */
    private int ndeg;
    private int nord;
        
    /** Initialization of the acceleration. */
    private double[] fpot;
    
    /** Rotating body. */
    private RotatingBody body;

}

