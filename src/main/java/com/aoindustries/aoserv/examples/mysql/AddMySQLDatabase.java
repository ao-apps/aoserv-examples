/*
 * aoserv-examples - Automation examples for the AOServ Platform.
 * Copyright (C) 2001-2009, 2017, 2018  AO Industries, Inc.
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
