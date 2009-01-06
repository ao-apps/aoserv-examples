package com.aoindustries.aoserv.examples.mysql;

/*
 * Copyright 2001-2009 by AO Industries, Inc.,
 * 7262 Bull Pen Cir, Mobile, Alabama, 36695, U.S.A.
 * All rights reserved.
 */
import com.aoindustries.aoserv.client.Package;
import com.aoindustries.aoserv.client.*;
import com.aoindustries.io.*;
import com.aoindustries.sql.*;
import java.io.*;
import java.sql.*;

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
    String name,
    String mysqlServer,
    String server,
    String packageName
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
    String name,
    String mysqlServer,
    String server,
    String packageName
) throws IOException, SQLException {

    // Resolve the AOServer
    AOServer ao=conn.aoServers.get(server);

    // Resolve the MySQLServer
    MySQLServer ms=ao.getMySQLServer(mysqlServer);
    
    // Resolve the Package
    Package pk=conn.packages.get(packageName);

    // Add the MySQLDatabase
    int mdPKey=ms.addMySQLDatabase(name, pk);
    MySQLDatabase md=conn.mysqlDatabases.get(mdPKey);

    // Return the object
    return md;
}
}