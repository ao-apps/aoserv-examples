/*
 * Copyright 2001-2009, 2017, 2018 by AO Industries, Inc.,
 * 7262 Bull Pen Cir, Mobile, Alabama, 36695, U.S.A.
 * All rights reserved.
 */
package com.aoindustries.aoserv.examples.mysql;

import com.aoindustries.aoserv.client.AOServConnector;
import com.aoindustries.aoserv.client.SimpleAOClient;
import com.aoindustries.aoserv.client.account.Account;
import com.aoindustries.aoserv.client.billing.Package;
import com.aoindustries.aoserv.client.mysql.Database;
import com.aoindustries.aoserv.client.mysql.Server;
import com.aoindustries.net.DomainName;
import java.io.IOException;
import java.sql.SQLException;

/**
 * Adds a new <code>Database</code> to the system.
 *
 * @see  AddMySQLUser
 *
 * @author  AO Industries, Inc.
 */
final public class AddMySQLDatabase {

	/**
	 * Adds a <code>Database</code> to a <code>Host</code>
	 *
	 * @param  aoClient     the <code>SimpleAOClient</code> to use
	 * @param  name         the name of the database to add
	 * @param  mysqlServer  the name of the MySQL instance
	 * @param  server       the hostname of the server to add the database to
	 * @param  packageName  the name of the <code>Package</code> that owns the new database
	 */
	public static void addMySQLDatabase(
		SimpleAOClient aoClient,
		Database.Name name,
		Server.Name mysqlServer,
		String server,
		Account.Name packageName
	) throws IOException, SQLException {
		aoClient.addMySQLDatabase(name, mysqlServer, server, packageName);
	}

	/**
	 * Adds a <code>Database</code> to a <code>Host</code>
	 *
	 * @param  conn  the <code>AOServConnector</code> to use
	 * @param  name  the name of the database to add
	 * @param  mysqlServer  the name of the MySQL instance
	 * @param  server  the hostname of the server to add the database to
	 * @param  packageName  the name of the <code>Package</code> that owns the new database
	 *
	 * @return  the new <code>Database</code>
	 */
	public static Database addMySQLDatabase(
		AOServConnector conn,
		Database.Name name,
		Server.Name mysqlServer,
		DomainName server,
		Account.Name packageName
	) throws IOException, SQLException {

		// Resolve the Server
		com.aoindustries.aoserv.client.linux.Server ao=conn.getLinux().getServer().get(server);

		// Resolve the Server
		Server ms=ao.getMySQLServer(mysqlServer);

		// Resolve the Package
		Package pk=conn.getBilling().getPackage().get(packageName);

		// Add the Database
		int mdPKey=ms.addMySQLDatabase(name, pk);
		Database md=conn.getMysql().getDatabase().get(mdPKey);

		// Return the object
		return md;
	}

	private AddMySQLDatabase() {}
}
