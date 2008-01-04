package fr.cs.orekit.errors;

import java.util.ListResourceBundle;

/** This class gather the message resources for the orekit library.
 * @author Luc Maisonobe
 */
public class MessagesResources
  extends ListResourceBundle {

  /** Simple constructor.
   */
  public MessagesResources() {
  }

  public Object[][] getContents() {
    return (Object[][]) contents.clone();
  }

  static final Object[][] contents = {
    { "unable to converge after {0} iterations",
      "unable to converge after {0} iterations" },
    { "trajectory inside the Brillouin sphere (r = {0})",
      "trajectory inside the Brillouin sphere (r = {0})" },
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
    { "{0} is not a directory",
      "{0} is not a directory" },
    { "missing Earth Orientation Parameters between {0} and {1}",
      "missing Earth Orientation Parameters between {0} and {1}" },
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
      "unable to find nutation model file {0}" },
    { "Choosen frame type is not correct",
      "Choosen frame type is not correct" },
    { "Choosen attitude type is not correct",
      "Choosen attitude type is not correct" },
    { "Mass is null or negative",
      "Mass is null or negative" },
    { "Flow rate (dm/dt) is positive : ",
      "Flow rate (dm/dt) is positive : " },
    { "null norm",
      "null norm" },
    { "polar trajectory (r1 = {0})",
      "polar trajectory (r1 = {0})" },
    { "C and S should have the same size :" +
      " (C = [{0}][{1}] ; S = [{2}][{3}])",
      "C and S should have the same size :" +
      " (C = [{0}][{1}] ; S = [{2}][{3}])" },
    { "the reader has not been tested ",
      "the reader has not been tested " },
    { "too large degree (n = {0}), potential maximal degree is {1})",
      "too large degree (n = {0}), potential maximal degree is {1})" },
    { "too large order (m = {0}), potential maximal order is {1})",
      "too large order (m = {0}), potential maximal order is {1})" },
    { "Unknown file format ",
      "Unknown file format " },
    { "Cheksum of line 1 is not correct. Should be: {0} but is: {1}",
      "Cheksum of line 1 is not correct. Should be: {0} but is: {1}" },
    { "Cheksum of line 2 is not correct. Should be: {0} but is: {1}",
      "Cheksum of line 2 is not correct. Should be: {0} but is: {1}" },
    { "Eccentricity is becoming greater than 1. Unable to continue TLE propagation." +
      "Satellite number : {0}. Element number : {1}.",
      "Eccentricity is becoming greater than 1. Unable to continue TLE propagation." +
      "Satellite number : {0}. Element number : {1}." },
    { "Perige within earth." + "Satellite number : {0}. Element number : {1}.",
      "Perige within earth." + "Satellite number : {0}. Element number : {1}." },
    { "Current date is out of range. " +
      "Solar activity datas are not available" ,
      "Current date is out of range. " +
      "Solar activity datas are not available" },
    { "non-existent month {0}",
      "non-existent month {0}" },
    { "non-existent date {0}-{1}-{2}",
      "non-existent date {0}-{1}-{2}" },
    { "non-existent hour {0}:{1}:{2}",
      "non-existent hour {0}:{1}:{2}" },
    { "out of range seconds number: {0}",
      "out of range seconds number: {0}" }

  };

}
