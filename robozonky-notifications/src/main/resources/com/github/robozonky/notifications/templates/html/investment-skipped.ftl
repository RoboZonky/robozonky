<p>Půjčka s následujícími parametry byla přeskočena:</p>

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
    <th>Požadovaná částka:</th>
    <td>${data.loanAmount?string.currency}</td>
  </tr>
  <tr>
    <th>Navržená výše investice:</th>
    <td>${data.loanRecommendation?string.currency}</td>
  </tr>
  <tr>
    <th>Záchranná vesta:</th>
    <td><#if data.insurance>Ano<#else>Ne</#if>.</td>
  </tr>
</table>

<#include "additional-loan-info.ftl">
