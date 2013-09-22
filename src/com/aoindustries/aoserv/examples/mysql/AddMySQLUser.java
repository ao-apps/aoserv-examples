/*
 * Copyright 2001-2013 by AO Industries, Inc.,
 * 7262 Bull Pen Cir, Mobile, Alabama, 36695, U.S.A.
 * All rights reserved.
 */
package com.aoindustries.aoserv.examples.mysql;

import com.aoindustries.aoserv.client.Package;
import com.aoindustries.aoserv.client.*;
import com.aoindustries.aoserv.client.validator.DomainName;
import java.io.*;
import java.sql.*;

/**
 * Adds a <code>MySQLUser</code> to the system.
 *
 * @author  AO Industries, Inc.
 */
final public class AddMySQLUser {

/**
 * Adds a <code>MySQLUser</code> to the system.
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
    String packageName,
    String username,
    String mysqlServer,
    String server,
    String database,
    String password
) throws IOException, SQLException {
    // Reserve the username
    aoClient.addUsername(packageName, username);

    // Indicate the username will be used for MySQL accounts
    aoClient.addMySQLUser(username);

    // Grant access to the server
    aoClient.addMySQLServerUser(username, mysqlServer, server, MySQLServerUser.ANY_LOCAL_HOST);

    // Grant access to the database
    aoClient.addMySQLDBUser(database, mysqlServer, server, username, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true);

    // Commit the changes before setting the password
    aoClient.waitForMySQLUserRebuild(server);

    // Set the password
    aoClient.setMySQLServerUserPassword(username, mysqlServer, server, password);
}

/**
 * Adds a <code>MySQLUser</code> to the system.
 *
 * @param  conn         the <code>AOServConnector</code> to use
 * @param  packageName  the name of the <code>Package</code>
 * @param  username     the new username to allocate
 * @param  mysqlServer  the name of the MySQL instance
 * @param  server       the hostname of the server to add the account to
 * @param  database     the new user will be granted access to this database
 * @param  password     the password for the new account
 *
 * @return  the new <code>MySQLServerUser</code>
 */
public static MySQLServerUser addMySQLUser(
    AOServConnector conn,
    String packageName,
    String username,
    String mysqlServer,
    DomainName server,
    String database,
    String password
) throws IOException, SQLException {
    // Find the Package
    Package pk=conn.getPackages().get(packageName);

    // Resolve the Server
    AOServer ao=conn.getAoServers().get(server);

    // Resolve the MySQLServer
    MySQLServer ms=ao.getMySQLServer(mysqlServer);

    // Reserve the username
    pk.addUsername(username);
    Username un=conn.getUsernames().get(username);

    // Indicate the username will be used for MySQL accounts
    un.addMySQLUser();
    MySQLUser mu=un.getMySQLUser();

    // Grant access to the server
    int msuPKey=mu.addMySQLServerUser(ms, MySQLServerUser.ANY_LOCAL_HOST);
    MySQLServerUser msu=conn.getMysqlServerUsers().get(msuPKey);
    
    // Find the MySQLDatabase
    MySQLDatabase md=ms.getMySQLDatabase(database);

    // Grant access to the database
    md.addMySQLServerUser(msu, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true);

    // Commit the changes before setting the password
    ao.waitForMySQLUserRebuild();

    // Set the password
    msu.setPassword(password);
    
    // Return the object
    return msu;
}
}