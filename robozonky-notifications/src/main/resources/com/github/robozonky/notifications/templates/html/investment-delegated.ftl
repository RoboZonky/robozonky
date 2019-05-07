<p>Půjčka <@idLoan data=data /> byla předána jinému nástroji.</p>

<table style="width: 60%;">
  <tr>
    <th style="width: 20%; text-align: right;">Navržená výše investice:</th>
    <td>${data.loanRecommendation?string.currency}</td>
  </tr>
  <tr>
    <th style="width: 20%; text-align: right;">Cílový nástroj:</th>
    <td>${data.confirmationProviderId}</td>
  </tr>
</table>

<#include "additional-loan-info.ftl">

