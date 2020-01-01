<table style="width: 60%">
  <caption><h2>Výkonnost portfolia</h2></caption>
  <thead>
    <tr>
        <th></th>
        <th style="padding: 5px; text-align: right;">Očekáváno</th>
        <th style="padding: 5px; text-align: right;">Udáváno <@zonky />*</th>
        <th style="padding: 5px; text-align: right;">Ideální</th>
    </tr>
  </thead>
  <tbody>
    <tr>
      <th style="padding: 5px; text-align: right;">Výnos</th>
      <th style="padding: 5px; text-align: right;">${data.portfolio.minimalProfitability?string.@interest} p.a.</th>
      <th style="padding: 5px; text-align: right;">${data.portfolio.profitability?string.@interest} p.a.</th>
      <th style="padding: 5px; text-align: right;">${data.portfolio.optimalProfitability?string.@interest} p.a.</th>
    </tr>
    <tr>
      <th style="padding: 5px; text-align: right;">Měsíčně</th>
      <th style="padding: 5px; text-align: right;">${data.portfolio.minimalMonthlyProfit?string.currency}</th>
      <th style="padding: 5px; text-align: right;">${data.portfolio.monthlyProfit?string.currency}</th>
      <th style="padding: 5px; text-align: right;">${data.portfolio.optimalMonthlyProfit?string.currency}</th>
    </tr>
  </tbody>
</table>

<p><small>* Pro relativně nové investory Zonky nepočítá výkonnost portfolia. Zde je tato situace reprezentována nulovou
hodnotou.</small></p>

<table style="width: 60%">
  <caption><h2>Struktura portfolia k ${data.portfolio.timestamp?time?iso_local_ms_nz}</h2></caption>
  <thead>
    <tr>
        <th style="padding: 5px; text-align: right;">&nbsp;</th>
        <th style="padding: 5px; text-align: center;" colspan="4">Stav portfolia</th>
        <th style="padding: 5px; text-align: center;" colspan="4">Odkupní hodnota</th>
    </tr>
    <tr>
        <th style="padding: 5px; text-align: right;">Úrok</th>
        <th style="padding: 5px;" colspan="2">Investováno</th>
        <th style="padding: 5px;" colspan="2">Ohroženo</th>
        <th style="padding: 5px;" colspan="2">Celková</th>
        <th style="padding: 5px;" colspan="2">Bez poplatků</th>
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
