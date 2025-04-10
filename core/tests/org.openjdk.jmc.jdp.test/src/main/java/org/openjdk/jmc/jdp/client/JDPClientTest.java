/*
 * Copyright (c) 2018, 2025, Oracle and/or its affiliates. All rights reserved.
 * 
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * The contents of this file are subject to the terms of either the Universal Permissive License
 * v 1.0 as shown at https://oss.oracle.com/licenses/upl
 *
 * or the following license:
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted
 * provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice, this list of conditions
 * and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright notice, this list of
 * conditions and the following disclaimer in the documentation and/or other materials provided with
 * the distribution.
 * 
 * 3. Neither the name of the copyright holder nor the names of its contributors may be used to
 * endorse or promote products derived from this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 * FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY
 * WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.openjdk.jmc.jdp.client;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.Properties;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;

import org.openjdk.jmc.jdp.common.Configuration;
import org.openjdk.jmc.jdp.server.JDPServer;

@SuppressWarnings("nls")
public class JDPClientTest {
	public final static Logger LOGGER = Logger.getLogger("org.openjdk.jmc.jdp.test");
	private final Configuration config = TestToolkit.createConfiguration();
	private volatile BlockingQueue<DiscoveryEvent> eventQueue;
	private String discoverableID;

	@Before
	public void setUp() throws Exception {
		discoverableID = TestToolkit.generateNewID("JDPClientTest");
		System.out.println("JDP test with discoverable id " + discoverableID);
	}

	@Test
	public void testJDPClient() throws Exception {
		Assume.assumeTrue(TestToolkit.areBroadcastingTestsEnabled());
		JDPClient client = createDefaultClient();
		JDPServer server = createDefaultServer();
		client.start();
		server.start();

		DiscoveryEvent event = eventQueue.poll(config.getBroadcastPeriod() * 2, TimeUnit.MILLISECONDS);
		assertNotNull(event);
		assertEquals(DiscoveryEvent.Kind.FOUND, event.getKind());

		server.stop();
		long sleepTime = (long) Math.max(Pruner.PRUNING_INTERVAL,
				config.getBroadcastPeriod() * Pruner.HB_MISSED_BEFORE_DOWN);
		event = eventQueue.poll(sleepTime * 8, TimeUnit.MILLISECONDS);

		assertNotNull(event);
		assertEquals(DiscoveryEvent.Kind.LOST, event.getKind());
		client.stop();
	}

	@Test
	public void testChangePacket() throws Exception {
		Assume.assumeTrue(TestToolkit.areBroadcastingTestsEnabled());
		JDPClient client = createDefaultClient();
		JDPServer server = createDefaultServer();
		client.start();
		server.start();

		DiscoveryEvent event = eventQueue.poll(config.getBroadcastPeriod() * 2, TimeUnit.MILLISECONDS);
		assertNotNull(event);
		assertEquals(event.getKind(), DiscoveryEvent.Kind.FOUND);
		assertEquals(event.getDiscoverable().getPayload().get("apa"), "gorilla");

		event = null;
		Properties props = createDefaultData();
		props.setProperty("apa", "mongo");
		server.setDiscoveryData(props);

		event = eventQueue.poll(config.getBroadcastPeriod() * 2, TimeUnit.MILLISECONDS);
		assertNotNull(event);
		assertEquals(event.getKind(), DiscoveryEvent.Kind.CHANGED);
		assertEquals(event.getDiscoverable().getPayload().get("apa"), "mongo");
		client.stop();
		server.stop();
	}

	public JDPClient createDefaultClient() {
		eventQueue = new LinkedBlockingQueue<>();
		JDPClient client = new JDPClient(config.getMulticastAddress(), config.getMulticastPort());
		client.addDiscoveryListener(new DiscoveryListener() {

			@Override
			public void onDiscovery(DiscoveryEvent event) {
				if (discoverableID.equals(event.getDiscoverable().getPayload().get(JDPServer.KEY_DISCOVERABLE_ID))) {
					eventQueue.add(event);
				}
			}
		});
		return client;
	}

	public JDPServer createDefaultServer() {
		JDPServer server = new JDPServer(discoverableID, config);
		TestToolkit.printServerSettings(server);

		server.setDiscoveryData(createDefaultData());
		return server;
	}

	private Properties createDefaultData() {
		Properties props = new Properties();
		props.setProperty("test", "JDPClientTest");
		props.setProperty("apa", "gorilla");
		return props;
	}
}
