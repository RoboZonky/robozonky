Nový zůstatek na Zonky účtu je ${data.portfolio.balance?string.currency}.

Struktura portfolia k ${data.portfolio.timestamp?time?iso_local_ms_nz}:
<#list data.ratings as rating>
<#assign code = rating.getCode()>
<#assign abs = data.portfolio.absoluteShare[code]>
<#assign rel = data.portfolio.relativeShare[code]>
<#assign absRisk = data.portfolio.absoluteRisk[code]>
<#assign relRisk = data.portfolio.relativeRisk[code]>
Úrok <@idRating id=code />: ${abs?string.currency?left_pad(13)}, ${rel?string.@interest?left_pad(4)} portfolia. (Ohroženo ${absRisk?string.currency?left_pad(12)}, ${relRisk?string.@interest?left_pad(4)}.)
</#list>
Celkem je v portfoliu ${data.portfolio.total?string.currency}. (Ohroženo ${data.portfolio.totalRisk?string.currency}, ${data.portfolio.totalShare?string.@interest}.)
