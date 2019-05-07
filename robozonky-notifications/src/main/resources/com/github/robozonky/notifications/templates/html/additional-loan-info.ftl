<table style="width: 60%;">
  <caption><h2>Parametry půjčky</h2></caption>
  <tbody>
    <tr>
      <th style="width: 20%; text-align: right;">Výše úvěru:</th>
      <td>${data.loanAmount?string.currency} s úrokem <@idRating id=data.loanInterestRate /></td>
    </tr>
    <tr>
      <th style="width: 20%; text-align: right;">Amortizace:</th>
      <td>${data.loanTerm?c} měsíců po ${data.loanAnnuity?string.currency}</td>
    </tr>
    <tr>
      <th style="width: 20%; text-align: right;">Účel:</th>
      <td>${data.loanPurpose.getCode()?cap_first}</td>
    </tr>
    <tr>
      <th style="width: 20%; text-align: right;">Klient:</th>
      <td>${data.loanMainIncomeType.getCode()?cap_first}, ${data.loanRegion.getRichCode()?cap_first}</td>
    </tr>
    <tr>
      <th style="width: 20%; text-align: right;">Pojištěno:</th>
      <td><#if data.insurance>Ano<#else>Ne</#if>.</td>
    </tr>
<#if data.investedOn??>
    <tr>
      <th style="width: 20%; text-align: right;">Zainvestováno:</th>
      <td>${data.investedOn?date}</td>
    </tr>
</#if>
  </tbody>
</table>
