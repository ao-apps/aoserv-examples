/*
 * aoserv-examples - Automation examples for the AOServ Platform.
 * Copyright (C) 2001-2013, 2017, 2018, 2019  AO Industries, Inc.
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
package com.aoindustries.aoserv.examples.postgres;

import com.aoindustries.aoserv.client.AOServConnector;
import com.aoindustries.aoserv.client.SimpleAOClient;
import com.aoindustries.aoserv.client.account.Account;
import com.aoindustries.aoserv.client.billing.Package;
import com.aoindustries.aoserv.client.postgresql.Server;
import com.aoindustries.aoserv.client.postgresql.User;
import com.aoindustries.aoserv.client.postgresql.UserServer;
import java.io.IOException;
import java.sql.SQLException;

/**
 * Adds a <code>User</code> to the system.
 *
 * @author  AO Industries, Inc.
 */
final public class AddPostgresUser {

	/**
	 * Adds a <code>User</code> to the system.
	 *
	 * @param  aoClient        the <code>SimpleAOClient</code> to use
	 * @param  packageName     the name of the <code>Package</code>
	 * @param  username        the new username to allocate
	 * @param  postgresServer  the name of the PostgreSQL server
	 * @param  server          the hostname of the server to add the account to
	 * @param  password        the password for the new account
	 */
	public static void addPostgresUser(
		SimpleAOClient aoClient,
		Account.Name packageName,
		User.Name username,
		Server.Name postgresServer,
		String server,
		String password
	) throws IOException, SQLException {
		// Reserve the username
		aoClient.addUsername(packageName, username);

		// Indicate the username will be used for PostgreSQL accounts
		aoClient.addPostgresUser(username);

		// Grant access to the server
		aoClient.addPostgresServerUser(username, postgresServer, server);

		// Commit the changes before setting the password
		aoClient.waitForPostgresUserRebuild(server);

		// Set the password
		aoClient.setPostgresServerUserPassword(username, postgresServer, server, password);
	}

	/**
	 * Adds a <code>User</code> to the system.
	 *
	 * @param  conn            the <code>AOServConnector</code> to use
	 * @param  packageName     the name of the <code>Package</code>
	 * @param  username        the new username to allocate
	 * @param  postgresServer  the name of the PostgreSQL server
	 * @param  server          the hostname of the server to add the account to
	 * @param  password        the password for the new account
	 *
	 * @return  the new <code>UserServer</code>
	 */
	public static UserServer addPostgresUser(
		AOServConnector conn,
		Account.Name packageName,
		User.Name username,
		Server.Name postgresServer,
		String server,
		String password
	) throws IOException, SQLException {
		// Find the Package
		Package pk = conn.getBilling().getPackage().get(packageName);

		// Reserve the username
		pk.addUsername(username);
		com.aoindustries.aoserv.client.account.User un = conn.getAccount().getUser().get(username);

		// Indicate the username will be used for PostgreSQL accounts
		un.addPostgresUser();
		User pu = un.getPostgresUser();

		// Resolve the Host
		com.aoindustries.aoserv.client.linux.Server linuxServer = conn.getNet().getHost().get(server).getLinuxServer();

		// Resolve the Server
		Server ps = linuxServer.getPostgresServer(postgresServer);

		// Grant access to the server
		int psuPKey = pu.addPostgresServerUser(ps);
		UserServer psu = conn.getPostgresql().getUserServer().get(psuPKey);

		// Commit the changes before setting the password
		linuxServer.waitForPostgresUserRebuild();

		// Set the password
		psu.setPassword(password);

		// Return the object
		return psu;
	}

	private AddPostgresUser() {}
}
