/*
 * aoserv-examples - Automation examples for the AOServ Platform.
 * Copyright (C) 2009-2013, 2017, 2018, 2019, 2020  AO Industries, Inc.
 *     support@aoindustries.com
 *     7262 Bull Pen Cir
 *     Mobile, AL 36695
 *
 * This file is part of aoserv-examples.
 *
 * aoserv-examples is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * aoserv-examples is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with aoserv-examples.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.aoindustries.aoserv.examples.vnc;

import com.aoindustries.aoserv.client.AOServClientConfiguration;
import com.aoindustries.aoserv.client.AOServConnector;
import com.aoindustries.aoserv.client.infrastructure.VirtualServer;
import com.aoindustries.aoserv.client.linux.Server;
import com.aoindustries.aoserv.client.net.Host;
import com.aoindustries.aoserv.daemon.client.AOServDaemonConnection;
import com.aoindustries.aoserv.daemon.client.AOServDaemonConnector;
import com.aoindustries.aoserv.daemon.client.AOServDaemonProtocol;
import com.aoindustries.io.AOPool;
import com.aoindustries.io.stream.StreamableInput;
import com.aoindustries.io.stream.StreamableOutput;
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

	@SuppressWarnings("UseOfSystemOutOrSystemErr")
	public static void main(String[] args) {
		if(args.length!=3) {
			System.err.println("usage: "+VncConsoleTunnel.class.getName()+" virtual_server listen_address listen_port");
			System.exit(1);
		} else {
			try {
				AOServConnector conn = AOServConnector.getConnector();
				Host host = conn.getNet().getHost().get(args[0]);
				if(host == null) throw new SQLException("Unable to find Host: " + args[0]);
				VirtualServer virtualServer = host.getVirtualServer();
				if(virtualServer==null) throw new SQLException("Host is not a VirtualServer: "+args[0]);
				new VncConsoleTunnel(
					virtualServer,
					InetAddress.getByName(args[1]),
					Integer.parseInt(args[2])
				).run();
			} catch(IOException | NumberFormatException | SQLException err) {
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

	@Override
	@SuppressWarnings("SleepWhileInLoop")
	public void run() {
		while(true) {
			try {
				try (ServerSocket serverSocket = new ServerSocket(listenPort, 50, listenAddress)) {
					while(true) {
						final Socket socket = serverSocket.accept();
						new Thread(
							() -> {
								try {
									Server.DaemonAccess daemonAccess = virtualServer.requestVncConsoleAccess();
									AOServDaemonConnector daemonConnector=AOServDaemonConnector.getConnector(
										daemonAccess.getHost(),
										com.aoindustries.net.InetAddress.UNSPECIFIED_IPV4,
										daemonAccess.getPort(),
										daemonAccess.getProtocol(),
										null,
										100,
										AOPool.DEFAULT_MAX_CONNECTION_AGE,
										AOServClientConfiguration.getSslTruststorePath(),
										AOServClientConfiguration.getSslTruststorePassword()
									);
									final AOServDaemonConnection daemonConn=daemonConnector.getConnection();
									try {
										final StreamableOutput daemonOut = daemonConn.getRequestOut(AOServDaemonProtocol.VNC_CONSOLE);
										daemonOut.writeLong(daemonAccess.getKey());
										daemonOut.flush();

										final StreamableInput daemonIn = daemonConn.getResponseIn();
										int result=daemonIn.read();
										if(result==AOServDaemonProtocol.NEXT) {
											final OutputStream socketOut = socket.getOutputStream();
											final InputStream socketIn = socket.getInputStream();
											// socketIn -> daemonOut in another thread
											Thread inThread = new Thread(
												() -> {
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
													} catch(RuntimeException | IOException T) {
														logger.log(Level.SEVERE, null, T);
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
								} catch(RuntimeException | IOException | SQLException T) {
									logger.log(Level.SEVERE, null, T);
								} finally {
									try {
										socket.close();
									} catch(ThreadDeath TD) {
										throw TD;
									} catch(RuntimeException | IOException T) {
										logger.log(Level.SEVERE, null, T);
									}
								}
							}
						).start();
					}
				}
			} catch(ThreadDeath TD) {
				throw TD;
			} catch(RuntimeException | IOException T) {
				logger.log(Level.SEVERE, null, T);
				try {
					Thread.sleep(10000);
				} catch(InterruptedException err) {
					logger.log(Level.WARNING, null, err);
				}
			}
		}
	}
}
