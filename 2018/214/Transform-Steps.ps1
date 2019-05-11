[CmdletBinding()]
param ( [Parameter(ValueFromPipeline)] [psobject] $Step )
begin
{
    "import turtle"
    "import math"
    [string] $RADICAL = "√"
    $ENCODING = New-Object System.Text.UTF7Encoding
    Write-Host ([System.Text.Encoding]::UTF7.GetString($ENCODING.GetBytes($RADICAL)))
}
process
{
    if ($Step.Seth)
    {
        "turtle.seth($($Step.Seth))"
    }
    if ($Step.Paces)
    {
        Write-Host $Step.Paces.ToCharArray()
        if ($Step.Paces.Contains($RADICAL))
        {
            Write-Host 'here'
            $comps = $Step.Paces.Split($RADICAL)
            [int] $base = $comps[0]
            [int] $mult = $comps[2]
            $expr = "$base * math.sqrt($mult)"
            "turtle.fd($expr)"
        }
        else
        {
            "turtle.fd($($Step.Paces))"
        }
    }
    if ($Step.Degrees)
    {
        $comps = $Step.Degrees.Split(',')
        $dist = $comps[0]
        $angle = if ($comps[1]) { $comps[1] } else { 'None' }
        "turtle.circle($dist, extent=$angle)"
    }
}