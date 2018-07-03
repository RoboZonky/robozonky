<p>Půjčka s následujícími parametry nově není v prodlení:</p>

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
    <td>${data.amountRemaining?string.currency} z ${data.amountHeld?string.currency}</td>
  </tr>
  <tr>
    <th>Záchranná vesta:</th>
    <td><#if data.insurance>Ano<#else>Ne</#if>.</td>
  </tr>
  <tr>
    <th>Odklad splácení:</th>
    <td><#if data.postponed>Ano<#else>Ne</#if>.</td>
  </tr>
</table>

<#include "additional-loan-info.ftl">

