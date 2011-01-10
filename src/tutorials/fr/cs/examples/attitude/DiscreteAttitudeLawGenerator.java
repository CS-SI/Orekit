package fr.cs.examples.attitude;

import java.util.ArrayList;
import java.util.List;

import org.orekit.attitudes.Attitude;
import org.orekit.attitudes.AttitudeProvider;
import org.orekit.attitudes.DiscreteAttitudeLaw;
import org.orekit.errors.OrekitException;
import org.orekit.errors.PropagationException;
import org.orekit.frames.Frame;
import org.orekit.propagation.Propagator;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.sampling.OrekitFixedStepHandler;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.PVCoordinatesProvider;

/** This class allows to generate a discrete attitude law.

 * The state can be slightly shifted to close dates. This shift is based on
 * a linear extrapolation for attitude taking the spin rate into account.
 * It is <em>not</em> intended as a replacement for proper attitude propagation
 * but should be sufficient for either small time shifts or coarse accuracy.
 * </p>
 * <p>The instance <code>Attitude</code> is guaranteed to be immutable.</p>
 * @see     org.orekit.orbits.Orbit
 * @author V&eacute;ronique Pommier-Maurussane
 * @version $Revision:1665 $ $Date:2008-06-11 12:12:59 +0200 (mer., 11 juin 2008) $
 */

public class DiscreteAttitudeLawGenerator {

    List<Attitude> attEphem;

    /** Generates a discrete attitude law from a complete propagator.
     * @param propagator propagator on which computations are based
     * @param endDate attitude ephemeris end date
     * @param step computation step
     * @throws PropagationException if some specific error occurs during propagation
     */
    public DiscreteAttitudeLaw generate(Propagator propagator, AbsoluteDate endDate, double step) 
        throws PropagationException {        
    
    attEphem = new ArrayList<Attitude>();
    propagator.setMasterMode(step, new LocalStepHandler());
    propagator.propagate(endDate);
    return new DiscreteAttitudeLaw(attEphem);
}

    /** Generates a discrete attitude law from an attitude provider and a pv coordinates provider.
     * @param pvProv PV coordinates provider
     * @param attProv attitude provider
     * @param frame frame in which pv coordinates are expressed and from which attitude is computed
     * @param startDate attitude ephemeris start date
     * @param endDate attitude ephemeris end date
     * @param step computation step
     * @throws OrekitException if some specific error occurs
     */
    public DiscreteAttitudeLaw generate(PVCoordinatesProvider pvProv, AttitudeProvider attProv, Frame frame, 
                                        AbsoluteDate startDate, AbsoluteDate endDate, double step) 
        throws OrekitException {        
        
        final double dtTot = endDate.compareTo(startDate);
        final int nStep = (int) Math.floor(dtTot/step);
        AbsoluteDate date = startDate;

        for (int i = 0; i <= nStep; i++) { 
            date = startDate.shiftedBy(i*step);
            Attitude attitude = attProv.getAttitude(pvProv, date, frame);
            attEphem.add(attitude);
        }
        Attitude attitude = attProv.getAttitude(pvProv, endDate, frame);
        attEphem.add(attitude);
        
        return new DiscreteAttitudeLaw(attEphem);
    }


    /** Local step handler for attitude recording. */
    private class LocalStepHandler implements OrekitFixedStepHandler {
        
        /** Serializable UID.  */
        private static final long serialVersionUID = -7156433414135699565L;

        public void handleStep(SpacecraftState state, boolean isLast) {
            Attitude att = state.getAttitude();
            attEphem.add(att);
        }
    }
}
