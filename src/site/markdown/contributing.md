<!--- Copyright 2002-2020 CS Group
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
5. Be sure to activate checkstyle (use the **checkstyle.xml** file at the root of the project) to help you follow the coding rules of Orekit.
6. Perform your developpement and validation.
7. Update the **changes.xml** file in *src/changes/* directory  (see former entries to help you).
8. Run all Orekit tests to ensure everything work.
9. Commit your code on your branch.
10. Submit a merge request on the forge (click on the green item). Be sure to submit it on the **develop** branch. By default, GitLab will propose you to submit it on the **master** branch. ![merge requests](./images/merge-requests.png)
11. Wait for one of the developers to merge your code on the repository.

If your contribution consists of adding or changing a lot of code lines or class architectures, we invite you to discuss the contribution before on the Orekit [forum](https://forum.orekit.org/). Indeed, a developer can be currently working on the code you want to modify. Moreover, the forum is a good place to discuss future additions to Orekit.

If you have any question during your contribution you can also visit the forum and ask them. The larger the community is, the better Orekit will be. The main rule is that everything intended to be included in Orekit core must be distributed under the Apache License Version 2.0.
