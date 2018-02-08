package com.after_sunrise.cryptocurrency.cryptotrader.core;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Set;

/**
 * @author takanori.takase
 * @version 0.0.1
 */
public interface PropertyManager {

    Instant getNow();

    String getVersion();

    Duration getTradingInterval();

    Integer getTradingThreads();

    List<Composite> getTradingTargets();

    Boolean getTradingActive(String site, String instrument);

    Integer getTradingFrequency(String site, String instrument);

    Integer getTradingSeed(String site, String instrument);

    BigDecimal getTradingSpread(String site, String instrument);

    BigDecimal getTradingSpreadAsk(String site, String instrument);

    BigDecimal getTradingSpreadBid(String site, String instrument);

    BigDecimal getTradingSigma(String site, String instrument);

    Integer getTradingSamples(String site, String instrument);

    BigDecimal getTradingExposure(String site, String instrument);

    BigDecimal getTradingThreshold(String site, String instrument);

    BigDecimal getTradingMaximum(String site, String instrument);

    BigDecimal getTradingMinimum(String site, String instrument);

    BigDecimal getTradingResistance(String site, String instrument);

    BigDecimal getTradingAversion(String site, String instrument);

    String getTradingInstruction(String site, String instrument);

    Integer getTradingSplit(String site, String instrument);

    Duration getTradingDuration(String site, String instrument);

    BigDecimal getFundingOffset(String site, String instrument);

    List<Composite> getFundingMultiplierProducts(String site, String instrument);

    BigDecimal getFundingPositiveMultiplier(String site, String instrument);

    BigDecimal getFundingNegativeMultiplier(String site, String instrument);

    BigDecimal getFundingPositiveThreshold(String site, String instrument);

    BigDecimal getFundingNegativeThreshold(String site, String instrument);

    List<Composite> getDeviationProducts(String site, String instrument);

    List<Composite> getAversionProducts(String site, String instrument);

    List<Composite> getHedgeProducts(String site, String instrument);

    Set<String> getEstimators(String site, String instrument);

    List<Composite> getEstimatorComposites(String site, String instrument);

    BigDecimal getEstimationThreshold(String site, String instrument);

    BigDecimal getEstimationAversion(String site, String instrument);

}
