package com.bdis.modules.growth.support;

import static org.assertj.core.api.Assertions.assertThat;

import com.bdis.modules.growth.support.DigitalLifeHashChain.CanonicalEvent;
import com.bdis.modules.growth.support.DigitalLifeHashChain.HashedEvent;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

class DigitalLifeHashChainTest {

    private DigitalLifeHashChain chain;

    @BeforeEach
    void setUp() {
        chain = new DigitalLifeHashChain(new ObjectMapper());
    }

    @Test
    void normalChainVerifiesAndLinksPreviousHash() {
        List<HashedEvent> events = chain.build(events());

        assertThat(chain.verifyStored(events).verified()).isTrue();
        assertThat(events).hasSize(2);
        assertThat(events.get(0).previousHash()).isNull();
        assertThat(events.get(1).previousHash()).isEqualTo(events.get(0).eventHash());
    }

    @Test
    void changedMiddlePayloadFailsVerification() {
        List<HashedEvent> events = new ArrayList<>(chain.build(events()));
        HashedEvent original = events.get(1);
        events.set(
                1,
                new HashedEvent(
                        original.sequence(),
                        original.event(),
                        original.canonicalData().replace("21.5", "99.9"),
                        original.previousHash(),
                        original.eventHash()));

        assertThat(chain.verifyStored(events).verified()).isFalse();
        assertThat(chain.verifyStored(events).failedSequence()).isEqualTo(2);
    }

    @Test
    void changedSequenceFailsVerification() {
        List<HashedEvent> events = new ArrayList<>(chain.build(events()));
        HashedEvent second = events.get(1);
        events.set(
                1,
                new HashedEvent(
                        3,
                        second.event(),
                        second.canonicalData(),
                        second.previousHash(),
                        second.eventHash()));

        assertThat(chain.verifyStored(events).verified()).isFalse();
        assertThat(chain.verifyStored(events).failedSequence()).isEqualTo(2);
    }

    @Test
    void changedPreviousHashFailsVerification() {
        List<HashedEvent> events = new ArrayList<>(chain.build(events()));
        HashedEvent second = events.get(1);
        events.set(
                1,
                new HashedEvent(
                        second.sequence(),
                        second.event(),
                        second.canonicalData(),
                        "tampered",
                        second.eventHash()));

        assertThat(chain.verifyStored(events).verified()).isFalse();
    }

    @Test
    void emptyChainIsValidButHasNoRoot() {
        List<HashedEvent> events = chain.build(List.of());

        assertThat(events).isEmpty();
        assertThat(chain.verifyStored(events).verified()).isTrue();
    }

    @Test
    void sameInputProducesSameHash() {
        assertThat(chain.build(events()).get(1).eventHash())
                .isEqualTo(chain.build(events()).get(1).eventHash());
    }

    @Test
    void chineseDecimalAndMapOrderAreCanonicalizedStably() {
        Map<String, Object> first = new LinkedHashMap<>();
        first.put("名称", "黄连");
        first.put("株高", new BigDecimal("21.500"));
        Map<String, Object> second = new LinkedHashMap<>();
        second.put("株高", new BigDecimal("21.5"));
        second.put("名称", "黄连");
        CanonicalEvent left = event(1L, "growth_record_created", first, 9);
        CanonicalEvent right = event(1L, "growth_record_created", second, 9);

        assertThat(chain.canonicalData(left)).isEqualTo(chain.canonicalData(right));
        assertThat(chain.build(List.of(left)).get(0).eventHash())
                .isEqualTo(chain.build(List.of(right)).get(0).eventHash());
    }

    private List<CanonicalEvent> events() {
        return List.of(
                event(
                        2L,
                        "growth_record_created",
                        Map.of("plantHeight", new BigDecimal("21.5")),
                        10),
                event(1L, "task_created", Map.of("taskName", "黄连连续观测"), 9));
    }

    private CanonicalEvent event(
            Long targetId, String eventType, Map<String, Object> payload, int hour) {
        return new CanonicalEvent(
                "test",
                targetId,
                eventType,
                LocalDateTime.of(2026, 7, 15, hour, 0),
                null,
                null,
                payload);
    }
}
