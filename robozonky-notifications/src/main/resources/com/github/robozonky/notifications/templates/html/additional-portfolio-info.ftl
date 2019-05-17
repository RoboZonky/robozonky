<table style="width: 60%">
  <caption><h2>Struktura portfolia k ${data.portfolio.timestamp?time?iso_local_ms_nz}</h2></caption>
  <thead>
    <tr>
        <th style="padding: 5px; text-align: right;">Úrok</th>
        <th style="padding: 5px;" colspan="2">Investováno</th>
        <th style="padding: 5px;" colspan="2">Ohroženo</th>
        <th style="padding: 5px;" colspan="2">Prodatelné</th>
        <th style="padding: 5px;" colspan="2">Prodatelné bez poplatku</th>
    </tr>
  </thead>
  <tfoot>
    <tr>
      <th style="padding: 5px; text-align: right;">Celkem</th>
      <th colspan="2" style="padding: 5px; text-align: right;">${data.portfolio.total?string.currency}</th>
      <th style="padding: 5px; text-align: right;">${data.portfolio.totalRisk?string.currency}</th>
      <th style="padding: 5px; text-align: right;">(${data.portfolio.totalShare?string.@interest})</th>
      <th style="padding: 5px; text-align: right;">${data.portfolio.totalSellable?string.currency}</th>
      <th style="padding: 5px; text-align: right;">(${data.portfolio.totalSellableShare?string.@interest})</th>
      <th style="padding: 5px; text-align: right;">${data.portfolio.totalSellableFeeless?string.currency}</th>
      <th style="padding: 5px; text-align: right;">(${data.portfolio.totalSellableFeelessShare?string.@interest})</th>
    </tr>
    <tr>
      <th style="padding: 5px; text-align: right;">Disponibilní zůstatek</th>
      <td colspan="8" style="padding: 5px; text-align: right;">${data.portfolio.balance?string.currency}</td>
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
        <#assign absSellable = data.portfolio.absoluteSellable[code]>
        <#assign relSellable = data.portfolio.relativeSellable[code]>
        <#assign absSellableFeeless = data.portfolio.absoluteSellableFeeless[code]>
        <#assign relSellableFeeless = data.portfolio.relativeSellableFeeless[code]>
        <td style="padding: 5px; text-align: right;"><@idRating id=code /></td>
        <td style="padding: 5px; text-align: right;">${abs?string.currency}</td>
        <td style="padding: 5px; text-align: right;">(${rel?string.@interest})</td>
        <td style="padding: 5px; text-align: right;">${absRisk?string.currency}</td>
        <td style="padding: 5px; text-align: right;">(${relRisk?string.@interest})</td>
        <td style="padding: 5px; text-align: right;">${absSellable?string.currency}</td>
        <td style="padding: 5px; text-align: right;">(${relSellable?string.@interest})</td>
        <td style="padding: 5px; text-align: right;">${absSellableFeeless?string.currency}</td>
        <td style="padding: 5px; text-align: right;">(${relSellableFeeless?string.@interest})</td>
      </tr>
    </#list>
  </tbody>
</table>

<p><small>Pozn.: Jsou-li pole "Prodatelné" vynulovaná, robot se zrovna trefil do chvíle, kdy Zonky aktualizuje portfolio
a dočasně vypíná sekundár. Této situaci nelze zcela zabránit. V pozdějších notifikacích by již měla být čísla
správně.</small></p>
