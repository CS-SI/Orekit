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
      + " after {0} iterations"}
  };

}
