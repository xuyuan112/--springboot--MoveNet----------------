package com.rehab.model.enums;

/**
 * 业务枚举统一契约：所有落库/对外传输码值的枚举实现本接口。
 *
 * <p>配合 MyBatis-Plus {@code @EnumValue} 完成数据库码值映射，
 * 配合 Jackson {@code @JsonValue}/{@code @JsonCreator} 完成前后端 JSON 码值互转。</p>
 *
 * @param <T> 码值类型（本平台统一为 String）
 */
public interface BaseEnum<T> {

    /**
     * 枚举码值（落库值 / 接口传输值）
     */
    T getCode();
}
