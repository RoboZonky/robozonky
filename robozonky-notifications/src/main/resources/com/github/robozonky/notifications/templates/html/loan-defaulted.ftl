<p>Půjčka <@idLoan data=data /> byla zesplatněna.</p>

<table>
  <tr>
    <th style="text-align: right;">Zbývá splátek:</th>
    <td><div title="Počet splátek v prodlení + 1 závěrečná">${data.loanTermRemaining?c} z ${data.loanTerm?c}</div></td>
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
