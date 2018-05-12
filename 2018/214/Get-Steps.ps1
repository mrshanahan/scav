[CmdletBinding()]
param ([Parameter(Mandatory, Position=1)][string] $File)

[xml] $item214 = Get-Content $File -Raw
$seth = $item214.SelectNodes('//tr/*[1]')
$paces = $item214.SelectNodes('//tr/*[2]')
$degrees = $item214.SelectNodes('//tr/*[3]')

0..($seth.Count - 1) | % {
    New-Object PSObject -Property @{Seth = $seth[$_].'#text'; Paces = $paces[$_].'#text'; Degrees = $degrees[$_].'#text' }
}