package com.after_sunrise.cryptocurrency.cryptotrader.service.bitflyer;

import com.after_sunrise.cryptocurrency.bitflyer4j.Bitflyer4j;
import com.after_sunrise.cryptocurrency.bitflyer4j.entity.*;
import com.after_sunrise.cryptocurrency.bitflyer4j.service.AccountService;
import com.after_sunrise.cryptocurrency.bitflyer4j.service.MarketService;
import com.after_sunrise.cryptocurrency.bitflyer4j.service.OrderService;
import com.after_sunrise.cryptocurrency.bitflyer4j.service.RealtimeService;
import com.after_sunrise.cryptocurrency.cryptotrader.TestModule;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.*;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Context.Key;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Instruction.CancelInstruction;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Instruction.CreateInstruction;
import com.after_sunrise.cryptocurrency.cryptotrader.service.bitflyer.BitflyerService.ProductType;
import com.google.common.collect.Sets;
import org.apache.commons.configuration2.Configuration;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

import static com.after_sunrise.cryptocurrency.bitflyer4j.core.ConditionType.LIMIT;
import static com.after_sunrise.cryptocurrency.bitflyer4j.core.ConditionType.MARKET;
import static com.after_sunrise.cryptocurrency.bitflyer4j.core.SideType.BUY;
import static com.after_sunrise.cryptocurrency.bitflyer4j.core.SideType.SELL;
import static com.after_sunrise.cryptocurrency.cryptotrader.framework.Service.CurrencyType.*;
import static com.after_sunrise.cryptocurrency.cryptotrader.service.bitflyer.BitflyerService.AssetType.COLLATERAL;
import static com.after_sunrise.cryptocurrency.cryptotrader.service.bitflyer.BitflyerService.AssetType.FUTURE_BTC1W;
import static com.after_sunrise.cryptocurrency.cryptotrader.service.bitflyer.BitflyerService.ID;
import static com.after_sunrise.cryptocurrency.cryptotrader.service.bitflyer.BitflyerService.ProductType.*;
import static java.math.BigDecimal.valueOf;
import static java.math.BigDecimal.*;
import static java.math.RoundingMode.DOWN;
import static java.math.RoundingMode.UP;
import static java.util.Arrays.asList;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;
import static org.testng.Assert.*;

/**
 * @author takanori.takase
 * @version 0.0.1
 */
public class BitflyerContextTest {

    private BitflyerContext target;

    private TestModule module;

    private AccountService accountService;

    private MarketService marketService;

    private OrderService orderService;

    private RealtimeService realtimeService;

    @BeforeMethod
    public void setUp() throws Exception {

        module = new TestModule();
        accountService = module.getMock(AccountService.class);
        marketService = module.getMock(MarketService.class);
        orderService = module.getMock(OrderService.class);
        realtimeService = module.getMock(RealtimeService.class);

        when(module.getMock(Bitflyer4j.class).getAccountService()).thenReturn(accountService);
        when(module.getMock(Bitflyer4j.class).getMarketService()).thenReturn(marketService);
        when(module.getMock(Bitflyer4j.class).getOrderService()).thenReturn(orderService);
        when(module.getMock(Bitflyer4j.class).getRealtimeService()).thenReturn(realtimeService);

        target = new BitflyerContext(module.getMock(Bitflyer4j.class));
        target.setConfiguration(module.getMock(Configuration.class));
        target = spy(target);

    }

    @AfterMethod
    public void tearDown() throws Exception {
        target.close();
    }

    @Test(enabled = false)
    public void test() throws IOException {

        doCallRealMethod().when(target).request(any(), any(), any(), any());

        Key key = Key.builder().instrument("BTC_JPY").timestamp(Instant.now()).build();

        // Tick
        System.out.println("Ask : " + target.getBestAskPrice(key));
        System.out.println("Bid : " + target.getBestBidPrice(key));
        System.out.println("Mid : " + target.getMidPrice(key));
        System.out.println("Ltp : " + target.getLastPrice(key));

        // Trade
        System.out.println("TRD : " + target.listTrades(key, null));

        // Account
        System.out.println("IPS : " + target.getInstrumentPosition(key));
        System.out.println("FPS : " + target.getFundingPosition(key));

        // Reference
        System.out.println("COM : " + target.getCommissionRate(key));
        System.out.println("MGN : " + target.isMarginable(key));
        System.out.println("EXP : " + target.getExpiry(key));

        // Order Query
        System.out.println("ORD : " + target.fetchOrder(key));
        System.out.println("EXC : " + target.listExecutions(key));

    }

    @Test
    public void testClose() throws Exception {

        Context context = new BitflyerContext();

        context.close();

    }

    @Test
    public void testListener() {

        verify(realtimeService).addListener(any());

        target.onBoards(null, null);

        target.onTicks(null, null);

        target.onExecutions(null, null);

    }

    @Test
    public void testConvertProductAlias() {

        List<Product> products = new ArrayList<>();
        products.add(mock(Product.class));
        products.add(mock(Product.class));
        products.add(null);
        products.add(mock(Product.class));
        products.add(mock(Product.class));
        when(products.get(0).getProduct()).thenReturn("BTCJPY08JAN2017");
        when(products.get(1).getProduct()).thenReturn("BTCJPY14APR2017");
        when(products.get(3).getProduct()).thenReturn("BTCJPY08OCT2017");
        when(products.get(4).getProduct()).thenReturn(null);
        when(products.get(0).getAlias()).thenReturn(null);
        when(products.get(1).getAlias()).thenReturn("BTCJPY_MAT1WK");
        when(products.get(3).getAlias()).thenReturn("BTCJPY_MAT2WK");
        when(products.get(4).getAlias()).thenReturn("BTCJPY_MAT3WK");
        when(marketService.getProducts()).thenReturn(completedFuture(products)).thenReturn(completedFuture(null));

        Key.KeyBuilder b = Key.builder();
        assertEquals(target.convertProductAlias(b.instrument("BTCJPY08JAN2017").build()), "BTCJPY08JAN2017");
        assertEquals(target.convertProductAlias(b.instrument("BTCJPY14APR2017").build()), "BTCJPY14APR2017");
        assertEquals(target.convertProductAlias(b.instrument("BTCJPY08OCT2017").build()), "BTCJPY08OCT2017");
        assertEquals(target.convertProductAlias(b.instrument("BTCJPY_MAT1WK").build()), "BTCJPY14APR2017");
        assertEquals(target.convertProductAlias(b.instrument("BTCJPY_MAT2WK").build()), "BTCJPY08OCT2017");
        assertEquals(target.convertProductAlias(b.instrument("BTCJPY_MAT3WK").build()), null);
        assertEquals(target.convertProductAlias(b.instrument("TEST").build()), null);
        assertEquals(target.convertProductAlias(b.instrument(null).build()), null);
        assertEquals(target.convertProductAlias(null), null);
        verify(marketService, times(1)).getProducts();

        target.clear();
        assertEquals(target.convertProductAlias(b.instrument("BTCJPY08JAN2017").build()), null);
        assertEquals(target.convertProductAlias(b.instrument("BTCJPY14APR2017").build()), null);
        assertEquals(target.convertProductAlias(b.instrument("BTCJPY08OCT2017").build()), null);
        assertEquals(target.convertProductAlias(b.instrument("BTCJPY_MAT1WK").build()), null);
        assertEquals(target.convertProductAlias(b.instrument("BTCJPY_MAT2WK").build()), null);
        assertEquals(target.convertProductAlias(b.instrument("BTCJPY_MAT3WK").build()), null);
        assertEquals(target.convertProductAlias(b.instrument("TEST").build()), null);
        assertEquals(target.convertProductAlias(b.instrument(null).build()), null);
        assertEquals(target.convertProductAlias(null), null);
        verify(marketService, times(2)).getProducts();

    }

    @Test
    public void testGetBoard() throws Exception {

        Board board = mock(Board.class);
        ZonedDateTime now = ZonedDateTime.now();
        Key key = Key.from(Request.builder().instrument("i").currentTime(now.toInstant()).build());
        doReturn("a").when(target).convertProductAlias(key);
        when(marketService.getBoard(any())).thenReturn(completedFuture(board)).thenReturn(completedFuture(null));

        // Initial
        assertSame(target.getBoard(key).getDelegate(), board);
        verify(marketService, times(1)).getBoard(any());
        verify(realtimeService, times(1)).subscribeBoard(singletonList("a"));

        // Cached
        assertSame(target.getBoard(key).getDelegate(), board);
        verify(marketService, times(1)).getBoard(any());
        verify(realtimeService, times(1)).subscribeBoard(singletonList("a"));

        // Not found
        target.clear();
        assertSame(target.getBoard(key), null);
        verify(marketService, times(2)).getBoard(any());
        verify(realtimeService, times(1)).subscribeBoard(singletonList("a"));

        // Realtime found, but no time.
        doReturn(null).when(target).getNow();
        target.clear();
        target.onBoardsSnapshot("a", board);
        assertSame(target.getBoard(key), null);
        verify(marketService, times(3)).getBoard(any());
        verify(realtimeService, times(1)).subscribeBoard(singletonList("a"));

        // Realtime found, but old.
        doReturn(now.minusMinutes(10).toInstant()).when(target).getNow();
        target.clear();
        target.onBoardsSnapshot("a", board);
        assertSame(target.getBoard(key), null);
        verify(marketService, times(4)).getBoard(any());
        verify(realtimeService, times(1)).subscribeBoard(singletonList("a"));

        // Realtime found.
        doReturn(now.plusMinutes(10).toInstant()).when(target).getNow();
        target.clear();
        target.onBoardsSnapshot("a", board);
        assertSame(target.getBoard(key).getDelegate(), board);
        verify(marketService, times(4)).getBoard(any());
        verify(realtimeService, times(1)).subscribeBoard(singletonList("a"));

    }

    @Test
    public void testGetTick() throws Exception {

        Tick tick = mock(Tick.class);
        ZonedDateTime now = ZonedDateTime.now();
        Key key = Key.from(Request.builder().instrument("i").currentTime(now.toInstant()).build());
        doReturn("a").when(target).convertProductAlias(key);
        when(marketService.getTick(any())).thenReturn(completedFuture(tick)).thenReturn(completedFuture(null));

        // Initial
        assertSame(target.getTick(key), tick);
        verify(marketService, times(1)).getTick(any());
        verify(realtimeService, times(1)).subscribeTick(singletonList("a"));

        // Cached
        assertSame(target.getTick(key), tick);
        verify(marketService, times(1)).getTick(any());
        verify(realtimeService, times(1)).subscribeTick(singletonList("a"));

        // Not found
        target.clear();
        assertSame(target.getTick(key), null);
        verify(marketService, times(2)).getTick(any());
        verify(realtimeService, times(1)).subscribeTick(singletonList("a"));

        // Realtime found, but no time.
        target.clear();
        target.onTicks("a", singletonList(tick));
        assertSame(target.getTick(key), null);
        verify(marketService, times(3)).getTick(any());
        verify(realtimeService, times(1)).subscribeTick(singletonList("a"));

        // Realtime found, but old.
        target.clear();
        when(tick.getTimestamp()).thenReturn(now.minusMinutes(10));
        assertSame(target.getTick(key), null);
        verify(marketService, times(4)).getTick(any());
        verify(realtimeService, times(1)).subscribeTick(singletonList("a"));

        // Realtime found.
        target.clear();
        when(tick.getTimestamp()).thenReturn(now.plusMinutes(10));
        assertSame(target.getTick(key), tick);
        verify(marketService, times(4)).getTick(any());
        verify(realtimeService, times(1)).subscribeTick(singletonList("a"));

    }

    @Test
    public void testGetBesAskPrice() throws Exception {

        Key key = Key.from(Request.builder().build());
        Tick tick = mock(Tick.class);
        when(tick.getBestAskPrice()).thenReturn(ONE);

        doReturn(tick).when(target).getTick(key);
        assertEquals(target.getBestAskPrice(key), ONE);

        doReturn(null).when(target).getTick(key);
        assertEquals(target.getBestAskPrice(key), null);

    }

    @Test
    public void testGetBesBidPrice() throws Exception {

        Key key = Key.from(Request.builder().build());
        Tick tick = mock(Tick.class);
        when(tick.getBestBidPrice()).thenReturn(ONE);

        doReturn(tick).when(target).getTick(key);
        assertEquals(target.getBestBidPrice(key), ONE);

        doReturn(null).when(target).getTick(key);
        assertEquals(target.getBestBidPrice(key), null);

    }

    @Test
    public void testGetBesAskSize() throws Exception {

        Key key = Key.from(Request.builder().build());
        Tick tick = mock(Tick.class);
        when(tick.getBestAskSize()).thenReturn(ONE);

        doReturn(tick).when(target).getTick(key);
        assertEquals(target.getBestAskSize(key), ONE);

        doReturn(null).when(target).getTick(key);
        assertEquals(target.getBestAskSize(key), null);

    }

    @Test
    public void testGetBesBidSize() throws Exception {

        Key key = Key.from(Request.builder().build());
        Tick tick = mock(Tick.class);
        when(tick.getBestBidSize()).thenReturn(ONE);

        doReturn(tick).when(target).getTick(key);
        assertEquals(target.getBestBidSize(key), ONE);

        doReturn(null).when(target).getTick(key);
        assertEquals(target.getBestBidSize(key), null);

    }

    @Test
    public void testGetLastPrice() throws Exception {

        Key key = Key.from(Request.builder().build());
        Tick tick = mock(Tick.class);
        when(tick.getTradePrice()).thenReturn(ONE);

        doReturn(tick).when(target).getTick(key);
        assertEquals(target.getLastPrice(key), ONE);

        doReturn(null).when(target).getTick(key);
        assertEquals(target.getLastPrice(key), null);

    }

    @Test
    public void testListTrades() {

        ZonedDateTime time = ZonedDateTime.now();
        doReturn(time.toInstant()).when(target).getNow();
        doReturn("id").when(target).convertProductAlias(any());

        List<Execution> execs = new ArrayList<>();

        for (int i = 0; i < 10; i++) {
            Execution exec = mock(Execution.class);
            when(exec.getTimestamp()).thenReturn(time.plusSeconds(i));
            when(exec.getPrice()).thenReturn(BigDecimal.valueOf(100 + i));
            when(exec.getSize()).thenReturn(BigDecimal.valueOf(1000 + i));
            execs.add(exec);
        }

        // Skip
        when(execs.get(0).getTimestamp()).thenReturn(null);
        when(execs.get(4).getTimestamp()).thenReturn(null);
        when(execs.get(7).getPrice()).thenReturn(null);
        when(execs.get(8).getSize()).thenReturn(null);

        // Randomize response
        List<Execution> shuffled = new ArrayList<>(execs);
        Collections.shuffle(shuffled);
        when(marketService.getExecutions(any())).thenReturn(completedFuture(shuffled)).thenReturn(null);

        // All
        Key key = Key.from(Request.builder().instrument("inst").build());
        List<Trade> results = target.listTrades(key, null);
        assertEquals(results.size(), 6);
        verify(marketService, times(2)).getExecutions(any());
        verify(realtimeService).subscribeExecution(singletonList("id"));

        // Filtered by time (cached)
        List<Trade> filtered = target.listTrades(key, time.toInstant().plusSeconds(2));
        assertEquals(filtered.size(), 5);
        verify(marketService, times(2)).getExecutions(any());
        verify(realtimeService, times(1)).subscribeExecution(any());

    }

    @Test
    public void testListTrades_Empty() {

        doReturn("id").when(target).convertProductAlias(any());
        when(marketService.getExecutions(any())).thenReturn(null);

        // All
        Key key = Key.from(Request.builder().instrument("inst").build());
        List<Trade> results = target.listTrades(key, null);
        assertEquals(results.size(), 0);
        verify(marketService, times(1)).getExecutions(any());
        verify(realtimeService).subscribeExecution(singletonList("id"));

        // Cached
        List<Trade> filtered = target.listTrades(key, null);
        assertEquals(filtered.size(), 0);
        verify(marketService, times(1)).getExecutions(any());
        verify(realtimeService, times(1)).subscribeExecution(any());

    }

    @Test
    public void testGetInstrumentCurrency() {

        for (ProductType type : ProductType.values()) {

            Key key = Key.builder().instrument(type.name()).build();

            assertEquals(target.getInstrumentCurrency(key), type.getStructure().getCurrency());

        }

        assertNull(target.getInstrumentCurrency(Key.builder().build()));

        assertNull(target.getInstrumentCurrency(null));

    }

    @Test
    public void testGetFundingCurrency() {

        for (ProductType type : ProductType.values()) {

            Key key = Key.builder().instrument(type.name()).build();

            assertEquals(target.getFundingCurrency(key), type.getFunding().getCurrency());

        }

        assertNull(target.getFundingCurrency(Key.builder().build()));

        assertNull(target.getFundingCurrency(null));

    }

    @Test
    public void testFindProduct() {
        assertEquals(target.findProduct(null, BTC, JPY), "BTC_JPY");
        assertEquals(target.findProduct(null, ETH, BTC), "ETH_BTC");
        assertEquals(target.findProduct(null, BCH, BTC), "BCH_BTC");
        assertEquals(target.findProduct(null, JPY, BTC), null);
    }

    @Test
    public void testGetConversionPrice() {

        doReturn(null).when(target).getMidPrice(any());
        doReturn(valueOf(840000)).when(target).getMidPrice(Key.builder().instrument(BTC_JPY.name()).build());
        doReturn(valueOf(830000)).when(target).getMidPrice(Key.builder().instrument(FX_BTC_JPY.name()).build());
        doReturn(valueOf(820000)).when(target).getMidPrice(Key.builder().instrument(BTCJPY_MAT1WK.name()).build());
        doReturn(valueOf(810000)).when(target).getMidPrice(Key.builder().instrument(BTCJPY_MAT2WK.name()).build());
        doReturn(valueOf(4, 1)).when(target).getMidPrice(Key.builder().instrument(ETH_BTC.name()).build());
        doReturn(valueOf(6, 1)).when(target).getMidPrice(Key.builder().instrument(BCH_BTC.name()).build());
        doReturn(new BigDecimal("0.75")).when(module.getMock(Configuration.class)).getBigDecimal(
                "com.after_sunrise.cryptocurrency.cryptotrader.service.bitflyer.BitflyerContext.conversion.ratio",
                ZERO
        );

        for (Service.CurrencyType currency : Service.CurrencyType.values()) {

            for (ProductType product : ProductType.values()) {

                Key key = Key.builder().instrument(product.name()).build();

                BigDecimal expect = null;

                switch (currency) {
                    case JPY:
                        switch (product) {
                            case CASH_JPY:
                            case COLLATERAL_JPY:
                                expect = ONE;
                                break;
                            case BTC_JPY:
                            case FX_BTC_JPY:
                            case BTCJPY_MAT1WK:
                            case BTCJPY_MAT2WK:
                            case BTCJPY_MAT3M:
                            case COLLATERAL_BTC:
                                // First one found = 1 / 840000
                                expect = new BigDecimal("-0.0000011905");
                                break;
                        }
                        break;
                    case BTC:
                        switch (product) {
                            case BTC_JPY:
                            case BTCJPY_MAT1WK:
                            case BTCJPY_MAT2WK:
                            case BTCJPY_MAT3M:
                            case COLLATERAL_BTC:
                                expect = ONE;
                                break;
                            case CASH_JPY:
                            case COLLATERAL_JPY:
                                expect = valueOf(-840000);
                                break;
                            case ETH_BTC:
                                expect = new BigDecimal("-2.5000000000");
                                break;
                            case BCH_BTC:
                                expect = new BigDecimal("-1.6666666667");
                                break;
                            case FX_BTC_JPY:
                                expect = new BigDecimal("1.0030120482");
                                break;
                        }
                        break;
                    case ETH:
                        switch (product) {
                            case ETH_BTC:
                                expect = ONE;
                                break;
                            case BTC_JPY:
                            case FX_BTC_JPY:
                            case BTCJPY_MAT1WK:
                            case BTCJPY_MAT2WK:
                            case BTCJPY_MAT3M:
                            case COLLATERAL_BTC:
                                expect = new BigDecimal("-0.4");
                                break;
                        }
                        break;
                    case BCH:
                        switch (product) {
                            case BCH_BTC:
                                expect = ONE;
                                break;
                            case BTC_JPY:
                            case FX_BTC_JPY:
                            case BTCJPY_MAT1WK:
                            case BTCJPY_MAT2WK:
                            case BTCJPY_MAT3M:
                            case COLLATERAL_BTC:
                                expect = new BigDecimal("-0.6");
                                break;
                        }
                        break;
                }

                String message = String.format("%s -> %s", currency, product);
                assertEquals(target.getConversionPrice(key, currency), expect, message);

                // Null Currency
                assertNull(target.getConversionPrice(key, null));

            }

            // Unknown Product
            Key key = Key.builder().site(ID).instrument("foo").build();
            assertNull(target.getConversionPrice(key, currency));

        }

        // Zero Price
        doReturn(new BigDecimal("0.0")).when(target).getMidPrice(any());

        for (Service.CurrencyType currency : Service.CurrencyType.values()) {

            for (ProductType product : ProductType.values()) {

                Key key = Key.builder().instrument(product.name()).build();

                BigDecimal expect = null;

                switch (currency) {
                    case JPY:
                        switch (product) {
                            case CASH_JPY:
                            case COLLATERAL_JPY:
                                expect = ONE;
                                break;
                        }
                        break;
                    case BTC:
                        switch (product) {
                            case BTC_JPY:
                            case BTCJPY_MAT1WK:
                            case BTCJPY_MAT2WK:
                            case BTCJPY_MAT3M:
                            case COLLATERAL_BTC:
                                expect = ONE;
                                break;
                            case FX_BTC_JPY:
                                expect = null;
                                break;
                        }
                        break;
                    case ETH:
                        switch (product) {
                            case ETH_BTC:
                                expect = ONE;
                                break;
                        }
                        break;
                    case BCH:
                        switch (product) {
                            case BCH_BTC:
                                expect = ONE;
                                break;
                        }
                        break;
                }

                String message = String.format("%s -> %s", currency, product);
                assertEquals(target.getConversionPrice(key, currency), expect, message);

            }

        }

    }

    @Test
    public void testGetInstrumentPosition() throws Exception {

        Key key = Key.from(Request.builder().instrument("BTC_JPY").build());

        CompletableFuture<List<Balance>> f1 = completedFuture(singletonList(mock(Balance.class)));
        CompletableFuture<List<Balance>> f2 = completedFuture(singletonList(mock(Balance.class)));
        when(f1.get().get(0).getCurrency()).thenReturn("BTC");
        when(f2.get().get(0).getCurrency()).thenReturn("BTC");
        when(f1.get().get(0).getAmount()).thenReturn(ONE);
        when(f2.get().get(0).getAmount()).thenReturn(TEN);
        when(accountService.getBalances()).thenReturn(f1, f2);

        assertEquals(target.getInstrumentPosition(key), ONE);
        assertEquals(target.getInstrumentPosition(key), ONE);
        target.clear();
        assertEquals(target.getInstrumentPosition(key), TEN);
        assertEquals(target.getInstrumentPosition(key), TEN);

    }

    @Test
    public void testGetInstrumentPosition_Margin() throws Exception {

        Key key = Key.from(Request.builder().instrument("BTCJPY_MAT1WK").build());

        BigDecimal margin = ONE.add(ONE);

        doAnswer(i -> {

            assertSame(i.getArgumentAt(0, Key.class), key);

            assertSame(i.getArgumentAt(1, Function.class).apply(BTCJPY_MAT1WK), FUTURE_BTC1W);

            return margin;

        }).when(target).forMargin(any(), any());

        assertEquals(target.getInstrumentPosition(key), margin);

    }

    @Test
    public void testGetFundingPosition() throws Exception {

        Key key = Key.from(Request.builder().instrument("BTC_JPY").build());

        CompletableFuture<List<Balance>> f1 = completedFuture(singletonList(mock(Balance.class)));
        CompletableFuture<List<Balance>> f2 = completedFuture(singletonList(mock(Balance.class)));
        when(f1.get().get(0).getCurrency()).thenReturn("JPY");
        when(f2.get().get(0).getCurrency()).thenReturn("JPY");
        when(f1.get().get(0).getAmount()).thenReturn(ONE);
        when(f2.get().get(0).getAmount()).thenReturn(TEN);
        when(accountService.getBalances()).thenReturn(f1, f2);

        assertEquals(target.getFundingPosition(key), ONE);
        assertEquals(target.getFundingPosition(key), ONE);
        target.clear();
        assertEquals(target.getFundingPosition(key), TEN);
        assertEquals(target.getFundingPosition(key), TEN);

    }

    @Test
    public void testGetFundingPosition_Margin() throws Exception {

        Key key = Key.from(Request.builder().instrument("BTCJPY_MAT1WK").build());

        BigDecimal margin = ONE.add(ONE);

        doAnswer(i -> {

            assertSame(i.getArgumentAt(0, Key.class), key);

            assertSame(i.getArgumentAt(1, Function.class).apply(BTCJPY_MAT1WK), COLLATERAL);

            return margin;

        }).when(target).forMargin(any(), any());

        assertEquals(target.getFundingPosition(key), margin);

    }

    @Test
    public void testForBalance() throws Exception {

        Balance b1 = null;
        Balance b2 = mock(Balance.class);
        Balance b3 = mock(Balance.class);

        when(b2.getCurrency()).thenReturn("BTC");
        when(b2.getAmount()).thenReturn(TEN);

        CompletableFuture<List<Balance>> f1 = completedFuture(asList(b1, null, b2));
        CompletableFuture<List<Balance>> f2 = completedFuture(asList(b2, null, b3));
        CompletableFuture<List<Balance>> f3 = completedFuture(null);
        when(accountService.getBalances()).thenReturn(f1).thenReturn(f2).thenReturn(f3);

        // Null key
        Key key = null;
        assertNull(target.forBalance(key, ProductType::getStructure));
        verifyNoMoreInteractions(accountService);

        // No instrument
        key = Key.from(Request.builder().build());
        assertNull(target.forBalance(key, ProductType::getStructure));
        verifyNoMoreInteractions(accountService);

        // Unknown instrument
        key = Key.from(Request.builder().instrument("hoge").build());
        assertNull(target.forBalance(key, ProductType::getStructure));
        verifyNoMoreInteractions(accountService);

        // Found
        key = Key.from(Request.builder().instrument("BTC_JPY").build());
        assertEquals(target.forBalance(key, ProductType::getStructure), TEN);
        assertEquals(target.forBalance(key, ProductType::getStructure), TEN);
        verify(accountService, times(1)).getBalances();

        // Not Found
        key = Key.from(Request.builder().instrument("ETH_BTC").build());
        assertEquals(target.forBalance(key, ProductType::getStructure), null);
        assertEquals(target.forBalance(key, ProductType::getStructure), null);
        verify(accountService, times(1)).getBalances();

        // Next query
        target.clear();
        key = Key.from(Request.builder().instrument("BTC_JPY").build());
        assertEquals(target.forBalance(key, ProductType::getStructure), TEN);
        assertEquals(target.forBalance(key, ProductType::getStructure), TEN);
        verify(accountService, times(2)).getBalances();

        // Null result
        target.clear();
        key = Key.from(Request.builder().instrument("BTC_JPY").build());
        assertNull(target.forBalance(key, ProductType::getStructure));
        assertNull(target.forBalance(key, ProductType::getStructure));
        verify(accountService, times(3)).getBalances();

    }

    @Test
    public void testForMargin_Collateral() {

        Collateral c = mock(Collateral.class);
        when(accountService.getCollateral()).thenReturn(completedFuture(c));

        Key key1 = Key.from(Request.builder().instrument("BTCJPY_MAT1WK").build());
        Key key2 = Key.from(Request.builder().instrument("BTC_JPY").build());
        Key key3 = Key.from(Request.builder().instrument("HOGE").build());
        Key key4 = null;

        when(c.getCollateral()).thenReturn(null);
        when(c.getRequiredCollateral()).thenReturn(ONE);
        assertEquals(target.forMargin(key1, ProductType::getFunding), null);
        assertEquals(target.forMargin(key2, ProductType::getFunding), null);
        assertEquals(target.forMargin(key3, ProductType::getFunding), null);
        assertEquals(target.forMargin(key4, ProductType::getFunding), null);
        verify(accountService, times(1)).getCollateral();

        when(c.getCollateral()).thenReturn(TEN);
        when(c.getRequiredCollateral()).thenReturn(null);
        assertEquals(target.forMargin(key1, ProductType::getFunding), null);
        assertEquals(target.forMargin(key2, ProductType::getFunding), null);
        assertEquals(target.forMargin(key3, ProductType::getFunding), null);
        assertEquals(target.forMargin(key4, ProductType::getFunding), null);
        verify(accountService, times(1)).getCollateral();

        when(c.getCollateral()).thenReturn(TEN);
        when(c.getRequiredCollateral()).thenReturn(ONE);
        assertEquals(target.forMargin(key1, ProductType::getFunding), TEN.subtract(ONE));
        assertEquals(target.forMargin(key2, ProductType::getFunding), null);
        assertEquals(target.forMargin(key3, ProductType::getFunding), null);
        assertEquals(target.forMargin(key4, ProductType::getFunding), null);
        verify(accountService, times(1)).getCollateral();

        // Cached
        when(accountService.getCollateral()).thenReturn(null);
        assertEquals(target.forMargin(key1, ProductType::getFunding), TEN.subtract(ONE));
        assertEquals(target.forMargin(key2, ProductType::getFunding), null);
        assertEquals(target.forMargin(key3, ProductType::getFunding), null);
        assertEquals(target.forMargin(key4, ProductType::getFunding), null);
        verify(accountService, times(1)).getCollateral();

        // Not found
        target.clear();
        assertEquals(target.forMargin(key1, ProductType::getFunding), null);
        assertEquals(target.forMargin(key2, ProductType::getFunding), null);
        assertEquals(target.forMargin(key3, ProductType::getFunding), null);
        assertEquals(target.forMargin(key4, ProductType::getFunding), null);
        verify(accountService, times(2)).getCollateral();

    }

    @Test
    public void testForMargin_Margin() {

        Margin m1 = mock(Margin.class);
        Margin m2 = mock(Margin.class);
        when(m1.getCurrency()).thenReturn("JPY");
        when(m2.getCurrency()).thenReturn("BTC");
        when(m1.getAmount()).thenReturn(new BigDecimal("12345"));
        when(m2.getAmount()).thenReturn(new BigDecimal("2.5"));
        when(accountService.getMargins())
                .thenReturn(completedFuture(asList(m1, null)))
                .thenReturn(completedFuture(asList(null, m2)))
                .thenReturn(completedFuture(asList(m1, null, m2)))
                .thenReturn(completedFuture(null));

        Key key1 = Key.from(Request.builder().instrument("COLLATERAL_JPY").build());
        Key key2 = Key.from(Request.builder().instrument("COLLATERAL_BTC").build());

        // Only JPY
        target.clear();
        assertEquals(target.forMargin(key1, ProductType::getStructure), new BigDecimal("12345"));
        assertEquals(target.forMargin(key2, ProductType::getStructure), null);
        verify(accountService, times(1)).getMargins();

        // Only BTC
        target.clear();
        assertEquals(target.forMargin(key1, ProductType::getStructure), null);
        assertEquals(target.forMargin(key2, ProductType::getStructure), new BigDecimal("2.5"));
        verify(accountService, times(2)).getMargins();

        // Both
        target.clear();
        assertEquals(target.forMargin(key1, ProductType::getStructure), new BigDecimal("12345"));
        assertEquals(target.forMargin(key2, ProductType::getStructure), new BigDecimal("2.5"));
        verify(accountService, times(3)).getMargins();

        // Null
        target.clear();
        assertEquals(target.forMargin(key1, ProductType::getStructure), null);
        assertEquals(target.forMargin(key2, ProductType::getStructure), null);
        verify(accountService, times(4)).getMargins();

    }

    @Test
    public void testForMargin_Position() {

        Product i1 = mock(Product.class);
        Product i2 = mock(Product.class);
        when(i1.getProduct()).thenReturn("BTC_JPY");
        when(i2.getProduct()).thenReturn("BTCJPY14APR2017");
        when(i2.getAlias()).thenReturn("BTCJPY_MAT1WK");
        when(marketService.getProducts()).thenReturn(completedFuture(asList(i1, null, i2)));

        TradePosition p1 = mock(TradePosition.class);
        TradePosition p2 = mock(TradePosition.class);
        TradePosition p3 = mock(TradePosition.class);
        TradePosition p4 = mock(TradePosition.class);
        TradePosition p5 = mock(TradePosition.class);
        TradePosition p6 = mock(TradePosition.class);
        when(p2.getSide()).thenReturn(BUY); // No price
        when(p3.getSide()).thenReturn(BUY);
        when(p4.getSide()).thenReturn(SELL);
        when(p5.getSide()).thenReturn(SELL);
        when(p3.getSize()).thenReturn(TEN); // +10
        when(p4.getSize()).thenReturn(ONE); // -1
        when(p5.getSize()).thenReturn(ONE); // -1
        when(p6.getSize()).thenReturn(ONE); // No side
        when(orderService.listPositions(any())).thenAnswer(i -> {

            TradePosition.Request request = i.getArgumentAt(0, TradePosition.Request.class);

            // Should be converted from "BTCJPY_MAT1WK" to "BTCJPY14APR2017"
            if ("BTCJPY14APR2017".equals(request.getProduct())) {
                return completedFuture(asList(p1, p2, p3, null, p4, p5, p6));
            }

            return completedFuture(null);

        }).thenReturn(completedFuture(null));

        Key key1 = Key.from(Request.builder().instrument("BTCJPY_MAT1WK").build());
        Key key2 = Key.from(Request.builder().instrument("BTCJPY_MAT2WK").build());

        // Found
        assertEquals(target.forMargin(key1, ProductType::getStructure), TEN.subtract(ONE).subtract(ONE));
        assertEquals(target.forMargin(key2, ProductType::getStructure), ZERO);
        verify(orderService, times(2)).listPositions(any());

        // Cached
        assertEquals(target.forMargin(key1, ProductType::getStructure), TEN.subtract(ONE).subtract(ONE));
        assertEquals(target.forMargin(key2, ProductType::getStructure), ZERO);
        verify(orderService, times(2)).listPositions(any());

        // No data
        target.clear();
        assertEquals(target.forMargin(key1, ProductType::getStructure), ZERO);
        assertEquals(target.forMargin(key2, ProductType::getStructure), ZERO);
        verify(orderService, times(4)).listPositions(any());

    }

    @Test
    public void testRoundLotSize() throws Exception {

        Key key = Key.from(Request.builder().instrument("BTC_JPY").build());
        BigDecimal input = new BigDecimal("0.1234");

        assertEquals(target.roundLotSize(key, input, DOWN), new BigDecimal("0.123"));
        assertEquals(target.roundLotSize(key, input, UP), new BigDecimal("0.124"));
        assertNull(target.roundLotSize(null, input, UP));
        assertNull(target.roundLotSize(key, null, UP));
        assertNull(target.roundLotSize(key, input, null));
        assertNull(target.roundLotSize(Key.from(Request.builder().instrument("TEST").build()), input, UP));

    }

    @Test
    public void testRoundTickSize() throws Exception {

        Key key = Key.from(Request.builder().instrument("BTC_JPY").build());
        BigDecimal input = new BigDecimal("123.45");

        assertEquals(target.roundTickSize(key, input, DOWN), new BigDecimal("123"));
        assertEquals(target.roundTickSize(key, input, UP), new BigDecimal("124"));
        assertNull(target.roundTickSize(null, input, UP));
        assertNull(target.roundTickSize(key, null, UP));
        assertNull(target.roundTickSize(key, input, null));
        assertNull(target.roundTickSize(Key.from(Request.builder().instrument("TEST").build()), input, UP));

    }

    @Test
    public void testGetCommissionRate() throws Exception {

        Key key = Key.from(Request.builder().instrument("i").build());

        CompletableFuture<TradeCommission> f = completedFuture(mock(TradeCommission.class));
        when(f.get().getRate()).thenReturn(ONE.movePointLeft(3));
        when(orderService.getCommission(any())).thenReturn(f).thenReturn(null);

        assertEquals(target.getCommissionRate(key), ONE.movePointLeft(3));
        assertEquals(target.getCommissionRate(key), ONE.movePointLeft(3));
        target.clear();
        assertEquals(target.getCommissionRate(key), null);
        assertEquals(target.getBestAskPrice(key), null);

    }

    @Test
    public void testIsMarginable() {

        Key.KeyBuilder builder = Key.builder();

        assertFalse(target.isMarginable(builder.build()));
        assertFalse(target.isMarginable(builder.instrument("FOO").build()));
        assertFalse(target.isMarginable(builder.instrument("JPY").build()));
        assertFalse(target.isMarginable(builder.instrument("BTC_JPY").build()));
        assertFalse(target.isMarginable(null));

        assertTrue(target.isMarginable(builder.instrument("FX_BTC_JPY").build()));
        assertTrue(target.isMarginable(builder.instrument("BTCJPY_MAT1WK").build()));
        assertTrue(target.isMarginable(builder.instrument("BTCJPY_MAT2WK").build()));

    }

    @Test
    public void testGetExpiry() {

        Key key = Key.builder().instrument("BTCJPY_MAT1WK").build();

        ZoneId zone = ZoneId.of("Asia/Tokyo");
        LocalTime time = LocalTime.of(11, 0);

        // Valid
        LocalDate date = LocalDate.of(2017, 9, 15);
        doReturn("BTCJPY15SEP2017").when(target).convertProductAlias(key);
        assertEquals(target.getExpiry(key), ZonedDateTime.of(date, time, zone));

        // Invalid (Sep 31)
        date = LocalDate.of(2017, 10, 1);
        doReturn("BTCJPY31SEP2017").when(target).convertProductAlias(key);
        assertEquals(target.getExpiry(key), ZonedDateTime.of(date, time, zone));

        // Invalid (Sep 00)
        date = LocalDate.of(2017, 8, 31);
        doReturn("BTCJPY00SEP2017").when(target).convertProductAlias(key);
        assertEquals(target.getExpiry(key), ZonedDateTime.of(date, time, zone));

        // Invalid Month (???)
        doReturn("BTCJPY15???2017").when(target).convertProductAlias(key);
        assertNull(target.getExpiry(key));

        // Pattern Mismatch
        doReturn("01SEP2017").when(target).convertProductAlias(key);
        assertNull(target.getExpiry(key));

        // Code not found
        doReturn(null).when(target).convertProductAlias(key);
        assertNull(target.getExpiry(key));

    }

    @Test
    public void testFetchOrder() {

        Key key = Key.from(Request.builder().instrument("inst").build());
        doReturn("prod").when(target).convertProductAlias(key);

        OrderList c1 = mock(OrderList.class);
        OrderList c2 = mock(OrderList.class);
        when(c1.getPrice()).thenReturn(ONE);
        when(c2.getPrice()).thenReturn(TEN);
        when(orderService.listOrders(any())).thenAnswer(i -> {
            assertEquals(i.getArgumentAt(0, OrderList.Request.class).getProduct(), "prod");
            assertNull(i.getArgumentAt(0, OrderList.Request.class).getState());
            assertNull(i.getArgumentAt(0, OrderList.Request.class).getAcceptanceId());
            assertNull(i.getArgumentAt(0, OrderList.Request.class).getOrderId());
            assertNull(i.getArgumentAt(0, OrderList.Request.class).getParentId());
            return completedFuture(asList(c1, null, c2));
        }).thenReturn(null);

        ParentList p1 = mock(ParentList.class);
        ParentList p2 = mock(ParentList.class);
        when(p1.getPrice()).thenReturn(ONE.add(ONE));
        when(p2.getPrice()).thenReturn(TEN.add(TEN));
        when(orderService.listParents(any())).thenAnswer(i -> {
            assertEquals(i.getArgumentAt(0, ParentList.Request.class).getProduct(), "prod");
            assertNull(i.getArgumentAt(0, ParentList.Request.class).getState());
            return completedFuture(asList(p1, null, p2));
        }).thenReturn(null);

        // Queried
        List<? extends Order> orders = target.fetchOrder(key);
        assertEquals(orders.size(), 4);
        assertEquals(orders.get(0).getOrderPrice(), ONE);
        assertEquals(orders.get(1).getOrderPrice(), TEN);
        assertEquals(orders.get(2).getOrderPrice(), ONE.add(ONE));
        assertEquals(orders.get(3).getOrderPrice(), TEN.add(TEN));
        verify(orderService).listOrders(any());

        // Cached
        orders = target.fetchOrder(key);
        assertEquals(orders.size(), 4);
        assertEquals(orders.get(0).getOrderPrice(), ONE);
        assertEquals(orders.get(1).getOrderPrice(), TEN);
        assertEquals(orders.get(2).getOrderPrice(), ONE.add(ONE));
        assertEquals(orders.get(3).getOrderPrice(), TEN.add(TEN));
        verify(orderService).listOrders(any());

        target.clear();

        // Queried
        orders = target.fetchOrder(key);
        assertEquals(orders.size(), 0);
        verify(orderService, times(2)).listOrders(any());

    }

    @Test
    public void testFindOrder() throws Exception {

        Key key = Key.from(Request.builder().instrument("inst").build());

        BitflyerOrder o1 = mock(BitflyerOrder.class);
        BitflyerOrder o2 = mock(BitflyerOrder.class);
        BitflyerOrder o3 = mock(BitflyerOrder.class);
        when(o2.getId()).thenReturn("id");
        doReturn(asList(o1, o2, o3)).when(target).fetchOrder(key);

        assertSame(target.findOrder(key, "id"), o2);
        assertNull(target.findOrder(key, "di"));
        assertNull(target.findOrder(key, null));

        reset(o2);

        assertNull(target.findOrder(key, "id"));
        assertNull(target.findOrder(key, "di"));
        assertNull(target.findOrder(key, null));

    }

    @Test
    public void testListActiveOrders() throws Exception {

        Key key = Key.from(Request.builder().instrument("inst").build());

        BitflyerOrder.Child o1 = mock(BitflyerOrder.Child.class);
        BitflyerOrder.Child o2 = mock(BitflyerOrder.Child.class);
        BitflyerOrder.Child o3 = mock(BitflyerOrder.Child.class);
        BitflyerOrder.Child o4 = mock(BitflyerOrder.Child.class);
        BitflyerOrder.Child o5 = mock(BitflyerOrder.Child.class);
        doAnswer(i -> i.getArgumentAt(0, BitflyerOrder.Visitor.class).visit(o1)).when(o1).accept(any());
        doAnswer(i -> i.getArgumentAt(0, BitflyerOrder.Visitor.class).visit(o2)).when(o2).accept(any());
        doAnswer(i -> i.getArgumentAt(0, BitflyerOrder.Visitor.class).visit(o3)).when(o3).accept(any());
        doAnswer(i -> i.getArgumentAt(0, BitflyerOrder.Visitor.class).visit(o4)).when(o4).accept(any());
        doAnswer(i -> i.getArgumentAt(0, BitflyerOrder.Visitor.class).visit(o5)).when(o5).accept(any());
        when(o2.getActive()).thenReturn(true);
        when(o3.getActive()).thenReturn(false);
        when(o4.getActive()).thenReturn(true);
        when(o5.getActive()).thenReturn(true);
        doReturn(asList(o1, o2, null, o3, o4, o5)).when(target).fetchOrder(key);

        // Child Only
        List<Order> results = target.listActiveOrders(key);
        assertEquals(results.size(), 3);
        assertSame(results.get(0), o2);
        assertSame(results.get(1), o4);
        assertSame(results.get(2), o5);

        // With Parent
        BitflyerOrder.Parent o6 = mock(BitflyerOrder.Parent.class);
        BitflyerOrder.Parent o7 = mock(BitflyerOrder.Parent.class);
        BitflyerOrder.Parent o8 = mock(BitflyerOrder.Parent.class);
        doAnswer(i -> i.getArgumentAt(0, BitflyerOrder.Visitor.class).visit(o6)).when(o6).accept(any());
        doAnswer(i -> i.getArgumentAt(0, BitflyerOrder.Visitor.class).visit(o7)).when(o7).accept(any());
        doAnswer(i -> i.getArgumentAt(0, BitflyerOrder.Visitor.class).visit(o8)).when(o8).accept(any());
        when(o6.getActive()).thenReturn(true);
        when(o7.getActive()).thenReturn(false);
        when(o8.getActive()).thenReturn(true);
        doReturn(asList(o1, o6, o2, null, o3, o7, o4, o8, o5)).when(target).fetchOrder(key);

        results = target.listActiveOrders(key);
        assertEquals(results.size(), 2);
        assertSame(results.get(0), o6);
        assertSame(results.get(1), o8);

    }

    @Test
    public void testListExecutions() {

        Key key = Key.from(Request.builder().instrument("inst").build());
        doReturn("prod").when(target).convertProductAlias(key);

        TradeExecution r1 = mock(TradeExecution.class);
        TradeExecution r2 = mock(TradeExecution.class);
        when(r1.getPrice()).thenReturn(ONE);
        when(r2.getPrice()).thenReturn(TEN);
        when(orderService.listExecutions(any())).thenAnswer(i -> {

            assertEquals(i.getArgumentAt(0, TradeExecution.Request.class).getProduct(), "prod");
            assertNull(i.getArgumentAt(0, TradeExecution.Request.class).getChildOrderId());
            assertNull(i.getArgumentAt(0, TradeExecution.Request.class).getChildOrderAcceptanceId());

            return completedFuture(asList(r1, null, r2));

        }).thenReturn(null);

        // Queried
        List<Order.Execution> execs = target.listExecutions(key);
        assertEquals(execs.size(), 2);
        assertEquals(execs.get(0).getPrice(), ONE);
        assertEquals(execs.get(1).getPrice(), TEN);
        verify(orderService).listExecutions(any());

        // Cached
        execs = target.listExecutions(key);
        assertEquals(execs.size(), 2);
        assertEquals(execs.get(0).getPrice(), ONE);
        assertEquals(execs.get(1).getPrice(), TEN);
        verify(orderService).listExecutions(any());

        target.clear();

        // Queried
        execs = target.listExecutions(key);
        assertEquals(execs.size(), 0);
        verify(orderService, times(2)).listExecutions(any());

    }

    @Test
    public void testCreateOrder() throws Exception {

        Key key = Key.from(Request.builder().instrument("inst").build());
        doReturn("prod").when(target).convertProductAlias(key);
        CreateInstruction.CreateInstructionBuilder builder = CreateInstruction.builder().price(TEN).size(ONE);
        CompletableFuture<OrderCreate> future = completedFuture(mock(OrderCreate.class));
        AtomicReference<OrderCreate.Request> reference = new AtomicReference<>();

        when(future.get().getAcceptanceId()).thenReturn("aid");
        when(orderService.sendOrder(any())).thenAnswer(i -> {

            reference.set(i.getArgumentAt(0, OrderCreate.Request.class));

            return future;

        });

        // Buy
        CreateInstruction instruction = builder.build();
        Map<CreateInstruction, String> results = target.createOrders(key, singleton(instruction));
        assertEquals(results.get(instruction), future.get().getAcceptanceId());
        verify(orderService, times(1)).sendOrder(any());
        assertEquals(reference.get().getProduct(), "prod");
        assertEquals(reference.get().getType(), LIMIT);
        assertEquals(reference.get().getSide(), BUY);
        assertEquals(reference.get().getPrice(), TEN);
        assertEquals(reference.get().getSize(), ONE);

        // Sell
        instruction = builder.size(ONE.negate()).build();
        results = target.createOrders(key, singleton(instruction));
        assertEquals(results.get(instruction), future.get().getAcceptanceId());
        verify(orderService, times(2)).sendOrder(any());
        assertEquals(reference.get().getProduct(), "prod");
        assertEquals(reference.get().getType(), LIMIT);
        assertEquals(reference.get().getSide(), SELL);
        assertEquals(reference.get().getPrice(), TEN);
        assertEquals(reference.get().getSize(), ONE);

        // Market
        instruction = builder.price(ZERO).build();
        results = target.createOrders(key, singleton(instruction));
        assertEquals(results.get(instruction), future.get().getAcceptanceId());
        verify(orderService, times(3)).sendOrder(any());
        assertEquals(reference.get().getProduct(), "prod");
        assertEquals(reference.get().getType(), MARKET);
        assertEquals(reference.get().getSide(), SELL);
        assertEquals(reference.get().getPrice(), ZERO);
        assertEquals(reference.get().getSize(), ONE);

        // Invalid Key (Null)
        instruction = builder.build();
        results = target.createOrders(null, singleton(instruction));
        assertEquals(results.get(instruction), null);

        // Invalid Key (Empty)
        instruction = builder.build();
        results = target.createOrders(Key.from(Request.builder().build()), singleton(instruction));
        assertEquals(results.get(instruction), null);
        verifyNoMoreInteractions(orderService);

        // Invalid Instruction
        assertEquals(target.createOrders(key, null).size(), 0);
        assertEquals(target.createOrders(key, singleton(null)).size(), 1);
        assertEquals(target.createOrders(key, singleton(builder.price(null).size(ONE).build())).size(), 1);
        assertEquals(target.createOrders(key, singleton(builder.price(TEN).size(null).build())).size(), 1);
        verifyNoMoreInteractions(orderService);

    }

    @Test
    public void testCancelOrder() throws Exception {

        Key key = Key.from(Request.builder().instrument("inst").build());
        doReturn("prod").when(target).convertProductAlias(key);

        CancelInstruction ic1 = CancelInstruction.builder().id("cid1").build();
        CancelInstruction ic2 = CancelInstruction.builder().id("cid2").build();
        CancelInstruction ip1 = CancelInstruction.builder().id("pid1").build();
        CancelInstruction ip2 = CancelInstruction.builder().id("hoge").build();
        Set<CancelInstruction> instructions = Sets.newHashSet(ic1, ip1, null, ic2, null, ip2);

        BitflyerOrder.Child oc1 = mock(BitflyerOrder.Child.class);
        BitflyerOrder.Child oc2 = mock(BitflyerOrder.Child.class);
        BitflyerOrder.Parent op1 = mock(BitflyerOrder.Parent.class);
        BitflyerOrder.Parent op2 = mock(BitflyerOrder.Parent.class);
        when(oc1.getId()).thenReturn("cid1");
        when(oc2.getId()).thenReturn("cid2");
        when(op1.getId()).thenReturn("pid1");
        when(op2.getId()).thenReturn("pid2");
        when(oc1.getActive()).thenReturn(true);
        when(oc2.getActive()).thenReturn(true);
        when(op1.getActive()).thenReturn(true);
        when(op2.getActive()).thenReturn(true);
        doAnswer(i -> i.getArgumentAt(0, BitflyerOrder.Visitor.class).visit(oc1)).when(oc1).accept(any());
        doAnswer(i -> i.getArgumentAt(0, BitflyerOrder.Visitor.class).visit(oc2)).when(oc2).accept(any());
        doAnswer(i -> i.getArgumentAt(0, BitflyerOrder.Visitor.class).visit(op1)).when(op1).accept(any());
        doAnswer(i -> i.getArgumentAt(0, BitflyerOrder.Visitor.class).visit(op2)).when(op2).accept(any());

        doAnswer(i -> completedFuture(mock(OrderCancel.class))).when(orderService).cancelOrder(any());
        doAnswer(i -> completedFuture(mock(ParentCancel.class))).when(orderService).cancelParent(any());
        doAnswer(i -> completedFuture(mock(ProductCancel.class))).when(orderService).cancelProduct(any());
        doReturn(Arrays.asList(oc1, op1, oc2, op2)).when(target).fetchOrder(key);

        // Cancel None
        Map<CancelInstruction, String> results = target.cancelOrders(key, null);
        assertEquals(results.size(), 0);
        verifyNoMoreInteractions(orderService);

        // Cancel 3, Remaining 1
        results = target.cancelOrders(key, instructions);
        assertEquals(results.size(), 5);
        assertEquals(results.get(ic1), ic1.getId());
        assertEquals(results.get(ic2), ic2.getId());
        assertEquals(results.get(ip1), ip1.getId());
        assertEquals(results.get(ip2), null);
        verify(orderService, times(2)).cancelOrder(any());
        verify(orderService, times(1)).cancelParent(any());
        verifyNoMoreInteractions(orderService);

        // Cancel All
        when(op2.getActive()).thenReturn(null);
        results = target.cancelOrders(key, instructions);
        assertEquals(results.size(), 5);
        assertEquals(results.get(ic1), ic1.getId());
        assertEquals(results.get(ic2), ic2.getId());
        assertEquals(results.get(ip1), ip1.getId());
        assertEquals(results.get(ip2), null);
        verify(orderService, times(1)).cancelProduct(any());
        verifyNoMoreInteractions(orderService);

        // Null Key
        results = target.cancelOrders(null, instructions);
        assertEquals(results.size(), 5);
        assertEquals(results.get(ic1), null);
        assertEquals(results.get(ic2), null);
        assertEquals(results.get(ip1), null);
        assertEquals(results.get(ip2), null);
        verifyNoMoreInteractions(orderService);

    }

}
