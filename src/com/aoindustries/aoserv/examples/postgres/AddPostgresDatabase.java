package com.aoindustries.aoserv.examples.postgres;

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
 * Before creating a new PostgreSQL database, please make sure that a <code>PostgresUser</code>
 * has been added for use as the Database Administrator (DBA).<BR>
 * <BR>
 * The possible values for <code>encoding</code> may be found in the <code>postgres_encodings</code> table.
 *
 * @see  AddPostgresUser
 * @see  com.aoindustries.aoserv.client.PostgresEncoding
 *
 * @author  AO Industries, Inc.
 */
final public class AddPostgresDatabase {

/**
 * Adds a <code>PostgresDatabase</code> to a <code>Server</code>
 *
 * @param  aoClient        the <code>SimpleAOClient</code> to use
 * @param  name            the name of the database to add
 * @param  postgresServer  the name of the PostgreSQL server
 * @param  server          the hostname of the server to add the database to
 * @param  datdba          the username of the database administrator <code>PostgresUser</code>
 * @param  encoding        the encoding to use
 * @param  enablePostgis   enables PostGIS on the database
 */
public static void addPostgresDatabase(
    SimpleAOClient aoClient,
    String name,
    String postgresServer,
    String server,
    String datdba,
    String encoding,
    boolean enablePostgis
) throws IOException, SQLException {
    aoClient.addPostgresDatabase(name, postgresServer, server, datdba, encoding, enablePostgis);
}

/**
 * Adds a <code>PostgresDatabase</code> to a <code>Server</code>
 *
 * @param  conn            the <code>AOServConnector</code> to use
 * @param  name            the name of the database to add
 * @param  postgresServer  the name of the PostgreSQL server
 * @param  server          the hostname of the server to add the database to
 * @param  datdba          the username of the database administrator <code>PostgresUser</code>
 * @param  encoding        the encoding to use
 * @param  enablePostgis   enables PostGIS on the database
 *
 * @return  the new <code>PostgresDatabase</code>
 */
public static PostgresDatabase addPostgresDatabase(
    AOServConnector conn,
    String name,
    String postgresServer,
    String server,
    String datdba,
    String encoding,
    boolean enablePostgis
) throws IOException, SQLException {

    // Resolve the Server
    AOServer ao=conn.getAoServers().get(server);

    // Resolve the PostgresServer
    PostgresServer ps=ao.getPostgresServer(postgresServer);

    // Resolve the datdba PostgresServerUser
    PostgresServerUser psu=ps.getPostgresServerUser(datdba);

    // Resolve the PostgresEncoding
    PostgresEncoding pe=ps.getPostgresVersion().getPostgresEncoding(conn, encoding);

    // Add the PostgresDatabase
    int pdPKey=ps.addPostgresDatabase(name, psu, pe, enablePostgis);
    
    // Return the object
    return conn.getPostgresDatabases().get(pdPKey);
}
}
