<p>Půjčka s následujícími parametry byla odepsána:</p>

<table>
  <caption><a href="${data.loanUrl}">#${data.loanId?c} ${data.loanName?cap_first}</a></caption>
  <tr>
    <th>Rating:</th>
    <td>${data.loanRating}</td>
  </tr>
  <tr>
    <th>Ztraceno:</th>
    <td>${data.amountRemaining?string.currency} z ${data.amountHeld?string.currency}</td>
  </tr>
</table>

<#include "additional-loan-info.ftl">
