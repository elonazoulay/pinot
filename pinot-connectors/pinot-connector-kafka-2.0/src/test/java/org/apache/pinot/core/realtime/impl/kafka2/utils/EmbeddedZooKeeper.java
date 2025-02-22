/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.pinot.core.realtime.impl.kafka2.utils;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import org.apache.commons.io.FileUtils;
import org.apache.zookeeper.server.NIOServerCnxnFactory;
import org.apache.zookeeper.server.ZooKeeperServer;


public class EmbeddedZooKeeper implements Closeable {

  private static final int TICK_TIME = 500;
  private final NIOServerCnxnFactory factory;
  private final ZooKeeperServer zookeeper;
  private final File tmpDir;
  private final int port;

  EmbeddedZooKeeper() throws IOException, InterruptedException {
    this.tmpDir = Files.createTempDirectory(null).toFile();
    this.factory = new NIOServerCnxnFactory();
    this.zookeeper = new ZooKeeperServer(new File(tmpDir, "data"), new File(tmpDir, "log"),
        TICK_TIME);
    InetSocketAddress addr = new InetSocketAddress("127.0.0.1", 0);
    factory.configure(addr, 0);
    factory.startup(zookeeper);
    this.port = zookeeper.getClientPort();
  }

  public int getPort() {
    return port;
  }

  @Override
  public void close() throws IOException {
    zookeeper.shutdown();
    factory.shutdown();
    FileUtils.deleteDirectory(tmpDir);
  }
}
