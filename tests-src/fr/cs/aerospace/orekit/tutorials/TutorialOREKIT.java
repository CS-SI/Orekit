package fr.cs.aerospace.orekit.tutorials;

import java.text.ParseException;
import fr.cs.aerospace.orekit.errors.OrekitException;

public class TutorialOREKIT {

  /** This tutorial program is a first approach of the OREKIT usage. 
   * @param args
   * @throws OrekitException 
   * @throws ParseException 
   * @throws OrekitException 
   */
  public static void main(String[] args) throws ParseException, OrekitException {
    
        TransformAndFrames.transform();
        KeplerianPropagation.keplerianPropagation();
        NumericalPropagation.numericalPropagation();
        NumericalPropagation.numericalPropagationWithStepHandler();
        
  }

}
