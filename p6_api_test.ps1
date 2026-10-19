﻿﻿﻿$ErrorActionPreference = 'Stop'
$base = 'http://localhost:8080/api/v1'

function Write-Step($m) { Write-Host "`n===== $m =====" -ForegroundColor Cyan }

function Login($u, $p) {
  $cap = Invoke-RestMethod -Uri "$base/auth/captcha" -Method Get
  $client = New-Object System.Net.Sockets.TcpClient('192.168.10.102', 6379)
  $st = $client.GetStream()
  $w = New-Object System.IO.StreamWriter($st); $r = New-Object System.IO.StreamReader($st)
  $w.WriteLine("GET auth:captcha:$($cap.data.captchaId)"); $w.Flush()
  Start-Sleep -Milliseconds 100
  $r.ReadLine() | Out-Null; $code = $r.ReadLine()
  $w.Close(); $r.Close(); $client.Close()
  $body = @{ username = $u; password = $p; captchaId = $cap.data.captchaId; captchaCode = $code } | ConvertTo-Json
  return Invoke-RestMethod -Uri "$base/auth/login" -Method Post -Body $body -ContentType 'application/json'
}

$th = Login 'p2therapist01' 'Rehab@123456'
$h = @{ Authorization = "Bearer $($th.data.accessToken)" }
Write-Host "therapist login OK (userId=$($th.data.user.id))"

Write-Step '1. GET /therapist/dashboard'
$d = Invoke-RestMethod -Uri "$base/therapist/dashboard" -Headers $h
Write-Host "patients=$($d.data.patientCount) todayTasks=$($d.data.todayTaskCompleted)/$($d.data.todayTaskTotal) rate=$($d.data.todayCompletionRate) pendingReports=$($d.data.pendingReportCount) pendingConsults=$($d.data.pendingConsultCount) lowScoreToday=$($d.data.lowScoreTodayCount)"

Write-Step '2. GET /therapist/patients'
$pl = Invoke-RestMethod -Uri "$base/therapist/patients?page=1&size=10" -Headers $h
Write-Host "total=$($pl.data.total)"
foreach ($p in $pl.data.records) { Write-Host "  patientId=$($p.patientId) name=$($p.name) part=$($p.rehabPartName) relation=$($p.relationType) activePlans=$($p.activePlanCount) latest=$($p.latestPlanStatus) risk=$($p.hasRiskHint)" }
$pid1 = $pl.data.records[0].patientId

Write-Step "3. GET /therapist/patients/$pid1 detail"
$pd = Invoke-RestMethod -Uri "$base/therapist/patients/$pid1" -Headers $h
Write-Host "name=$($pd.data.name) phone=$($pd.data.phone) height=$($pd.data.heightCm) riskHints=$($pd.data.riskHints.Count)"

Write-Step "4. GET /therapist/patients/$pid1/plans"
$plans = Invoke-RestMethod -Uri "$base/therapist/patients/$pid1/plans?page=1&size=10" -Headers $h
Write-Host "total=$($plans.data.total)"
foreach ($x in $plans.data.records) { Write-Host "  planId=$($x.planId) v$($x.versionNo) $($x.name) status=$($x.status)" }

Write-Step "5. GET /therapist/patients/$pid1/tasks"
$tasks = Invoke-RestMethod -Uri "$base/therapist/patients/$pid1/tasks?page=1&size=5" -Headers $h
Write-Host "total=$($tasks.data.total) first=$($tasks.data.records[0].taskDate) $($tasks.data.records[0].actionName) $($tasks.data.records[0].status)"

Write-Step "6. GET /therapist/patients/$pid1/alerts"
$alerts = Invoke-RestMethod -Uri "$base/therapist/patients/$pid1/alerts?page=1&size=10" -Headers $h
Write-Host "total=$($alerts.data.total)"

Write-Step '7. GET /therapist/actions (editor dropdown source)'
$acts = Invoke-RestMethod -Uri "$base/therapist/actions?page=1&size=10" -Headers $h
Write-Host "total=$($acts.data.total)"
foreach ($a in $acts.data.records) { Write-Host "  actionVersionId=$($a.actionVersionId) $($a.name) difficulty=$($a.difficulty)" }
$avId = $acts.data.records[0].actionVersionId

Write-Step "8. POST /therapist/patients/$pid1/plans (create DRAFT)"
$today = Get-Date -Format 'yyyy-MM-dd'
$end = (Get-Date).AddDays(30).ToString('yyyy-MM-dd')
$dict = Invoke-RestMethod -Uri "$base/public/rehab-parts" -Method Get
$pc = $dict.data[0].code
Write-Host "use rehabPartCode=$pc"
$createBody = @{
  name = "P6 API test plan $today"
  rehabPartCode = $pc
  startDate = $today
  endDate = $end
  remark = 'P6 e2e'
  items = @( @{ actionVersionId = $avId; sets = 2; targetReps = 10; targetAngle = 90; sequenceNo = 1 } )
} | ConvertTo-Json -Depth 5
try {
  $created = Invoke-RestMethod -Uri "$base/therapist/patients/$pid1/plans" -Method Post -Body $createBody -ContentType 'application/json; charset=utf-8' -Headers $h
  $newPlanId = $created.data.planId
  Write-Host "created planId=$newPlanId status=$($created.data.status) items=$($created.data.items.Count)"
} catch {
  Write-Host "CREATE FAILED status=$($_.Exception.Response.StatusCode.value__) body=$($_.ErrorDetails.Message)"
  throw
}

Write-Step "9. PUT /therapist/plans/$newPlanId (update draft)"
$updBody = @{ name = "P6 API test plan v2 $today"; rehabPartCode = $created.data.rehabPartCode; startDate = $today; endDate = $end; remark = 'P6 e2e updated' } | ConvertTo-Json
$upd = Invoke-RestMethod -Uri "$base/therapist/plans/$newPlanId" -Method Put -Body $updBody -ContentType 'application/json; charset=utf-8' -Headers $h
Write-Host "updated name=$($upd.data.name)"

Write-Step "10. PUT /therapist/plans/$newPlanId/schedule (WEEKLY 1,3,5)"
$schBody = @{ frequencyType = 'WEEKLY'; weekdays = @(1,3,5) } | ConvertTo-Json
try {
  $sch = Invoke-RestMethod -Uri "$base/therapist/plans/$newPlanId/schedule" -Method Put -Body $schBody -ContentType 'application/json' -Headers $h
  Write-Host "schedule=$($sch.data.frequencyType) weekdays=$($sch.data.weekdays -join ',')"
} catch {
  $b = $_.ErrorDetails.Message
  Write-Host "schedule FAILED $($_.Exception.Response.StatusCode.value__) body=$b"
  # read DTO field names from error and retry with alternates
  $sd = $b | ConvertFrom-Json
  Write-Host "message=$($sd.message)"
}

Write-Step "11. POST /therapist/plans/$newPlanId/items (add second item)"
$itemBody = @{ actionVersionId = $avId; targetSets = 3; targetReps = 12; targetAmplitude = 80 } | ConvertTo-Json
try {
  $item = Invoke-RestMethod -Uri "$base/therapist/plans/$newPlanId/items" -Method Post -Body $itemBody -ContentType 'application/json' -Headers $h
  Write-Host "itemId=$($item.data.planItemId) sets=$($item.data.targetSets) reps=$($item.data.targetReps)"
  $newItemId = $item.data.planItemId
} catch {
  Write-Host "add item FAILED $($_.Exception.Response.StatusCode.value__) body=$($_.ErrorDetails.Message)"
}

Write-Step "12. POST /therapist/plans/$newPlanId/publish"
$pub = Invoke-RestMethod -Uri "$base/therapist/plans/$newPlanId/publish" -Method Post -Headers $h
Write-Host "published status=$($pub.data.status)"

Write-Step "13. PUT plan after publish (expect 409)"
try {
  Invoke-RestMethod -Uri "$base/therapist/plans/$newPlanId" -Method Put -Body $updBody -ContentType 'application/json; charset=utf-8' -Headers $h | Out-Null
  Write-Host "UNEXPECTED: update published plan succeeded"
} catch {
  Write-Host "got $($_.Exception.Response.StatusCode.value__) (expect 409)"
}

Write-Step "14. pause -> resume -> archive"
Invoke-RestMethod -Uri "$base/therapist/plans/$newPlanId/pause" -Method Post -Body (@{ reason = 'e2e pause' } | ConvertTo-Json) -ContentType 'application/json; charset=utf-8' -Headers $h | Out-Null
Write-Host "paused"
Invoke-RestMethod -Uri "$base/therapist/plans/$newPlanId/resume" -Method Post -Headers $h | Out-Null
Write-Host "resumed"
Invoke-RestMethod -Uri "$base/therapist/plans/$newPlanId/archive" -Method Post -Body (@{ reason = 'e2e archive' } | ConvertTo-Json) -ContentType 'application/json; charset=utf-8' -Headers $h | Out-Null
Write-Host "archived"

Write-Step "15. POST /therapist/plans/$newPlanId/versions (copy new draft)"
$ver = Invoke-RestMethod -Uri "$base/therapist/plans/$newPlanId/versions" -Method Post -Headers $h
Write-Host "new version planId=$($ver.data.planId) v$($ver.data.versionNo) status=$($ver.data.status) items=$($ver.data.items.Count)"
$copyId = $ver.data.planId

Write-Step "16. DELETE draft copy"
Invoke-RestMethod -Uri "$base/therapist/plans/$copyId" -Method Delete -Headers $h | Out-Null
Write-Host "deleted copy planId=$copyId"

Write-Step '17. NEGATIVE: unbound patient -> 403'
try {
  Invoke-RestMethod -Uri "$base/therapist/patients/999" -Headers $h | Out-Null
  Write-Host "UNEXPECTED 200"
} catch {
  Write-Host "got $($_.Exception.Response.StatusCode.value__) (expect 403)"
}

Write-Step '18. NEGATIVE: patient role calls /therapist/dashboard -> 403'
$pat = Login 'm3pat01' 'Rehab@123456'
$ph = @{ Authorization = "Bearer $($pat.data.accessToken)" }
try {
  Invoke-RestMethod -Uri "$base/therapist/dashboard" -Headers $ph | Out-Null
  Write-Host "UNEXPECTED 200"
} catch {
  Write-Host "got $($_.Exception.Response.StatusCode.value__) (expect 401/403)"
}

Write-Host "`n===== P6 API TEST DONE (cleanup: created plans archived/deleted; planId=$newPlanId archived) =====" -ForegroundColor Green
