<table style="width: 60%">
  <caption><h2>Struktura portfolia k ${data.portfolio.timestamp?time?iso_local_ms_nz}</h2></caption>
  <thead>
    <tr>
        <th style="padding: 5px; text-align: right;">Úrok</th>
        <th style="padding: 5px;" colspan="2">Investováno</th>
    </tr>
  </thead>
  <tfoot>
    <tr>
      <th style="padding: 5px; text-align: right;">Celkem</th>
      <th style="padding: 5px; text-align: right;">${data.portfolio.total?string.currency}</th>
      <th style="padding: 5px; text-align: right;">(${data.portfolio.totalShare?string.@interest})</th>
    </tr>
    <tr>
      <th style="padding: 5px; text-align: right;">Disponibilní zůstatek</th>
      <td colspan="2" style="padding: 5px; text-align: right;">${data.portfolio.balance?string.currency}</td>
    </tr>
  </tfoot>
  <tbody>
    <#list data.ratings as rating>
      <tr>
        <#assign code = rating.getCode()>
        <#assign abs = data.portfolio.absoluteShare[code]>
        <#assign rel = data.portfolio.relativeShare[code]>
        <td style="padding: 5px; text-align: right;"><@idRating id=code /></td>
        <td style="padding: 5px; text-align: right;">${abs?string.currency}</td>
        <td style="padding: 5px; text-align: right;">(${rel?string.@interest})</td>
      </tr>
    </#list>
  </tbody>
</table>
