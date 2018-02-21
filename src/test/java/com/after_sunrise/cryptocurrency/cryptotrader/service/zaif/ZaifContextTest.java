package com.after_sunrise.cryptocurrency.cryptotrader.service.zaif;

import com.after_sunrise.cryptocurrency.cryptotrader.framework.Context.Key;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Trade;
import com.google.common.io.Resources;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static com.after_sunrise.cryptocurrency.cryptotrader.framework.Service.CurrencyType.*;
import static com.after_sunrise.cryptocurrency.cryptotrader.service.template.TemplateContext.RequestType.GET;
import static com.after_sunrise.cryptocurrency.cryptotrader.service.zaif.ZaifContext.URL_TICKER;
import static com.after_sunrise.cryptocurrency.cryptotrader.service.zaif.ZaifContext.URL_TRADE;
import static com.google.common.io.Resources.getResource;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.mockito.Mockito.*;
import static org.testng.Assert.*;

/**
 * @author takanori.takase
 * @version 0.0.1
 */
public class ZaifContextTest {

    private ZaifContext target;

    @BeforeMethod
    public void setUp() throws Exception {

        target = spy(new ZaifContext());

        doReturn(null).when(target).request(any(), any(), any(), any());

    }

    @AfterMethod
    public void tearDown() throws Exception {
        target.close();
    }

    @Test(enabled = false)
    public void test() throws IOException {

        doCallRealMethod().when(target).request(any(), any(), any(), any());

        Key key = Key.builder().instrument("bch_btc").build();

        System.out.println("ASK : " + target.getBestAskPrice(key));
        System.out.println("BID : " + target.getBestBidPrice(key));
        System.out.println("MID : " + target.getMidPrice(key));
        System.out.println("LTP : " + target.getLastPrice(key));
        System.out.println("TRD : " + target.listTrades(key, null));

    }

    @Test
    public void testGet() {
        assertEquals(target.get(), "zaif");
    }

    @Test
    public void testQueryTick() throws Exception {

        String data = Resources.toString(getResource("json/zaif_ticker.json"), UTF_8);
        doReturn(data).when(target).request(GET, URL_TICKER + "btc_jpy", null, null);

        // Found
        ZaifTick tick = target.queryTick(Key.builder().instrument("btc_jpy").build()).get();
        assertEquals(tick.getAsk(), new BigDecimal("403755"));
        assertEquals(tick.getBid(), new BigDecimal("403690"));
        assertEquals(tick.getLast(), new BigDecimal("403720"));

        // Not found
        assertFalse(target.queryTick(Key.builder().instrument("FOO_BAR").build()).isPresent());

        // Cached
        doReturn(null).when(target).request(any(), any(), any(), any());
        ZaifTick cached = target.queryTick(Key.builder().instrument("btc_jpy").build()).get();
        assertSame(cached, tick);

    }

    @Test
    public void testGetBestAskPrice() throws Exception {

        Key key = Key.builder().instrument("foo").build();

        ZaifTick tick = mock(ZaifTick.class);
        when(tick.getAsk()).thenReturn(BigDecimal.TEN);

        doReturn(Optional.of(tick)).when(target).queryTick(key);
        assertEquals(target.getBestAskPrice(key), tick.getAsk());

        doReturn(Optional.empty()).when(target).queryTick(key);
        assertNull(target.getBestAskPrice(key));

    }

    @Test
    public void testGetBestBidPrice() throws Exception {

        Key key = Key.builder().instrument("foo").build();

        ZaifTick tick = mock(ZaifTick.class);
        when(tick.getBid()).thenReturn(BigDecimal.TEN);

        doReturn(Optional.of(tick)).when(target).queryTick(key);
        assertEquals(target.getBestBidPrice(key), tick.getBid());

        doReturn(Optional.empty()).when(target).queryTick(key);
        assertNull(target.getBestBidPrice(key));

    }

    @Test
    public void testGetLastPrice() throws Exception {

        Key key = Key.builder().instrument("foo").build();

        ZaifTick tick = mock(ZaifTick.class);
        when(tick.getLast()).thenReturn(BigDecimal.TEN);

        doReturn(Optional.of(tick)).when(target).queryTick(key);
        assertEquals(target.getLastPrice(key), tick.getLast());

        doReturn(Optional.empty()).when(target).queryTick(key);
        assertNull(target.getLastPrice(key));

    }

    @Test
    public void testListTrades() throws Exception {

        Key key = Key.builder().instrument("btc_jpy").build();
        String data = Resources.toString(getResource("json/zaif_trade.json"), UTF_8);
        doReturn(data).when(target).request(GET, URL_TRADE + "btc_jpy", null, null);

        // Found
        List<Trade> values = target.listTrades(key, null);
        assertEquals(values.size(), 2);
        assertEquals(values.get(0).getTimestamp(), Instant.ofEpochMilli(1505657008000L));
        assertEquals(values.get(0).getPrice(), new BigDecimal("407470"));
        assertEquals(values.get(0).getSize(), new BigDecimal("0.1871"));
        assertEquals(values.get(1).getTimestamp(), Instant.ofEpochMilli(1505657007000L));
        assertEquals(values.get(1).getPrice(), new BigDecimal("407505"));
        assertEquals(values.get(1).getSize(), new BigDecimal("0.0011"));

        // Not found
        List<Trade> unknown = target.listTrades(Key.builder().instrument("FOO").build(), null);
        assertEquals(unknown, null);

        // Cached
        doReturn(null).when(target).request(any(), any(), any(), any());
        List<Trade> cached = target.listTrades(key, null);
        assertEquals(cached, values);

        // Filtered
        List<Trade> filtered = target.listTrades(key, Instant.ofEpochMilli(1505657007500L));
        assertEquals(filtered.size(), 1);
        assertEquals(filtered.get(0), values.get(0));

    }

    @Test
    public void testFindProduct() {
        assertEquals(target.findProduct(null, BTC, JPY), "btc_jpy");
        assertEquals(target.findProduct(null, ETH, BTC), "eth_btc");
        assertEquals(target.findProduct(null, BCH, BTC), "bch_btc");
        assertEquals(target.findProduct(null, JPY, BTC), null);
    }

}
