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
    { "non-increasing dates in file {0}, line {1}",
      "non-increasing dates in file {0}, line {1}" },
    { "null parent frame",
      "null parent frame" },
    { "IERS root directory {0} does not exist",
      "IERS root directory {0} does not exist" },
    { "missing IERS data between modified julian days {0} and {1}",
      "missing IERS data between modified julian days {0} and {1}" },
    { "duplicated IERS data at modified julian day {0}",
      "duplicated IERS data at modified julian day {0}" },
    { "unable to parse line {0} in IERS data file {1}",
      "unable to parse line {0} in IERS data file {1}" },
    { "file {0} is not an IERS data file",
      "file {0} is not an IERS data file" },
    { "several IERS UTC-TAI history files found: {0} and {1}",
      "several IERS UTC-TAI history files found: {0} and {1}" },
    { "unexpected data line {0} in file {1} (line {2} should not be followed by data)",
      "unexpected data line {0} in file {1} (line {2} should not be followed by data)" },
    { "file {0} is not an IERS UTC-TAI history file",
      "file {0} is not an IERS UTC-TAI history file" },
    { "unable to parse line {0} in IERS UTC-TAI history file {1}",
      "unable to parse line {0} in IERS UTC-TAI history file {1}" },
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
