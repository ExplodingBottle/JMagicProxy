Set oFSO = WScript.CreateObject("Scripting.FileSystemObject")
Set WshShell = CreateObject("WScript.Shell")
Set oInput = oFSO.OpenTextFile("wupd_certificate.sed", 1)
sData = Replace(oInput.ReadAll, "${SourceFolder}", WshShell.CurrentDirectory)
sData = Replace(sData, "${TargetName}", WshShell.CurrentDirectory & "\wupdcerts_installer.exe")
Set oOutput = oFSO.CreateTextFile("wupd_certificate.sed", True)
oOutput.Write sData
oInput.Close
oOutput.Close
