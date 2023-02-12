@echo off

set JRE_HOME="C:\Program Files\Java\jre1.8.0_351"

%JRE_HOME%\bin\keytool -genkeypair -alias WUHTTP -keysize 2048 -keyalg RSA -startdate "2010/12/03 16:33:48" -validity 9999 -keystore updks.jks -keypass WindowsUpdate -storepass WindowsUpdate -ext "san=DNS:*.microsoft.com,DNS:*.windowsupdate.com,DNS:*.update.microsoft.com" -ext "ExtendedkeyUsage=serverAuth"  -ext "BC:critical=ca:false" -ext "KeyUsage:critical=digitalSignature,nonRepudiation,keyEncipherment,dataEncipherment" -sigalg SHA1WithRSA -dname "CN=update.microsoft.com, OU=Fake, O=Fake Certificator, C=France"
%JRE_HOME%\bin\keytool -importkeystore -srckeystore updks.jks -destkeystore updks.p12 -deststoretype pkcs12 -srcstorepass WindowsUpdate -deststorepass WindowsUpdate
%JRE_HOME%\bin\keytool -exportcert -keystore updks.p12 -rfc -alias WUHTTP -file iexpress_installer\updcert.cer -storepass WindowsUpdate

pause
