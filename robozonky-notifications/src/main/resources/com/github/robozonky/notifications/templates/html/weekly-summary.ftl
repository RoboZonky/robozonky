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

<table style="width: 60%">
  <caption><h2>Peněžní toky</h2></caption>
  <thead>
    <tr>
        <th></th>
        <th style="padding: 5px; text-align: right;">K Vám</th>
        <th style="padding: 5px; text-align: right;">Od Vás</th>
        <th style="padding: 5px; text-align: right;">Výsledek</th>
    </tr>
  </thead>
  <tbody>
    <tr>
      <th style="padding: 5px; text-align: right;">Celkem</th>
      <th style="padding: 5px; text-align: right;">${data.inTotal?string.currency}</th>
      <th style="padding: 5px; text-align: right;">${data.outTotal?string.currency}</th>
      <th style="padding: 5px; text-align: right;">${data.total?string.currency}</th>
    </tr>
    <tr>
      <th style="padding: 5px; text-align: right;">Pouze převody</th>
      <th style="padding: 5px; text-align: right;">${data.inFromDeposits?string.currency}</th>
      <th style="padding: 5px; text-align: right;">${data.outFromWithdrawals?string.currency}</th>
      <th style="padding: 5px; text-align: right;">${data.totalDepositsAndWithdrawals?string.currency}</th>
    </tr>
    <tr>
      <th style="padding: 5px; text-align: right;">Pouze poplatky</th>
      <th style="padding: 5px; text-align: right;">0,00 Kč</th>
      <th style="padding: 5px; text-align: right;">${data.outFromFees?string.currency}</th>
      <th style="padding: 5px; text-align: right;"><#if data.outFromFees gt 0>-</#if>${data.outFromFees?string.currency}</th>
    </tr>
  </tbody>
</table>

<table style="width: 60%">
  <caption><h2>Změny v portfoliu</h2></caption>
  <thead>
    <tr>
        <th style="padding: 5px; width: 50%;">Nové investice (vč. nákupů)</th>
        <th style="padding: 5px; width: 50%;">Ukončené investice (vč. prodejů)</th>
    </tr>
  </thead>
  <tbody>
    <tr>
      <th>
        <#list data.incomingInvestments>
            <table style="width: 100%">
                <#items as item>
                    <tr>
                        <th style="padding: 1px; text-align: right;">${item.amountHeld?string.currency}</th>
                        <th style="padding: 1px; text-align: right;"><@idRating id=item.loanInterestRate /></th>
                        <th style="padding: 1px; text-align: right;">${item.loanTerm?c} m.</th>
                        <th style="padding: 1px; text-align: left;"><@idLoan data=item /></th>
                    </tr>
                </#items>
            </table>
        <#else>
            Žádné.
        </#list>
      </th>
      <th>
        <#list data.outgoingInvestments>
            <table style="width: 100%">
                <#items as item>
                    <tr>
                        <th style="padding: 1px; text-align: right;">${item.amountHeld?string.currency}</th>
                        <th style="padding: 1px; text-align: right;"><@idRating id=item.loanInterestRate /></th>
                        <th style="padding: 1px; text-align: right;">${item.loanTerm?c} m.</th>
                        <th style="padding: 1px; text-align: left;"><@idLoan data=item /></th>
                    </tr>
                </#items>
            </table>
        <#else>
            Žádné.
        </#list>
      </th>
    </tr>
  </tbody>
</table>
