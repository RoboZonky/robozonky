<p>Půjčka <@idLoan data=data /> byla zcela splacena.</p>

<table>
  <tr>
    <th style="text-align: right;">Doba držení:</th>
    <td>${data.monthsElapsed?c} měsíců</td>
  </tr>
  <tr>
    <th style="text-align: right;">Zaplaceno:</th>
    <td>${data.amountPaid?string.currency} za půjčených ${data.amountHeld?string.currency}</td>
  </tr>
</table>

<#include "additional-loan-info.ftl">

<#include "additional-portfolio-info.ftl">
