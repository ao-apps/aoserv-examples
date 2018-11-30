/*
 * Copyright 2001-2013, 2017, 2018 by AO Industries, Inc.,
 * 7262 Bull Pen Cir, Mobile, Alabama, 36695, U.S.A.
 * All rights reserved.
 */
package com.aoindustries.aoserv.examples.postgres;

import com.aoindustries.aoserv.client.AOServConnector;
import com.aoindustries.aoserv.client.SimpleAOClient;
import com.aoindustries.aoserv.client.account.Username;
import com.aoindustries.aoserv.client.billing.Package;
import com.aoindustries.aoserv.client.linux.AOServer;
import com.aoindustries.aoserv.client.postgresql.PostgresServer;
import com.aoindustries.aoserv.client.postgresql.PostgresServerUser;
import com.aoindustries.aoserv.client.postgresql.PostgresUser;
import com.aoindustries.aoserv.client.validator.AccountingCode;
import com.aoindustries.aoserv.client.validator.PostgresServerName;
import com.aoindustries.aoserv.client.validator.PostgresUserId;
import java.io.IOException;
import java.sql.SQLException;

/**
 * Adds a <code>PostgresUser</code> to the system.
 *
 * @author  AO Industries, Inc.
 */
final public class AddPostgresUser {

	/**
	 * Adds a <code>PostgresUser</code> to the system.
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
		AccountingCode packageName,
		PostgresUserId username,
		PostgresServerName postgresServer,
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
	 * Adds a <code>PostgresUser</code> to the system.
	 *
	 * @param  conn            the <code>AOServConnector</code> to use
	 * @param  packageName     the name of the <code>Package</code>
	 * @param  username        the new username to allocate
	 * @param  postgresServer  the name of the PostgreSQL server
	 * @param  server          the hostname of the server to add the account to
	 * @param  password        the password for the new account
	 *
	 * @return  the new <code>PostgresServerUser</code>
	 */
	public static PostgresServerUser addPostgresUser(
		AOServConnector conn,
		AccountingCode packageName,
		PostgresUserId username,
		PostgresServerName postgresServer,
		String server,
		String password
	) throws IOException, SQLException {
		// Find the Package
		Package pk=conn.getPackages().get(packageName);

		// Reserve the username
		pk.addUsername(username);
		Username un=conn.getUsernames().get(username);

		// Indicate the username will be used for PostgreSQL accounts
		un.addPostgresUser();
		PostgresUser pu=un.getPostgresUser();

		// Resolve the Server
		AOServer ao=conn.getServers().get(server).getAOServer();

		// Resolve the PostgresServer
		PostgresServer ps=ao.getPostgresServer(postgresServer);

		// Grant access to the server
		int psuPKey=pu.addPostgresServerUser(ps);
		PostgresServerUser psu=conn.getPostgresServerUsers().get(psuPKey);

		// Commit the changes before setting the password
		ao.waitForPostgresUserRebuild();

		// Set the password
		psu.setPassword(password);

		// Return the object
		return psu;
	}

	private AddPostgresUser() {}
}
