package fr.cs.aerospace.orekit.perturbations;


import java.io.*;
import java.util.StringTokenizer;
import java.util.NoSuchElementException;
import java.util.Vector;

/*
 * TestLecture.java
 *
 * Created on 6 août 2002, 14:14
 */

/** Class TestLecture run the user problem of orbital extrapolation.
 *
 *<p> This class allows the user to choose his extrapolation model (Analytic or numerical model),
 * and then to choose the force models he want to add according to his need for precision. When
 * creating the initial orbit, an extrapolation method can be applied.</p>
 *
 *@version $Id$
 * @author  M. Romero
 * @author  E. Delente
 */


public class PotentialCoefficientsTab {
    
    /** File to read */
    public String fileName;
    
    /** Model name */
    public String modelName;
    
    /** Model dimension */
    public int ndeg;
    public int nord;

    /** Normalized Coefficients */
    double[][] normalizedClm;
    double[][] normalizedSlm;
    
    /** Creates a new instance  */
    public PotentialCoefficientsTab(String name){
     this.fileName = name;
    }
    
    /** Gets ndeg */
    public int getNdeg() {
        return this.ndeg;
    }

    /** Gets nord */
    public int getNord() {
        return this.nord;
    }
    
    /** Gets the normalized Clm coefficients */
    public double[][] getNormalizedClm() {
        return this.normalizedClm;
    }
    
    /** Gets the normalized Slm coefficients */
    public double[][] getNormalizedSlm() {
        return this.normalizedSlm;
    }
    
    /** Reading method  */
    public void read(){
    
        int i, j, l;
        int lDim = 1;
        int mDim = 1;
                
        Vector vectorL = new Vector();
        Vector vectorM = new Vector();
        Vector vectorClm = new Vector();
        Vector vectorSlm = new Vector();
        Vector comment = new Vector();
        Vector ClmStandardDeviation = new Vector();
        Vector SlmStandardDeviation = new Vector(); 
         
        i = 0;
        
        try{
            BufferedReader potentialModel = new BufferedReader(new FileReader(fileName));
//            System.out.println("passage dans le constructeur de CentralBodyPotential");
            String currentLine;
            while((currentLine = potentialModel.readLine()) != null){
                j = 0;
                StringTokenizer st = new StringTokenizer(currentLine);
//                System.out.println("count tokens = " + st.countTokens());
                while(st.hasMoreTokens()){
                    if(i ==0 && j == 0){
                        lDim =  Integer.parseInt(st.nextToken()) + 1;
                    }
                    if(i ==0 && j == 1){
                        mDim = Integer.parseInt(st.nextToken()) + 1;
                    }                       
                    if(i >=15 && j == 0){
                        vectorL.addElement(st.nextToken());
                    }
                    if(i >=15 && j == 1){
                         vectorM.addElement(st.nextToken());
                    }
                    if(i >=15 && j == 2){
                         vectorClm.addElement(st.nextToken());
                    }
                    if(i >=15 && j == 3){
                         vectorSlm.addElement(st.nextToken());
                    }
                    if(i >=15 && j == 4){
                         ClmStandardDeviation.addElement(st.nextToken());
                    }                    
                     if(i >=15 && j == 5){
                         SlmStandardDeviation.addElement(st.nextToken());
                    }                                     
                    if(i >=1 && i < 15){
                        comment.addElement(st.nextToken());
                    }
                    j++;                   
                }
                i++;
            }
            
            normalizedClm = new double[lDim][mDim];
            normalizedSlm = new double[lDim][mDim];
            for(int n = 0 ; n < lDim ; n++){
                for(int p = 0 ; p < mDim ; p++){
                    normalizedClm[n][p] = 0.0;
                    normalizedSlm[n][p] = 0.0;
                }
            }
            
            for(l = 0; l < vectorL.size(); l++){ 
                 this.normalizedClm[Integer.parseInt(vectorL.get(l).toString())][Integer.parseInt(vectorM.get(l).toString())] = Double.parseDouble(vectorClm.get(l).toString());
                 this.normalizedSlm[Integer.parseInt(vectorL.get(l).toString())][Integer.parseInt(vectorM.get(l).toString())] = Double.parseDouble(vectorSlm.get(l).toString());                
            }
            
        }
        catch(FileNotFoundException fnfe){
            System.err.println(fnfe);
        }
        catch(NoSuchElementException nsee){
            System.err.println(nsee);
        }
        catch (NullPointerException npe){
            System.err.println(npe);
        }
        catch (IOException ioe){
            System.err.println(ioe);
        }
        
    this.ndeg = lDim - 1;
    this.nord = mDim - 1;
    
    }
    

}
