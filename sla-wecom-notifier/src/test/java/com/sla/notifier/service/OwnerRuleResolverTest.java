package com.sla.notifier.service;

import com.sla.notifier.mapper.SlaSystemOwnerMapper;
import com.sla.notifier.model.SlaSystemOwner;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class OwnerRuleResolverTest {

    private OwnerRuleResolver resolver;

    @BeforeEach
    void setUp() {
        SlaSystemOwnerMapper mapper = mock(SlaSystemOwnerMapper.class);
        when(mapper.selectAll()).thenReturn(Arrays.asList(
                row("子系统", "北京亦庄工程交易系统gfm", "王华聪", "13521207069"),
                row("父系统", "北京亦庄工程交易系统gfm", "刘彩霞", "15221477921"),
                row("子系统", "某某音视频系统", "张洪涛", "18210189117")));
        resolver = new OwnerRuleResolver(mapper);
    }

    @Test
    void dictionaryHitReturnsOwnerAndMobile() {
        OwnerRuleResolver.ResolvedOwner owner = resolver.resolve("子系统", "北京亦庄工程交易系统gfm");
        assertTrue(owner.isMatched());
        assertEquals("王华聪", owner.getName());
        assertEquals("13521207069", owner.getMobile());
    }

    @Test
    void sameNameDifferentLevelAreDistinct() {
        Map<String, SlaSystemOwner> snapshot = resolver.loadSnapshot();
        OwnerRuleResolver.ResolvedOwner sub = resolver.resolve(
                "子系统", "北京亦庄工程交易系统gfm", snapshot);
        OwnerRuleResolver.ResolvedOwner parent = resolver.resolve(
                "父系统", "北京亦庄工程交易系统gfm", snapshot);
        assertEquals("王华聪", sub.getName());
        assertEquals("刘彩霞", parent.getName());
    }

    @Test
    void trimsLevelAndName() {
        OwnerRuleResolver.ResolvedOwner owner = resolver.resolve(" 子系统 ", " 某某音视频系统 ");
        assertEquals("张洪涛", owner.getName());
        assertEquals("18210189117", owner.getMobile());
    }

    @Test
    void missIsUnmatched() {
        OwnerRuleResolver.ResolvedOwner owner = resolver.resolve("子系统", "不存在的系统");
        assertFalse(owner.isMatched());
        assertNull(owner.getName());
        assertNull(owner.getMobile());
    }

    private static SlaSystemOwner row(String level, String name, String owner, String mobile) {
        SlaSystemOwner item = new SlaSystemOwner();
        item.setSystemLevel(level);
        item.setSystemName(name);
        item.setOwnerName(owner);
        item.setMobile(mobile);
        return item;
    }
}
