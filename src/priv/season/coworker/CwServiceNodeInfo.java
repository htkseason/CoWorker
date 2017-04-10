package priv.season.coworker;

import priv.season.net.tcp.SsTcpClient;

public class CwServiceNodeInfo {

	protected final SsTcpClient client;
	protected final int totalThreads;
	protected Integer availableThreads;
	protected final double processSpeed;

	public CwServiceNodeInfo(SsTcpClient client, int totalThreads, double processSpeed) {
		this.client = client;
		this.totalThreads = totalThreads;
		this.availableThreads = totalThreads;
		this.processSpeed = processSpeed;
	}

	public boolean isOnline() {
		return !client.getSocket().isClosed();
	}

	public SsTcpClient getClient() {
		return client;
	}

	public int getTotalThreads() {
		return totalThreads;
	}

	public int getAvailableThreads() {
		return availableThreads;
	}

	public boolean isAvailable() {
		return availableThreads > 0;
	}

	public void occupyThread() {
		synchronized (availableThreads) {
			availableThreads--;
		}
	}
	
	public void releaseThread() {
		synchronized (availableThreads) {
			availableThreads++;
		}
	}

	public double getProcessSpeed() {
		return processSpeed;
	}

}
