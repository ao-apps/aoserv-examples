/*
 * Copyright 2001-2013, 2017 by AO Industries, Inc.,
 * 7262 Bull Pen Cir, Mobile, Alabama, 36695, U.S.A.
 * All rights reserved.
 */
package com.aoindustries.aoserv.examples.email;

import com.aoindustries.aoserv.client.AOServConnector;
import com.aoindustries.aoserv.client.AOServer;
import com.aoindustries.aoserv.client.EmailAddress;
import com.aoindustries.aoserv.client.EmailDomain;
import com.aoindustries.aoserv.client.LinuxAccount;
import com.aoindustries.aoserv.client.LinuxAccountType;
import com.aoindustries.aoserv.client.LinuxGroup;
import com.aoindustries.aoserv.client.LinuxServerAccount;
import com.aoindustries.aoserv.client.Package;
import com.aoindustries.aoserv.client.Shell;
import com.aoindustries.aoserv.client.SimpleAOClient;
import com.aoindustries.aoserv.client.Username;
import com.aoindustries.aoserv.client.validator.Gecos;
import com.aoindustries.net.DomainName;
import java.io.IOException;
import java.sql.SQLException;

/**
 * An email inbox is a restricted Linux account. It can be used for sending 
 * and receiving email using the <code>POP2</code>, <code>POP3</code>, 
 * <code>IMAP</code>, <code>SPOP3</code>, <code>SIMAP</code>, and <code>SMTP</code> 
 * protocols. If a shell connection is established, either via SSH or Telnet, the 
 * user is prompted to change their password. All other protocols are refused, 
 * including <code>FTP</code>.
 *
 * @author  AO Industries, Inc.
 */
final public class AddEmailInbox {

	/**
	 * Creates a new email inbox.
	 *
	 * @param  aoClient     the <code>SimpleAOClient</code> to use
	 * @param  packageName  the name of the <code>Package</code>
	 * @param  username     the new username to allocate
	 * @param  fullName     the user's full name
	 * @param  server       the hostname of the server to add the user to
	 * @param  address      the part of the email address before the <code>@</code>
	 * @param  password     the password for the new user
	 */
	public static void addEmailInbox(
		SimpleAOClient aoClient,
		String packageName,
		String username,
		Gecos fullName,
		String server,
		String address,
		DomainName domain,
		String password
	) throws IOException, SQLException {
		// Reserve the username
		aoClient.addUsername(packageName, username);

		// Indicate the username will be used for Linux accounts
		aoClient.addLinuxAccount(username, LinuxGroup.MAILONLY, fullName, null, null, null, LinuxAccountType.EMAIL, Shell.PASSWD);

		// Grant the new Linux account access to the server
		aoClient.addLinuxServerAccount(username, server, null);

		// Attach the email address to the new inbox
		aoClient.addLinuxAccAddress(address, domain, server, username);

		// Wait for rebuild
		aoClient.waitForLinuxAccountRebuild(server);

		// Set the password
		aoClient.setLinuxServerAccountPassword(username, server, password);
	}

	/**
	 * Creates a new email inbox.
	 *
	 * @param  conn         the <code>AOServConnector</code> to use
	 * @param  packageName  the name of the <code>Package</code>
	 * @param  username     the new username to allocate
	 * @param  fullName     the user's full name
	 * @param  server       the hostname of the server to add the user to
	 * @param  address      the part of the email address before the <code>@</code>
	 * @param  password     the password for the new account
	 *
	 * @return  the new <code>LinuxServerAccount</code>
	 */
	public static LinuxServerAccount addEmailInbox(
		AOServConnector conn,
		String packageName,
		String username,
		Gecos fullName,
		String server,
		String address,
		DomainName domain,
		String password
	) throws IOException, SQLException {
		// Resolve the Package
		Package pk=conn.getPackages().get(packageName);

		// Reserve the username
		pk.addUsername(username);
		Username un=conn.getUsernames().get(username);

		// Indicate the username will be used for Linux accounts
		un.addLinuxAccount(LinuxGroup.MAILONLY, fullName, null, null, null, LinuxAccountType.EMAIL, Shell.PASSWD);
		LinuxAccount la=un.getLinuxAccount();

		// Find the AOServer
		AOServer ao=conn.getServers().get(server).getAOServer();

		// Grant the new Linux account access to the server
		int lsaPKey=la.addLinuxServerAccount(ao, LinuxServerAccount.getDefaultHomeDirectory(username));
		LinuxServerAccount lsa=conn.getLinuxServerAccounts().get(lsaPKey);

		// Find the EmailDomain
		EmailDomain sd=ao.getEmailDomain(domain);

		// Create the new email address
		int eaPKey=sd.addEmailAddress(address);
		EmailAddress ea=conn.getEmailAddresses().get(eaPKey);

		// Attach the email address to the new inbox
		lsa.addEmailAddress(ea);

		// Wait for rebuild
		ao.waitForLinuxAccountRebuild();

		// Set the password
		lsa.setPassword(password);

		// Return the new object
		return lsa;
	}
}
