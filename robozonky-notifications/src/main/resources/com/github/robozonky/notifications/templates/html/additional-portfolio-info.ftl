<p>Nový zůstatek na Zonky účtu je ${data.portfolio.balance?string.currency}.</p>

<table>
  <caption>Aktuální struktura portfolia</caption>
  <thead>
    <tr>
        <th>Rating</th>
        <th colspan="2">Investováno</th>
        <th colspan="2">Ohroženo</th>
    </tr>
  </thead>
  <tfoot>
    <th>Celkem</th>
    <th colspan="2">${data.portfolio.total?string.currency}</th>
    <th>${data.portfolio.totalRisk?string.currency}</th>
    <th>${data.portfolio.totalShare?string.@interest}</th>
  </tfoot>
  <tbody>
    <#list data.ratings as rating>
      <tr>
        <#assign code = rating.getCode()>
        <#assign abs = data.portfolio.absoluteShare[code]>
        <#assign rel = data.portfolio.relativeShare[code]>
        <#assign absRisk = data.portfolio.absoluteRisk[code]>
        <#assign relRisk = data.portfolio.relativeRisk[code]>
        <td>${code}</td>
        <td>${abs?string.currency}</td>
        <td>(${rel?string.@interest})</td>
        <td>${absRisk?string.currency}</td>
        <td>(${relRisk?string.@interest})</td>
      </tr>
    </#list>
  </tbody>
</table>

