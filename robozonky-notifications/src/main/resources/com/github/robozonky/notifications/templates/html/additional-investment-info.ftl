<table style="width: 60%;">
  <caption><h2>Informace o splácení</h2></caption>
  <tbody>
    <tr>
      <th style="width: 20%; text-align: right;">Bilance:</th>
      <td><strong>${data.balance?string.currency} po ${data.monthsElapsed} m.</strong></td>
    </tr>
    <tr>
      <th style="width: 20%; text-align: right;">Zaplacená jistina:</th>
      <td>${data.principalPaid?string.currency} z původních ${data.amountHeld?string.currency}</td>
    </tr>
    <tr>
      <th style="width: 20%; text-align: right;">Zaplacené úroky:</th>
      <td>${data.interestPaid?string.currency} z původních ${data.interestExpected?string.currency}</td>
    </tr>
    <tr>
      <th style="width: 20%; text-align: right;">Zaplacené pokuty:</th>
      <td>${data.penaltiesPaid?string.currency}</td>
    </tr>
  </tbody>
</table>
