<table>
  <caption>Dodatečné informace o půjčce</caption>
  <tbody>
    <tr>
      <th>Účel:</th>
      <td>${data.loanPurpose.getCode()?cap_first}</td>
    </tr>
    <tr>
      <th>Kraj:</th>
      <td>${data.loanRegion.getCode()?cap_first}</td>
    </tr>
    <tr>
      <th>Zdroj příjmů:</th>
      <td>${data.loanMainIncomeType.getCode()?cap_first}</td>
    </tr>
  </tbody>
</table>
