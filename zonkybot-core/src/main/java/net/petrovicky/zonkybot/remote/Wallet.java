package net.petrovicky.zonkybot.remote;

import java.math.BigDecimal;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlTransient;

public class Wallet {

    private int id;
    private BigDecimal balance, availableBalance, blockedBalance;
    private int variableSymbol;
    @XmlTransient
    private Object account;

    public int getId() {
        return id;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("Wallet{");
        sb.append("id=").append(id);
        sb.append(", balance=").append(balance);
        sb.append(", availableBalance=").append(availableBalance);
        sb.append(", blockedBalance=").append(blockedBalance);
        sb.append('}');
        return sb.toString();
    }

    @XmlElement
    public BigDecimal getBalance() {
        return balance;
    }

    @XmlElement
    public BigDecimal getAvailableBalance() {
        return availableBalance;
    }

    @XmlElement
    public BigDecimal getBlockedBalance() {
        return blockedBalance;
    }

    @XmlElement
    public int getVariableSymbol() {
        return variableSymbol;
    }

    @XmlTransient
    public Object getAccount() {
        return account;
    }
}
