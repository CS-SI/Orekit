/* Copyright 2002-2009 CS Communication & Systèmes
 * Licensed to CS Communication & Systèmes (CS) under one or more
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

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.Authenticator;
import java.net.PasswordAuthentication;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
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
 * @version $Revision$ $Date$
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

        final JDialog dialog =
            new JDialog((JDialog) null, "enter password", true);
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        final SpringLayout layout = new SpringLayout();
        dialog.setLayout(layout);

        final JLabel messageLabel = new JLabel(getRequestingPrompt());
        layout.putConstraint(SpringLayout.NORTH,             messageLabel, 5,
                             SpringLayout.NORTH,             dialog.getContentPane());
        layout.putConstraint(SpringLayout.HORIZONTAL_CENTER, messageLabel, 0,
                             SpringLayout.HORIZONTAL_CENTER, dialog.getContentPane());
        dialog.add(messageLabel);

        final JLabel userNameLabel = new JLabel("username");
        layout.putConstraint(SpringLayout.NORTH, userNameLabel, 5,
                             SpringLayout.SOUTH, messageLabel);
        layout.putConstraint(SpringLayout.WEST,  userNameLabel, 10,
                             SpringLayout.WEST,  dialog.getContentPane());
        dialog.add(userNameLabel);
        final JTextField userNameField = new JTextField(10);
        layout.putConstraint(SpringLayout.BASELINE, userNameField, 0,
                             SpringLayout.BASELINE, userNameLabel);
        layout.putConstraint(SpringLayout.WEST,     userNameField, 20,
                             SpringLayout.EAST,     userNameLabel);
        dialog.add(userNameField);

        final JLabel passwordLabel = new JLabel("password");
        layout.putConstraint(SpringLayout.NORTH,    passwordLabel, 5,
                             SpringLayout.SOUTH,    userNameLabel);
        layout.putConstraint(SpringLayout.WEST,     passwordLabel, 0,
                             SpringLayout.WEST,     userNameLabel);
        dialog.add(passwordLabel);
        final JPasswordField passwordField = new JPasswordField(10);
        layout.putConstraint(SpringLayout.BASELINE, passwordField, 0,
                             SpringLayout.BASELINE, passwordLabel);
        layout.putConstraint(SpringLayout.WEST,     passwordField, 0,
                             SpringLayout.WEST,     userNameField);
        layout.putConstraint(SpringLayout.EAST,     passwordField, 0,
                             SpringLayout.EAST,     userNameField);
        dialog.add(passwordField);

        final JButton okButton = new JButton("OK");
        layout.putConstraint(SpringLayout.NORTH,             okButton, 15,
                             SpringLayout.SOUTH,             passwordLabel);
        layout.putConstraint(SpringLayout.EAST,              okButton, -15,
                             SpringLayout.HORIZONTAL_CENTER, dialog.getContentPane());
        dialog.add(okButton);

        final JButton cancelButton = new JButton("Cancel");
        layout.putConstraint(SpringLayout.BASELINE,          cancelButton, 0,
                             SpringLayout.BASELINE,          okButton);
        layout.putConstraint(SpringLayout.WEST,              cancelButton, 15,
                             SpringLayout.HORIZONTAL_CENTER, dialog.getContentPane());
        dialog.add(cancelButton);

        layout.putConstraint(SpringLayout.SOUTH, dialog.getContentPane(), 0,
                             SpringLayout.SOUTH, cancelButton);
        layout.putConstraint(SpringLayout.EAST,  dialog.getContentPane(), 10,
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
