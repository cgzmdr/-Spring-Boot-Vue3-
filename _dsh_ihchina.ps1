param([string[]]$Keywords)

function Get-Text($html) {
  $t = $html -replace '(?s)<script.*?</script>','' -replace '(?s)<style.*?</style>',''
  $t = $t -replace '<[^>]+>',' ' -replace '&nbsp;',' ' -replace '\s+',' '
  return $t
}

foreach ($k in $Keywords) {
  Write-Output "########## KEYWORD: $k"
  $enc = [uri]::EscapeDataString($k)
  $api = "https://www.ihchina.cn/Article/Index/getProject.html?province=&rx_time=&type=&cate=&keywords=$enc&page=1&limit=20"
  $projs = $null
  for ($i=1; $i -le 3; $i++) {
    try { $projs = (Invoke-WebRequest -Uri $api -UseBasicParsing -TimeoutSec 40).Content | ConvertFrom-Json; break }
    catch { Start-Sleep -Seconds 3 }
  }
  if (-not $projs) { Write-Output "  API-FAIL"; continue }
  Write-Output "  total=$($projs.total)"
  foreach ($p in $projs.list) {
    $title = $p.title; $num = $p.num; $prov = $p.province; $rt = ($p.rx_time -replace '<br\s*/?>',' ') -replace '\s+',' '
    Write-Output "  -- [$num] $title | $prov | $rt | id=$($p.id)"
    $u = "https://www.ihchina.cn/project_details/$($p.id)"
    $html = $null
    for ($i=1; $i -le 3; $i++) {
      try { $html = (Invoke-WebRequest -Uri $u -UseBasicParsing -TimeoutSec 40).Content; break }
      catch { Start-Sleep -Seconds 3 }
    }
    if (-not $html) { Write-Output "     PAGE-FAIL"; continue }
    $t = Get-Text $html
    $idx = $t.IndexOf("相关传承人 编号")
    if ($idx -lt 0) { Write-Output "     NO-INHERITOR-SECTION"; continue }
    $seg = $t.Substring($idx, [Math]::Min(2500, $t.Length - $idx))
    $end = $seg.IndexOf("相关项目")
    if ($end -gt 0) { $seg = $seg.Substring(0, $end) }
    Write-Output "     INHERITORS: $seg"
  }
  Start-Sleep -Seconds 2
}
