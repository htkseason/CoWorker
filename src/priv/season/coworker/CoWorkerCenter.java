package priv.season.coworker;

import java.io.File;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import priv.season.coworker.utils.Utils;
import priv.season.net.tcp.ITcpCallback;
import priv.season.net.tcp.SsProtocol;
import priv.season.net.tcp.SsTcpClient;
import priv.season.net.tcp.SsTcpServer;

public class CoWorkerCenter implements ITcpCallback {
	protected SsTcpServer server = new SsTcpServer();
	protected BlockingQueue<CwPack> taskResultQueue = new LinkedBlockingQueue<CwPack>();
	protected List<CwServiceNodeInfo> clientList = new ArrayList<CwServiceNodeInfo>();

	protected SsProtocol ssp = new SsProtocol();

	public final ArrayList<String> classPath = new ArrayList<String>();


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
				case CwPack.TAG_SERVICE_REGISTER: {
					int threads = Integer.parseInt(pack.getParam(CwPack.PARAM_PROCESS_THREADS));
					double speed = Double.parseDouble(pack.getParam(CwPack.PARAM_PROCESS_SPEED));
					synchronized (clientList) {
						clientList.add(new CwServiceNodeInfo(client, threads, speed));
					}
					System.out.println("client registered " + client.getSocket().getRemoteSocketAddress()
							+ " : cores = " + threads + " / speed = " + speed);
					for (String path : classPath)
						shareClassFile(client, new File(path), path);
				}
					break;
				case CwPack.TAG_TASK_RESULT: {
				}
					taskResultQueue.offer(pack);

				}
				break;

			} catch (Exception e) {
				e.printStackTrace();
				System.err.println("process msg error");
			}
		}
	}

	@Override
	public void onClose(SsTcpClient client) {
		ITcpCallback.super.onClose(client);
		synchronized (clientList) {
			for (CwServiceNodeInfo node : clientList) {
				if (node.getClient() == client) {
					clientList.remove(node);
					break;
				}
			}
		}
		System.err.println("client on " + client.getSocket().getRemoteSocketAddress() + " disconnected");

	}

	@Override
	public void onConnected(SsTcpClient client) {
		ITcpCallback.super.onConnected(client);
	}

	protected ITcpCallback serverSocketCallback = new ITcpCallback() {

		@Override
		public void onServerSocketClosed(SsTcpServer server) {
			ITcpCallback.super.onServerSocketClosed(server);
		}

		@Override
		public void onAccepted(SsTcpServer server, Socket clientSocket) {
			ITcpCallback.super.onAccepted(server, clientSocket);
			System.out.println("client accepted! remote = " + clientSocket.getRemoteSocketAddress());
			new SsTcpClient(clientSocket, CoWorkerCenter.this);

		}

	};

	public void close() {
		server.close();

		// since onClose() remove the element, iter an duplicate array can
		// prevent ConcurrentModificationException
		synchronized (clientList) {
			for (CwServiceNodeInfo node : clientList) {
				node.getClient().close();
			}
			clientList.clear();
		}
	}

	public void startServer(int port) throws IOException {

		server.setCallback(serverSocketCallback);
		server.start(port);

	}

	public void requestService(String broadCast, int udpPort) {
		DatagramSocket udpSocket = null;
		try {
			InetAddress addr = InetAddress.getByName(broadCast);
			CwPack pack = new CwPack(CwPack.TAG_CENTER_ADDR);
			pack.addParam(CwPack.PARAM_CENTER_HOST, InetAddress.getLocalHost().getHostAddress());
			pack.addParam(CwPack.PARAM_CENTER_PORT, new Integer(server.getServerSocket().getLocalPort()).toString());

			byte[] sendBuf = Utils.serializeObject(pack);
			DatagramPacket sendPacket = new DatagramPacket(sendBuf, sendBuf.length, addr, udpPort);
			udpSocket = new DatagramSocket();
			udpSocket.send(sendPacket);
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (udpSocket != null)
				udpSocket.close();
		}

	}

	public boolean assignTask(String taskId, int taskIndex, String className, String methodName, Object obj,
			Object... params) {
		CwServiceNodeInfo[] clientArray = getClientArray();
		if (clientArray.length == 0)
			return false;
		Arrays.sort(clientArray, new Comparator<CwServiceNodeInfo>() {
			@Override
			public int compare(CwServiceNodeInfo o1, CwServiceNodeInfo o2) {
				return -(o1.getAvailableThreads() - o2.getAvailableThreads());
			}
		});
		CwPack tosend = CwPack.fromTask(taskId, taskIndex, className, methodName, obj, params);
		clientArray[0].getClient().send(tosend.toSspMsg());
		return true;
	}

	public synchronized CwPack[] getTaskResults(String taskId, int counts, int timeOut) {
		final int interval = 10;
		List<CwPack> resultLst = new LinkedList<CwPack>();
		List<CwPack> temp_queue = new LinkedList<CwPack>();
		int i = counts;
		while (i > 0) {
			try {
				CwPack pack = taskResultQueue.poll(interval, TimeUnit.MILLISECONDS);
				if (pack == null) {
					timeOut -= interval;
					if (timeOut < 0)
						break;
					else
						continue;
				}
				if (!pack.getParam(CwPack.PARAM_TASK_ID).equals(taskId)) {
					temp_queue.add(pack);
				} else {
					resultLst.add(pack);
					i--;
				}
			} catch (InterruptedException e) {

			}
		}
		taskResultQueue.addAll(temp_queue);
		if (i > 0)
			return null;

		CwPack[] result = new CwPack[resultLst.size()];
		resultLst.toArray(result);
		Arrays.sort(result, new Comparator<CwPack>() {
			@Override
			public int compare(CwPack o1, CwPack o2) {
				int taskIndex1 = Integer.parseInt(o1.getParam(CwPack.PARAM_TASK_INDEX));
				int taskIndex2 = Integer.parseInt(o2.getParam(CwPack.PARAM_TASK_INDEX));
				return taskIndex1 - taskIndex2;
			}
		});
		return result;
	}

	public void shareData(String dataId, int dataLevel, Object data) {
		for (CwServiceNodeInfo node : getClientArray()) {
			node.getClient().send(CwPack.fromSharedData(dataId, dataLevel, data).toSspMsg());
		}
	}

	public void clearSharedData(int dataLevel) {
		for (CwServiceNodeInfo node : clientList) {
			node.getClient().send(CwPack.fromSharedDataClean(dataLevel).toSspMsg());
		}
	}

	public void clearTaskResult() {
		taskResultQueue.clear();
	}

	public void shareClassFile(SsTcpClient client, File file, String parentPath) throws IOException {
		if (file.isDirectory()) {
			for (File f : file.listFiles())
				shareClassFile(client, f, parentPath);
		} else {
			String fileName = file.getPath().substring(file.getPath().indexOf(parentPath) + parentPath.length() + 1);
			client.send(CwPack.fromClassFile(file, fileName).toSspMsg());
		}
	}

	public CwServiceNodeInfo[] getClientArray() {
		CwServiceNodeInfo[] clientArray;
		synchronized (clientList) {
			clientArray = new CwServiceNodeInfo[clientList.size()];
			clientList.toArray(clientArray);
		}
		return clientArray;
	}
}
