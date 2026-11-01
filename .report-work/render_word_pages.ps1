$ErrorActionPreference='Stop'
Add-Type -AssemblyName System.Drawing
$docPath=Join-Path (Split-Path $PSScriptRoot -Parent) 'outputs/report-authentication/Bao_cao_do_an_bo_sung_Authentication.docx'
$renderDir=Join-Path $PSScriptRoot 'pages'
New-Item -ItemType Directory -Path $renderDir -Force | Out-Null
$app=$null;$document=$null
try {
    $app=New-Object -ComObject Word.Application
    $app.Visible=$false;$app.DisplayAlerts=0;$app.AutomationSecurity=3
    $app.Options.UpdateLinksAtOpen=$false
    $document=$app.Documents.Open($docPath,$false,$false)
    $document.ActiveWindow.View.Type=3
    Write-Output 'Document opened for page rendering'
    foreach($toc in $document.TablesOfContents){$toc.Update()}
    $document.Save()
    Write-Output 'TOC updated and DOCX saved'
    $pageCollection=$document.ActiveWindow.Panes.Item(1).Pages
    $pageCount=$pageCollection.Count
    Write-Output ('Pages: '+$pageCount)
    for($pageIndex=1;$pageIndex -le $pageCount;$pageIndex++) {
        $bits=[byte[]]$pageCollection.Item($pageIndex).EnhMetaFileBits
        $stream=[System.IO.MemoryStream]::new($bits,0,$bits.Length)
        $emf=[System.Drawing.Imaging.Metafile]::new($stream)
        $bitmap=[System.Drawing.Bitmap]::new(1000,1414)
        $graphics=[System.Drawing.Graphics]::FromImage($bitmap)
        $graphics.Clear([System.Drawing.Color]::White)
        $graphics.DrawImage($emf,0,0,1000,1414)
        $bitmap.Save((Join-Path $renderDir ('page-'+$pageIndex+'.png')),[System.Drawing.Imaging.ImageFormat]::Png)
        $graphics.Dispose();$bitmap.Dispose();$emf.Dispose();$stream.Dispose()
        Write-Output ('Rendered page '+$pageIndex)
    }
} finally {
    if($null -ne $document){$document.Close(0)|Out-Null}
    if($null -ne $app){$app.Quit()|Out-Null}
}
