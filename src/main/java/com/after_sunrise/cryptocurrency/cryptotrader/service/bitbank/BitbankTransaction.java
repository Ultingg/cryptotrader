package com.after_sunrise.cryptocurrency.cryptotrader.service.bitbank;

import cc.bitbank.entity.Transactions;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Trade;
import lombok.ToString;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * @author takanori.takase
 * @version 0.0.1
 */
@ToString
public class BitbankTransaction implements Trade {

    private final Transactions.Transaction delegate;

    public BitbankTransaction(Transactions.Transaction delegate) {
        this.delegate = delegate;
    }

    public String getId() {
        return String.valueOf(delegate.getTransactionId());
    }

    @Override
    public Instant getTimestamp() {
        return delegate == null || delegate.getExecutedAt() == null ? null
                : Instant.ofEpochMilli(delegate.getExecutedAt().getTime());
    }

    @Override
    public BigDecimal getPrice() {
        return delegate == null ? null : delegate.getPrice();
    }

    @Override
    public BigDecimal getSize() {
        return delegate == null ? null : delegate.getAmount();
    }

}
