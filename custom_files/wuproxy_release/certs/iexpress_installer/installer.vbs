Set WshShell = WScript.CreateObject("WScript.Shell")

RetCode = WshShell.Run("certutil -addstore -v Root updcert.cer", 0, TRUE)
If RetCode = 0 Then
   MsgBox "The certificate has been successfully installed !", vbInformation, "Installation success"
ElseIf RetCode = -2147024156 Then
   MsgBox "The certificate installation failed with error " & RetCode & ". Try running the program as administrator to continue." , vbExclamation, "Installation failure"
Else
   MsgBox "The certificate installation failed with error " & RetCode & "." , vbExclamation, "Installation failure"
End If