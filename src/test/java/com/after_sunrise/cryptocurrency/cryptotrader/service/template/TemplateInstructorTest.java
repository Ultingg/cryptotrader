package com.after_sunrise.cryptocurrency.cryptotrader.service.template;

import com.after_sunrise.cryptocurrency.cryptotrader.framework.Adviser.Advice;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Context;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Context.Key;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Instruction;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Instruction.CancelInstruction;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Instruction.CreateInstruction;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Order;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Request;
import org.apache.commons.configuration2.Configuration;
import org.apache.commons.configuration2.MapConfiguration;
import org.apache.commons.lang3.StringUtils;
import org.mockito.invocation.InvocationOnMock;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;

import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import static java.math.BigDecimal.*;
import static java.time.Instant.ofEpochMilli;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;
import static org.apache.commons.lang3.math.NumberUtils.INTEGER_ZERO;
import static org.mockito.Mockito.*;
import static org.testng.Assert.*;

/**
 * @author takanori.takase
 * @version 0.0.1
 */
public class TemplateInstructorTest {

    private TemplateInstructor target;

    private Context context;

    private Configuration configuration;

    private Request.RequestBuilder builder;

    @BeforeMethod
    public void setUp() throws Exception {

        context = mock(Context.class);

        BiFunction<InvocationOnMock, BigDecimal, BigDecimal> f = (i, unit) -> {

            RoundingMode mode = i.getArgumentAt(2, RoundingMode.class);

            BigDecimal value = i.getArgumentAt(1, BigDecimal.class);

            if (value == null || mode == null) {
                return null;
            }

            BigDecimal units = value.divide(unit, INTEGER_ZERO, mode);

            return units.multiply(unit);

        };
        when(context.roundTickSize(any(), any(), any())).thenAnswer(i -> f.apply(i, new BigDecimal("0.003")));
        when(context.roundLotSize(any(), any(), any())).thenAnswer(i -> f.apply(i, new BigDecimal("0.3")));

        configuration = spy(new MapConfiguration(new HashMap<>()));

        builder = Request.builder().site("s").instrument("i")
                .currentTime(ofEpochMilli(123L)).targetTime(ofEpochMilli(456L))
                .tradingExposure(ZERO).tradingSplit(5).tradingSpread(ZERO).tradingInstruction("IOC");

        target = spy(new TemplateInstructor("test"));

        target.setConfiguration(configuration);

    }

    @Test
    public void testGet() throws Exception {
        Assert.assertEquals(target.get(), "test");
    }

    @Test
    public void testInstruct() throws Exception {

        Request request = builder.build();
        Advice advice = Advice.builder().build();
        CreateInstruction i1 = CreateInstruction.builder().build();
        CreateInstruction i2 = CreateInstruction.builder().build();
        CancelInstruction i3 = CancelInstruction.builder().build();
        Map<CancelInstruction, Order> cancels = singletonMap(i3, mock(Order.class));
        List<Instruction> instructions = asList(i3, i1, i2);

        doReturn(singletonList(i1)).when(target).createBuys(context, request, advice);
        doReturn(singletonList(i2)).when(target).createSells(context, request, advice);
        doReturn(cancels).when(target).createCancels(context, request);
        doReturn(instructions).when(target).merge(asList(i1, i2), cancels);

        assertSame(target.instruct(context, request, advice), instructions);

    }

    @Test
    public void testCreateCancels() throws Exception {

        Order o1 = mock(Order.class);
        Order o2 = mock(Order.class);
        Order o3 = mock(Order.class);
        Order o4 = mock(Order.class);
        when(o1.getActive()).thenReturn(null);
        when(o2.getActive()).thenReturn(TRUE);
        when(o3.getActive()).thenReturn(TRUE);
        when(o4.getActive()).thenReturn(FALSE);
        when(o2.getId()).thenReturn("id");
        List<Order> orders = asList(o1, null, o2, o3, o4);

        when(context.listActiveOrders(Key.from(builder.build()))).thenReturn(orders);
        Map<CancelInstruction, Order> results = target.createCancels(context, builder.build());
        assertEquals(results.size(), 1);
        assertTrue(results.values().contains(o2));

        when(context.listActiveOrders(Key.from(builder.build()))).thenReturn(null);
        results = target.createCancels(context, builder.build());
        assertEquals(results.size(), 0);

    }

    @Test
    public void testCreateBuys() throws Exception {

        Request request = builder.build();
        Advice.AdviceBuilder builder = Advice.builder()
                .buyLimitPrice(ONE).buyLimitSize(TEN).buySpread(new BigDecimal("0.06"));

        List<CreateInstruction> results = target.createBuys(context, request, builder.build());
        assertEquals(results.size(), 5, results.toString());
        results.forEach(r -> assertEquals(r.getStrategy(), "IOC"));
        results.forEach(r -> assertEquals(r.getTimeToLive(), Duration.ofMillis(456 - 123)));

        assertEquals(results.get(0).getPrice(), new BigDecimal("0.999"));
        assertEquals(results.get(1).getPrice(), new BigDecimal("0.969"));
        assertEquals(results.get(2).getPrice(), new BigDecimal("0.924"));
        assertEquals(results.get(3).getPrice(), new BigDecimal("0.864"));
        assertEquals(results.get(4).getPrice(), new BigDecimal("0.789"));

        assertEquals(results.get(0).getSize(), new BigDecimal("2.1"));
        assertEquals(results.get(1).getSize(), new BigDecimal("2.1"));
        assertEquals(results.get(2).getSize(), new BigDecimal("2.1"));
        assertEquals(results.get(3).getSize(), new BigDecimal("2.1"));
        assertEquals(results.get(4).getSize(), new BigDecimal("1.5"));

        // Fraction size
        results = target.createBuys(context, request, builder.buyLimitSize(ONE).build());
        assertEquals(results.size(), 3, results.toString());
        assertEquals(results.get(0).getPrice(), new BigDecimal("0.999"));
        assertEquals(results.get(1).getPrice(), new BigDecimal("0.939"));
        assertEquals(results.get(2).getPrice(), new BigDecimal("0.849"));
        assertEquals(results.get(0).getSize(), new BigDecimal("0.3"));
        assertEquals(results.get(1).getSize(), new BigDecimal("0.3"));
        assertEquals(results.get(2).getSize(), new BigDecimal("0.3"));
        results.forEach(r -> assertEquals(r.getStrategy(), "IOC"));
        results.forEach(r -> assertEquals(r.getTimeToLive(), Duration.ofMillis(456 - 123)));

        // Null Price
        results = target.createBuys(context, request, builder.buyLimitPrice(null).build());
        assertEquals(results.size(), 0, results.toString());

        // Too small
        results = target.createBuys(context, request, builder.buyLimitSize(ONE.movePointLeft(1)).build());
        assertEquals(results.size(), 0);

        // Null size
        results = target.createBuys(context, request, builder.buyLimitSize(null).build());
        assertEquals(results.size(), 0);

        // Zero size
        results = target.createBuys(context, request, builder.buyLimitSize(ZERO).build());
        assertEquals(results.size(), 0);

        // Negative size
        results = target.createBuys(context, request, builder.buyLimitSize(ONE.negate()).build());
        assertEquals(results.size(), 0);

    }

    @Test
    public void testCreateSells() throws Exception {

        Request request = builder.build();
        Advice.AdviceBuilder builder = Advice.builder()
                .sellLimitPrice(ONE).sellLimitSize(TEN).sellSpread(new BigDecimal("0.06"));

        List<CreateInstruction> results = target.createSells(context, request, builder.build());
        assertEquals(results.size(), 5, results.toString());
        results.forEach(r -> assertEquals(r.getStrategy(), "IOC"));
        results.forEach(r -> assertEquals(r.getTimeToLive(), Duration.ofMillis(456 - 123)));

        assertEquals(results.get(0).getPrice(), new BigDecimal("1.002"));
        assertEquals(results.get(1).getPrice(), new BigDecimal("1.032"));
        assertEquals(results.get(2).getPrice(), new BigDecimal("1.077"));
        assertEquals(results.get(3).getPrice(), new BigDecimal("1.137"));
        assertEquals(results.get(4).getPrice(), new BigDecimal("1.212"));

        assertEquals(results.get(0).getSize(), new BigDecimal("-2.1"));
        assertEquals(results.get(1).getSize(), new BigDecimal("-2.1"));
        assertEquals(results.get(2).getSize(), new BigDecimal("-2.1"));
        assertEquals(results.get(3).getSize(), new BigDecimal("-2.1"));
        assertEquals(results.get(4).getSize(), new BigDecimal("-1.5"));

        // Fraction size
        results = target.createSells(context, request, builder.sellLimitSize(ONE).build());
        assertEquals(results.size(), 3, results.toString());
        assertEquals(results.get(0).getPrice(), new BigDecimal("1.002"));
        assertEquals(results.get(1).getPrice(), new BigDecimal("1.062"));
        assertEquals(results.get(2).getPrice(), new BigDecimal("1.152"));
        assertEquals(results.get(0).getSize(), new BigDecimal("-0.3"));
        assertEquals(results.get(1).getSize(), new BigDecimal("-0.3"));
        assertEquals(results.get(2).getSize(), new BigDecimal("-0.3"));
        results.forEach(r -> assertEquals(r.getStrategy(), "IOC"));
        results.forEach(r -> assertEquals(r.getTimeToLive(), Duration.ofMillis(456 - 123)));

        // Null Price
        results = target.createSells(context, request, builder.sellLimitPrice(null).build());
        assertEquals(results.size(), 0, results.toString());

        // Too small
        results = target.createSells(context, request, builder.sellLimitSize(ONE.movePointLeft(1)).build());
        assertEquals(results.size(), 0);

        // Null size
        results = target.createSells(context, request, builder.sellLimitSize(null).build());
        assertEquals(results.size(), 0);

        // Zero size
        results = target.createSells(context, request, builder.sellLimitSize(ZERO).build());
        assertEquals(results.size(), 0);

        // Negative size
        results = target.createSells(context, request, builder.sellLimitSize(ONE.negate()).build());
        assertEquals(results.size(), 0);

    }

    @Test
    public void testSplitSize() {

        Request request = builder.tradingSplit(4).build();

        // Zero Quantity
        List<BigDecimal> results = target.splitSize(context, request, new BigDecimal("0.00"));
        assertEquals(results.size(), 0, results.toString());

        // Null Quantity
        results = target.splitSize(context, request, null);
        assertEquals(results.size(), 0, results.toString());

        // Insufficient Quantity
        results = target.splitSize(context, request, new BigDecimal("0.29"));
        assertEquals(results.size(), 0, results.toString());

        // One lot
        results = target.splitSize(context, request, new BigDecimal("0.30"));
        assertEquals(results.size(), 1, results.toString());
        assertEquals(results.get(0), new BigDecimal("0.3")); // 1 lot

        // Two lots
        results = target.splitSize(context, request, new BigDecimal("0.89"));
        assertEquals(results.size(), 2, results.toString());
        assertEquals(results.get(0), new BigDecimal("0.3")); // 1 lot
        assertEquals(results.get(1), new BigDecimal("0.3")); // 1 lot

        // Three lots
        results = target.splitSize(context, request, new BigDecimal("0.91"));
        assertEquals(results.size(), 3, results.toString());
        assertEquals(results.get(0), new BigDecimal("0.3")); // 1 lot
        assertEquals(results.get(1), new BigDecimal("0.3")); // 1 lot
        assertEquals(results.get(2), new BigDecimal("0.3")); // 1 lot

        // Four lots
        results = target.splitSize(context, request, new BigDecimal("1.21"));
        assertEquals(results.size(), 4, results.toString());
        assertEquals(results.get(0), new BigDecimal("0.3")); // 1 lot
        assertEquals(results.get(1), new BigDecimal("0.3")); // 1 lot
        assertEquals(results.get(2), new BigDecimal("0.3")); // 1 lot
        assertEquals(results.get(3), new BigDecimal("0.3")); // 1 lot

        // Five lots
        results = target.splitSize(context, request, new BigDecimal("1.51"));
        assertEquals(results.size(), 4, results.toString());
        assertEquals(results.get(0), new BigDecimal("0.3")); // 1 lot
        assertEquals(results.get(1), new BigDecimal("0.3")); // 1 lot
        assertEquals(results.get(2), new BigDecimal("0.3")); // 1 lot
        assertEquals(results.get(3), new BigDecimal("0.6")); // 2 lots

        // 100+ lots (30.5 / 0.3 = 101.66...)
        results = target.splitSize(context, request, new BigDecimal("30.5"));
        assertEquals(results.size(), 4, results.toString());
        assertEquals(results.get(0), new BigDecimal("7.5")); // 25 lots
        assertEquals(results.get(1), new BigDecimal("7.5")); // 25 lots
        assertEquals(results.get(2), new BigDecimal("7.5")); // 25 lots
        assertEquals(results.get(3), new BigDecimal("7.8")); // 26 lots

        // 1000+ lots (543.2 / 0.3 = 1810.66...)
        results = target.splitSize(context, request, new BigDecimal("543.2"));
        assertEquals(results.size(), 4, results.toString());
        assertEquals(results.get(0), new BigDecimal("135.0")); // 452 -> 450 lots
        assertEquals(results.get(1), new BigDecimal("135.0")); // 452 -> 450 lots
        assertEquals(results.get(2), new BigDecimal("135.0")); // 452 -> 450 lots
        assertEquals(results.get(3), new BigDecimal("138.0")); // 460 lots

        // With minimum
        // 1000+ lots (543.2 / 0.3 = 1810.66...)
        request = builder.tradingMinimum(new BigDecimal("200")).build(); // 666.66.. lots
        results = target.splitSize(context, request, new BigDecimal("543.2"));
        assertEquals(results.size(), 2, results.toString());
        assertEquals(results.get(0), new BigDecimal("200.1")); // 667 lots
        assertEquals(results.get(1), new BigDecimal("342.9")); // 667 + 476 = 1143 lots

        // With minimum
        // 1000+ lots (543.2 / 0.3 = 1810.66...)
        request = builder.tradingMinimum(new BigDecimal("2000")).build(); // 666.66.. lots
        results = target.splitSize(context, request, new BigDecimal("543.2"));
        assertEquals(results.size(), 1, results.toString());
        assertEquals(results.get(0), new BigDecimal("543.0")); // 1810 lots

        // With maximum
        request = builder.tradingMinimum(null).tradingMaximum(new BigDecimal("12")).build();
        results = target.splitSize(context, request, new BigDecimal("543.2"));
        assertEquals(results.size(), 4, results.toString());
        assertEquals(results.get(0), new BigDecimal("12.0"));
        assertEquals(results.get(1), new BigDecimal("12.0"));
        assertEquals(results.get(2), new BigDecimal("12.0"));
        assertEquals(results.get(3), new BigDecimal("12.0"));

        // With minimum + maximum
        request = builder.tradingMinimum(new BigDecimal(12)).tradingMaximum(new BigDecimal("18")).build();
        results = target.splitSize(context, request, new BigDecimal("543.2"));
        assertEquals(results.size(), 4, results.toString());
        assertEquals(results.get(0), new BigDecimal("18.0"));
        assertEquals(results.get(1), new BigDecimal("18.0"));
        assertEquals(results.get(2), new BigDecimal("18.0"));
        assertEquals(results.get(3), new BigDecimal("18.0"));

        // With minimum + maximum (reversed)
        request = builder.tradingMinimum(new BigDecimal(18)).tradingMaximum(new BigDecimal("12")).build();
        results = target.splitSize(context, request, new BigDecimal("543.2"));
        assertEquals(results.size(), 4, results.toString());
        assertEquals(results.get(0), new BigDecimal("12.0"));
        assertEquals(results.get(1), new BigDecimal("12.0"));
        assertEquals(results.get(2), new BigDecimal("12.0"));
        assertEquals(results.get(3), new BigDecimal("12.0"));

    }

    @Test
    public void testMerge() {

        CreateInstruction new1 = CreateInstruction.builder().price(valueOf(11)).size(valueOf(21)).build();
        CreateInstruction new2 = CreateInstruction.builder().price(valueOf(12)).size(valueOf(22)).build();
        CreateInstruction new3 = CreateInstruction.builder().price(valueOf(13)).size(valueOf(23)).build();
        CreateInstruction new4 = CreateInstruction.builder().price(valueOf(14)).size(valueOf(24)).build();
        CreateInstruction new5 = CreateInstruction.builder().price(valueOf(15)).size(valueOf(25)).build();
        CreateInstruction new6 = CreateInstruction.builder().price(null).size(valueOf(26)).build();
        CreateInstruction new7 = CreateInstruction.builder().price(valueOf(17)).size(null).build();
        CreateInstruction new8 = CreateInstruction.builder().price(valueOf(18)).size(valueOf(0)).build();
        CreateInstruction new9 = CreateInstruction.builder().price(valueOf(0)).size(valueOf(29)).build();
        List<CreateInstruction> creates = asList(new4, new8, new2, new9, new7, new3, new6, new5, new1); // Shuffled

        CancelInstruction cancel1 = CancelInstruction.builder().id("c1").build();
        CancelInstruction cancel2 = CancelInstruction.builder().id("c2").build();
        CancelInstruction cancel3 = CancelInstruction.builder().id("c3").build();
        CancelInstruction cancel4 = CancelInstruction.builder().id("c4").build();
        CancelInstruction cancel5 = CancelInstruction.builder().id("c5").build();
        CancelInstruction cancel6 = CancelInstruction.builder().id("c6").build();
        CancelInstruction cancel7 = CancelInstruction.builder().id("c7").build();
        CancelInstruction cancel8 = CancelInstruction.builder().id("c8").build();
        CancelInstruction cancel9 = CancelInstruction.builder().id("c9").build();

        Map<CancelInstruction, Order> cancels = new IdentityHashMap<>();

        Runnable initializer = () -> {
            cancels.put(cancel1, mock(Order.class));
            cancels.put(cancel2, mock(Order.class));
            cancels.put(cancel3, mock(Order.class));
            cancels.put(cancel4, mock(Order.class));
            cancels.put(cancel5, mock(Order.class));
            cancels.put(cancel6, mock(Order.class));
            cancels.put(cancel7, mock(Order.class));
            cancels.put(cancel8, mock(Order.class));
            cancels.put(cancel9, mock(Order.class));
            when(cancels.get(cancel1).getOrderPrice()).thenReturn(new1.getPrice());
            when(cancels.get(cancel1).getRemainingQuantity()).thenReturn(new1.getSize());
            when(cancels.get(cancel2).getOrderPrice()).thenReturn(new2.getPrice());
            when(cancels.get(cancel2).getRemainingQuantity()).thenReturn(new2.getSize());
            when(cancels.get(cancel3).getOrderPrice()).thenReturn(new3.getPrice());
            when(cancels.get(cancel3).getRemainingQuantity()).thenReturn(new3.getSize());
            when(cancels.get(cancel4).getOrderPrice()).thenReturn(new4.getPrice());
            when(cancels.get(cancel4).getRemainingQuantity()).thenReturn(new4.getSize());
            when(cancels.get(cancel5).getOrderPrice()).thenReturn(new5.getPrice());
            when(cancels.get(cancel5).getRemainingQuantity()).thenReturn(new5.getSize());
            when(cancels.get(cancel6).getOrderPrice()).thenReturn(new6.getPrice());
            when(cancels.get(cancel6).getRemainingQuantity()).thenReturn(new6.getSize());
            when(cancels.get(cancel7).getOrderPrice()).thenReturn(new7.getPrice());
            when(cancels.get(cancel7).getRemainingQuantity()).thenReturn(new7.getSize());
            when(cancels.get(cancel8).getOrderPrice()).thenReturn(new8.getPrice());
            when(cancels.get(cancel8).getRemainingQuantity()).thenReturn(new8.getSize());
            when(cancels.get(cancel9).getOrderPrice()).thenReturn(new9.getPrice());
            when(cancels.get(cancel9).getRemainingQuantity()).thenReturn(new9.getSize());
        };

        initializer.run();
        List<Instruction> results = target.merge(creates, cancels);
        assertEquals(results.size(), 8, StringUtils.join(results, '\n'));
        assertTrue(results.contains(cancel6));
        assertTrue(results.contains(cancel7));
        assertTrue(results.contains(cancel8));
        assertTrue(results.contains(cancel9));
        assertTrue(results.contains(new6));
        assertTrue(results.contains(new7));
        assertTrue(results.contains(new8));
        assertTrue(results.contains(new9));

        // Zero Price/Size
        initializer.run();
        when(cancels.get(cancel2).getOrderPrice()).thenReturn(valueOf(0.0));
        when(cancels.get(cancel3).getRemainingQuantity()).thenReturn(valueOf(0.0));
        results = target.merge(creates, cancels);
        assertEquals(results.size(), 12, StringUtils.join(results, '\n'));
        assertTrue(results.contains(cancel2));
        assertTrue(results.contains(cancel3));
        assertTrue(results.contains(cancel6));
        assertTrue(results.contains(cancel7));
        assertTrue(results.contains(cancel8));
        assertTrue(results.contains(cancel9));
        assertTrue(results.contains(new2));
        assertTrue(results.contains(new3));
        assertTrue(results.contains(new6));
        assertTrue(results.contains(new7));
        assertTrue(results.contains(new8));
        assertTrue(results.contains(new9));

        // Null Price/Size
        initializer.run();
        when(cancels.get(cancel2).getOrderPrice()).thenReturn(null);
        when(cancels.get(cancel3).getRemainingQuantity()).thenReturn(null);
        results = target.merge(creates, cancels);
        assertEquals(results.size(), 12, StringUtils.join(results, '\n'));
        assertTrue(results.contains(cancel2));
        assertTrue(results.contains(cancel3));
        assertTrue(results.contains(cancel6));
        assertTrue(results.contains(cancel7));
        assertTrue(results.contains(cancel8));
        assertTrue(results.contains(cancel9));
        assertTrue(results.contains(new2));
        assertTrue(results.contains(new3));
        assertTrue(results.contains(new6));
        assertTrue(results.contains(new7));
        assertTrue(results.contains(new8));
        assertTrue(results.contains(new9));

        for (BigDecimal delta : new BigDecimal[]{new BigDecimal("+0.0001"), new BigDecimal("-0.0001")}) {

            // Different Price/Size
            initializer.run();
            when(cancels.get(cancel2).getOrderPrice()).thenReturn(new2.getPrice().add(delta));
            when(cancels.get(cancel3).getRemainingQuantity()).thenReturn(new3.getSize().add(delta));

            // Zero Tolerance
            results = target.merge(creates, cancels);
            assertEquals(results.size(), 12, StringUtils.join(results, '\n'));
            assertTrue(results.contains(cancel2));
            assertTrue(results.contains(cancel3));
            assertTrue(results.contains(cancel6));
            assertTrue(results.contains(cancel7));
            assertTrue(results.contains(cancel8));
            assertTrue(results.contains(cancel9));
            assertTrue(results.contains(new2));
            assertTrue(results.contains(new3));
            assertTrue(results.contains(new6));
            assertTrue(results.contains(new7));
            assertTrue(results.contains(new8));
            assertTrue(results.contains(new9));

            // Price Threshold
            configuration.addProperty(
                    "com.after_sunrise.cryptocurrency.cryptotrader.service.template.TemplateInstructor.threshold.price",
                    new BigDecimal("0.01")
            );
            results = target.merge(creates, cancels);
            assertEquals(results.size(), 10, StringUtils.join(results, '\n'));
            assertTrue(results.contains(cancel3));
            assertTrue(results.contains(cancel6));
            assertTrue(results.contains(cancel7));
            assertTrue(results.contains(cancel8));
            assertTrue(results.contains(cancel9));
            assertTrue(results.contains(new3));
            assertTrue(results.contains(new6));
            assertTrue(results.contains(new7));
            assertTrue(results.contains(new8));
            assertTrue(results.contains(new9));

            configuration.clear();

            // Size Threshold
            configuration.addProperty(
                    "com.after_sunrise.cryptocurrency.cryptotrader.service.template.TemplateInstructor.threshold.size",
                    new BigDecimal("0.01")
            );
            results = target.merge(creates, cancels);
            if (delta.signum() > 0) {
                assertEquals(results.size(), 12, StringUtils.join(results, '\n'));
                assertTrue(results.contains(cancel2));
                assertTrue(results.contains(cancel3));
                assertTrue(results.contains(cancel6));
                assertTrue(results.contains(cancel7));
                assertTrue(results.contains(cancel8));
                assertTrue(results.contains(cancel9));
                assertTrue(results.contains(new2));
                assertTrue(results.contains(new3));
                assertTrue(results.contains(new6));
                assertTrue(results.contains(new7));
                assertTrue(results.contains(new8));
                assertTrue(results.contains(new9));
            } else {
                assertEquals(results.size(), 10, StringUtils.join(results, '\n'));
                assertTrue(results.contains(cancel2));
                assertTrue(results.contains(cancel6));
                assertTrue(results.contains(cancel7));
                assertTrue(results.contains(cancel8));
                assertTrue(results.contains(cancel9));
                assertTrue(results.contains(new2));
                assertTrue(results.contains(new6));
                assertTrue(results.contains(new7));
                assertTrue(results.contains(new8));
                assertTrue(results.contains(new9));
            }

            configuration.clear();

        }

    }

}
