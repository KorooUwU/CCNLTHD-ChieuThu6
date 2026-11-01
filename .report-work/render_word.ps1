$ErrorActionPreference = 'Stop'
$workspaceRoot = Split-Path $PSScriptRoot -Parent
$docPath = Join-Path $workspaceRoot 'outputs/report-authentication/Bao_cao_do_an_bo_sung_Authentication.docx'
$renderPath = Join-Path $PSScriptRoot 'render'
New-Item -ItemType Directory -Force -Path $renderPath | Out-Null
$wordApp = $null
$wordDoc = $null
try {
    $wordApp = New-Object -ComObject Word.Application
    $wordApp.Visible = $false
    $wordApp.DisplayAlerts = 0
    $wordApp.AutomationSecurity = 3
    $wordApp.Options.UpdateLinksAtOpen = $false
    Write-Output 'Word initialized'
    $wordDoc = $wordApp.Documents.Open($docPath, $false, $false)
    Write-Output 'Document opened'
    # TOC was updated and saved in the preceding run.
    $wordDoc.SaveAs2((Join-Path $renderPath 'report.pdf'),17)
    Write-Output 'PDF exported'
    Write-Output ('Pages: '+$wordDoc.ComputeStatistics(2))
    Write-Output ('TOC count: '+$wordDoc.TablesOfContents.Count)
} finally {
    if ($null -ne $wordDoc) { $wordDoc.Close(0) | Out-Null }
    if ($null -ne $wordApp) { $wordApp.Quit() | Out-Null }
}
