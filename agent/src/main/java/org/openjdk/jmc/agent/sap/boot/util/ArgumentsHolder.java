package org.openjdk.jmc.agent.sap.boot.util;

public class ArgumentsHolder {

	private volatile Arguments args;

	public ArgumentsHolder(Arguments args) {
		this.args = args;
	}

	public void set(Arguments args) {
		this.args = args;
	}

	public Arguments get() {
		return args;
	}
}
