JMagicProxy
===============
This proxy consists in a system which allows HTTP and HTTPS requests to be modified.

1) Command line arguments
-help					| Displays the help.
-override-config:<cfg>	| Overrides the path to the configuration file which is jmagicproxy.cfg by default.

2) Configuration file
proxy.ssl.keystoretype					| Represents the type of the keystore ( pkcs12 by default )
proxy.plugins							| Represents the list of plugins to modify requests. Multiple plugins can be used with a semicolon
											Example: io.github.explodingbottle.jmagicproxy.implementation.WUProxy;io.github.explodingbottle.jmagicproxy.implementation.BasicProxy
											The plugin which is at the left will be the most priority, and the one at the right will be the less priority one.
proxy.ssl.keystorepass					| Represents the password to access the keystore.
proxy.ssl.warn.algorithms				| Choose whether or not you must be warned if the java.security file disables some algorithms.
proxy.ssl.keystorepath					| Where to find the keystore file.
proxy.server.port						| Represents the proxy port for both HTTP and HTTPS.
proxy.logging.logfile					| Represents the naming of log files.
											&$LNUM$ is a placeholder that can be used and denotes the current milliseconds.
proxy.plugin.wuproxy.redirectjs			| A setting specific for WUProxy: Where can we find a replaced version of redirect.js
proxy.ssl.scan.startingport				| Represents what is the first port to scan to find where a SSL Server Socket can be created on the local machine.
proxy.logging.logsfolder				| Represents the folder in which you will find log files.
proxy.ssl.enabled						| Choose whether or not if SSL will be supported.

3) Known issues
	- When gracefully shutting down, the proxy may just not stop.
	- A lot of exceptions can be thrown in the console.

4) WUProxy Configuration.
IMPORTANT: Some files may be missing if you download the standard release.

To allow Windows Update to be used, you must change the property proxy.plugins to
	io.github.explodingbottle.jmagicproxy.implementation.WUProxy;io.github.explodingbottle.jmagicproxy.implementation.BasicProxy
in order to allow the WUProxy plugin impact requests.
Next, you must set proxy.plugin.wuproxy.redirectjs to where you can find a replaced redirect.js ( very important as it allows you to access the Windows Update website )
Be sure to generate a certificate using the tools available in the certs folder and to install it as computer account.

Operating System Status:

Windows 2000: If you install the proxy certificate as well as the Microsoft Root Certificate Authority
(it can be extracted from https://fe2.update.microsoft.com/v8/windowsupdate/redir/muv3wuredir.cab), if you
also configure Internet Explorer Proxy AND the System Proxy to point to this proxy, everything will work fine with no modifications.

Windows XP: You need to install this proxy certificate, configure Internet Explorer Proxy AND the System Proxy to point to this proxy
and then configure the WSUS Server locations as https://fe2.update.microsoft.com/v6
The website won't work.
