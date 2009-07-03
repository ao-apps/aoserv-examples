package com.aoindustries.aoserv.examples.ftp;

/*
 * Copyright 2001-2009 by AO Industries, Inc.,
 * 7262 Bull Pen Cir, Mobile, Alabama, 36695, U.S.A.
 * All rights reserved.
 */
import com.aoindustries.aoserv.client.AOServConnector;
import com.aoindustries.aoserv.client.AOServer;
import com.aoindustries.aoserv.client.LinuxAccount;
import com.aoindustries.aoserv.client.LinuxAccountType;
import com.aoindustries.aoserv.client.LinuxServerAccount;
import com.aoindustries.aoserv.client.Package;
import com.aoindustries.aoserv.client.Shell;
import com.aoindustries.aoserv.client.SimpleAOClient;
import com.aoindustries.aoserv.client.Username;
import java.io.IOException;
import java.sql.SQLException;

/**
 * An FTP Guest User is a restricted Linux Account.  The account is allowed to
 * transfer files via FTP only.  The account may not be used for use as an email
 * inbox.  If the user logs into the server via SSH or telnet, they are allowed
 * to change their password and then they are immediately disconnected.<br />
 * <br />
 * FTP Guest Users may only transfer files into and out of their home directories.
 * By making the home directory of the user be the <code>/www/<i>sitename</i>/webapps</code>
 * directory, the account is effectively restricted to accessing and updating the
 * content of a single web site.  Keep in mind, however, that the user may still upload
 * code that can access files outside the site.
 *
 * @author  AO Industries, Inc.
 */
final public class AddFTPGuestUser {

/**
 * Adds a <code>FTPGuestUser</code> to the system.
 *
 * @param  aoClient     the <code>SimpleAOClient</code> to use
 * @param  packageName  the name of the package to add the account to
 * @param  username     the username to allocate
 * @param  fullName     the full name of the user
 * @param  group        the name of the Linux group they can access
 * @param  server       the hostname of the server to add the database to
 * @param  home         the directory the user has access to
 * @param  password     the password for the new account
 */
public static void addFTPGuestuser(
    SimpleAOClient aoClient,
    String packageName,
    String username,
    String fullName,
    String group,
    String server,
    String home,
    String password
) throws IOException, SQLException {
    // Allocate the username
    aoClient.addUsername(packageName, username);
    
    // Reserve the username for use as a Linux account
    aoClient.addLinuxAccount(username, group, fullName, null, null, null, LinuxAccountType.FTPONLY, Shell.FTPPASSWD);

    // Limit the FTP transfers to the users home directory
    aoClient.addFTPGuestUser(username);
    
    // Grant the user access to the server
    aoClient.addLinuxServerAccount(username, server, home);

    // Wait for rebuild
    aoClient.waitForLinuxAccountRebuild(server);

    // Set the password
    aoClient.setLinuxServerAccountPassword(username, server, password);
}

/**
 * Adds a <code>FTPGuestUser</code> to the system.
 *
 * @param  conn         the <code>AOServConnector</code> to use
 * @param  packageName  the name of the package to add the account to
 * @param  username     the username to allocate
 * @param  fullName     the full name of the user
 * @param  group        the name of the Linux group they can access
 * @param  server       the hostname of the server to add the database to
 * @param  home         the directory the user has access to
 * @param  password     the password for the new account
 *
 * @return  the new <code>LinuxServerAccount</code>
 */
public static LinuxServerAccount addFTPGuestuser(
    AOServConnector conn,
    String packageName,
    String username,
    String fullName,
    String group,
    String server,
    String home,
    String password
) throws IOException, SQLException {
    // Resolve the Package
    Package pk=conn.getPackages().get(packageName);

    // Allocate the username
    pk.addUsername(username);
    Username un=conn.getUsernames().get(username);
    
    // Reserve the username for use as a Linux account
    un.addLinuxAccount(fullName, group, null, null, null, LinuxAccountType.FTPONLY, Shell.FTPPASSWD);
    LinuxAccount la=un.getLinuxAccount();

    // Limit the FTP transfers to the users home directory
    la.addFTPGuestUser();

    // Find the server
    AOServer ao=conn.getAoServers().get(server);

    // Grant the user access to the server
    int lsaPKey=la.addLinuxServerAccount(ao, home);
    LinuxServerAccount lsa=conn.getLinuxServerAccounts().get(lsaPKey);

    // Wait for rebuild
    ao.waitForLinuxAccountRebuild();

    // Set the password
    lsa.setPassword(password);
    
    // Return the new object
    return lsa;
}
}
