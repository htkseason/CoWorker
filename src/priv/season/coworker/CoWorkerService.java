package priv.season.coworker;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import priv.season.coworker.utils.Utils;
import priv.season.net.tcp.*;

public class CoWorkerService implements ITcpCallback {
	public static final int CPU_CORES = Runtime.getRuntime().availableProcessors();
	public static final String TEMP_FILE_DIR = "./temp/";
	public static final String TASK_THREAD_NAME = "CWSERVICE_TASK_THREAD_MARK";
	static {
		Utils.deleteDictionary(new File(TEMP_FILE_DIR));
	}
	protected SsTcpClient client;

	protected BlockingQueue<CwPack> taskQueue = new LinkedBlockingQueue<CwPack>();
	protected Map<String, CwPack> sharedData = new HashMap<String, CwPack>();
	protected SsProtocol ssp = new SsProtocol();

	protected boolean serviceEndFlag = true;

	protected int udpPort;

	@Override
	public void onRecv(SsTcpClient client, byte[] data, int offset, int length) {
		ITcpCallback.super.onRecv(client, data, offset, length);
		ssp.pushData(data, offset, length);
		byte[] msg = null;
		while ((msg = ssp.parse()) != null) {
			try {

				CwPack pack = (CwPack) Utils.deserializeObject(msg, 0, msg.length);
				if (pack == null)
					return;
				switch (pack.tag) {
				case CwPack.TAG_JAVAFILE: {
					String className = pack.getParam(CwPack.PARAM_CLASS_NAME);
					String fileName = className.substring(className.lastIndexOf('.') + 1) + ".java";
					File file = new File(TEMP_FILE_DIR + fileName);
					if (!file.getParentFile().exists())
						file.getParentFile().mkdirs();
					FileOutputStream fos = new FileOutputStream(file);
					fos.write((byte[]) pack.getData(CwPack.DATA_FILE_DATA));
					fos.close();

					Utils.addJavaFile(file);
					file.delete();

				}
					break;
				case CwPack.TAG_CLASSFILE: {
					String fileName = pack.getParam(CwPack.PARAM_FILE_NAME);
					File file = new File(TEMP_FILE_DIR + fileName);
					if (!file.getParentFile().exists())
						file.getParentFile().mkdirs();
					FileOutputStream fos = new FileOutputStream(file);
					fos.write((byte[]) pack.getData(CwPack.DATA_FILE_DATA));
					fos.close();

					Utils.addClassPath(new File(TEMP_FILE_DIR));

				}
					break;

				case CwPack.TAG_JARFILE: {
					String fileName = pack.getParam(CwPack.PARAM_FILE_NAME);
					File file = new File(TEMP_FILE_DIR + fileName);
					if (!file.getParentFile().exists())
						file.getParentFile().mkdirs();
					FileOutputStream fos = new FileOutputStream(file);
					fos.write((byte[]) pack.getData(CwPack.DATA_FILE_DATA));
					fos.close();
					Utils.addJarFile(file);

				}
					break;
				case CwPack.TAG_DLIBFILE: {
					String fileName = pack.getParam(CwPack.PARAM_FILE_NAME);
					File file = new File(TEMP_FILE_DIR + fileName);
					if (!file.getParentFile().exists())
						file.getParentFile().mkdirs();
					FileOutputStream fos = new FileOutputStream(file);
					fos.write((byte[]) pack.getData(CwPack.DATA_FILE_DATA));
					fos.close();
					System.loadLibrary(TEMP_FILE_DIR + fileName.substring(0, fileName.lastIndexOf('.')));

				}
					break;
				case CwPack.TAG_RESOUCEFILE: {
					String fileName = pack.getParam(CwPack.PARAM_FILE_NAME);
					File file = new File(TEMP_FILE_DIR + fileName);
					if (!file.getParentFile().exists())
						file.getParentFile().mkdirs();
					FileOutputStream fos = new FileOutputStream(file);
					fos.write((byte[]) pack.getData(CwPack.DATA_FILE_DATA));
					fos.close();
				}
					break;

				case CwPack.TAG_SHARED_DATA: {
					String uid = pack.getParam(CwPack.PARAM_SHARED_DATA_ID);
					sharedData.put(uid, pack);
				}
					break;

				case CwPack.TAG_SHARED_DATA_CLEAN: {

					int level = Integer.parseInt(pack.getParam(CwPack.PARAM_SHARED_DATA_LEVEL));
					Set<String> set = sharedData.keySet();
					for (Object key : set.toArray()) {
						int l = Integer.parseInt(sharedData.get(key).getParam(CwPack.PARAM_SHARED_DATA_LEVEL));
						if (l <= level)
							sharedData.remove(key);
					}

				}
					break;

				case CwPack.TAG_TASK: {
					taskQueue.offer(pack);
				}
					break;

				}// end switch

			} catch (Throwable e) {
				e.printStackTrace();
				System.err.println("process msg error");
			}
		} // end while
	}

	@Override
	public void onClose(SsTcpClient client) {
		ITcpCallback.super.onClose(client);
		System.out.println("server disconnected...");
		taskQueue.clear();
		sharedData.clear();
		ssp.clear();
		System.gc();
		waitForRequest(udpPort);
	}

	@Override
	public void onConnected(SsTcpClient client) {
		ITcpCallback.super.onConnected(client);

		System.out.println("server connected! remote = " + client.getSocket().getRemoteSocketAddress());
		CwPack pack = new CwPack(CwPack.TAG_SERVICE_REGISTER);
		pack.addParam(CwPack.PARAM_PROCESS_THREADS, new Integer(CPU_CORES).toString());
		pack.addParam(CwPack.PARAM_PROCESS_SPEED, new Double(1.0).toString());

		client.send(pack.toSspMsg());

	}

	public void waitForRequest(int udpPort) {
		System.out.println("listening on udp port " + udpPort);
		this.udpPort = udpPort;
		DatagramSocket server = null;
		try {
			server = new DatagramSocket(udpPort);

			byte[] recvBuf = new byte[8 * 1024];
			DatagramPacket recvPacket = new DatagramPacket(recvBuf, recvBuf.length);
			server.receive(recvPacket);
			byte[] data = recvPacket.getData();
			CwPack pack = (CwPack) Utils.deserializeObject(data, 0, data.length);
			if (!pack.tag().equals(CwPack.TAG_CENTER_ADDR))
				return;
			String host = pack.getParam(CwPack.PARAM_CENTER_HOST);
			String port = pack.getParam(CwPack.PARAM_CENTER_PORT);
			System.out.println("connecting to " + host + " : " + port);
			startService(host, Integer.parseInt(port));
		} catch (Exception e) {
			e.printStackTrace();

		} finally {
			if (server != null)
				server.close();
		}

	}

	public void startService(String host, int port) throws UnknownHostException, IOException {
		client = new SsTcpClient(host, port, this);
		if (serviceEndFlag == true) {
			serviceEndFlag = false;
			for (int i = 0; i < CPU_CORES; i++) {
				Thread t = new Thread(taskThread, TASK_THREAD_NAME);

				t.start();
			}
		}
	}

	protected Runnable taskThread = new Runnable() {

		@Override
		public void run() {
			CwPack resultPack = null;

			while (!serviceEndFlag) {
				resultPack = new CwPack(CwPack.TAG_TASK_RESULT);

				// wait for task
				CwPack pack = null;
				try {
					pack = taskQueue.poll(10, TimeUnit.MILLISECONDS);
				} catch (InterruptedException e1) {
				}

				try {
					if (pack == null)
						continue;

					// get task info
					String className = pack.getParam(CwPack.PARAM_CLASS_NAME);
					String methodName = pack.getParam(CwPack.PARAM_METHOD_NAME);
					String taskId = pack.getParam(CwPack.PARAM_TASK_ID);
					String taskIndex = pack.getParam(CwPack.PARAM_TASK_INDEX);
					resultPack.addParam(CwPack.PARAM_TASK_ID, taskId);
					resultPack.addParam(CwPack.PARAM_TASK_INDEX, taskIndex);

					Object obj = pack.getData(CwPack.DATA_METHOD_OBJECT);
					Object[] params = (Object[]) pack.getData(CwPack.DATA_METHOD_PARAMS);
					Class<?> cls = Class.forName(className);

					// find method
					Class<?>[] paramClasses = new Class<?>[params.length];
					for (int i = 0; i < params.length; i++) {
						if (params[i].getClass() == CwPack.class) {
							String sdId = ((CwPack) params[i]).getParam(CwPack.PARAM_SHARED_DATA_ID);
							params[i] = sharedData.get(sdId).getData(CwPack.DATA_OBJECT);
							paramClasses[i] = params[i].getClass();
						}
						paramClasses[i] = Utils.unBoxing(params[i].getClass());
					}
					Method method = cls.getMethod(methodName, paramClasses);

					// invoke method
					boolean isStatic = Modifier.isStatic(method.getModifiers());
					Object result = null;
					if (isStatic)
						result = method.invoke(cls, params);
					else
						result = method.invoke(obj, params);

					// return result
					resultPack.addData(CwPack.DATA_METHOD_RESULT, result);
					if (!isStatic)
						resultPack.addData(CwPack.DATA_METHOD_OBJECT, obj);

					client.send(resultPack.toSspMsg());
				} catch (Exception e) {
					// e.printStackTrace();

					// return error info
					resultPack.addData(CwPack.DATA_METHOD_EXCEPTION, e);

					client.send(resultPack.toSspMsg());

					System.err.println("process task err. " + e);
				}  // end task try-catch

			} // end while

		} // end runnable

	};

	public void close() {
		serviceEndFlag = true;
		if (client != null)
			client.close();
	}

	public boolean isTaskThread(Thread thread) {
		return thread.getName().equals(TASK_THREAD_NAME);

	}

	public boolean isTaskThread() {
		return isTaskThread(Thread.currentThread());
	}

}
