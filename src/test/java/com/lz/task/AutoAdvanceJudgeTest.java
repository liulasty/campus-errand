package com.lz.task;

import org.junit.jupiter.api.Test;

import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * AutoAdvanceJudge 纯判定单测
 *
 * @author lz
 */
class AutoAdvanceJudgeTest {

    private Date hoursAgo(long hours) {
        return new Date(System.currentTimeMillis() - hours * 3600 * 1000L);
    }

    @Test
    void noNode_expired_advanceToNode1() {
        assertThat(AutoAdvanceJudge.judge(0, hoursAgo(10), new Date(), 6, 24)).isEqualTo(1);
    }

    @Test
    void noNode_notExpired_noAction() {
        assertThat(AutoAdvanceJudge.judge(0, hoursAgo(2), new Date(), 6, 24)).isEqualTo(0);
    }

    @Test
    void nodeReached2_expired_advanceTo3() {
        assertThat(AutoAdvanceJudge.judge(2, hoursAgo(10), new Date(), 6, 24)).isEqualTo(3);
    }

    @Test
    void nodeReached3_completeExpired_complete() {
        assertThat(AutoAdvanceJudge.judge(3, hoursAgo(30), new Date(), 6, 24)).isEqualTo(-1);
    }

    @Test
    void nodeReached3_completeNotExpired_noAction() {
        assertThat(AutoAdvanceJudge.judge(3, hoursAgo(10), new Date(), 6, 24)).isEqualTo(0);
    }

    @Test
    void nullAnchor_noAction() {
        assertThat(AutoAdvanceJudge.judge(0, null, new Date(), 6, 24)).isEqualTo(0);
    }

    @Test
    void lessThanFullHour_notTrigger() {
        Date anchor = new Date(System.currentTimeMillis() - (5 * 3600 * 1000L + 59 * 60 * 1000L));
        assertThat(AutoAdvanceJudge.judge(0, anchor, new Date(), 6, 24)).isEqualTo(0);
    }
}
