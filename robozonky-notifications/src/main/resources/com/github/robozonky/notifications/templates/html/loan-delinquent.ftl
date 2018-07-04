<p>Půjčka <@idLoan data=data /> je v prodlení.</p>

<table>
  <tr>
    <th style="text-align: right;">Zbývá splátek:</th>
    <td>${data.loanTermRemaining?c} z ${data.loanTerm?c}</td>
  </tr>
  <tr>
    <th style="text-align: right;">Zbývající jistina:</th>
    <td>${data.amountRemaining?string.currency} z ${data.amountHeld?string.currency}</td>
  </tr>
  <tr>
    <th style="text-align: right;">Po splatnosti od:</th>
    <td>${data.since?date}</td>
  </tr>
</table>

<#include "additional-collections-info.ftl">

<#include "additional-loan-info.ftl">
