/*
 * Copyright 2001-2013, 2017, 2018, 2020 by AO Industries, Inc.,
 * 7262 Bull Pen Cir, Mobile, Alabama, 36695, U.S.A.
 * All rights reserved.
 */
package com.aoindustries.aoserv.examples.ftp;

import com.aoindustries.aoserv.client.AOServConnector;
import com.aoindustries.aoserv.client.SimpleAOClient;
import com.aoindustries.aoserv.client.account.Account;
import com.aoindustries.aoserv.client.linux.Group;
import com.aoindustries.aoserv.client.linux.PosixPath;
import com.aoindustries.aoserv.client.linux.Server;
import com.aoindustries.aoserv.client.linux.Shell;
import com.aoindustries.aoserv.client.linux.User;
import com.aoindustries.aoserv.client.linux.User.Gecos;
import com.aoindustries.aoserv.client.linux.UserServer;
import com.aoindustries.aoserv.client.linux.UserType;
import com.aoindustries.net.DomainName;
import java.io.IOException;
import java.sql.SQLException;

/**
 * <p>
 * An FTP Guest User is a restricted Linux Account.  The account is allowed to
 * transfer files via FTP only.  The account may not be used for use as an email
 * inbox.  If the user logs into the server via SSH or telnet, they are allowed
 * to change their password and then they are immediately disconnected.
 * </p>
 * <p>
 * FTP Guest Users may only transfer files into and out of their home directories.
 * By making the home directory of the user be the <code>/www/<i>sitename</i>/webapps</code>
 * directory, the account is effectively restricted to accessing and updating the
 * content of a single web site.  Keep in mind, however, that the user may still upload
 * code that can access files outside the site.
 * </p>
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
		Account.Name packageName,
		User.Name username,
		Gecos fullName,
		Group.Name group,
		String server,
		PosixPath home,
		String password
	) throws IOException, SQLException {
		// Allocate the username
		aoClient.addUsername(packageName, username);

		// Reserve the username for use as a Linux account
		aoClient.addLinuxAccount(username, group, fullName, null, null, null, UserType.FTPONLY, Shell.FTPPASSWD);

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
	 * @return  the new <code>UserServer</code>
	 */
	public static UserServer addFTPGuestuser(
		AOServConnector conn,
		Account.Name packageName,
		User.Name username,
		Gecos fullName,
		Group.Name group,
		DomainName server,
		PosixPath home,
		String password
	) throws IOException, SQLException {
		// Resolve the Package
		com.aoindustries.aoserv.client.billing.Package pk=conn.getBilling().getPackage().get(packageName);

		// Allocate the username
		pk.addUsername(username);
		com.aoindustries.aoserv.client.account.User un = conn.getAccount().getUser().get(username);

		// Reserve the username for use as a Linux account
		un.addLinuxAccount(group, fullName, null, null, null, UserType.FTPONLY, Shell.FTPPASSWD);
		User la=un.getLinuxAccount();

		// Limit the FTP transfers to the users home directory
		la.addFTPGuestUser();

		// Find the server
		Server ao=conn.getLinux().getServer().get(server);

		// Grant the user access to the server
		int lsaPKey=la.addLinuxServerAccount(ao, home);
		UserServer lsa=conn.getLinux().getUserServer().get(lsaPKey);

		// Wait for rebuild
		ao.waitForLinuxAccountRebuild();

		// Set the password
		lsa.setPassword(password);

		// Return the new object
		return lsa;
	}

	private AddFTPGuestUser() {}
}
