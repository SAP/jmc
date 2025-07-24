/*
 * Copyright (c) 2025 SAP SE. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 *
 */

package org.openjdk.jmc.agent;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.lang.instrument.Instrumentation;
import java.lang.instrument.UnmodifiableClassException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.jar.JarFile;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.openjdk.jmc.agent.impl.DefaultTransformRegistry;
import org.openjdk.jmc.agent.jfr.JFRTransformDescriptor;
import org.openjdk.jmc.agent.jmx.AgentManagementFactory;
import org.openjdk.jmc.agent.sap.boot.converters.GenericLogger;
import org.openjdk.jmc.agent.sap.boot.util.Command;
import org.openjdk.jmc.agent.sap.boot.util.Commands;
import org.openjdk.jmc.agent.sap.boot.util.Dumps;
import org.openjdk.jmc.agent.util.ModuleUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class SapAgent {

	private static final String CONFIGS_PATH = "org/openjdk/jmc/agent/sap/";
	private static final String CONVERTERS_PREFIX = "org.openjdk.jmc.agent.sap.boot.converters.";
	private static Logger logger = Logger.getLogger(SapAgent.class.getName());
	private static Instrumentation instr;
	private static boolean addedBootJar = false;
	private static HashMap<String, String> seenCommands = new HashMap<>();

	public static void premain(String agentArguments, Instrumentation instrumentation) throws Exception {
		agentmain(agentArguments, instrumentation);
	}

	public static void agentmain(String agentArguments, Instrumentation instrumentation) throws Exception {
		ModuleUtils.openUnsafePackage(instrumentation);
		instr = instrumentation;

		if (agentArguments == null || agentArguments.trim().length() == 0) {
			initializeAgent(null, instrumentation);
		} else {
			if (agentArguments.equals("help")) {
				ensureBootJarAdded();
				Commands.printAllCommands();
				System.exit(0);
			}

			if (agentArguments.startsWith("dump=")) {
				ensureBootJarAdded();
				Dumps.performDump(agentArguments.substring(5));
				return;
			}

			try (InputStream stream = new ByteArrayInputStream(getXmlConfig(agentArguments))) {
				initializeAgent(stream, instrumentation);
			} catch (XMLStreamException | IOException | XMLValidationException e) {
				logger.log(Level.SEVERE, "Failed to read jfr probe definitions from " + agentArguments, e);
			}
		}
	}

	private static void addBootJarIfNeeded(TransformRegistry registry) throws IOException {
		// Check if we need converters from the boot jar.
		boolean needsBootJar = false;

		outer: for (String className : registry.getClassNames()) {
			for (TransformDescriptor descriptor : registry.getTransformData(className)) {
				JFRTransformDescriptor impl = (JFRTransformDescriptor) descriptor;

				for (Field field : impl.getFields()) {
					if (field.hasConverter() && field.getConverterDefinition().startsWith(CONVERTERS_PREFIX)) {
						needsBootJar = true;
						break outer;
					}
				}

				for (Parameter param : impl.getParameters()) {
					if (param.hasConverter() && param.getConverterDefinition().startsWith(CONVERTERS_PREFIX)) {
						needsBootJar = true;
						break outer;
					}
				}
			}
		}

		if (needsBootJar) {
			ensureBootJarAdded();
		}
	}

	private static void ensureBootJarAdded() throws IOException {
		if (addedBootJar) {
			return;
		}

		ClassLoader cl = SapAgent.class.getClassLoader();

		// Find out where the agent jar is, since the boot jar should live
		// there too.
		URL url = cl.getResource(SapAgent.class.getName().replace('.', '/') + ".class");
		String file = url.getFile();

		if (!file.startsWith("file:/")) {
			throw new IOException("Could not determine agent jar from " + file);
		}

		System.err.println("Agent base file: " + file);

		String os = AccessController.doPrivileged((PrivilegedAction<String>) () -> System.getProperty("os.name"));
		int skip = os.toLowerCase().startsWith("win") ? 6 : 5;
		String jar = file.substring(skip, file.indexOf(".jar!")) + "-boot.jar";

		if (!new File(jar).canRead()) {
			throw new IOException("Could not find boot jar at " + jar);
		}

		instr.appendToBootstrapClassLoaderSearch(new JarFile(jar));
		addedBootJar = true;
	}

	private static InputStream getStreamForConfig(String config) throws Exception {
		try {
			return new FileInputStream(config);
		} catch (FileNotFoundException e) {
			ClassLoader cl = SapAgent.class.getClassLoader();
			InputStream is = cl.getResourceAsStream(CONFIGS_PATH + config + ".xml");

			if (is != null) {
				return is;
			}

			throw e;
		}
	}

	private static void preInitCommands() throws IOException {
		if (seenCommands.size() > 0) {
			ensureBootJarAdded();
		}

		for (String commandName : seenCommands.keySet()) {
			Command command = Commands.getCommand(commandName);

			if (command != null) {
				command.addCommandArgs(seenCommands.get(commandName));
			}
		}
	}

	private static StringBuilder addCommandOptions(String commandName, StringBuilder options) {
		if (commandName != null) {
			seenCommands.put(commandName, options.toString());
		}

		return new StringBuilder();
	}

	private static byte[] getXmlConfig(String agentArguments) throws Exception {
		String[] parts = agentArguments.split("(?<!\\\\),");
		StringBuilder configProp = new StringBuilder();
		DocumentBuilderFactory factory = DocumentBuilderFactory.newDefaultInstance();

		// We merge into a dummy which is guaranteed to contain all the stuff we expect.
		// spotless:off
		String DUMMY = "<jfragent>\n"
		             + "    <config>\n"
				     + "        <classprefix>__JFREvent</classprefix>\n"
				     + "        <allowtostring>false</allowtostring>\n"
				     + "        <allowconverter>false</allowconverter>\n"
				     + "    </config>\n"
				     + "    <events>\n"
				     + "    </events>\n"
				     + "</jfragent>\n";
		// spotless:on

		Document base = factory.newDocumentBuilder()
				.parse(new ByteArrayInputStream(DUMMY.getBytes(StandardCharsets.UTF_8)));
		Node events = base.getElementsByTagName("events").item(0);
		String requestedPrefix = null;
		String configName = null;

		for (String part : parts) {
			if (part.equals("help")) {
				configProp.append("help=true,");
			} else if (part.startsWith(GenericLogger.GENERIC_COMMAND_PREFIX)) {
				// Do nothing, just pick up the options and make sure the converter is accessible.
				configProp = addCommandOptions(configName, configProp);
				configName = part;
			} else if (part.indexOf('=') > 0) {
				configProp.append(part).append(',');
			} else {
				InputStream is = getStreamForConfig(part);
				Document doc = factory.newDocumentBuilder().parse(is);
				configProp = addCommandOptions(configName, configProp);

				if (is instanceof FileInputStream) {
					// No configuration for direct XML file.
					configName = null;
				} else {
					configName = part;
				}

				if (getBool(doc, "allowtostring", false)) {
					setText(base, "allowtostring", "true");
				}

				if (getBool(doc, "allowconverter", false)) {
					setText(base, "allowconverter", "true");
				}

				String prefix = getString(doc, "classprefix");

				if (requestedPrefix == null) {
					requestedPrefix = prefix;
					setText(base, "classprefix", prefix);
				} else if ((prefix != null) && !requestedPrefix.equals(prefix)) {
					System.out.println("Conflicting class prefixes " + prefix + " vs. " + requestedPrefix);
				}

				NodeList list = doc.getElementsByTagName("event");

				for (int j = 0; j < list.getLength(); ++j) {
					Node toAdd = list.item(j).cloneNode(true);
					events.getOwnerDocument().adoptNode(toAdd);
					events.appendChild(toAdd);
				}
			}
		}

		addCommandOptions(configName, configProp);
		preInitCommands();

		// If we added our boot jar, check the options now.
		if (addedBootJar && !Commands.checkCommands()) {
			System.exit(1);
		}

		TransformerFactory tf = TransformerFactory.newInstance();
		javax.xml.transform.Transformer trans = tf.newTransformer();
		StringWriter sw = new StringWriter();
		trans.transform(new DOMSource(base), new StreamResult(sw));

		return sw.toString().getBytes();
	}

	private static void setText(Document doc, String tag, String text) {
		NodeList list = doc.getElementsByTagName(tag);

		if (list.getLength() == 1) {
			list.item(0).setTextContent(text);
		}
	}

	private static String getString(Document doc, String tag) {
		NodeList list = doc.getElementsByTagName(tag);

		if (list.getLength() != 1) {
			return null;
		}

		return list.item(0).getTextContent();
	}

	private static boolean getBool(Document doc, String tag, boolean fallback) {
		String text = getString(doc, tag);

		return text == null ? fallback : Boolean.parseBoolean(text);
	}

	public static void initializeAgent(InputStream configuration, Instrumentation instrumentation)
			throws XMLStreamException, XMLValidationException, IOException {
		TransformRegistry registry = configuration != null ? DefaultTransformRegistry.from(configuration)
				: DefaultTransformRegistry.empty();
		addBootJarIfNeeded(registry);
		instrumentation.addTransformer(new SapTransformer(registry), true);
		AgentManagementFactory.createAndRegisterAgentControllerMBean(instrumentation,
				new SapTransformRegistry(registry));

		List<Class<?>> classesToRetransform = new ArrayList<>();
		Set<String> clazzes = registry.getClassNames().stream().map((name) -> name.replace('/', '.'))
				.collect(Collectors.toSet());

		for (Class<?> clazz : instrumentation.getAllLoadedClasses()) {
			if (clazzes.contains(clazz.getName())) {
				classesToRetransform.add(clazz);
				System.out.println("Class to retransform: " + clazz);
			}
		}

		try {
			instrumentation.retransformClasses(classesToRetransform.toArray(new Class<?>[0]));
		} catch (UnmodifiableClassException e) {
			logger.log(Level.SEVERE, "Unable to retransform classes", e);
		}
	}
}
