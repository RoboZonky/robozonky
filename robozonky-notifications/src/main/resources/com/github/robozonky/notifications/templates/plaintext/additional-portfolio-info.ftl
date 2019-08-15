Struktura portfolia k ${data.portfolio.timestamp?time?iso_local_ms_nz}:
<#list data.ratings as rating>
<#assign code = rating.getCode()>
<#assign abs = data.portfolio.absoluteShare[code]>
<#assign rel = data.portfolio.relativeShare[code]>
Úrok <@idRating id=code />: ${abs?string.currency?left_pad(13)}, ${rel?string.@interest?left_pad(4)} portfolia.
</#list>
Celkem je v portfoliu ${data.portfolio.total?string.currency}.
