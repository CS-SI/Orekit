package fr.cs.aerospace.orekit.errors;

import java.util.ListResourceBundle;

/** This class gather the message resources for the orekit library.
 * @author Luc Maisonobe
 * @version $Id$
 */
public class MessagesResources
  extends ListResourceBundle {

  /** Simple constructor.
   */
  public MessagesResources() {
  }

  public Object[][] getContents() {
    return contents;
  }

  static final Object[][] contents = {
    { "unable to converge after {0} iterations",
      "unable to converge after {0} iterations" },
    { "underground trajectory (r = {0})",
      "underground trajectory (r = {0})" },
    { "polar trajectory (r = {0})",
      "polar trajectory (r = {0})" },
    { "too excentric orbit (e = {0})",
      "too excentric orbit (e = {0})" },
    { "almost equatorial orbit (i = {0} degrees)",
      "almost equatorial orbit (i = {0} degrees)" },
    { "almost critically inclined orbit (i = {0} degrees)",
      "almost critically inclined orbit (i = {0} degrees)" },
    { "unable to compute Eckstein-Hechler mean parameters"
    + " after {0} iterations",
      "unable to compute Eckstein-Hechler mean parameters"
      + " after {0} iterations"},
    { "unexpected element \"{0}\" in UTC/TAI time steps file",
      "unexpected element \"{0}\" in UTC/TAI time steps file" },
    { "missing attribute \"{0}\" in UTC/TAI time steps file",
      "missing attribute \"{0}\" in UTC/TAI time steps file" },
    { "unparsable date in UTC/TAI time steps file ({0})",
      "unparsable date in UTC/TAI time steps file ({0})" },
    { "unparsable step value in UTC/TAI time steps file ({0})",
      "unparsable step value in UTC/TAI time steps file ({0})" },
    { "non-increasing dates in UTC/TAI time steps file ({0})",
      "non-increasing dates in UTC/TAI time steps file ({0})" }
  };

}
