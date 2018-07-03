<p>Půjčka s následujícími parametry byla úspěšně zainvestována:</p>

<table>
  <caption><a href="${data.loanUrl}">#${data.loanId?c} ${data.loanName?cap_first}</a></caption>
  <tr>
    <th>Rating:</th>
    <td>${data.loanRating}</td>
  </tr>
  <tr>
    <th>Délka splácení:</th>
    <td>${data.loanTerm?c} měsíců</td>
  </tr>
  <tr>
    <th>Investovaná částka:</th>
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
