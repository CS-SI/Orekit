package fr.cs.aerospace.orekit.perturbations;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.StringTokenizer;



/** Class TestLecture run the user problem of orbital extrapolation.
 *
 *<p> This class allows the user to choose his extrapolation model (Analytic or numerical model),
 * and then to choose the force models he wants to add according to his need for precision. When
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
    public void read() throws IOException {
    
        int i, j, l;
        int lDim = 1;
        int mDim = 1;
                
        ArrayList listL = new ArrayList();
        ArrayList listM = new ArrayList();
        ArrayList listClm = new ArrayList();
        ArrayList listSlm = new ArrayList();
        ArrayList comment = new ArrayList();
        ArrayList ClmStandardDeviation = new ArrayList();
        ArrayList SlmStandardDeviation = new ArrayList(); 
         
        i = 0;
        
            BufferedReader potentialModel = new BufferedReader(new FileReader(fileName));
            String currentLine;
            while((currentLine = potentialModel.readLine()) != null){
                j = 0;
                StringTokenizer st = new StringTokenizer(currentLine);
                while(st.hasMoreTokens()){
                    if(i ==0 && j == 0){
                        lDim =  Integer.parseInt(st.nextToken()) + 1;
                    }
                    if(i ==0 && j == 1){
                        mDim = Integer.parseInt(st.nextToken()) + 1;
                    }                       
                    if(i >=15 && j == 0){
                        listL.add(st.nextToken());
                    }
                    if(i >=15 && j == 1){
                         listM.add(st.nextToken());
                    }
                    if(i >=15 && j == 2){
                         listClm.add(st.nextToken());
                    }
                    if(i >=15 && j == 3){
                         listSlm.add(st.nextToken());
                    }
                    if(i >=15 && j == 4){
                         ClmStandardDeviation.add(st.nextToken());
                    }                    
                     if(i >=15 && j == 5){
                         SlmStandardDeviation.add(st.nextToken());
                    }                                     
                    if(i >=1 && i < 15){
                        comment.add(st.nextToken());
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
            
            for(l = 0; l < listL.size(); l++){ 
                 this.normalizedClm[Integer.parseInt(listL.get(l).toString())][Integer.parseInt(listM.get(l).toString())] = Double.parseDouble(listClm.get(l).toString());
                 this.normalizedSlm[Integer.parseInt(listL.get(l).toString())][Integer.parseInt(listM.get(l).toString())] = Double.parseDouble(listSlm.get(l).toString());                
            }
            
        
    this.ndeg = lDim - 1;
    this.nord = mDim - 1;
    
    }
    

}
