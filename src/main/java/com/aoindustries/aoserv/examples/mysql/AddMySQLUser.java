/*
 * aoserv-examples - Automation examples for the AOServ Platform.
 * Copyright (C) 2001-2013, 2017, 2018, 2020, 2021, 2022  AO Industries, Inc.
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
 * along with aoserv-examples.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.aoindustries.aoserv.examples.mysql;

import com.aoapps.net.DomainName;
import com.aoindustries.aoserv.client.AOServConnector;
import com.aoindustries.aoserv.client.SimpleAOClient;
import com.aoindustries.aoserv.client.account.Account;
import com.aoindustries.aoserv.client.billing.Package;
import com.aoindustries.aoserv.client.mysql.Database;
import com.aoindustries.aoserv.client.mysql.Server;
import com.aoindustries.aoserv.client.mysql.User;
import com.aoindustries.aoserv.client.mysql.UserServer;
import java.io.IOException;
import java.sql.SQLException;

/**
 * Adds a <code>User</code> to the system.
 *
 * @author  AO Industries, Inc.
 */
public final class AddMySQLUser {

  /** Make no instances. */
  private AddMySQLUser() {
    throw new AssertionError();
  }

  /**
   * Adds a <code>User</code> to the system.
   *
   * @param  aoClient     the <code>SimpleAOClient</code> to use
   * @param  packageName  the name of the <code>Package</code>
   * @param  username     the new username to allocate
   * @param  mysqlServer  the name of the MySQL instance
   * @param  server       the hostname of the server to add the account to
   * @param  database     the new user will be granted access to this database
   * @param  password     the password for the new account
   */
  public static void addMySQLUser(
    SimpleAOClient aoClient,
    Account.Name packageName,
    User.Name username,
    Server.Name mysqlServer,
    String server,
    Database.Name database,
    String password
  ) throws IOException, SQLException {
    // Reserve the username
    aoClient.addUsername(packageName, username);

    // Indicate the username will be used for MySQL accounts
    aoClient.addMySQLUser(username);

    // Grant access to the server
    aoClient.addMySQLServerUser(username, mysqlServer, server, UserServer.ANY_LOCAL_HOST);

    // Grant access to the database
    aoClient.addMySQLDBUser(database, mysqlServer, server, username, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true);

    // Commit the changes before setting the password
    aoClient.waitForMySQLUserRebuild(server);

    // Set the password
    aoClient.setMySQLServerUserPassword(username, mysqlServer, server, password);
  }

  /**
   * Adds a <code>User</code> to the system.
   *
   * @param  conn         the <code>AOServConnector</code> to use
   * @param  packageName  the name of the <code>Package</code>
   * @param  username     the new username to allocate
   * @param  mysqlServer  the name of the MySQL instance
   * @param  server       the hostname of the server to add the account to
   * @param  database     the new user will be granted access to this database
   * @param  password     the password for the new account
   *
   * @return  the new <code>UserServer</code>
   */
  public static UserServer addMySQLUser(
    AOServConnector conn,
    Account.Name packageName,
    User.Name username,
    Server.Name mysqlServer,
    DomainName server,
    Database.Name database,
    String password
  ) throws IOException, SQLException {
    // Find the Package
    Package pk=conn.getBilling().getPackage().get(packageName);

    // Resolve the Host
    com.aoindustries.aoserv.client.linux.Server ao=conn.getLinux().getServer().get(server);

    // Resolve the Server
    Server ms=ao.getMySQLServer(mysqlServer);

    // Reserve the username
    pk.addUsername(username);
    com.aoindustries.aoserv.client.account.User un=conn.getAccount().getUser().get(username);

    // Indicate the username will be used for MySQL accounts
    un.addMySQLUser();
    User mu=un.getMySQLUser();

    // Grant access to the server
    int msuPKey=mu.addMySQLServerUser(ms, UserServer.ANY_LOCAL_HOST);
    UserServer msu=conn.getMysql().getUserServer().get(msuPKey);

    // Find the Database
    Database md=ms.getMySQLDatabase(database);

    // Grant access to the database
    conn.getMysql().getDatabaseUser().addMySQLDBUser(md, msu, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true);

    // Commit the changes before setting the password
    ao.waitForMySQLUserRebuild();

    // Set the password
    msu.setPassword(password);

    // Return the object
    return msu;
  }
}
