<p>Na sekundárním trhu byla právě zakoupena následující participace:</p>

<table>
  <caption><a href="${data.loanUrl}">#${data.loanId?c} ${data.loanName?cap_first}</a></caption>
  <tr>
    <th>Rating:</th>
    <td>${data.loanRating}</td>
  </tr>
  <tr>
    <th>Zbývá splátek:</th>
    <td>${data.loanTermRemaining?c} z ${data.loanTerm?c}</td>
  </tr>
  <tr>
    <th>Zbývající jistina:</th>
    <td>${data.amountHeld?string.currency}</td>
  </tr>
  <tr>
    <th>Dosažitelný výnos:</th>
    <td>${data.yield?string.currency} (${data.relativeYield?string.@interest} p. a.)</td>
  </tr>
  <tr>
    <th>Záchranná vesta:</th>
    <td><#if data.insurance>Ano<#else>Ne</#if>.</td>
  </tr>
</table>

<#include "additional-loan-info.ftl">

<#include "additional-portfolio-info.ftl">
