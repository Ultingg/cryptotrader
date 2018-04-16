package com.after_sunrise.cryptocurrency.cryptotrader.core;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.Set;

/**
 * @author takanori.takase
 * @version 0.0.1
 */
public interface PropertyController extends PropertyManager {

    void setTradingInterval(Duration value);

    void setTradingExtension(Integer value);

    void setTradingThreads(Integer value);

    void setTradingTargets(List<Composite> values);

    void setTradingActive(String site, String instrument, Boolean value);

    void setTradingFrequency(String site, String instrument, Integer value);

    void setTradingSeed(String site, String instrument, Integer value);

    void setTradingSpread(String site, String instrument, BigDecimal value);

    void setTradingSpreadAsk(String site, String instrument, BigDecimal value);

    void setTradingSpreadBid(String site, String instrument, BigDecimal value);

    void setTradingSigma(String site, String instrument, BigDecimal value);

    void setTradingSamples(String site, String instrument, Integer value);

    void setTradingExposure(String site, String instrument, BigDecimal value);

    void setTradingThreshold(String site, String instrument, BigDecimal value);

    void setTradingMaximum(String site, String instrument, BigDecimal value);

    void setTradingMinimum(String site, String instrument, BigDecimal value);

    void setTradingResistance(String site, String instrument, BigDecimal value);

    void setTradingAversion(String site, String instrument, BigDecimal value);

    void setTradingInstruction(String site, String instrument, String value);

    void setTradingSplit(String site, String instrument, Integer value);

    void setTradingDuration(String site, String instrument, Duration value);

    void setFundingOffset(String site, String instrument, BigDecimal value);

    void setFundingMultiplierProducts(String site, String instrument, List<Composite> values);

    void setFundingPositiveMultiplier(String site, String instrument, BigDecimal value);

    void setFundingNegativeMultiplier(String site, String instrument, BigDecimal value);

    void setFundingPositiveThreshold(String site, String instrument, BigDecimal value);

    void setFundingNegativeThreshold(String site, String instrument, BigDecimal value);

    void setDeviationProducts(String site, String instrument, List<Composite> values);

    void setAversionProducts(String site, String instrument, List<Composite> values);

    void setHedgeProducts(String site, String instrument, List<Composite> values);

    void setEstimators(String site, String instrument, Set<String> values);

    void setEstimatorComposites(String site, String instrument, List<Composite> values);

    void setEstimationThreshold(String site, String instrument, BigDecimal value);

    void setEstimationAversion(String site, String instrument, BigDecimal value);

}
