<p>Na sekundárním trhu byla zakoupena participace k půjčce <@idLoan data=data />.</p>

<table>
  <tr>
    <th style="text-align: right;">Zbývá splátek:</th>
    <td>${data.loanTermRemaining?c}</td>
  </tr>
  <tr>
    <th style="text-align: right;">Zbývající jistina:</th>
    <td>${data.amountHeld?string.currency}</td>
  </tr>
  <tr>
    <th style="text-align: right;">Dosažitelný výnos:</th>
    <td>${data.yield?string.currency} (${data.relativeYield?string.@interest} p. a.)</td>
  </tr>
</table>

<#include "additional-loan-info.ftl">

<#include "additional-portfolio-info.ftl">
