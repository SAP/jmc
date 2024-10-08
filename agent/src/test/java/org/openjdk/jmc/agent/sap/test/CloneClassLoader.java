package org.openjdk.jmc.agent.sap.test;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.security.SecureClassLoader;
import java.util.Arrays;
import java.util.HashSet;

/**
 * This is a class loader which can load the same classes as another class loader.
 * <p>
 * This is mainly useful for tests when you want to load a class, but do it with a class loader you
 * can dispose. The clone loader just asks the loader to be cloned to get the bytecodes, but defines
 * the class itself.
 * <p>
 * Additionally you can specify a set of classes the loader should not be able to load.
 *
 * @author Ralf Schmelter
 */
public class CloneClassLoader extends SecureClassLoader {

	/**
	 * The class loaded to clone.
	 */
	private final ClassLoader toClone;

	/**
	 * The strings we cannot load.
	 */
	private final HashSet<String> notLoadable;

	/**
	 * The strings we just delegate.
	 */
	private final HashSet<String> simpleDelegate;

	/**
	 * Creates a class loader which can load the same classes as the loader which loaded the
	 * <code>CloneClassLoader</code> class itself.
	 * <p>
	 * Only the bootstrap classes are delegated to the bootstrap class loader.
	 *
	 * @param toClone
	 *            the class loader to mimic. The clone class loader will be able to load the same
	 *            classes as the 'toClone' loader.
	 */
	public CloneClassLoader(ClassLoader toClone) {
		this(null, toClone, new String[0], new String[0]);
	}

	/**
	 * Creates a class loader which can load the same classes as the loader which loaded the
	 * <code>CloneClassLoader</code> class itself.
	 * <p>
	 * Only the bootstrap classes are delegated to the bootstrap class loader.
	 *
	 * @param toClone
	 *            the class loader to mimic. The clone class loader will be able to load the same
	 *            classes as the 'toClone' loader.
	 * @param notLoadable
	 *            The classes we should not be able to load via this loader.
	 */
	public CloneClassLoader(ClassLoader toClone, String[] notLoadable) {
		this(null, toClone, notLoadable, new String[0]);
	}

	/**
	 * Creates a class loader which can load the same classes as the loader which loaded the
	 * <code>CloneClassLoader</code> class itself.
	 * <p>
	 * Only the classes which are loadable by the 'parent' loader are delegated to that loader (to
	 * make it possible mix classes).
	 *
	 * @param parent
	 *            the parent loader which is first asked to load a class.
	 * @param toClone
	 *            the class loader to mimic. The clone class loader will be able to load the same
	 *            classes as the 'toClone' loader.
	 */
	public CloneClassLoader(ClassLoader parent, ClassLoader toClone) {
		this(parent, toClone, new String[0], new String[0]);
	}

	/**
	 * Creates a class loader which can load the same classes as the loader which loaded the
	 * <code>CloneClassLoader</code> class itself.
	 * <p>
	 * Only the classes which are loadable by the 'parent' loader are delegated to that loader (to
	 * make it possible mix classes).
	 *
	 * @param parent
	 *            the parent loader which is first asked to load a class.
	 * @param toClone
	 *            the class loader to mimic. The clone class loader will be able to load the same
	 *            classes as the 'toClone' loader.
	 * @param notLoadable
	 *            The classes we should not be able to load via this loader.
	 * @param simpleDelegate
	 *            The names of the classes for which we simply delegate.
	 */
	public CloneClassLoader(ClassLoader parent, ClassLoader toClone, String[] notLoadable, String[] simpleDelegate) {
		super(parent);

		this.toClone = toClone;
		this.notLoadable = new HashSet<>(Arrays.asList(notLoadable));
		this.simpleDelegate = new HashSet<>(Arrays.asList(simpleDelegate));
	}

	/**
	 * @see java.lang.ClassLoader#findClass(java.lang.String)
	 */
	@Override
	protected Class<?> findClass(String name) throws ClassNotFoundException {
		if (notLoadable.contains(name)) {
			throw new ClassNotFoundException("The clone class loader explicitely " + "didn't found the class");
		}

		if (simpleDelegate.contains(name)) {
			return toClone.loadClass(name);
		}

		// We just ask the wrapper class loader to find the resource for us
		URL res = toClone.getResource(name.replace('.', '/') + ".class");

		if (res == null) {
			throw new ClassNotFoundException(name);
		}

		try {
			InputStream is = res.openStream();
			byte[] code = readStreamIntoBuffer(is, 8192);
			is.close();
			return defineClass(name, code, 0, code.length);
		} catch (IOException e) {
			throw new ClassNotFoundException(name, e);
		}
	}

	/**
	 * Reads all data of a stream into a byte array. The method allocates as much memory as
	 * necessary to put the whole data into that byte array. The data is read in chunks of
	 * <code>chunkSize</code> chunks.<br>
	 * <br>
	 * <b>Implementation Note: </b> The data is read in chunks of <code>chunkSize</code> bytes. The
	 * data is copied to the result array. The memory consumption at the end of the reading is
	 * <code>2 x [size of resulting array] + chunkSize</code>.
	 *
	 * @param is
	 *            the stream to read the data from
	 * @param chunkSize
	 *            the size of the chunks the data should be read in
	 * @return the <b>whole</b> data of the stream read into an byte array
	 * @throws IllegalArgumentException
	 *             if chunkSize <= 0
	 * @throws NullPointerException
	 *             if is == null
	 * @throws IOException
	 *             thrown if the provided stream encounters IO problems
	 */
	public static byte[] readStreamIntoBuffer(InputStream is, int chunkSize) throws IOException {

		// check preconditions
		if (chunkSize <= 0) {
			throw new IllegalArgumentException("chunkSize <= 0");
		} else if (is == null) {
			throw new NullPointerException("is is null");
		}

		// temporary buffer for read operations and result buffer
		byte[] tempBuffer = new byte[chunkSize];
		byte[] buffer = new byte[0];

		int bytesRead = 0; // bytes actual read
		int oldSize = 0; // size of the resulting buffer

		while ((bytesRead = is.read(tempBuffer)) > 0) {

			// temporary reference to the buffer for the copy operation
			byte[] oldBuffer = buffer;

			// create a new buffer with the size needed and copy data
			buffer = new byte[oldSize + bytesRead];
			System.arraycopy(oldBuffer, 0, buffer, 0, oldBuffer.length);

			// copy the new data
			System.arraycopy(tempBuffer, 0, buffer, oldSize, bytesRead);
			oldSize += bytesRead;
		}

		return buffer;
	}
}
