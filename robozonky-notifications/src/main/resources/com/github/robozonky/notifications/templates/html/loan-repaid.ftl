Půjčka s následujícími parametry byla zcela splacena:

<table>
  <caption><a href="${data.loanUrl}">#${data.loanId?c} ${data.loanName?cap_first}</a></caption>
  <tr>
    <th>Rating:</th>
    <td>${data.loanRating}</td>
  </tr>
  <tr>
    <th>Doba držení:</th>
    <td>${data.monthsElapsed?c} měsíců</td>
  </tr>
  <tr>
    <th>Zaplaceno:</th>
    <td>${data.amountPaid?string.currency} za půjčených ${data.amountHeld?string.currency}</td>
  </tr>
  <tr>
    <th>Dosažený výnos:</th>
    <td>${data.yield?string.currency}</td>
  </tr>
</table>

<#include "additional-loan-info.ftl">

<#include "additional-portfolio-info.ftl">
