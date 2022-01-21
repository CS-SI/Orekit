/* Copyright 2002-2022 CS GROUP
 * Licensed to CS GROUP (CS) under one or more
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
package org.orekit.gnss.metric.ntrip;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;

public class DummyServer {

    private final String[]            fileNames;
    private final ServerSocket        server;
    private final Map<String, String> requestProperties;

    public DummyServer(final String... fileNames) throws IOException {
        this.fileNames         = fileNames.clone();
        this.server            = new ServerSocket(0);
        this.requestProperties = new HashMap<>();
    }

    public int getServerPort() {
        return server.getLocalPort();
    }

    public String getRequestProperty(final String key) {
        return requestProperties.get(key);
    }

    public void run() {
        Executors.newSingleThreadExecutor().execute(() -> {
            try {

                for (String fileName : fileNames) {
                    // get connection from client
                    final Socket             socket = server.accept();
                    final BufferedReader     br1    = new BufferedReader(new InputStreamReader(socket.getInputStream(),
                                                                                               StandardCharsets.UTF_8));

                    // read request until an empty line is found
                    for (String line = br1.readLine(); line != null; line = br1.readLine()) {
                        if (line.trim().isEmpty()) {
                            break;
                        } else {
                            final int colon = line.indexOf(':');
                            if (colon > 0) {
                                requestProperties.put(line.substring(0, colon), line.substring(colon + 2));
                            }
                        }
                    }

                    // serve the file content
                    if (fileName.endsWith(".txt")) {
                        // serve the file as text
                        final OutputStreamWriter osw =
                                        new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8);
                        try (FileInputStream   fis = new FileInputStream(fileName);
                             InputStreamReader isr = new InputStreamReader(fis, StandardCharsets.UTF_8);
                             BufferedReader    br2 = new BufferedReader(isr)) {
                            for (String line = br2.readLine(); line != null; line = br2.readLine()) {
                                osw.write(line);
                                osw.write("\r\n");
                                osw.flush();
                            }
                        }
                    } else {
                        // serve the file as binary
                        final OutputStream os = socket.getOutputStream();
                        try (FileInputStream fis = new FileInputStream(fileName)) {
                            final byte[] buffer = new byte[4096];
                            for (int r = fis.read(buffer); r >= 0; r = fis.read(buffer)) {
                                os.write(buffer, 0, r);
                            }
                        }
                    }

                    socket.close();
                }

            } catch (IOException ioe) {
                throw new RuntimeException(ioe);
            }
        });
    }

}
