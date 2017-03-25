/*
 * Copyright 2009-2013, 2017 by AO Industries, Inc.,
 * 7262 Bull Pen Cir, Mobile, Alabama, 36695, U.S.A.
 * All rights reserved.
 */
package com.aoindustries.aoserv.examples.vnc;

import com.aoindustries.aoserv.client.AOServClientConfiguration;
import com.aoindustries.aoserv.client.AOServConnector;
import com.aoindustries.aoserv.client.AOServer;
import com.aoindustries.aoserv.client.Server;
import com.aoindustries.aoserv.client.VirtualServer;
import com.aoindustries.aoserv.daemon.client.AOServDaemonConnection;
import com.aoindustries.aoserv.daemon.client.AOServDaemonConnector;
import com.aoindustries.aoserv.daemon.client.AOServDaemonProtocol;
import com.aoindustries.io.AOPool;
import com.aoindustries.io.CompressedDataInputStream;
import com.aoindustries.io.CompressedDataOutputStream;
import com.aoindustries.util.ErrorPrinter;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Listens on a TCP socket and tunnels VNC connections through to the
 * virtual server.
 *
 * @author  AO Industries, Inc.
 */
public class VncConsoleTunnel implements Runnable {

	private static final Logger logger = Logger.getLogger(VncConsoleTunnel.class.getName());

	public static void main(String[] args) {
		if(args.length!=3) {
			System.err.println("usage: "+VncConsoleTunnel.class.getName()+" virtual_server listen_address listen_port");
			System.exit(1);
		} else {
			try {
				AOServConnector conn = AOServConnector.getConnector(logger);
				Server server = conn.getServers().get(args[0]);
				if(server==null) throw new SQLException("Unable to find Server: "+args[0]);
				VirtualServer virtualServer = server.getVirtualServer();
				if(virtualServer==null) throw new SQLException("Server is not a VirtualServer: "+args[0]);
				new VncConsoleTunnel(
					virtualServer,
					InetAddress.getByName(args[1]),
					Integer.parseInt(args[2])
				).run();
			} catch(Exception err) {
				ErrorPrinter.printStackTraces(err);
				System.exit(2);
			}
		}
	}

	private final VirtualServer virtualServer;
	private final InetAddress listenAddress;
	private final int listenPort;

	public VncConsoleTunnel(VirtualServer virtualServer, InetAddress listenAddress, int listenPort) {
		this.virtualServer = virtualServer;
		this.listenAddress = listenAddress;
		this.listenPort = listenPort;
	}

	public void run() {
		while(true) {
			try {
				ServerSocket serverSocket = new ServerSocket(listenPort, 50, listenAddress);
				try {
					while(true) {
						final Socket socket = serverSocket.accept();
						new Thread(
							new Runnable() {
								public void run() {
									try {
										AOServer.DaemonAccess daemonAccess = virtualServer.requestVncConsoleAccess();
										AOServDaemonConnector daemonConnector=AOServDaemonConnector.getConnector(
											daemonAccess.getHost(),
											com.aoindustries.net.InetAddress.UNSPECIFIED,
											daemonAccess.getPort(),
											daemonAccess.getProtocol(),
											null,
											100,
											AOPool.DEFAULT_MAX_CONNECTION_AGE,
											AOServClientConfiguration.getSslTruststorePath(),
											AOServClientConfiguration.getSslTruststorePassword(),
											logger
										);
										final AOServDaemonConnection daemonConn=daemonConnector.getConnection();
										try {
											final CompressedDataOutputStream daemonOut = daemonConn.getOutputStream(AOServDaemonProtocol.VNC_CONSOLE);
											daemonOut.writeLong(daemonAccess.getKey());
											daemonOut.flush();

											final CompressedDataInputStream daemonIn = daemonConn.getInputStream();
											int result=daemonIn.read();
											if(result==AOServDaemonProtocol.NEXT) {
												final OutputStream socketOut = socket.getOutputStream();
												final InputStream socketIn = socket.getInputStream();
												// socketIn -> daemonOut in another thread
												Thread inThread = new Thread(
													new Runnable() {
														public void run() {
															try {
																try {
																	byte[] buff = new byte[4096];
																	int ret;
																	while((ret=socketIn.read(buff, 0, 4096))!=-1) {
																		daemonOut.write(buff, 0, ret);
																		daemonOut.flush();
																	}
																} finally {
																	daemonConn.close();
																}
															} catch(ThreadDeath TD) {
																throw TD;
															} catch(Throwable T) {
																logger.log(Level.SEVERE, null, T);
															}
														}
													}
												);
												inThread.start();
												//try {
													// daemonIn -> socketOut in this thread
													byte[] buff = new byte[4096];
													int ret;
													while((ret=daemonIn.read(buff, 0, 4096))!=-1) {
														socketOut.write(buff, 0, ret);
														socketOut.flush();
													}
												//} finally {
													// Let the in thread complete its work before closing streams
												//    inThread.join();
												//}
											} else {
												if (result == AOServDaemonProtocol.IO_EXCEPTION) throw new IOException(daemonIn.readUTF());
												else if (result == AOServDaemonProtocol.SQL_EXCEPTION) throw new SQLException(daemonIn.readUTF());
												else if (result==-1) throw new EOFException();
												else throw new IOException("Unknown result: " + result);
											}
										} finally {
											daemonConn.close(); // Always close after VNC tunnel
											daemonConnector.releaseConnection(daemonConn);
										}
									} catch(ThreadDeath TD) {
										throw TD;
									} catch(Throwable T) {
										logger.log(Level.SEVERE, null, T);
									} finally {
										try {
											socket.close();
										} catch(ThreadDeath TD) {
											throw TD;
										} catch(Throwable T) {
											logger.log(Level.SEVERE, null, T);
										}
									}
								}
							}
						).start();
					}
				} finally {
					serverSocket.close();
				}
			} catch(ThreadDeath TD) {
				throw TD;
			} catch(Throwable T) {
				logger.log(Level.SEVERE, null, T);
				try {
					Thread.sleep(10000);
				} catch(InterruptedException err) {
					logger.log(Level.WARNING, null, err);
					// Restore the interrupted status
					Thread.currentThread().interrupt();
				}
			}
		}
	}
}
