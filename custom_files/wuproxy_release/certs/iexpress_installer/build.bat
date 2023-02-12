@echo off
cd "%~dp0"
copy wupd_certificate.sed_ wupd_certificate.sed
cscript "replacetool.vbs
%WINDIR%\System32\iexpress /N /Q wupd_certificate.sed
%WINDIR%\SysWOW64\iexpress /N /Q wupd_certificate.sed
del wupd_certificate.sed
