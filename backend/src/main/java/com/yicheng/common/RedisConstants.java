package com.yicheng.common;

public class RedisConstants {
    //缓存前缀:

    //短信登录的KEY
    public static final String LOGIN_CODE_KEY = "login:code:";
    public static final String LOGIN_LIMIT_KEY = "login:limit:";

    //用户TOKEN的KEY
    public static final String LOGIN_TOKEN_KEY = "login:token:";

    //表的缓存KEY
    public static final String BED_CACHE_KEY = "cache:bed:";
    public static final String ELDERLY_CACHE_KEY = "cache:elderly:";
    public static final String SYSDICT_CACHE_KEY = "cache:sysdict:";
    public static final String EMPLOYEE_CACHE_KEY = "cache:employee:";

    //抢号库存key
    public static final String DOCTOR_STOCK_KEY = "cache:seckill:stock:";
    //用户抢号时间限制key
    public static final String SECKILL_LIMIT_KEY = "cache:seckill:limit:";
    //用户抢号唯一限制key
    public static final String SECKILL_SUCCESS_KEY = "cache:seckill:success:";


    //医生热度key
    public static final String DOCTOR_HOT_KEY="cache:doctor:hot:";


    //GEO
    public static final String HOSPITAL_GEO_KEY = "cache:hospital:geo:";

    //打卡BitMap
    public static final String ELDERLY_SIGN_KEY="cache:elderly:sign:";

    //分布式锁前缀:

    //锁定床位分配
    public static final String LOCK_BED_ASSIGN_KEY="loc:bed:assign:";




}
