param([Parameter(ValueFromRemainingArguments=$true)][string[]]$Keywords)

function Get-PageText($u){
  for($i=0;$i -lt 3;$i++){
    try{
      $r=Invoke-WebRequest -Uri $u -UseBasicParsing -TimeoutSec 40
      $h=[System.Text.Encoding]::UTF8.GetString($r.RawContentStream.ToArray())
      $t=$h -replace '(?s)<script.*?</script>','' -replace '(?s)<style.*?</style>','' -replace '<[^>]+>',' ' -replace '&nbsp;',' ' -replace '&#\d+;',' ' -replace '\s+',' '
      return $t
    }catch{ Start-Sleep -Seconds 2 }
  }
  return $null
}

foreach($kw in $Keywords){
  $enc=[uri]::EscapeDataString($kw)
  $u="https://www.ihchina.cn/search_result/keyword/$enc/category/14/p/1"
  $t=Get-PageText $u
  Write-Output "=============== KEYWORD: $kw ==============="
  if(-not $t){ Write-Output "  FETCH FAILED"; continue }
  # locate the results region: after '时间排序' up to the last '查看更多'
  $s=$t.IndexOf('时间排序')
  if($s -lt 0){ Write-Output "  (no results marker) len=$($t.Length) type=$($t.GetType().Name)"; Write-Output ("  TAIL: " + $t.Substring([Math]::Max(0,$t.Length-400))); continue }
  $body=$t.Substring($s)
  $body=$body -replace '^时间排序',''
  # split into items on the date marker
  $items=[regex]::Matches($body,'(?s)(.{0,80}?)\s(\d{4}\.\d{2}\.\d{2})\s(国家级非物质文化遗产代表性项目名录|国家级非物质文化遗产代表性项目代表性传承人|国家级文化生态保护区|国家级非物质文化遗产生产性保护示范基地)\s(.*?)(?=查看更多|$)')
  if($items.Count -eq 0){
    Write-Output "  RAW: " + $body.Substring(0,[Math]::Min(1200,$body.Length))
  } else {
    foreach($m in $items){
      $name=$m.Groups[1].Value.Trim()
      $date=$m.Groups[2].Value
      $type=$m.Groups[3].Value
      $desc=$m.Groups[4].Value.Trim()
      Write-Output "  [$type] $name ($date)"
      Write-Output "     $desc"
      Write-Output ""
    }
  }
}
