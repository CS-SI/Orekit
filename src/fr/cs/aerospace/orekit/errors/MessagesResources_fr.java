package fr.cs.aerospace.orekit.errors;

import java.util.ListResourceBundle;

/** This class gather the french translations of the message resources
 * for the orekit library.
 * @author Luc Maisonobe
 * @version $Id$
 */
public class MessagesResources_fr
  extends ListResourceBundle {

  /** Simple constructor.
   */
  public MessagesResources_fr() {
  }

  public Object[][] getContents() {
    return contents;
  }

  static final Object[][] contents = {
    { "unable to converge after {0} iterations",
      "impossible de converger apr\u00e8s {0} it\u00e9rations" },
    { "underground trajectory (r = {0})",
      "trajectoire souterraine (r = {0})" },
    { "polar trajectory (r = {0})",
      "trajectoire polaire (r = {0})" },
    { "too excentric orbit (e = {0})",
      "orbite trop excentrique (e = {0})" },
    { "almost equatorial orbit (i = {0} degrees)",
      "orbite quasiment \u00e9quatoriale (i = {0} degr\u00e9s)" },
    { "almost critically inclined orbit (i = {0} degrees)",
      "orbite quasiment \u00e0 inclinaison critique (i = {0} degr\u00e9s)" },
    { "unable to compute Eckstein-Hechler mean parameters"
    + " after {0} iterations",
      "impossible de calculer les param\u00e8tres moyens au sens de"
    + " Eckstein-Hechler apr\u00e8s {0} it\u00e9rations"}
  };

}
