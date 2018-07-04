<p>Půjčka <@idLoan data=data /> se uzdravila.</p>

<table>
  <tr>
    <th style="text-align: right;">Zbývá splátek:</th>
    <td>${data.loanTermRemaining?c} z ${data.loanTerm?c}</td>
  </tr>
  <tr>
    <th style="text-align: right;">Zbývající jistina:</th>
    <td>${data.amountRemaining?string.currency} z ${data.amountHeld?string.currency}</td>
  </tr>
</table>

<#include "additional-loan-info.ftl">

