<p>Participace v půjčce <@idLoan data=data /> byla prodána.</p>

<table>
  <tr>
    <th style="text-align: right;">Doba držení:</th>
    <td>${data.monthsElapsed?c} měsíců</td>
  </tr>
  <tr>
    <th style="text-align: right;">Zbývající jistina:</th>
    <td>${data.amountRemaining?string.currency}</td>
  </tr>
</table>

<#include "additional-loan-info.ftl">

<#include "additional-portfolio-info.ftl">
