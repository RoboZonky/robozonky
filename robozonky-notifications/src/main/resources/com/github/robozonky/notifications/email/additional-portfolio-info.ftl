Nový zůstatek na Zonky účtu je ${data.portfolio.balance?string.currency}.

Aktuální struktura portfolia:
<#list data.ratings as rating>
<#assign code = rating.getCode()>
<#assign abs = data.portfolio.absoluteShare[code]>
<#assign rel = data.portfolio.relativeShare[code]>
<#assign absRisk = data.portfolio.absoluteRisk[code]>
<#assign relRisk = data.portfolio.relativeRisk[code]>
Rating ${code?right_pad(3)}: ${abs?string.currency?left_pad(13)}, ${rel?string.@interest?left_pad(4)} portfolia. (Ohroženo ${absRisk?string.currency?left_pad(12)}, ${relRisk?string.@interest?left_pad(4)}.)
</#list>
Celkem je v portfoliu ${data.portfolio.total?string.currency}, ${data.portfolio.totalRisk?string.currency} ohroženo.
