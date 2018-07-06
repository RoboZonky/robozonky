<h2>Aktuální situace na <@zonky /> účtu</h2>
<table>
  <thead>
    <tr>
        <th style="text-align: right;">Rating</th>
        <th colspan="2">Investováno</th>
        <th colspan="2">Ohroženo</th>
    </tr>
  </thead>
  <tfoot>
    <tr>
      <th style="text-align: right;">Celkem</th>
      <th colspan="2" style="text-align: right;">${data.portfolio.total?string.currency}</th>
      <th style="text-align: right;">${data.portfolio.totalRisk?string.currency}</th>
      <th style="text-align: right;">(${data.portfolio.totalShare?string.@interest})</th>
    </tr>
    <tr>
      <th style="text-align: right;">Disponibilní zůstatek</th>
      <td colspan="4" style="text-align: right;">${data.portfolio.balance?string.currency}</td>
    </tr>
  </tfoot>
  <tbody>
    <#list data.ratings as rating>
      <tr>
        <#assign code = rating.getCode()>
        <#assign abs = data.portfolio.absoluteShare[code]>
        <#assign rel = data.portfolio.relativeShare[code]>
        <#assign absRisk = data.portfolio.absoluteRisk[code]>
        <#assign relRisk = data.portfolio.relativeRisk[code]>
        <td style="text-align: right;"><@idRating id=code /></td>
        <td style="text-align: right;">${abs?string.currency}</td>
        <td style="text-align: right;">(${rel?string.@interest})</td>
        <td style="text-align: right;">${absRisk?string.currency}</td>
        <td style="text-align: right;">(${relRisk?string.@interest})</td>
      </tr>
    </#list>
  </tbody>
</table>

