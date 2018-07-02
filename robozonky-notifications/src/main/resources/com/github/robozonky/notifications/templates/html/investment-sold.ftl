<p>Participace s následujícími parametry byla úspěšně prodána:</p>

<table>
  <caption><a href="${data.loanUrl}">#${data.loanId?c} ${data.loanName?cap_first}</a></caption>
  <tr>
    <th>Rating:</th>
    <td>${data.loanRating}</td>
  </tr>
  <tr>
    <th>Doba držení:</th>
    <td>${data.monthsElapsed?c} měsíců</td>
  </tr>
  <tr>
    <th>Zbývající jistina:</th>
    <td>${data.amountRemaining?string.currency}</td>
  </tr>
  <tr>
    <th>Dosažený výnos:</th>
    <td>${data.yield?string.currency}</td>
  </tr>
  <tr>
    <th>Záchranná vesta:</th>
    <td><#if data.insurance>Ano<#else>Ne</#if>.</td>
  </tr>
</table>

<#include "additional-loan-info.ftl">

<#include "additional-portfolio-info.ftl">
