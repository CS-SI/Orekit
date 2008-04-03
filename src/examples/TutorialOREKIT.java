

import java.text.ParseException;
import fr.cs.orekit.errors.OrekitException;

public class TutorialOREKIT {

    /** This tutorial program is a first approach of the OREKIT usage.
     * @param args
     * @exception OrekitException
     * @exception ParseException
     * @exception OrekitException
     */
    public static void main(String[] args) throws ParseException, OrekitException {

        TransformAndFrames.transform();
        KeplerianPropagation.keplerianPropagation();
        NumericalPropagation.numericalPropagation();
        NumericalPropagation.numericalPropagationWithStepHandler();

    }

}
