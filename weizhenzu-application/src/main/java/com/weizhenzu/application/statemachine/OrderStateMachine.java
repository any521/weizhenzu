package com.weizhenzu.application.statemachine;

import com.weizhenzu.common.exception.BizException;
import com.weizhenzu.common.result.ResultCode;
import com.weizhenzu.domain.enums.OrderStatus;
import org.springframework.stereotype.Component;

/**
 * 订单状态机
 *
 * @author weizhenzu
 * @since 1.0.0
 */
@Component
public class OrderStateMachine {

    /**
     * 校验状态转移是否合法
     */
    public void transit(OrderStatus from, OrderStatus to) {
        if (!from.canTransitTo(to)) {
            throw new BizException(ResultCode.ORDER_STATUS_ERROR,
                    String.format("订单状态不允许从[%s]变更为[%s]", from.getDesc(), to.getDesc()));
        }
    }

    /**
     * 判断是否可转移
     */
    public boolean canTransit(OrderStatus from, OrderStatus to) {
        return from.canTransitTo(to);
    }
}
