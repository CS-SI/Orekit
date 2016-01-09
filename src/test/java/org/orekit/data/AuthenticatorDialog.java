/* Copyright 2002-2016 CS Systèmes d'Information
 * Licensed to CS Systèmes d'Information (CS) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * CS licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.orekit.data;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.Authenticator;
import java.net.PasswordAuthentication;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.Spring;
import javax.swing.SpringLayout;

/** Simple swing-based dialog window to ask username/password.
 * <p>
 * In order to use this class, it should be registered as a default authenticator.
 * This can be done by calling:
 * <pre>
 *   Authenticator.setDefault(new AuthenticatorDialog());
 * </pre>
 * </p>
 * @author Luc Maisonobe
 */
public class AuthenticatorDialog extends Authenticator {

    /** User name. */
    private String userName;

    /** Password. */
    private char[] password;

    /** Simple constructor.
     */
    public AuthenticatorDialog() {
        userName = new String();
        password = new char[0];
    }

    /** {@inheritDoc} */
    protected PasswordAuthentication getPasswordAuthentication() {

        synchronized (AuthenticatorDialog.class) {
        final JDialog dialog = new JDialog();
        dialog.setTitle("enter password");
        dialog.setModal(true);
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        final Component cp = dialog.getContentPane();
        final SpringLayout layout = new SpringLayout();
        dialog.setLayout(layout);

        final JLabel messageLabel = new JLabel(getRequestingPrompt());
        dialog.add(messageLabel);
        layout.putConstraint(SpringLayout.NORTH, messageLabel, 5,
                             SpringLayout.NORTH, cp);
        // SpringLayout.HORIZONTAL_CENTER is not available in Java 5, we do it the hard way
        SpringLayout.Constraints c = layout.getConstraints(messageLabel);
        c.setX(Spring.scale(Spring.sum(Spring.width(cp),
                                       Spring.minus(Spring.width(messageLabel))),
                            0.5f));

        final JLabel userNameLabel = new JLabel("username");
        dialog.add(userNameLabel);
        layout.putConstraint(SpringLayout.NORTH, userNameLabel, 5,
                             SpringLayout.SOUTH, messageLabel);
        layout.putConstraint(SpringLayout.WEST,  userNameLabel, 10,
                             SpringLayout.WEST,  cp);

        final JTextField userNameField = new JTextField(10);
        dialog.add(userNameField);
        layout.putConstraint(SpringLayout.SOUTH, userNameField, 0,
                             SpringLayout.SOUTH, userNameLabel);
        layout.putConstraint(SpringLayout.WEST,  userNameField, 20,
                             SpringLayout.EAST,  userNameLabel);

        final JLabel passwordLabel = new JLabel("password");
        dialog.add(passwordLabel);
        layout.putConstraint(SpringLayout.NORTH, passwordLabel, 5,
                             SpringLayout.SOUTH, userNameLabel);
        layout.putConstraint(SpringLayout.WEST,  passwordLabel, 0,
                             SpringLayout.WEST,  userNameLabel);

        final JPasswordField passwordField = new JPasswordField(10);
        dialog.add(passwordField);
        layout.putConstraint(SpringLayout.SOUTH, passwordField, 0,
                             SpringLayout.SOUTH, passwordLabel);
        layout.putConstraint(SpringLayout.WEST,  passwordField, 0,
                             SpringLayout.WEST,  userNameField);
        layout.putConstraint(SpringLayout.EAST,  passwordField, 0,
                             SpringLayout.EAST,  userNameField);

        final JButton okButton = new JButton("OK");
        dialog.add(okButton);
        layout.putConstraint(SpringLayout.NORTH, okButton, 15,
                             SpringLayout.SOUTH, passwordLabel);
        // SpringLayout.HORIZONTAL_CENTER is not available in Java 5, we do it the hard way
        c = layout.getConstraints(okButton);
        c.setX(Spring.sum(Spring.sum(Spring.scale(Spring.width(cp), 0.5f), Spring.constant(-15)),
                          Spring.minus(Spring.width(okButton))));

        final JButton cancelButton = new JButton("Cancel");
        dialog.add(cancelButton);
        layout.putConstraint(SpringLayout.SOUTH, cancelButton, 0,
                             SpringLayout.SOUTH, okButton);
        // SpringLayout.HORIZONTAL_CENTER is not available in Java 5, we do it the hard way
        c = layout.getConstraints(cancelButton);
        c.setX(Spring.sum(Spring.scale(Spring.width(cp), 0.5f), Spring.constant(15)));

        layout.putConstraint(SpringLayout.SOUTH, cp, 0,
                             SpringLayout.SOUTH, cancelButton);
        layout.putConstraint(SpringLayout.EAST,  cp, 10,
                             SpringLayout.EAST,  passwordField);
        dialog.pack();

        ActionListener al = new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (e.getSource() == cancelButton) {
                    userName = new String();
                    password = new char[0];
                } else {
                    userName = userNameField.getText();
                    password = passwordField.getPassword();
                }
                userNameField.setText(null);
                passwordField.setText(null);
                dialog.setVisible(false);
            }
        };
        passwordField.addActionListener(al);
        okButton.addActionListener(al);
        cancelButton.addActionListener(al);

        dialog.setVisible(true);

        // retrieve user input and reset everything to empty
        // to prevent credentials lying around in memory
        PasswordAuthentication authentication =
            new PasswordAuthentication(userName, password);
        userName = new String();
        password = new char[0];

        return authentication;
        }

    }

}
