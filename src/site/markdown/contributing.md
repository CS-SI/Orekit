<!--- Copyright 2002-2020 CS GROUP
  Licensed under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License.
  You may obtain a copy of the License at
  
    http://www.apache.org/licenses/LICENSE-2.0
  
  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.
-->

# Contributing to Orekit

Orekit is free software, which means you can use the source code as you wish,
without charges, in your applications, and that you can improve it and have
your improvements included in the next mainstream release.

If you are interested in participating in the development effort, **thank you very much**!
Here are the steps you will need to follow:

1. If not already done, create an account on the Orekit forge [https://gitlab.orekit.org/orekit/orekit](https://gitlab.orekit.org/orekit/orekit).
2. Fork the Orekit project using the link on the main page of the forge (red rectangle on the following image). ![fork](./images/orekit-fork.png)
3. Pull the **develop** branch of Orekit.
4. Create a new branch on your fork. The branch must:
	- have **develop** branch as source branch.
	- have a name related to the future contribution. For instance, if you want to correct an issue, the name must be **issue-XXX** where **XXX** represents the issue number.
5. Be sure to activate checkstyle (use the **checkstyle.xml** file at the root of the project) to help you follow the coding rules of Orekit (see Eclipse example below).
6. Perform your developpement and validation.
7. Update the **changes.xml** file in *src/changes/* directory  (see former entries to help you).
8. Run all Orekit tests to ensure everything work.
9. Commit your code on your branch.
10. Submit a merge request on the forge (click on the green item). Be sure to submit it on the **develop** branch. By default, GitLab will propose you to submit it on the **master** branch. ![merge requests](./images/merge-requests.png)
11. Wait for one of the developers to merge your code on the repository.

If your contribution consists of adding or changing a lot of code lines or class architectures, we invite you to discuss the contribution before on the Orekit [forum](https://forum.orekit.org/). Indeed, a developer can be currently working on the code you want to modify. Moreover, the forum is a good place to discuss future additions to Orekit.

If you have any question during your contribution you can also visit the forum and ask them. The larger the community is, the better Orekit will be. The main rule is that everything intended to be included in Orekit core must be distributed under the Apache License Version 2.0.

## Configure Orekit checkstyle

Checkstyle is a development tool to help programmers write Java code
that adheres to a coding standard. It automates the process of checking
Java code to spare humans of this boring (but important) task.
This makes it ideal for projects that want to enforce a coding standard. 

Configuring checkstyle can be a difficult task when installing
Orekit in an Integrated Development Environment (IDE). However,
it is an important step for contributing to the library.

### Configure checkstyle in Eclipse

Here are the steps you will need to follow to configure checkstyle in Eclipse

#### Installing Eclipse Checkstyle plugin.

In your Eclipse IDE, select *Help* --> *Install New Software...*

Pressing the *Add...* button at the top right of the install wizard will display a small popup asking for the name and location of a new software site.

* Name = Checkstyle
* Location = http://eclipse-cs.sf.net/update/

Press *Add* to close the popup.

Select *Checkstyle* in the *Install* window and click on *Next >*.
Proceed to the end of the wizard, accepting the licenses of the plugin and installing it.

#### Configuring the project.

To create the local configuration, select *Properties* by right-clicking in the context menu of the project explorer panel.

In the *Properties* popup, select *Checkstyle* entry.

In this second popup (i.e. *Local Check Configurations*) define a project relative configuration as presented in the figure below. We browse the workspace to select our checkstyle.xml file, and we tick the *Protect Checkstyle configuration file* check box to prevent the plugin to alter the configuration.

![checkstyle-plugin](./images/project-checkstyle-configuration.png).

The property must be defined by pressing the *Additional properties...* button, which will trigger yet another popup into which the property can be configured as shown below.

![additional-properties](./images/additional-properties.png)

Pressing the OK button in the last open popup ends the local check configuration creation.
Select the *Main* tab in the first popup, which should still be opened.

In the *Main* tab, un-tick the *Use simple configuration* checkbox at the top right. A few buttons should appear, allowing to remove the default Sun checks global configuration (selecting it and pressing the *Remove* button) and add our local configuration instead (pressing the *Add...* button).
Using for example **src/main/java/.*\.java** will apply checkstyle only to the files in the **src/main/java** directory and not to the files in the **src/test/java** directory.

![main-checkstyle-configuration](./images/main-checkstyle-configuration.png)

Press *OK* and *Apply and Close* to finish the installation.

#### Activate the checkstyle. 

Select *Checkstyle* --> *Activate Checkstyle* by right-clicking in the context menu of the project explorer panel.
