<p>Rezervace půjčky <@idLoan data=data /> byla potvrzena.</p>

<table>
  <tr>
    <th style="text-align: right;">Investovaná částka:</th>
    <td>${data.amountHeld?string.currency}</td>
  </tr>
  <tr>
    <th style="text-align: right;">Dosažitelný výnos:</th>
    <td>${data.yield?string.currency} (${data.relativeYield?string.@interest} p. a.)</td>
  </tr>
</table>

<#include "additional-loan-info.ftl">

<#include "additional-portfolio-info.ftl">
