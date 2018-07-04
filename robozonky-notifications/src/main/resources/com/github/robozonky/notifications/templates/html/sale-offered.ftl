<p>Participace v půjčce <@idLoan data=data /> byla vystavena na sekundární trh.</p>

<table>
  <tr>
    <th style="text-align: right;">Zbývá splátek:</th>
    <td>${data.loanTermRemaining?c}</td>
  </tr>
  <tr>
    <th style="text-align: right;">Zbývající jistina:</th>
    <td>${data.amountRemaining?string.currency}</td>
  </tr>
</table>

<#include "additional-loan-info.ftl">
