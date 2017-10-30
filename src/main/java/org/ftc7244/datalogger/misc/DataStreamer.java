package org.ftc7244.datalogger.misc;

import org.ftc7244.datalogger.DataLogger;
import org.ftc7244.datalogger.listeners.OnConnectionUpdate;
import org.ftc7244.datalogger.listeners.OnReceiveData;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by FTC 7244 on 10/29/2017.
 */
public class DataStreamer {

	public static final int DEFAULT_PORT = 8709;

	private String ip;
	private int port;
	private AtomicBoolean running;
	private Set<OnConnectionUpdate> onConnectionUpdates;
	private Set<OnReceiveData> onDataReceived;

	public DataStreamer(String ip) {
		this(ip, DEFAULT_PORT);
	}

	public DataStreamer(String ip, int port) {
		this.port = port;
		this.ip = ip;
		running = new AtomicBoolean(false);
		onConnectionUpdates = new HashSet<>();
		onDataReceived = new HashSet<>();
	}

	public DataStreamer start() {
		if (!running.get()) {
			running.set(true);
			DataLogger.getService().submit(this::run);
		}
		return this;
	}

	private void run() {
		Socket socket = null;
		try {
			socket = new Socket(ip, port);
			BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			this.onConnectionUpdates.forEach(x -> x.onConnectionUpdate(true, null));
			while (!Thread.currentThread().isInterrupted() && running.get()) {
				String[] split = in.readLine().split(":");
				String tag = split[0];
				double[] data = new double[tag.length() - 1];
				for (int i = 1; i < split.length; i++)
					data[i - 1] = Double.parseDouble(split[i]);
				this.onDataReceived.forEach(x -> x.onReceiveData(tag, data));
			}
		} catch (IOException e) {
			this.onConnectionUpdates.forEach(x -> x.onConnectionUpdate(false, e));
		} finally {
			stop();
			try {
				if (socket != null)
					socket.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	public boolean isRunning() {
		return running.get();
	}

	public void stop() {
		running.set(false);
	}

	public DataStreamer addConnectionListener(OnConnectionUpdate onConnectionUpdate) {
		this.onConnectionUpdates.add(onConnectionUpdate);
		return this;
	}

	public DataStreamer addDataListener(OnReceiveData onDataReceived) {
		this.onDataReceived.add(onDataReceived);
		return this;
	}
}