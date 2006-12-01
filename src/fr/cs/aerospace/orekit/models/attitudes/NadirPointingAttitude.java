package fr.cs.aerospace.orekit.models.attitudes;

import fr.cs.aerospace.orekit.attitudes.AttitudeKinematics;
import fr.cs.aerospace.orekit.attitudes.AttitudeKinematicsProvider;
import fr.cs.aerospace.orekit.errors.OrekitException;
import fr.cs.aerospace.orekit.frames.Frame;
import fr.cs.aerospace.orekit.time.AbsoluteDate;
import fr.cs.aerospace.orekit.utils.PVCoordinates;


public class NadirPointingAttitude implements AttitudeKinematicsProvider {

  public AttitudeKinematics getAttitudeKinematics(AbsoluteDate date,
                                                  PVCoordinates pv, Frame frame)
      throws OrekitException {
    // TODO Auto-generated method stub
    return null;
  }

}
