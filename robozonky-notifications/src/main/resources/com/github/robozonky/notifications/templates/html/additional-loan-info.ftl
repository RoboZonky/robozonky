<h2>Parametry půjčky</h2>
<table>
  <tbody>
    <tr>
      <th style="text-align: right;">Rating:</th>
      <td><@idRating id=data.loanRating /></td>
    </tr>
    <tr>
      <th style="text-align: right;">Výše úvěru:</th>
      <td>${data.loanAmount?string.currency}</td>
    </tr>
    <tr>
      <th style="text-align: right;">Délka splácení:</th>
      <td>${data.loanTerm?c} měsíců</td>
    </tr>
    <tr>
      <th style="text-align: right;">Účel:</th>
      <td>${data.loanPurpose.getCode()?cap_first}</td>
    </tr>
    <tr>
      <th style="text-align: right;">Klient:</th>
      <td>${data.loanMainIncomeType.getCode()?cap_first}, ${data.loanRegion.getCode()?cap_first}</td>
    </tr>
    <tr>
      <th style="text-align: right;">Pojištěno:</th>
      <td><#if data.insurance>Ano<#else>Ne</#if>.</td>
    </tr>
  </tbody>
</table>
