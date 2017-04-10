package priv.season.coworker.utils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;

public final class Utils {
	public static Class<?> __dloadClass(URL[] urls, String className) throws IOException, ClassNotFoundException {
		URLClassLoader loader = new URLClassLoader(urls);
		Class<?> cls = loader.loadClass(className);
		loader.close();
		return cls;
	}

	public static void __dloadJarFile(URL[] urls, JarFile jarFile) throws IOException, ClassNotFoundException {
		Enumeration<JarEntry> entrys = jarFile.entries();
		while (entrys.hasMoreElements()) {
			JarEntry jarEntry = entrys.nextElement();
			String className = jarEntry.getName();
			if (className.endsWith(".class")) {
				className = className.substring(0, className.length() - 6);
				className = className.replace('/', '.');
				Utils.__dloadClass(urls, className);
			}
		}
		jarFile.close();
	}

	public static void addJavaFile(File javaSrcFile) throws IOException {
		String fileDir = javaSrcFile.getParent();
		JavaCompiler javaCompiler = ToolProvider.getSystemJavaCompiler();
		int compileReuslt = javaCompiler.run(null, null, null, "-d", fileDir, javaSrcFile.getCanonicalPath());
		if (compileReuslt != 0)
			System.err.println("compile .java file err.");
		addClassPath(new File(fileDir));
	}

	public static void addJarFile(File jarFile) throws MalformedURLException, IOException {
		addClassPath(jarFile);
	}

	public static void addClassPath(File file) {
		try {
			URLClassLoader classLoader = (URLClassLoader) ClassLoader.getSystemClassLoader();
			Method add = URLClassLoader.class.getDeclaredMethod("addURL", new Class[] { URL.class });
			add.setAccessible(true);
			add.invoke(classLoader, file.getCanonicalFile().toURI().toURL());
		} catch (Exception e) {
			System.err.println("add classpath err.");
		}

	}

	public static void loadDlib(File dlibFile) throws IOException {
		System.loadLibrary(dlibFile.getCanonicalPath());
	}

	public static byte[] loadFile(File file) throws IOException {
		FileInputStream fis = new FileInputStream(file);
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		int count;
		byte data[] = new byte[8 * 1024];
		while ((count = fis.read(data, 0, data.length)) != -1) {
			bos.write(data, 0, count);
		}
		fis.close();
		return bos.toByteArray();

	}

	public static byte[] serializeObject(Object obj) throws IOException {
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		ObjectOutputStream oos = new ObjectOutputStream(bos);
		oos.writeUnshared(obj);
		oos.close();
		return bos.toByteArray();
	}

	public static Object deserializeObject(byte[] data, int offset, int length)
			throws IOException, ClassNotFoundException {
		ByteArrayInputStream bis = new ByteArrayInputStream(data, offset, length);
		ObjectInputStream ois = new ObjectInputStream(bis);
		Object result = ois.readObject();
		ois.close();
		bis.close();
		return result;
	}

	public static void deleteDictionary(File dir) {
		if (!dir.exists())
			return;
		if (dir.isDirectory()) {
			for (File f : dir.listFiles())
				deleteDictionary(f);
		}
		dir.delete();
	}

	public static Class<?> unBoxing(Class<?> cls) {
		switch (cls.getName()) {
		case "java.lang.Double":
			return double.class;
		case "java.lang.Integer":
			return int.class;
		case "java.lang.Float":
			return float.class;
		case "java.lang.Byte":
			return byte.class;
		case "java.lang.Long":
			return long.class;
		case "java.lang.Short":
			return short.class;
		case "java.lang.Boolean":
			return boolean.class;
		case "java.lang.Character":
			return char.class;
		}
		return cls;
	}

	protected static long timingRecord = 0;

	public static void startTiming() {
		timingRecord = System.nanoTime();
	}

	public static double getTiming() {
		return (System.nanoTime() - timingRecord) / 1000000.0;
	}

	public static void printTiming() {
		System.out.println((System.nanoTime() - timingRecord) / 1000000.0);
	}

}
