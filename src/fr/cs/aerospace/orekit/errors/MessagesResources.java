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
      "non-increasing dates in UTC/TAI time steps file ({0})" },
    { "null parent frame",
      "null parent frame" },
    { "IERS local cache top directory not defined",
      "IERS local cache top directory not defined" },
    { "IERS local cache top directory {0}",
      "IERS local cache top directory {0}" },
    { "{0} is not a directory",
      "{0} is not a directory" },
    { "no IERS data file found in local cache {0}",
      "no IERS data file found in local cache {0}" },
    { "missing IERS data between {0} and {1}",
      "missing IERS data between {0} and {1}" },
    { "duplicated IERS data at {0}",
      "duplicated IERS data at {0}" },
    { "unable to parse line {0} in IERS data file {1}",
      "unable to parse line {0} in IERS data file {1}" },
    { "file {0} is not an IERS data file",
      "file {0} is not an IERS data file" },
    { "missing serie j = {0} in nutation model file {1} (line {2})",
      "missing serie j = {0} in nutation model file {1} (line {2})" },
    { "unexpected end of nutation model file {0} (after line {1})",
      "unexpected end of nutation model file {0} (after line {1})" },
    { "unable to parse line {0} of nutation model file {1}:\n{2}",
      "unable to parse line {0} of nutation model file {1}:\n{2}" },
    { "unable to find nutation model file {0}",
      "unable to find nutation model file {0}" }

  };

}
