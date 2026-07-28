|Current result| Incident open?	|Threshold reached?	|New status	|Action|
|-|-|-|-|-|
Success | No	|X	|UP	|Reset failure count
Success	| Yes	|X	|UP	|Resolve incident and send recovery alert
Failure	| No	|No	|DEGRADED	|Increment failure count
Failure	| No	|Yes	|DOWN	|Create |incident and send outage alert
Failure	| Yes	|Yes	|DOWN	|Keep |existing incident open