package com.lz.task;

import java.time.Duration;
import java.util.Date;

/**
 * 防断流动作判定（纯逻辑，无 DB 依赖）
 *
 * @author lz
 */
public final class AutoAdvanceJudge {

    private AutoAdvanceJudge() {
    }

    /**
     * 判定防断流动作。
     *
     * @param nodeReached   该任务已到达的最高节点级别（0=无,1=已联系,2=已取件,3=已送达）
     * @param anchor        锚点时间（最新节点事件时间，或接单确认时间）
     * @param now           当前时间
     * @param nodeHours     节点超时小时数
     * @param completeHours 完成确认超时小时数
     *
     * @return 1..3 = 推进至该 NodeIndex；-1 = 置 COMPLETED；0 = 不处理
     */
    public static int judge(int nodeReached, Date anchor, Date now,
            long nodeHours, long completeHours) {
        if (anchor == null || now == null) {
            return 0;
        }
        long elapsedHours = Duration.between(anchor.toInstant(), now.toInstant()).toHours();
        if (nodeReached < 3) {
            return elapsedHours > nodeHours ? nodeReached + 1 : 0;
        }
        return elapsedHours > completeHours ? -1 : 0;
    }
}
