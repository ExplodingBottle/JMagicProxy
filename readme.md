JMagicProxy
===========

This proxy consists in a system which allows HTTP and HTTPS requests to be modified.\
This project requires ExplodingAULib for recompiling.

1) **Command line arguments**


   | Options                    | Description                                                                       |
   | ---------------------------- | --------------------------------------------------------------------------------- |
   | *-help*                    | Displays the help.                                                                |
   | *-override-config*:\<cfg\> | Overrides the path to the configuration file which is jmagicproxy.cfg by default. |

2) **Configuration file**
   | Options                    | Description                                                                       |
   | ---------------------------- | --------------------------------------------------------------------------------- |
   | *proxy.ssl.keystoretype*					| Represents the type of the keystore ( pkcs12 by default ) |
   | *proxy.plugins*							| Represents the list of plugins to modify requests. Multiple plugins can be used with a semicolon. Example: io.github.explodingbottle.jmagicproxy.implementation.WUProxy;io.github.explodingbottle.jmagicproxy.implementation.BasicProxy. The plugin which is at the left will be the most priority, and the one at the right will be the less priority one. |
   | *proxy.ssl.keystorepass*					| Represents the password to access the keystore. |
   | *proxy.ssl.warn.algorithms*				| Choose whether or not you must be warned if the java.security file disables some algorithms. |
   | *proxy.ssl.keystorepath*					| Where to find the keystore file. |
   | *proxy.server.port*						| Represents the proxy port for both HTTP and HTTPS. |
   | *proxy.logging.logfile*					| Represents the naming of log files. **&\$LNUM\$** is a placeholder that can be used and denotes the current milliseconds. |
   | *proxy.plugin.wuproxy.redirectjs*			| A setting specific for WUProxy: Where can we find a replaced version of redirect.js |
   | *proxy.ssl.scan.startingport*				| Represents what is the first port to scan to find where a SSL Server Socket can be created on the local machine. |
   | *proxy.logging.logsfolder*				| Represents the folder in which you will find log files. |
   | *proxy.ssl.enabled*						| Choose whether or not if SSL will be supported. |
   | *proxy.ssl.sortmode*				| Represents the sorting mode used to determine if a direct SSL connection must be established or instead if the Proxy must handle it. **NONE** means that every SSL requests will be handled by the proxy. **INCLUDE** means that only listed requests will be handled by the proxy and **EXCLUDE** means that only listed requests will be sent through a tunel directly. |
   | *proxy.ssl.sortlist*						| A list of requests splited with semi-colons that will be used with the sort mode. * can be used to mean everything. An example could be *.google.com;*.microsoft.com |
   | *proxy.plugin.wuproxy.redirwuclient*			| A setting specific for WUProxy: Defines if we must simulate an older version of the Windows Update client in order to allow Windows XP to update |
3) **Known issues**

   - A lot of exceptions can be thrown in the console.
4) **WUProxy Configuration**
   **IMPORTANT: Some files may be missing if you download the standard release.**

*For detailed instructions about using WUProxy with Windows 2000 and Windows XP, please check out the wiki.*

To allow Windows Update to be used, you must change the property proxy.plugins to
io.github.explodingbottle.jmagicproxy.implementation.WUProxy;io.github.explodingbottle.jmagicproxy.implementation.BasicProxy
in order to allow the WUProxy plugin impact requests.
Next, you must set proxy.plugin.wuproxy.redirectjs to where you can find a replaced redirect.js ( very important as it allows you to access the Windows Update website )
Be sure to generate a certificate using the tools available in the certs folder and to install it as computer account.

**Operating System Status**:

Windows 2000, Windows XP, Windows Server 2003, Windows POSReady 2009 and all the Windows NT 5 operating systems are supported. Please check out the wiki for configuration instructions.
