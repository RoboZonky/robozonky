<table>
  <caption>Parametry půjčky</caption>
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
      <th style="text-align: right;">Pojištěno:</th>
      <td><#if data.insurance>Ano<#else>Ne</#if>.</td>
    </tr>
  </tbody>
</table>

<table>
  <caption>Informace o klientovi</caption>
  <tbody>
    <tr>
      <th style="text-align: right;">Kraj:</th>
      <td>${data.loanRegion.getCode()?cap_first}</td>
    </tr>
    <tr>
      <th style="text-align: right;">Zdroj příjmů:</th>
      <td>${data.loanMainIncomeType.getCode()?cap_first}</td>
    </tr>
  </tbody>
</table>
