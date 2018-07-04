<p>Půjčka <@idLoan data=data /> byla předána jinému nástroji.</p>

<table>
  <tr>
    <th style="text-align: right;">Navržená výše investice:</th>
    <td>${data.loanRecommendation?string.currency}</td>
  </tr>
  <tr>
    <th style="text-align: right;">Cílový nástroj:</th>
    <td>${data.confirmationProviderId}</td>
  </tr>
</table>

<#include "additional-loan-info.ftl">

