/*
 * Copyright 2001-2009, 2017 by AO Industries, Inc.,
 * 7262 Bull Pen Cir, Mobile, Alabama, 36695, U.S.A.
 * All rights reserved.
 */
package com.aoindustries.aoserv.examples.mysql;

import com.aoindustries.aoserv.client.AOServConnector;
import com.aoindustries.aoserv.client.AOServer;
import com.aoindustries.aoserv.client.MySQLDatabase;
import com.aoindustries.aoserv.client.MySQLServer;
import com.aoindustries.aoserv.client.Package;
import com.aoindustries.aoserv.client.SimpleAOClient;
import com.aoindustries.aoserv.client.validator.AccountingCode;
import com.aoindustries.aoserv.client.validator.MySQLDatabaseName;
import com.aoindustries.aoserv.client.validator.MySQLServerName;
import com.aoindustries.net.DomainName;
import java.io.IOException;
import java.sql.SQLException;

/**
 * Adds a new <code>MySQLDatabase</code> to the system.
 *
 * @see  AddMySQLUser
 *
 * @author  AO Industries, Inc.
 */
final public class AddMySQLDatabase {

	/**
	 * Adds a <code>MySQLDatabase</code> to a <code>Server</code>
	 *
	 * @param  aoClient     the <code>SimpleAOClient</code> to use
	 * @param  name         the name of the database to add
	 * @param  mysqlServer  the name of the MySQL instance
	 * @param  server       the hostname of the server to add the database to
	 * @param  packageName  the name of the <code>Package</code> that owns the new database
	 */
	public static void addMySQLDatabase(
		SimpleAOClient aoClient,
		MySQLDatabaseName name,
		MySQLServerName mysqlServer,
		String server,
		AccountingCode packageName
	) throws IOException, SQLException {
		aoClient.addMySQLDatabase(name, mysqlServer, server, packageName);
	}

	/**
	 * Adds a <code>MySQLDatabase</code> to a <code>Server</code>
	 *
	 * @param  conn  the <code>AOServConnector</code> to use
	 * @param  name  the name of the database to add
	 * @param  mysqlServer  the name of the MySQL instance
	 * @param  server  the hostname of the server to add the database to
	 * @param  packageName  the name of the <code>Package</code> that owns the new database
	 *
	 * @return  the new <code>MySQLDatabase</code>
	 */
	public static MySQLDatabase addMySQLDatabase(
		AOServConnector conn,
		MySQLDatabaseName name,
		MySQLServerName mysqlServer,
		DomainName server,
		AccountingCode packageName
	) throws IOException, SQLException {

		// Resolve the AOServer
		AOServer ao=conn.getAoServers().get(server);

		// Resolve the MySQLServer
		MySQLServer ms=ao.getMySQLServer(mysqlServer);

		// Resolve the Package
		Package pk=conn.getPackages().get(packageName);

		// Add the MySQLDatabase
		int mdPKey=ms.addMySQLDatabase(name, pk);
		MySQLDatabase md=conn.getMysqlDatabases().get(mdPKey);

		// Return the object
		return md;
	}

	private AddMySQLDatabase() {}
}
